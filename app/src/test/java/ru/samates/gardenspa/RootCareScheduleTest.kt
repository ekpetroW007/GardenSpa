package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.domain.*

class RootCareScheduleTest {
    private val climate = ClimateFingerprint(MonthDay.of(4, 25), MonthDay.of(10, 10), 168,
        2600.0, 1750.0, 420.0, -18.0, ClimateConfidence.HIGH, 20)

    @Test fun allCatalogRootWorkRespectsClimateAcrossCropsCultivationAndStartSeasons() {
        val climates = listOf(climate.copy(safeSpringDay = MonthDay.of(3, 10)), climate,
            climate.copy(safeSpringDay = MonthDay.of(6, 1), safeAutumnDay = MonthDay.of(9, 10)),
            climate.copy(safeSpringDay = MonthDay.of(6, 1), safeAutumnDay = MonthDay.of(8, 1)))
        val violations = mutableListOf<String>()
        fun expect(condition: Boolean, detail: String) { if (!condition) violations += detail }
        for (template in PlantCareCatalog.all()) for (cultivation in template.supportedCultivationTypes)
            for (local in climates) for (start in listOf(LocalDate.of(2027, 1, 10), local.safeSpringDate(2027), LocalDate.of(2027, 7, 15))) {
                val description = "${template.id} $cultivation ${local.safeSpringDay} $start"
                val program = CareProgramGenerator().generate(template, CareProgramContext(start, cultivation, local))
                expect(template.version >= RootCareSchedule.VERSION, "$description old catalog version")
                val roots = program.steps.filter { RootCareSchedule.isRootStep(it.templateStepId) }
                expect(roots.isNotEmpty(), "$description missing root work")
                roots.forEach { step ->
                    val detail = "$description step=${step.templateStepId} date=${step.scheduledDate} window=${step.windowStart}..${step.windowEnd}"
                    expect(!step.scheduledDate.isBefore(local.safeSpringDate(step.scheduledDate.year)), "$detail before warm season")
                    expect(!step.windowStart.isBefore(local.safeSpringDate(step.scheduledDate.year)), "$detail early weather window")
                    expect(step.scheduledDate.isBefore(local.safeAutumnDate(step.scheduledDate.year)), "$detail after warm season")
                    expect(step.windowEnd.isBefore(local.safeAutumnDate(step.scheduledDate.year)), "$detail late weather window")
                    step.recurrence?.let { repeat ->
                        for (index in 0 until requireNotNull(repeat.count)) {
                            val occurrence = careOccurrenceDate(step.scheduledDate, repeat, index)
                            expect(occurrence == RootCareSchedule.nextWarmDate(occurrence, local), "$detail occurrence=$index at $occurrence outside warm season")
                        }
                    }
                    expect(step.note.contains("если земля замёрзла, перенесите работу"), "$detail missing soil condition")
                    expect(step.note.contains("не по измерению температуры почвы"), "$detail missing climate qualification")
                    expect(step.needsWeatherConfirmation, "$detail soil confirmation missing")
                    if (!RootCareSchedule.isPlantingStep(step.templateStepId)) {
                        val treatment = program.steps.filter { RootCareSchedule.isSpringTreatment(it.templateStepId) &&
                            it.scheduledDate.year == step.scheduledDate.year }.minOfOrNull { it.scheduledDate }
                        if (treatment != null) {
                            expect(!step.scheduledDate.isBefore(treatment), "$detail before spring=$treatment")
                            if (template.id in EradicationCare.fruitTreeIds) expect(step.scheduledDate.isAfter(treatment), "$detail fruit feeding not after spring=$treatment")
                        }
                    }
                }
                template.steps.filter { RootCareSchedule.isRootStep(it.id) }.forEach { source ->
                    val instances = roots.filter { programBaseStep(it.templateStepId) == programBaseStep(source.id) }
                    val dates = instances.flatMap { step -> step.recurrence?.let { repeat ->
                        (0 until requireNotNull(repeat.count)).map { careOccurrenceDate(step.scheduledDate, repeat, it) }
                    } ?: listOf(step.scheduledDate) }
                    expect(dates.size == (source.recurrence?.count ?: 1), "$description ${source.id} lost occurrences: ${dates.size}")
                    source.recurrence?.let { repeat -> dates.groupBy { it.year }.values.forEach { year ->
                        year.sorted().zipWithNext().forEach { (previous, next) ->
                            expect(!next.isBefore(careOccurrenceDate(previous, repeat, 1)), "$description ${source.id} too close: $previous -> $next")
                        }
                    } }
                }
            }
        assertTrue(violations.joinToString("\n"), violations.isEmpty())
    }

    @Test fun lateStartFeedingWaitsForTheFirstSprayStillPresentInTheCurrentSeason() {
        val cold = climate.copy(safeSpringDay = MonthDay.of(6, 1), safeAutumnDay = MonthDay.of(9, 10))
        val program = CareProgramGenerator().generate(PlantCareCatalog.find("Перец")!!,
            CareProgramContext(LocalDate.of(2027, 7, 15), CultivationType.OPEN_GROUND, cold))
        val feeding = program.steps.single { programBaseStep(it.templateStepId) == "mineral_nutrition" }
        assertEquals(LocalDate.of(2027, 7, 17), feeding.scheduledDate)
        assertTrue(program.steps.any { RootCareSchedule.isSpringTreatment(it.templateStepId) && it.scheduledDate == feeding.scheduledDate })
        assertEquals(LocalDate.of(2028, 7, 3), program.steps.single {
            it.templateStepId == "preventive_disease_treatment:remaining:1" }.scheduledDate)
    }

    @Test fun missedRoseFeedingAndColdTailKeepSeparateDatesWithTheOriginalInterval() {
        val cold = climate.copy(safeSpringDay = MonthDay.of(6, 1), safeAutumnDay = MonthDay.of(8, 1))
        val program = CareProgramGenerator().generate(PlantCareCatalog.find("Роза")!!,
            CareProgramContext(LocalDate.of(2027, 7, 15), CultivationType.OPEN_GROUND, cold))
        val feeding = program.steps.filter { programBaseStep(it.templateStepId) == "nutrition" }
        assertEquals(listOf("2027-07-16", "2028-06-01", "2028-07-16"), feeding.map { it.scheduledDate.toString() })
        assertEquals(3, feeding.map { it.templateStepId }.distinct().size)
    }

    @Test fun plantingProceduresKeepTheirPhaseWhileTheWholePlantingScheduleWaitsForSpring() {
        for (crop in listOf("tomato", "cucumber")) for (cultivation in CultivationType.entries) {
            val program = CareProgramGenerator().generate(PlantCareCatalog.all().single { it.id == crop },
                CareProgramContext(LocalDate.of(2027, 1, 1), cultivation, climate))
            val planting = program.steps.filter { RootCareSchedule.isPlantingStep(it.templateStepId) }
            assertTrue(planting.all { it.scheduledDate == LocalDate.of(2027, 4, 25) })
            val spraying = program.steps.single { it.templateStepId == "fitosporin_spraying" }
            assertEquals(if (crop == "tomato") 8L else 7L, ChronoUnit.DAYS.between(planting.first().scheduledDate, spraying.scheduledDate))
            assertTrue(program.steps.filter { RootCareSchedule.isRootStep(it.templateStepId) && !RootCareSchedule.isPlantingStep(it.templateStepId) }
                .all { !it.scheduledDate.isBefore(spraying.scheduledDate) })
        }
    }

    @Test fun climateFloorExcludesAnEarlierWarmDayAndFreezingForecastDoesNotQualify() {
        val spring = climate.safeSpringDate(2027)
        val template = RootCareSchedule.apply(PlantCareTemplate("apple", "Яблоня", emptySet(), 22,
            setOf(CultivationType.OPEN_GROUND), steps = listOf(
                CareStepTemplate("before_bud_break", "Обработать", offsetDays = 0, note = "Обработка"),
                CareStepTemplate("nutrition", "Подкормить", offsetDays = 1, windowBeforeDays = 3, windowAfterDays = 3,
                    note = "Корневая подкормка"))))
        fun day(date: LocalDate, minimum: Double) = ForecastWeatherDay(date, minimum, 12.0, 0.0, 1.0)
        val forecast = listOf(day(spring.minusDays(1), 8.0), day(spring, -2.0), day(spring.plusDays(1), 4.0))
        val step = CareProgramGenerator().generate(template,
            CareProgramContext(spring.minusDays(10), CultivationType.OPEN_GROUND, climate, forecast))
            .steps.single { it.templateStepId == "nutrition" }
        assertEquals(spring.plusDays(1), step.scheduledDate)
        assertEquals(spring, step.windowStart)
        assertTrue(step.weatherAdjusted)
        assertTrue(step.needsWeatherConfirmation) // A warm air forecast does not measure soil thaw.
        val frostOnly = CareProgramGenerator().generate(template,
            CareProgramContext(spring.minusDays(10), CultivationType.OPEN_GROUND, climate, forecast.take(2)))
            .steps.single { it.templateStepId == "nutrition" }
        assertTrue(frostOnly.needsWeatherConfirmation)
        assertFalse(frostOnly.weatherAdjusted)
        assertFalse(frostOnly.explanation.contains("соответствует условиям"))
    }

    @Test fun fruitFeedingFollowsTheForecastAdjustedSpringSprayAndDoesNotLoseItsRepeats() {
        val spring = climate.safeSpringDate(2027)
        val template = RootCareSchedule.apply(PlantCareTemplate("pear", "Груша", emptySet(), 22,
            setOf(CultivationType.OPEN_GROUND), steps = listOf(
                CareStepTemplate("before_bud_break", "Обработать", offsetDays = 0, windowAfterDays = 3,
                    weatherLimits = WeatherLimits(maximumPrecipitationMm = 1.0), note = "Обработка"),
                CareStepTemplate("nutrition", "Подкормить", offsetDays = 0,
                    recurrence = CareRecurrence(RepeatType.CUSTOM, 30, 3), note = "Подкормка"))))
        val forecast = (0L..3L).map { offset -> ForecastWeatherDay(spring.plusDays(offset), 4.0, 12.0,
            if (offset == 3L) 0.0 else 10.0, 1.0) }
        val program = CareProgramGenerator().generate(template, CareProgramContext(spring, CultivationType.OPEN_GROUND, climate, forecast))
        val spray = program.steps.single { it.templateStepId == "before_bud_break" }
        val root = program.steps.single { it.templateStepId == "nutrition" }
        assertEquals(spring.plusDays(3), spray.scheduledDate)
        assertEquals(spray.scheduledDate.plusDays(1), root.scheduledDate)
        assertEquals(CareRecurrence(RepeatType.CUSTOM, 30, 3), root.recurrence)
    }

    @Test fun movingHydrangeaRootCheckKeepsBothFiveDayIntervals() {
        val template = PlantCareCatalog.find("Гортензия крупнолистная")!!
        val program = CareProgramGenerator().generate(template,
            CareProgramContext(LocalDate.of(2027, 1, 1), CultivationType.OPEN_GROUND, climate))
        val checks = program.steps.filter { programBaseStep(it.templateStepId) in
            setOf("hydrangea_pale_acid", "hydrangea_pale_iron", "hydrangea_yellow_magnesium") }.sortedBy { it.scheduledDate }
        assertEquals(climate.safeSpringDate(2027), checks.first().scheduledDate)
        assertEquals(5L, ChronoUnit.DAYS.between(checks[0].scheduledDate, checks[1].scheduledDate))
        assertEquals(5L, ChronoUnit.DAYS.between(checks[1].scheduledDate, checks[2].scheduledDate))
        assertFalse(checks[1].note.contains(RootCareSchedule.SOIL_NOTICE))
    }

    @Test fun finiteAutomaticWateringPausesOverWinterWithoutLosingAnOccurrenceOrBecomingPersonal() {
        val cold = climate.copy(safeSpringDay = MonthDay.of(6, 1), safeAutumnDay = MonthDay.of(9, 10))
        val template = PlantCareCatalog.find("Туя")!!
        val program = CareProgramGenerator().generate(template,
            CareProgramContext(LocalDate.of(2027, 6, 1), CultivationType.OPEN_GROUND, cold))
        val water = program.steps.filter { programBaseStep(it.templateStepId) == "conifer_water" }
            .sortedBy { it.templateStepId.substringAfterLast(':').toInt() }
        assertEquals(8, water.size)
        assertTrue(water.all { it.recurrence == null })
        assertEquals(listOf("2027-06-08", "2027-06-22", "2027-07-06", "2027-07-20", "2027-08-03", "2027-08-17", "2027-08-31", "2028-06-01"),
            water.map { it.scheduledDate.toString() })
        val saved = program.installedRows(1, "Сад").filter { RootCareSchedule.isRootStep(it.programStepId) }
        assertTrue(saved.all { !it.userEdited && it.programBaseline == it.programFingerprint() })
        assertEquals("2027-09-03", saved.single { programBaseStep(it.programStepId) == "conifer_winter" }.creationDate)
    }

    @Test fun coldTailRetainsItsIntervalInTheNextWarmSeasonAndUnlimitedRuleStaysUnlimited() {
        val cold = climate.copy(safeSpringDay = MonthDay.of(6, 1), safeAutumnDay = MonthDay.of(7, 1))
        val start = LocalDate.of(2027, 6, 1)
        val repeat = CareRecurrence(RepeatType.CUSTOM, 14, 6)
        assertEquals(listOf("2027-06-01", "2027-06-15", "2027-06-29", "2028-06-01", "2028-06-15", "2028-06-29"),
            RootCareSchedule.finiteDates(start, repeat, cold).map { it.toString() })
        val template = RootCareSchedule.apply(PlantCareTemplate("unlimited-root", "Полив", emptySet(), 22,
            setOf(CultivationType.OPEN_GROUND), steps = listOf(CareStepTemplate("conifer_water", "Полить",
                offsetDays = 0, recurrence = repeat.copy(count = null), note = "Личный бессрочный повтор"))))
        val generated = CareProgramGenerator().generate(template, CareProgramContext(start, CultivationType.OPEN_GROUND, cold))
        assertEquals(1, generated.steps.size)
        assertEquals(repeat.copy(count = null), generated.steps.single().recurrence)
    }

    @Test fun autumnRootWorkAndDependentChecksCannotRunPastTheClimateBoundary() {
        val start = LocalDate.of(2027, 10, 8)
        val template = RootCareSchedule.apply(PlantCareTemplate("hydrangea", "Гортензия", emptySet(), 22,
            setOf(CultivationType.OPEN_GROUND), openGroundStartOffsetDays = 166, steps = listOf(
                CareStepTemplate("hydrangea_pale_acid", "Осмотр", offsetDays = 0, note = "Полив"),
                CareStepTemplate("hydrangea_pale_iron", "Осмотр", offsetDays = 5, note = "По листу"),
                CareStepTemplate("hydrangea_yellow_magnesium", "Осмотр", offsetDays = 10, note = "Полив"),
                CareStepTemplate("nutrition", "Подкормить", offsetDays = 4, note = "По оттаявшей почве"))))
        val program = CareProgramGenerator().generate(template, CareProgramContext(start, CultivationType.OPEN_GROUND, climate))
        assertEquals(LocalDate.of(2028, 4, 25), program.steps.single { it.templateStepId == "hydrangea_pale_acid" }.scheduledDate)
        assertEquals(LocalDate.of(2028, 4, 30), program.steps.single { it.templateStepId == "hydrangea_pale_iron" }.scheduledDate)
        assertEquals(LocalDate.of(2028, 5, 5), program.steps.single { it.templateStepId == "hydrangea_yellow_magnesium" }.scheduledDate)
        assertEquals(LocalDate.of(2028, 4, 25), program.steps.single { it.templateStepId == "nutrition" }.scheduledDate)
    }
}
