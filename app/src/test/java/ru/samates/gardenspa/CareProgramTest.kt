package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samates.gardenspa.domain.CareProgramContext
import ru.samates.gardenspa.domain.CareProgramGenerator
import ru.samates.gardenspa.domain.ClimateConfidence
import ru.samates.gardenspa.domain.ClimateFingerprint
import ru.samates.gardenspa.domain.CultivationType
import ru.samates.gardenspa.domain.ForecastWeatherDay
import ru.samates.gardenspa.domain.NaturalZone
import ru.samates.gardenspa.domain.PlantCareCatalog
import ru.samates.gardenspa.domain.PlantNameCatalog
import ru.samates.gardenspa.domain.ProgramStartChoice
import ru.samates.gardenspa.domain.ProgramStartPlanner
import ru.samates.gardenspa.domain.ReadyProgramDrugCatalog
import ru.samates.gardenspa.domain.naturalZone
import ru.samates.gardenspa.domain.recommendedEndDate
import ru.samates.gardenspa.domain.recommendedStartDate

class CareProgramTest {
    private val climate = ClimateFingerprint(
        safeSpringDay = MonthDay.of(4, 25),
        safeAutumnDay = MonthDay.of(10, 10),
        frostFreeDays = 168,
        growingDegreeDays5 = 2_600.0,
        growingDegreeDays10 = 1_750.0,
        warmSeasonPrecipitationMm = 420.0,
        winterMinimumP10 = -18.0,
        confidence = ClimateConfidence.HIGH,
        sourceYears = 20
    )

    @Test
    fun catalogRequiresAGroupWhereCareDiffers() {
        assertEquals(38, PlantCareCatalog.all().size)
        assertNull(PlantCareCatalog.find("  ПОМИДОР "))
        assertNull(PlantCareCatalog.find("Клубника"))
        assertNull(PlantCareCatalog.find("Болгарский перец"))
        assertEquals("beet", PlantCareCatalog.find("свекла")?.id)
        assertEquals("hydrangea-paniculata", PlantCareCatalog.find("Гортензия метельчатая")?.id)
        assertEquals("tomato-indeterminate", PlantCareCatalog.find("Томат высокорослый")?.id)
        assertNull(PlantCareCatalog.find("Неизвестное растение"))
    }

    @Test
    fun suggestionsMatchCanonicalNamesAndAliasesByPrefix() {
        assertEquals(setOf("tomato-determinate", "tomato-indeterminate"), PlantCareCatalog.suggestions("пом").map { it.id }.toSet())
        assertEquals(5, PlantCareCatalog.suggestions("горт").size)
        assertEquals(setOf("pear-standard-rootstock", "pear-dwarf-rootstock"), PlantCareCatalog.suggestions("гру").map { it.id }.toSet())
        assertTrue(PlantCareCatalog.suggestions("").isEmpty())
    }

    @Test
    fun plantCatalogShowsCareGroupsOnlyForSupportedPlants() {
        assertTrue(PlantNameCatalog.all().size >= 180)
        assertEquals("Абутилон", PlantNameCatalog.suggestions("абу").single().canonicalName)
        assertEquals(setOf("Томат детерминантный", "Томат индетерминантный"), PlantNameCatalog.suggestions("пом").map { it.canonicalName }.toSet())
        assertTrue(PlantNameCatalog.suggestions("горт").none { it.canonicalName == "Гортензия" })
        assertTrue(PlantNameCatalog.all().none { "F1" in it.canonicalName || "сорт" in it.canonicalName.lowercase() })
    }

    @Test
    fun climateStartDateDependsOnCropColdTolerance() {
        val carrot = requireNotNull(PlantCareCatalog.find("морковь"))
        val eggplant = requireNotNull(PlantCareCatalog.find("баклажан"))
        val chosenStart = LocalDate.of(2026, 4, 1)
        val generator = CareProgramGenerator()

        val carrotProgram = generator.generate(
            carrot,
            CareProgramContext(chosenStart, CultivationType.OPEN_GROUND, climate),
            instanceId = "carrot"
        )
        val eggplantProgram = generator.generate(
            eggplant,
            CareProgramContext(chosenStart, CultivationType.OPEN_GROUND, climate),
            instanceId = "eggplant"
        )

        assertEquals(LocalDate.of(2026, 4, 11), carrotProgram.recommendedStartDate)
        assertEquals(LocalDate.of(2026, 5, 11), eggplantProgram.recommendedStartDate)
    }

    @Test
    fun futureRecommendationUsesRecommendedDate() {
        val tomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val proposal = ProgramStartPlanner.propose(
            template = tomato,
            cultivationType = CultivationType.OPEN_GROUND,
            climate = climate,
            selectedDate = LocalDate.of(2026, 3, 10),
            today = LocalDate.of(2026, 3, 1)
        )

        assertEquals(false, proposal.recommendationHasPassed)
        assertEquals(LocalDate.of(2026, 5, 6), proposal.recommendedDate)
        assertEquals(
            LocalDate.of(2026, 5, 6),
            proposal.resolve(ProgramStartChoice.RECOMMENDED_DATE)
        )
    }

    @Test
    fun passedRecommendationCanMoveToNextYearOrUseSelectedDate() {
        val tomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val proposal = ProgramStartPlanner.propose(
            template = tomato,
            cultivationType = CultivationType.OPEN_GROUND,
            climate = climate,
            selectedDate = LocalDate.of(2026, 8, 15),
            today = LocalDate.of(2026, 8, 15)
        )

        assertTrue(proposal.recommendationHasPassed)
        assertEquals(LocalDate.of(2027, 5, 6), proposal.resolve(ProgramStartChoice.NEXT_YEAR))
        assertEquals(LocalDate.of(2026, 8, 15), proposal.resolve(ProgramStartChoice.USER_DATE))
    }

    @Test
    fun customDateKeepsOnlyRemainingProceduresWithoutMovingSeason() {
        val tomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val normalStart = recommendedStartDate(tomato, CultivationType.OPEN_GROUND, climate, 2026)
        val selectedDate = LocalDate.of(2026, 6, 20)
        val program = CareProgramGenerator().generate(
            template = tomato,
            context = CareProgramContext(
                startDate = normalStart,
                cultivationType = CultivationType.OPEN_GROUND,
                climate = climate,
                includeOnlyOnOrAfter = selectedDate
            ),
            instanceId = "remaining"
        )

        assertEquals(selectedDate, program.remainingFromDate)
        assertTrue(program.steps.all { !it.scheduledDate.isBefore(selectedDate) })
        assertTrue(program.steps.none { it.templateStepId == "pruning" })
        assertTrue(program.steps.none { it.templateStepId == "moisture_and_health_check" })
        assertTrue(program.steps.all { "pruning" in it.templateStepId || it.productDescription != null })
    }

    @Test
    fun customDateAfterSeasonProducesNoProcedures() {
        val tomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val normalStart = recommendedStartDate(tomato, CultivationType.OPEN_GROUND, climate, 2026)
        val program = CareProgramGenerator().generate(
            template = tomato,
            context = CareProgramContext(
                startDate = normalStart,
                cultivationType = CultivationType.OPEN_GROUND,
                climate = climate,
                includeOnlyOnOrAfter = LocalDate.of(2026, 11, 1)
            )
        )

        assertTrue(program.steps.isEmpty())
    }

    @Test
    fun generatedProgramOnlyContainsPruningAndProductProcedures() {
        val tomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val program = CareProgramGenerator().generate(
            template = tomato,
            context = CareProgramContext(LocalDate.of(2026, 5, 6), CultivationType.OPEN_GROUND, climate)
        )

        assertTrue(program.steps.any { it.templateStepId == "pruning" })
        assertTrue(program.steps.any { it.productDescription != null })
        assertTrue(program.steps.all { "pruning" in it.templateStepId || it.productDescription != null })
    }

    @Test
    fun everyCatalogProgramContainsBrandNeutralTreatmentCategories() {
        PlantCareCatalog.all().forEach { template ->
            assertTrue(template.version >= 2)
            val treatmentSteps = template.steps.filter { it.productDescription != null }
            assertTrue("${template.canonicalName} has no product steps", treatmentSteps.size >= 2)
            assertTrue(treatmentSteps.all { step ->
                val description = requireNotNull(step.productDescription)
                "разрешённое" in description && "бренд" !in description.lowercase()
            })
        }
    }

    @Test
    fun everyProgramProductStepHasAlternativesFromDifferentManufacturers() {
        val availableDrugs = ReadyProgramDrugCatalog.defaultDrugs

        PlantCareCatalog.all().forEach { template ->
            template.steps.filter { it.productDescription != null }.forEach { step ->
                val options = ReadyProgramDrugCatalog.recommendedFor(template.id, step.id, availableDrugs)
                assertTrue("${template.canonicalName} / ${step.title}: нужно минимум два препарата", options.size >= 2)
                assertTrue("${template.canonicalName} / ${step.title}: нужны разные производители", options.map { it.name.substringAfterLast("—").trim() }.distinct().size >= 2)
            }
        }
    }

    @Test
    fun popularProgramsCoverAnnualCareWithoutPlanting() {
        val popularPrefixes = setOf("tomato-", "cucumber-", "hydrangea-", "peony-", "rose-", "apple-", "pear-")
        val popular = PlantCareCatalog.all().filter { template -> popularPrefixes.any(template.id::startsWith) }

        assertEquals(21, popular.size)
        popular.forEach { template ->
            assertTrue("${template.canonicalName}: нужна корневая подкормка", template.steps.any { "root_feeding" in it.id })
            assertTrue("${template.canonicalName}: нужна внекорневая подкормка", template.steps.any { "foliar_feeding" in it.id })
            assertTrue("${template.canonicalName}: нужны обработки и опрыскивания", template.steps.count { "treatment" in it.id || "spraying" in it.id } >= 3)
            assertTrue("${template.canonicalName}: нужна подготовка к зиме", template.steps.any { it.id == "winter_preparation" })
            assertTrue("${template.canonicalName}: посадка не должна входить в план", template.steps.none { step ->
                "посад" in step.title.lowercase() || "посад" in step.note.lowercase()
            })
            assertTrue(template.supportedNaturalZones.isNotEmpty())
        }
    }

    @Test
    fun pruningDependsOnSelectedCareGroup() {
        val determinateTomato = requireNotNull(PlantCareCatalog.findById("tomato-determinate"))
        val indeterminateTomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val bigleafHydrangea = requireNotNull(PlantCareCatalog.findById("hydrangea-macrophylla"))
        val panicleHydrangea = requireNotNull(PlantCareCatalog.findById("hydrangea-paniculata"))

        assertTrue(determinateTomato.steps.none { "pruning" in it.id })
        assertTrue(indeterminateTomato.steps.any { it.id == "pruning" })
        assertTrue(bigleafHydrangea.steps.none { it.id == "pruning" })
        assertTrue(panicleHydrangea.steps.any { it.id == "pruning" })
    }

    @Test
    fun pruningTimingIsPlantSpecific() {
        val tomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val apple = requireNotNull(PlantCareCatalog.findById("apple-standard-rootstock"))
        val peony = requireNotNull(PlantCareCatalog.findById("peony-herbaceous"))

        assertTrue(tomato.steps.first().title.contains("пасынкование"))
        assertTrue(apple.openGroundStartOffsetDays < tomato.openGroundStartOffsetDays)
        assertTrue(peony.steps.any { it.id == "autumn_pruning" })
    }

    @Test
    fun naturalZonesProduceDifferentCareWindows() {
        val tomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val coldClimate = climate.copy(
            safeSpringDay = MonthDay.of(6, 5),
            safeAutumnDay = MonthDay.of(8, 25),
            frostFreeDays = 81,
            growingDegreeDays5 = 900.0,
            growingDegreeDays10 = 450.0,
            winterMinimumP10 = -38.0
        )
        val subtropicalClimate = climate.copy(
            safeSpringDay = MonthDay.of(3, 1),
            safeAutumnDay = MonthDay.of(12, 1),
            frostFreeDays = 275,
            growingDegreeDays5 = 4_800.0,
            growingDegreeDays10 = 3_200.0,
            warmSeasonPrecipitationMm = 650.0,
            winterMinimumP10 = -3.0
        )

        assertEquals(NaturalZone.TUNDRA_AND_FOREST_TUNDRA, coldClimate.naturalZone())
        assertEquals(NaturalZone.HUMID_SUBTROPICS, subtropicalClimate.naturalZone())
        assertTrue(recommendedStartDate(tomato, CultivationType.OPEN_GROUND, coldClimate, 2026) > recommendedStartDate(tomato, CultivationType.OPEN_GROUND, subtropicalClimate, 2026))
        assertTrue(recommendedEndDate(tomato, CultivationType.OPEN_GROUND, coldClimate, 2026) < recommendedEndDate(tomato, CultivationType.OPEN_GROUND, subtropicalClimate, 2026))
    }

    @Test
    fun generatorMovesWeatherSensitiveStepInsideAllowedWindow() {
        val tomato = requireNotNull(PlantCareCatalog.findById("tomato-indeterminate"))
        val start = LocalDate.of(2026, 5, 10)
        val initialFeedingDate = start.plusDays(8)
        val forecast = (-2L..3L).map { offset ->
            val date = initialFeedingDate.plusDays(offset)
            ForecastWeatherDay(
                date = date,
                minimumTemperatureC = 10.0,
                maximumTemperatureC = 20.0,
                precipitationMm = if (offset == 1L) 0.0 else 20.0,
                maximumWindMetersPerSecond = 3.0
            )
        }

        val program = CareProgramGenerator().generate(
            template = tomato,
            context = CareProgramContext(start, CultivationType.OPEN_GROUND, climate, forecast),
            instanceId = "test-instance"
        )
        val feeding = program.steps.first { it.templateStepId == "spring_root_feeding" }

        assertEquals(initialFeedingDate.plusDays(1), feeding.scheduledDate)
        assertTrue(feeding.weatherAdjusted)
        assertNull(program.warning)
    }
}
