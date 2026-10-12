package ru.samates.gardenspa.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

/** Climate dates are conservative planning limits, not measurements of soil thaw. */
object RootCareSchedule {
    const val VERSION = 23
    const val SOIL_NOTICE = "Только по оттаявшей почве; если земля замёрзла, перенесите работу. " +
        "Корневые подкормки проводите при возобновлении роста по умеренно влажной почве. " +
        "Не вносите удобрения в мёрзлую землю. Дата рассчитана по климату, а не по измерению температуры почвы; " +
        "перед работой проверьте почву на участке. Для теплицы ранний календарный срок сам по себе не подтверждает прогрев грунта."

    private val plantingIds = setOf("fitosporin_roots", "gumi_omi_planting", "kornesil_planting")
    private val rootIds = plantingIds + setOf("nutrition", "mineral_nutrition", "gumi_omi_feeding", "gumi_omi_7_8_leaves",
        "hydrangea_pale_acid", "hydrangea_yellow_magnesium", "conifer_water", "conifer_winter",
        "acid_plants_autumn_nutrition", "lawn_autumn_nutrition")
    private val springTreatmentIds = setOf(SpringCarePrograms.EARLY_STEP, "fitosporin_spraying", "preventive_disease_treatment")

    fun isRootStep(stepId: String?): Boolean = programBaseStep(stepId) in rootIds
    fun isPlantingStep(stepId: String?): Boolean = programBaseStep(stepId) in plantingIds
    fun isSpringTreatment(stepId: String?): Boolean = programBaseStep(stepId) in springTreatmentIds

    fun apply(template: PlantCareTemplate): PlantCareTemplate = template.copy(version = maxOf(template.version, VERSION),
        steps = template.steps.map { step -> if (!isRootStep(step.id)) step else step.copy(note = "${step.note}\n$SOIL_NOTICE",
            offsetDays = if (programBaseStep(step.id) == "conifer_winter") -7 else step.offsetDays,
            weatherLimits = step.weatherLimits.copy(minimumNightTemperatureC = maxOf(0.0, step.weatherLimits.minimumNightTemperatureC ?: 0.0))) })

    fun plantingStart(template: PlantCareTemplate, context: CareProgramContext, date: LocalDate): LocalDate =
        if (template.version >= VERSION && template.steps.any { isPlantingStep(it.id) })
            nextWarmDate(date, context.climate) else date

    fun nextWarmDate(date: LocalDate, climate: ClimateFingerprint,
        nextSeasonStart: (Int) -> LocalDate = climate::safeSpringDate): LocalDate = when {
        date.isBefore(climate.safeSpringDate(date.year)) -> climate.safeSpringDate(date.year)
        !date.isBefore(climate.safeAutumnDate(date.year)) -> nextSeasonStart(date.year + 1)
        else -> date
    }

    /** A carried-over feeding waits for the crop's next planting/spring-treatment phase too. */
    internal fun nextSeasonRootStart(template: PlantCareTemplate, cultivation: CultivationType,
        climate: ClimateFingerprint, year: Int): LocalDate {
        val recommended = recommendedStartDate(template, cultivation, climate, year)
        val start = if (template.steps.any { isPlantingStep(it.id) }) maxOf(recommended, climate.safeSpringDate(year)) else recommended
        val treatment = template.steps.filter { isSpringTreatment(it.id) }.minOfOrNull { step ->
            val anchor = when (step.anchor) {
                CareAnchor.START_DATE -> start
                CareAnchor.DORMANT_SPRING_DATE -> climate.safeSpringDate(year).minusDays(21)
                else -> recommended
            }
            anchor.plusDays(step.offsetDays.toLong())
        }
        return earliestDate(template.id, "nutrition", climate, year, treatment)
    }

    /** Keep every automatic occurrence, pausing a finite series over the cold season. */
    internal fun finiteDates(start: LocalDate, repeat: CareRecurrence, climate: ClimateFingerprint,
        nextSeasonStart: (Int) -> LocalDate = climate::safeSpringDate): List<LocalDate> = spacedDates(
        (0 until requireNotNull(repeat.count)).map { careOccurrenceDate(start, repeat, it) }, repeat, climate, nextSeasonStart)

    /** Missed work and a cold tail can converge on the same next season. Never merge their doses. */
    internal fun spacedDates(dates: List<LocalDate>, repeat: CareRecurrence, climate: ClimateFingerprint,
        nextSeasonStart: (Int) -> LocalDate = climate::safeSpringDate): List<LocalDate> {
        var previous: LocalDate? = null
        return dates.map { original ->
            val minimum = previous?.let { careOccurrenceDate(it, repeat, 1) } ?: original
            nextWarmDate(maxOf(original, minimum), climate, nextSeasonStart).also { previous = it }
        }
    }

    fun earliestDate(cropId: String, stepId: String?, climate: ClimateFingerprint, year: Int, springTreatment: LocalDate?): LocalDate {
        val climateStart = climate.safeSpringDate(year)
        if (isPlantingStep(stepId) || springTreatment == null) return climateStart
        val afterTreatment = springTreatment.plusDays(if (cropId in EradicationCare.fruitTreeIds) 1 else 0)
        return maxOf(climateStart, afterTreatment)
    }

    fun schedule(template: PlantCareTemplate, context: CareProgramContext, steps: List<GeneratedCareStep>): List<GeneratedCareStep> {
        if (template.version < VERSION) return steps
        val springTreatment = steps.filter { isSpringTreatment(it.templateStepId) }.minOfOrNull { it.scheduledDate }
        fun nextSeasonStart(year: Int): LocalDate = maxOf(
            nextSeasonRootStart(template, context.cultivationType, context.climate, year),
            earliestDate(template.id, "nutrition", context.climate, year, springTreatment?.withYear(year)))
        val adjusted = steps.map { step ->
            if (!isRootStep(step.templateStepId)) return@map step
            val earliest = earliestDate(template.id, step.templateStepId, context.climate, step.scheduledDate.year, springTreatment)
            var date = nextWarmDate(maxOf(step.scheduledDate, earliest), context.climate, ::nextSeasonStart)
            // Leave room for both dependent checks before the autumn soil boundary.
            if (programBaseStep(step.templateStepId) == "hydrangea_pale_acid" &&
                !date.plusDays(10).isBefore(context.climate.safeAutumnDate(date.year)))
                date = nextSeasonStart(date.year + 1)
            val shift = ChronoUnit.DAYS.between(step.scheduledDate, date)
            val windowStart = maxOf(earliest, context.climate.safeSpringDate(date.year), step.windowStart.plusDays(shift))
            val chainDays = if (programBaseStep(step.templateStepId) == "hydrangea_pale_acid") 10L else 0L
            val windowEnd = minOf(step.windowEnd.plusDays(shift), context.climate.safeAutumnDate(date.year).minusDays(1 + chainDays))
            val limits = template.steps.first { it.id == step.templateStepId }.weatherLimits
            val suitable = context.forecast.filter { !it.date.isBefore(windowStart) && !it.date.isAfter(windowEnd) &&
                it.minimumTemperatureC >= 0.0 && it.satisfies(limits) }
                .minWithOrNull(compareBy<ForecastWeatherDay> { ChronoUnit.DAYS.between(date, it.date).absoluteValue }.thenBy { it.date })
            val scheduled = suitable?.date ?: date
            step.copy(scheduledDate = scheduled, windowStart = windowStart, windowEnd = windowEnd,
                needsWeatherConfirmation = true,
                weatherAdjusted = suitable != null && scheduled != date || step.weatherAdjusted && shift == 0L,
                explanation = if (shift == 0L && scheduled == step.scheduledDate) step.explanation else
                    "Дата корневой процедуры перенесена с ${step.scheduledDate} на $scheduled с учётом климата и весенней обработки. Проверьте фактическое оттаивание почвы.")
        }
        // The three hydrangea checks remain one chain even when the first root watering has to wait.
        val acid = adjusted.firstOrNull { programBaseStep(it.templateStepId) == "hydrangea_pale_acid" }
        return adjusted.map { step ->
            val days = when (programBaseStep(step.templateStepId)) {
                "hydrangea_pale_iron" -> 5L
                "hydrangea_yellow_magnesium" -> 10L
                else -> null
            }
            if (acid == null || days == null) step else step.atDate(acid.scheduledDate.plusDays(days), context.climate)
        }.flatMap { splitSeasonalRepeats(it, context.climate, ::nextSeasonStart) }
    }

    /** A late start can move the first spray to another year and leave a later repeat in this one. */
    fun alignWithScheduledTreatments(template: PlantCareTemplate, context: CareProgramContext,
        steps: List<GeneratedCareStep>): List<GeneratedCareStep> {
        if (template.version < VERSION) return steps
        val treatments = steps.filter { isSpringTreatment(it.templateStepId) }
            .groupBy { it.scheduledDate.year }.mapValues { (_, rows) -> rows.minOf { it.scheduledDate } }
        fun seasonStart(year: Int): LocalDate = maxOf(nextSeasonRootStart(template, context.cultivationType, context.climate, year),
            earliestDate(template.id, "nutrition", context.climate, year, treatments[year]))
        val changed = mutableMapOf<String, GeneratedCareStep>()
        steps.filter { isRootStep(it.templateStepId) && !isPlantingStep(it.templateStepId) }
            .groupBy { programBaseStep(it.templateStepId) to it.scheduledDate.year }.values.forEach { group ->
                val ordered = group.sortedBy { it.scheduledDate }
                val first = ordered.first()
                val treatment = treatments[first.scheduledDate.year] ?: return@forEach
                val earliest = earliestDate(template.id, first.templateStepId, context.climate, first.scheduledDate.year, treatment)
                var shift = ChronoUnit.DAYS.between(first.scheduledDate, earliest).coerceAtLeast(0)
                if (shift == 0L) return@forEach
                ordered.forEach { step ->
                    val nominal = step.scheduledDate.plusDays(shift)
                    var date = nextWarmDate(nominal, context.climate, ::seasonStart)
                    if (programBaseStep(step.templateStepId) == "hydrangea_pale_acid" &&
                        !date.plusDays(10).isBefore(context.climate.safeAutumnDate(date.year))) date = seasonStart(date.year + 1)
                    shift += ChronoUnit.DAYS.between(nominal, date)
                    changed[step.templateStepId] = step.atDate(date, context.climate,
                        if (date.year == earliest.year) earliest else seasonStart(date.year))
                }
            }
        changed.values.firstOrNull { programBaseStep(it.templateStepId) == "hydrangea_pale_acid" }?.let { acid ->
            steps.forEach { step ->
                val days = when (programBaseStep(step.templateStepId)) {
                    "hydrangea_pale_iron" -> 5L
                    "hydrangea_yellow_magnesium" -> 10L
                    else -> return@forEach
                }
                changed[step.templateStepId] = step.atDate(acid.scheduledDate.plusDays(days), context.climate)
            }
        }
        val adjusted = steps.flatMap { original ->
            val step = changed[original.templateStepId] ?: original
            val bounded = if (isRootStep(step.templateStepId)) step.atDate(step.scheduledDate, context.climate) else step
            splitSeasonalRepeats(bounded, context.climate, ::seasonStart)
        }
        val spaced = mutableMapOf<String, GeneratedCareStep>()
        adjusted.filter { isRootStep(it.templateStepId) && it.templateStepId.contains(":remaining:") }
            .groupBy { programBaseStep(it.templateStepId) }.forEach { (base, group) ->
                val repeat = template.steps.firstOrNull { programBaseStep(it.id) == base }?.recurrence
                    ?.takeIf { it.count != null } ?: return@forEach
                val ordered = group.sortedBy { it.scheduledDate }
                val dates = spacedDates(ordered.map { it.scheduledDate }, repeat, context.climate, ::seasonStart)
                ordered.zip(dates).forEach { (step, date) -> spaced[step.templateStepId] = step.atDate(date, context.climate) }
            }
        return adjusted.map { spaced[it.templateStepId] ?: it }
    }

    private fun splitSeasonalRepeats(step: GeneratedCareStep, climate: ClimateFingerprint,
        nextSeasonStart: (Int) -> LocalDate): List<GeneratedCareStep> {
        val repeat = step.recurrence
        if (!isRootStep(step.templateStepId) || repeat?.count == null || repeat.type == RepeatType.NONE) return listOf(step)
        val dates = finiteDates(step.scheduledDate, repeat, climate, nextSeasonStart)
        if (dates.indices.all { dates[it] == careOccurrenceDate(step.scheduledDate, repeat, it) }) return listOf(step)
        return dates.mapIndexed { index, date -> step.atDate(date, climate,
            if (date.year > step.scheduledDate.year) nextSeasonStart(date.year) else climate.safeSpringDate(date.year)).copy(
            templateStepId = "${step.templateStepId}:remaining:${index + 1}", recurrence = null) }
    }

    private fun GeneratedCareStep.atDate(date: LocalDate, climate: ClimateFingerprint,
        earliest: LocalDate = climate.safeSpringDate(date.year)): GeneratedCareStep {
        val shift = ChronoUnit.DAYS.between(scheduledDate, date)
        return copy(scheduledDate = date,
            windowStart = maxOf(earliest, windowStart.plusDays(shift)),
            windowEnd = minOf(climate.safeAutumnDate(date.year).minusDays(1), windowEnd.plusDays(shift)),
            needsWeatherConfirmation = true, weatherAdjusted = weatherAdjusted && shift == 0L,
            explanation = if (shift == 0L) explanation else "Работа перенесена на $date с сохранением порядка процедур в тёплом сезоне. Проверьте фактическое оттаивание почвы.")
    }
}
