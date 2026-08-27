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
import ru.samates.gardenspa.domain.careTitleWithoutSeasonLabel

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
        assertEquals(16, PlantCareCatalog.all().size)
        assertEquals("tomato", PlantCareCatalog.find("  ПОМИДОР ")?.id)
        assertEquals("garden-strawberry", PlantCareCatalog.find("Клубника")?.id)
        assertEquals("apple", PlantCareCatalog.find("Яблоня на карликовом подвое")?.id)
        assertEquals("pear", PlantCareCatalog.find("Груша на сильнорослом подвое")?.id)
        assertEquals("zucchini", PlantCareCatalog.find("Кабачок плетистый")?.id)
        assertEquals("garlic", PlantCareCatalog.find("Чеснок стрелкующийся")?.id)
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
    fun approvedProgramsUseBashInkomAndOtherProgramsStayBrandNeutral() {
        PlantCareCatalog.all().forEach { template ->
            val treatmentSteps = template.steps.filter { it.productDescription != null }
            assertTrue("${template.canonicalName} has no product steps", treatmentSteps.size >= 2)
            if (template.id in setOf("tomato", "cucumber")) {
                assertEquals(3, template.version)
                assertTrue(treatmentSteps.all { "БашИнком" in requireNotNull(it.productDescription) })
                assertTrue(treatmentSteps.all { "https://www.bashinkom.ru/" in it.note })
            } else {
                assertEquals(2, template.version)
                assertTrue(treatmentSteps.all { step ->
                    val description = requireNotNull(step.productDescription)
                    "разрешённое" in description && "бренд" !in description.lowercase()
                })
            }
        }
    }

    @Test
    fun tomatoAndCucumberProgramsContainConfirmedRatesAndRainSensitiveSprays() {
        val tomato = requireNotNull(PlantCareCatalog.find("томат"))
        val cucumber = requireNotNull(PlantCareCatalog.find("огурец"))

        assertTrue(tomato.steps.any { "5 г в 10 л воды" in it.note && it.recurrence?.count == 2 })
        assertTrue(cucumber.steps.any { "10 г в 5 л воды" in it.note && it.recurrence?.count == 3 })
        listOf(tomato, cucumber).forEach { template ->
            template.steps.filter { "опрыск" in it.title.lowercase() || "по листьям" in it.title.lowercase() }.forEach { step ->
                assertTrue(step.weatherLimits.maximumPrecipitationMm != null)
                assertTrue(step.weatherLimits.maximumWindMetersPerSecond != null)
            }
        }
    }

    @Test
    fun generatorMovesWeatherSensitiveStepInsideAllowedWindow() {
        val tomato = requireNotNull(PlantCareCatalog.find("томат"))
        val start = LocalDate.of(2026, 5, 10)
        val initialSprayingDate = start.plusDays(8)
        val forecast = (-1L..2L).map { offset ->
            val date = initialSprayingDate.plusDays(offset)
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
        val spraying = program.steps.first { it.templateStepId == "fitosporin_spraying" }

        assertEquals(initialSprayingDate.plusDays(1), spraying.scheduledDate)
        assertTrue(spraying.weatherAdjusted)
        assertNull(program.warning)
    }

    @Test
    fun mergedPlantsUseOneCanonicalProgramAndDoNotExposeRemovedTasks() {
        assertEquals("Яблоня", requireNotNull(PlantCareCatalog.find("Яблоня на сильнорослом подвое")).canonicalName)
        assertEquals("Груша", requireNotNull(PlantCareCatalog.find("Груша на слаборослом подвое")).canonicalName)
        assertEquals("Земляника", requireNotNull(PlantCareCatalog.find("Земляника садовая ремонтантная")).canonicalName)
        assertEquals("Кабачок", requireNotNull(PlantCareCatalog.find("Кабачок кустовой")).canonicalName)
        assertEquals("Чеснок", requireNotNull(PlantCareCatalog.find("Чеснок нестрелкующийся")).canonicalName)

        val removedStepIds = setOf("post_harvest_pruning", "runner_pruning", "pruning_training", "scape_pruning")
        val removedTitles = setOf(
            "обновить землянику после окончания урожая",
            "удалить усы",
            "направить плети плетистого кабачка",
            "удалить стрелки чеснока"
        )
        PlantCareCatalog.all().forEach { template ->
            assertTrue(template.steps.none { it.id in removedStepIds })
            assertTrue(template.steps.none { step ->
                removedTitles.any { removed -> step.title.lowercase().contains(removed) }
            })
        }
    }

    @Test
    fun workTitlesDoNotContainSeasonLabels() {
        val forbidden = listOf("летняя", "осенняя", "весенняя", "предзимняя", "ранневесенняя", "позднезимняя")
        PlantCareCatalog.all().forEach { template ->
            template.steps.forEach { step ->
                assertTrue(
                    "${template.canonicalName}: ${step.title}",
                    forbidden.none(step.title.lowercase()::contains)
                )
            }
        }
        assertEquals(
            "Обрезка яблони",
            careTitleWithoutSeasonLabel("Позднезимняя или ранневесенняя обрезка яблони")
        )
        assertEquals("Корневая подкормка", careTitleWithoutSeasonLabel("Летняя корневая подкормка"))
    }
}
