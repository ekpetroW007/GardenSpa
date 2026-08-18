package ru.samates.gardenspa.domain

import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale
import java.util.UUID
import kotlin.math.absoluteValue

enum class CultivationType(val displayName: String) {
    OPEN_GROUND("Открытый грунт"),
    GREENHOUSE("Теплица")
}

enum class CareAnchor {
    START_DATE,
    SAFE_SPRING_DATE,
    SAFE_AUTUMN_DATE
}

enum class NaturalZone(val displayName: String) {
    TUNDRA_AND_FOREST_TUNDRA("Тундра и лесотундра"),
    TAIGA("Тайга"),
    MIXED_AND_BROADLEAF_FORESTS("Смешанные и широколиственные леса"),
    FOREST_STEPPE("Лесостепь"),
    STEPPE_AND_SEMI_DESERT("Степь и полупустыня"),
    HUMID_SUBTROPICS("Влажные субтропики")
}

data class WeatherLimits(
    val minimumNightTemperatureC: Double? = null,
    val maximumPrecipitationMm: Double? = null,
    val maximumWindMetersPerSecond: Double? = null
)

data class CareRecurrence(
    val type: RepeatType,
    val interval: Int,
    val count: Int
)

data class CareStepTemplate(
    val id: String,
    val title: String,
    val anchor: CareAnchor = CareAnchor.START_DATE,
    val offsetDays: Int,
    val windowBeforeDays: Int = 0,
    val windowAfterDays: Int = 0,
    val weatherLimits: WeatherLimits = WeatherLimits(),
    val recurrence: CareRecurrence? = null,
    val productDescription: String? = null,
    val note: String
)

data class PlantCareTemplate(
    val id: String,
    val canonicalName: String,
    val aliases: Set<String>,
    val version: Int,
    val supportedCultivationTypes: Set<CultivationType>,
    val supportedNaturalZones: Set<NaturalZone> = NaturalZone.entries.toSet(),
    val openGroundStartOffsetDays: Int = 3,
    val greenhouseStartOffsetDays: Int = -14,
    val zoneStartOffsetDays: Map<NaturalZone, Int> = emptyMap(),
    val openGroundEndOffsetDays: Int = -14,
    val greenhouseEndOffsetDays: Int = 14,
    val zoneEndOffsetDays: Map<NaturalZone, Int> = emptyMap(),
    val steps: List<CareStepTemplate>
)

data class CareProgramContext(
    val startDate: LocalDate,
    val cultivationType: CultivationType,
    val climate: ClimateFingerprint,
    val forecast: List<ForecastWeatherDay> = emptyList(),
    val includeOnlyOnOrAfter: LocalDate? = null
)

data class GeneratedCareStep(
    val templateStepId: String,
    val title: String,
    val scheduledDate: LocalDate,
    val windowStart: LocalDate,
    val windowEnd: LocalDate,
    val recurrence: CareRecurrence?,
    val weatherAdjusted: Boolean,
    val needsWeatherConfirmation: Boolean,
    val explanation: String,
    val productDescription: String?,
    val note: String
)

data class GeneratedCareProgram(
    val instanceId: String,
    val templateId: String,
    val templateVersion: Int,
    val plantName: String,
    val cultivationType: CultivationType,
    val naturalZone: NaturalZone,
    val recommendedStartDate: LocalDate,
    val recommendedEndDate: LocalDate,
    val chosenStartDate: LocalDate,
    val remainingFromDate: LocalDate?,
    val climateSummary: String,
    val warning: String?,
    val steps: List<GeneratedCareStep>
)

enum class ProgramStartChoice {
    RECOMMENDED_DATE,
    NEXT_YEAR,
    USER_DATE
}

data class ProgramStartProposal(
    val recommendedDate: LocalDate,
    val selectedDate: LocalDate,
    val recommendationHasPassed: Boolean
) {
    fun resolve(choice: ProgramStartChoice): LocalDate = when (choice) {
        ProgramStartChoice.RECOMMENDED_DATE -> recommendedDate
        ProgramStartChoice.NEXT_YEAR -> recommendedDate.plusYears(1)
        ProgramStartChoice.USER_DATE -> selectedDate
    }
}

object ProgramStartPlanner {
    fun propose(
        template: PlantCareTemplate,
        cultivationType: CultivationType,
        climate: ClimateFingerprint,
        selectedDate: LocalDate,
        today: LocalDate = LocalDate.now()
    ): ProgramStartProposal {
        val recommendationYear = maxOf(today.year, selectedDate.year)
        val recommendedDate = recommendedStartDate(
            template = template,
            cultivationType = cultivationType,
            climate = climate,
            year = recommendationYear
        )
        return ProgramStartProposal(
            recommendedDate = recommendedDate,
            selectedDate = selectedDate,
            recommendationHasPassed = recommendedDate.isBefore(today)
        )
    }
}

fun recommendedStartDate(
    template: PlantCareTemplate,
    cultivationType: CultivationType,
    climate: ClimateFingerprint,
    year: Int
): LocalDate {
    val springBase = climate.safeSpringDate(year)
    val cultivationOffset = when (cultivationType) {
        CultivationType.OPEN_GROUND -> springBase.plusDays(template.openGroundStartOffsetDays.toLong())
        CultivationType.GREENHOUSE -> springBase.plusDays(template.greenhouseStartOffsetDays.toLong())
    }
    return cultivationOffset.plusDays(template.zoneStartOffsetDays[climate.naturalZone()]?.toLong() ?: 0L)
}

fun recommendedEndDate(
    template: PlantCareTemplate,
    cultivationType: CultivationType,
    climate: ClimateFingerprint,
    year: Int
): LocalDate {
    val autumnBase = climate.safeAutumnDate(year)
    val cultivationOffset = when (cultivationType) {
        CultivationType.OPEN_GROUND -> template.openGroundEndOffsetDays
        CultivationType.GREENHOUSE -> template.greenhouseEndOffsetDays
    }
    return autumnBase.plusDays((cultivationOffset + (template.zoneEndOffsetDays[climate.naturalZone()] ?: 0)).toLong())
}

object PlantCareCatalog {
    private val baseTemplates = popularAnnualCareTemplates() + listOf(
        PlantCareTemplate(
            id = "garden-strawberry",
            canonicalName = "Земляника садовая",
            aliases = setOf("земляника", "земляника садовая", "клубника"),
            version = 2,
            supportedCultivationTypes = setOf(CultivationType.OPEN_GROUND),
            steps = listOf(
                CareStepTemplate("moisture_check", "Проверить влажность почвы", offsetDays = 1, recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 12), note = "Оценивайте почву под мульчей, если она используется."),
                CareStepTemplate("mulch_check", "Проверить мульчу", offsetDays = 5, recurrence = CareRecurrence(RepeatType.MONTHLY, 1, 4), note = "Мульча не должна закрывать центр розетки."),
                CareStepTemplate("leaf_inspection", "Осмотреть листья и ягоды", offsetDays = 7, recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 10), note = "Удаляйте только явно повреждённые части чистым инструментом.")
            ) + standardTreatmentSteps("садовой земляники")
        ),
        seasonalVegetable(
            id = "potato",
            name = "Картофель",
            aliases = setOf("картофель", "картошка"),
            openGroundStartOffsetDays = -7,
            moistureIntervalDays = 7,
            cropSpecificTask = "Проверить окучивание",
            cropSpecificNote = "Подсыпайте почву только при необходимости, не засыпая листья."
        ),
        seasonalVegetable(
            id = "sweet-pepper",
            name = "Перец сладкий",
            aliases = setOf("перец", "перец сладкий", "болгарский перец"),
            openGroundStartOffsetDays = 14,
            greenhouseStartOffsetDays = -10,
            moistureIntervalDays = 4,
            cropSpecificTask = "Проверить опоры и завязи",
            cropSpecificNote = "Подвязывайте побеги свободно и не удаляйте здоровые завязи без причины."
        ),
        seasonalVegetable(
            id = "eggplant",
            name = "Баклажан",
            aliases = setOf("баклажан", "баклажаны"),
            openGroundStartOffsetDays = 16,
            greenhouseStartOffsetDays = -8,
            moistureIntervalDays = 4,
            cropSpecificTask = "Проверить опору побегов",
            cropSpecificNote = "Не допускайте пережимания стеблей подвязкой."
        ),
        seasonalVegetable(
            id = "zucchini",
            name = "Кабачок",
            aliases = setOf("кабачок", "кабачки", "цуккини"),
            openGroundStartOffsetDays = 12,
            moistureIntervalDays = 5,
            cropSpecificTask = "Осмотреть центр куста",
            cropSpecificNote = "Удаляйте только явно повреждённые части чистым инструментом."
        ),
        seasonalVegetable(
            id = "pumpkin",
            name = "Тыква",
            aliases = setOf("тыква", "тыквы"),
            openGroundStartOffsetDays = 14,
            moistureIntervalDays = 6,
            cropSpecificTask = "Проверить плети и свободное место",
            cropSpecificNote = "Направляйте плети без резких перегибов и повреждения узлов."
        ),
        seasonalVegetable(
            id = "cabbage",
            name = "Капуста белокочанная",
            aliases = setOf("капуста", "капуста белокочанная", "белокочанная капуста"),
            openGroundStartOffsetDays = -10,
            moistureIntervalDays = 5,
            cropSpecificTask = "Осмотреть кочан и нижнюю сторону листьев",
            cropSpecificNote = "Отмечайте кладки и повреждения, не применяя средство до определения причины."
        ),
        seasonalVegetable(
            id = "carrot",
            name = "Морковь",
            aliases = setOf("морковь", "морковка"),
            openGroundStartOffsetDays = -14,
            moistureIntervalDays = 6,
            cropSpecificTask = "Проверить густоту всходов",
            cropSpecificNote = "Прореживайте только после появления устойчивых всходов и увлажнения почвы."
        ),
        seasonalVegetable(
            id = "beet",
            name = "Свёкла",
            aliases = setOf("свекла", "свёкла", "свекла столовая", "свёкла столовая"),
            openGroundStartOffsetDays = -7,
            moistureIntervalDays = 7,
            cropSpecificTask = "Проверить густоту всходов",
            cropSpecificNote = "Оставляйте более сильные растения, не повреждая корни соседних."
        ),
        seasonalVegetable(
            id = "onion",
            name = "Лук репчатый",
            aliases = setOf("лук", "лук репчатый", "репчатый лук"),
            openGroundStartOffsetDays = -18,
            moistureIntervalDays = 7,
            cropSpecificTask = "Осмотреть перо и шейку",
            cropSpecificNote = "Отмечайте пожелтение и размягчение; не увлажняйте посадки автоматически."
        ),
        seasonalVegetable(
            id = "garlic",
            name = "Чеснок",
            aliases = setOf("чеснок", "чеснока"),
            openGroundStartOffsetDays = -21,
            moistureIntervalDays = 8,
            cropSpecificTask = "Осмотреть листья и основание",
            cropSpecificNote = "Проверяйте посадки на пожелтение, повреждения и переувлажнение."
        )
    )
    private val templates = expandProgramVarieties(baseTemplates)

    private fun standardTreatmentSteps(cropLabel: String): List<CareStepTemplate> = listOf(
        CareStepTemplate(
            id = "preventive_disease_treatment",
            title = "Профилактическая обработка от болезней",
            offsetDays = 18,
            windowBeforeDays = 2,
            windowAfterDays = 3,
            weatherLimits = WeatherLimits(maximumPrecipitationMm = 3.0, maximumWindMetersPerSecond = 6.0),
            recurrence = CareRecurrence(RepeatType.CUSTOM, 14, 5),
            productDescription = "Средство профилактического действия против грибных и бактериальных болезней, разрешённое для $cropLabel.",
            note = "Конкретный препарат и дозировку выбирайте по актуальной инструкции с учётом культуры и стадии развития."
        ),
        CareStepTemplate(
            id = "pest_treatment_if_needed",
            title = "Обработка от вредителей при необходимости",
            offsetDays = 21,
            windowBeforeDays = 1,
            windowAfterDays = 3,
            weatherLimits = WeatherLimits(maximumPrecipitationMm = 3.0, maximumWindMetersPerSecond = 6.0),
            recurrence = CareRecurrence(RepeatType.CUSTOM, 10, 8),
            productDescription = "Средство против сосущих и листогрызущих вредителей, разрешённое для $cropLabel.",
            note = "Проводите обработку только после подтверждения вредителя; соблюдайте срок ожидания и инструкцию выбранного средства."
        )
    )

    private fun seasonalVegetable(
        id: String,
        name: String,
        aliases: Set<String>,
        openGroundStartOffsetDays: Int,
        greenhouseStartOffsetDays: Int = -14,
        moistureIntervalDays: Int,
        cropSpecificTask: String,
        cropSpecificNote: String
    ): PlantCareTemplate = PlantCareTemplate(
        id = id,
        canonicalName = name,
        aliases = aliases,
        version = 2,
        supportedCultivationTypes = if (greenhouseStartOffsetDays < 0 && id in setOf("sweet-pepper", "eggplant")) {
            CultivationType.entries.toSet()
        } else {
            setOf(CultivationType.OPEN_GROUND)
        },
        openGroundStartOffsetDays = openGroundStartOffsetDays,
        greenhouseStartOffsetDays = greenhouseStartOffsetDays,
        steps = listOf(
            CareStepTemplate(
                id = "adaptation_check",
                title = "Проверить состояние растения",
                offsetDays = 4,
                note = "Оцените рост, окраску и упругость листьев; зафиксируйте необычные изменения."
            ),
            CareStepTemplate(
                id = "moisture_check",
                title = "Проверить влажность почвы",
                offsetDays = 2,
                recurrence = CareRecurrence(RepeatType.CUSTOM, moistureIntervalDays, 16),
                note = "Решение о поливе принимайте по фактической влажности почвы и недавним осадкам."
            ),
            CareStepTemplate(
                id = "crop_specific_check",
                title = cropSpecificTask,
                offsetDays = 9,
                recurrence = CareRecurrence(RepeatType.CUSTOM, 10, 8),
                note = cropSpecificNote
            ),
            CareStepTemplate(
                id = "leaf_inspection",
                title = "Осмотреть на признаки стресса и вредителей",
                offsetDays = 7,
                recurrence = CareRecurrence(RepeatType.WEEKLY, 1, 12),
                note = "Сначала определите возможную причину; препарат выбирайте отдельно и применяйте только по инструкции."
            )
        ) + standardTreatmentSteps(name.lowercase(Locale.forLanguageTag("ru")))
    )

    fun find(userInput: String): PlantCareTemplate? {
        val normalized = normalizePlantName(userInput)
        return templates.filter { template ->
            normalized == normalizePlantName(template.canonicalName) ||
                template.aliases.any { normalizePlantName(it) == normalized }
        }.singleOrNull()
    }

    fun findById(id: String): PlantCareTemplate? = templates.firstOrNull { it.id == id } ?: baseTemplates.firstOrNull { it.id == id }

    fun suggestions(userInput: String, limit: Int = 6): List<PlantCareTemplate> {
        val normalized = normalizePlantName(userInput)
        if (normalized.isBlank()) return emptyList()
        return templates.asSequence()
            .filter { template ->
                normalizePlantName(template.canonicalName).startsWith(normalized) ||
                    template.aliases.any { normalizePlantName(it).startsWith(normalized) }
            }
            .sortedWith(compareByDescending<PlantCareTemplate> { normalizePlantName(it.canonicalName).startsWith(normalized) }.thenBy { it.canonicalName })
            .take(limit)
            .toList()
    }

    fun all(): List<PlantCareTemplate> = templates
}

class CareProgramGenerator {
    fun generate(
        template: PlantCareTemplate,
        context: CareProgramContext,
        instanceId: String = UUID.randomUUID().toString()
    ): GeneratedCareProgram {
        require(context.cultivationType in template.supportedCultivationTypes) {
            "Для выбранного способа выращивания программа пока не подготовлена"
        }
        val naturalZone = context.climate.naturalZone()
        require(naturalZone in template.supportedNaturalZones) {
            "Для природной зоны «${naturalZone.displayName}» программа этого растения пока не подготовлена"
        }

        val recommendedStart = recommendedStartDate(
            template = template,
            cultivationType = context.cultivationType,
            climate = context.climate,
            year = context.startDate.year
        )
        val warning = if (context.startDate.isBefore(recommendedStart)) {
            "Выбранная дата раньше рекомендуемого начала работ ${recommendedStart}. Проверьте местные условия."
        } else {
            null
        }
        val calculatedEnd = recommendedEndDate(template, context.cultivationType, context.climate, context.startDate.year)
        val recommendedEnd = if (calculatedEnd.isBefore(context.startDate)) calculatedEnd.plusYears(1) else calculatedEnd
        val forecastByDate = context.forecast.associateBy { it.date }

        val generatedSteps = template.steps.filter { step -> "pruning" in step.id || step.productDescription != null }.map { step ->
            val anchorDate = when (step.anchor) {
                CareAnchor.START_DATE -> context.startDate
                CareAnchor.SAFE_SPRING_DATE -> recommendedStart
                CareAnchor.SAFE_AUTUMN_DATE -> recommendedEnd
            }
            val initialDate = anchorDate.plusDays(step.offsetDays.toLong())
            val windowStart = initialDate.minusDays(step.windowBeforeDays.toLong())
            val windowEnd = initialDate.plusDays(step.windowAfterDays.toLong())
            val forecastCoversWindow = forecastByDate.keys.any { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
            val suitableDate = candidateOffsets(step.windowBeforeDays, step.windowAfterDays)
                .map { initialDate.plusDays(it.toLong()) }
                .filter { !it.isBefore(windowStart) && !it.isAfter(windowEnd) }
                .firstOrNull { date ->
                    val weather = forecastByDate[date] ?: return@firstOrNull false
                    weather.satisfies(step.weatherLimits)
                }
            val hasLimits = step.weatherLimits != WeatherLimits()
            val scheduledDate = when {
                !hasLimits -> initialDate
                suitableDate != null -> suitableDate
                else -> initialDate
            }
            val needsWeatherConfirmation = hasLimits && suitableDate == null
            val explanation = when {
                hasLimits && suitableDate != null && suitableDate != initialDate ->
                    "Дата сдвинута с $initialDate на $suitableDate по прогнозу погоды."
                hasLimits && suitableDate != null ->
                    "Прогноз на $scheduledDate соответствует условиям выполнения."
                hasLimits && forecastCoversWindow ->
                    "В доступном прогнозе нет полностью подходящего дня; проверьте погоду перед выполнением."
                hasLimits ->
                    "Дата рассчитана по программе; прогноз будет полезно проверить ближе к сроку."
                else ->
                    "Дата рассчитана относительно ${step.anchor.displayAnchorName()}."
            }

            GeneratedCareStep(
                templateStepId = step.id,
                title = step.title,
                scheduledDate = scheduledDate,
                windowStart = windowStart,
                windowEnd = windowEnd,
                recurrence = step.recurrence,
                weatherAdjusted = suitableDate != null && suitableDate != initialDate,
                needsWeatherConfirmation = needsWeatherConfirmation,
                explanation = explanation,
                productDescription = step.productDescription,
                note = step.note
            )
        }.mapNotNull { it.keepFrom(context.includeOnlyOnOrAfter) }.sortedBy { it.scheduledDate }

        return GeneratedCareProgram(
            instanceId = instanceId,
            templateId = template.id,
            templateVersion = template.version,
            plantName = template.canonicalName,
            cultivationType = context.cultivationType,
            naturalZone = naturalZone,
            recommendedStartDate = recommendedStart,
            recommendedEndDate = recommendedEnd,
            chosenStartDate = context.includeOnlyOnOrAfter ?: context.startDate,
            remainingFromDate = context.includeOnlyOnOrAfter,
            climateSummary = context.climate.displayName(),
            warning = warning,
            steps = generatedSteps
        )
    }

    private fun candidateOffsets(beforeDays: Int, afterDays: Int): List<Int> =
        (-beforeDays..afterDays).sortedWith(compareBy<Int> { it.absoluteValue }.thenBy { it })

    private fun GeneratedCareStep.keepFrom(cutoff: LocalDate?): GeneratedCareStep? {
        if (cutoff == null || !scheduledDate.isBefore(cutoff)) return this
        val schedule = recurrence ?: return null
        var nextDate = scheduledDate
        var skippedOccurrences = 0
        while (nextDate.isBefore(cutoff) && skippedOccurrences < schedule.count) {
            nextDate = nextDate.nextOccurrence(schedule)
            skippedOccurrences++
        }
        if (skippedOccurrences >= schedule.count) return null
        val dateShift = java.time.temporal.ChronoUnit.DAYS.between(scheduledDate, nextDate)
        return copy(
            scheduledDate = nextDate,
            windowStart = windowStart.plusDays(dateShift),
            windowEnd = windowEnd.plusDays(dateShift),
            recurrence = schedule.copy(count = schedule.count - skippedOccurrences),
            weatherAdjusted = false,
            needsWeatherConfirmation = needsWeatherConfirmation || productDescription != null,
            explanation = "Пропущено прошедших выполнений: $skippedOccurrences. В календарь добавлены только оставшиеся процедуры с $cutoff."
        )
    }

    private fun LocalDate.nextOccurrence(recurrence: CareRecurrence): LocalDate = when (recurrence.type) {
        RepeatType.NONE -> this
        RepeatType.DAILY, RepeatType.CUSTOM -> plusDays(recurrence.interval.toLong())
        RepeatType.WEEKLY -> plusWeeks(recurrence.interval.toLong())
        RepeatType.MONTHLY -> plusMonths(recurrence.interval.toLong())
        RepeatType.YEARLY -> plusYears(recurrence.interval.toLong())
    }

    private fun ForecastWeatherDay.satisfies(limits: WeatherLimits): Boolean =
        (limits.minimumNightTemperatureC == null || minimumTemperatureC >= limits.minimumNightTemperatureC) &&
            (limits.maximumPrecipitationMm == null || precipitationMm <= limits.maximumPrecipitationMm) &&
            (limits.maximumWindMetersPerSecond == null || maximumWindMetersPerSecond <= limits.maximumWindMetersPerSecond)
}

private fun CareAnchor.displayAnchorName(): String = when (this) {
    CareAnchor.START_DATE -> "начала ухода"
    CareAnchor.SAFE_SPRING_DATE -> "безопасного начала сезона"
    CareAnchor.SAFE_AUTUMN_DATE -> "окончания сезона и подготовки к зиме"
}

fun normalizePlantName(value: String): String = Normalizer
    .normalize(value.trim().lowercase(Locale.forLanguageTag("ru")), Normalizer.Form.NFD)
    .replace("ё", "е")
    .replace(Regex("\\p{M}+"), "")
    .replace(Regex("[^а-яa-z0-9\\s-]"), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
