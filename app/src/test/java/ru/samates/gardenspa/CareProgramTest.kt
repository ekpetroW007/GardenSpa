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
import ru.samates.gardenspa.domain.PlantCareCatalog
import ru.samates.gardenspa.domain.ProgramStartChoice
import ru.samates.gardenspa.domain.ProgramStartPlanner

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
    fun catalogRecognizesAliasesButNotUnknownPlants() {
        assertEquals(15, PlantCareCatalog.all().size)
        assertEquals("tomato", PlantCareCatalog.find("  ПОМИДОР ")?.id)
        assertEquals("garden-strawberry", PlantCareCatalog.find("Клубника")?.id)
        assertEquals("sweet-pepper", PlantCareCatalog.find("Болгарский перец")?.id)
        assertEquals("beet", PlantCareCatalog.find("свекла")?.id)
        assertNull(PlantCareCatalog.find("Неизвестное растение"))
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
        val tomato = requireNotNull(PlantCareCatalog.find("томат"))
        val proposal = ProgramStartPlanner.propose(
            template = tomato,
            cultivationType = CultivationType.OPEN_GROUND,
            climate = climate,
            selectedDate = LocalDate.of(2026, 3, 10),
            today = LocalDate.of(2026, 3, 1)
        )

        assertEquals(false, proposal.recommendationHasPassed)
        assertEquals(LocalDate.of(2026, 4, 28), proposal.recommendedDate)
        assertEquals(
            LocalDate.of(2026, 4, 28),
            proposal.resolve(ProgramStartChoice.RECOMMENDED_DATE)
        )
    }

    @Test
    fun passedRecommendationCanMoveToNextYearOrDayAfterUserDate() {
        val tomato = requireNotNull(PlantCareCatalog.find("томат"))
        val proposal = ProgramStartPlanner.propose(
            template = tomato,
            cultivationType = CultivationType.OPEN_GROUND,
            climate = climate,
            selectedDate = LocalDate.of(2026, 8, 15),
            today = LocalDate.of(2026, 8, 15)
        )

        assertTrue(proposal.recommendationHasPassed)
        assertEquals(LocalDate.of(2027, 4, 28), proposal.resolve(ProgramStartChoice.NEXT_YEAR))
        assertEquals(LocalDate.of(2026, 8, 16), proposal.resolve(ProgramStartChoice.USER_DATE))
    }

    @Test
    fun everyCatalogProgramContainsBrandNeutralTreatmentCategories() {
        PlantCareCatalog.all().forEach { template ->
            assertEquals(2, template.version)
            val treatmentSteps = template.steps.filter { it.productDescription != null }
            assertTrue("${template.canonicalName} has no product steps", treatmentSteps.size >= 2)
            assertTrue(treatmentSteps.all { step ->
                val description = requireNotNull(step.productDescription)
                "разрешённое" in description && "бренд" !in description.lowercase()
            })
        }
    }

    @Test
    fun generatorMovesWeatherSensitiveStepInsideAllowedWindow() {
        val tomato = requireNotNull(PlantCareCatalog.find("томат"))
        val start = LocalDate.of(2026, 5, 10)
        val initialFeedingDate = start.plusDays(12)
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
        val feeding = program.steps.first { it.templateStepId == "first_feeding" }

        assertEquals(initialFeedingDate.plusDays(1), feeding.scheduledDate)
        assertTrue(feeding.weatherAdjusted)
        assertNull(program.warning)
    }
}
