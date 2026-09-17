package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.domain.*

class SpringCareProgramsTest {
    private val climate = ClimateFingerprint(MonthDay.of(4, 25), MonthDay.of(10, 10), 168,
        2600.0, 1750.0, 420.0, -18.0, ClimateConfidence.HIGH, 20)

    @Test fun onlyWoodyProgramsGetTwoSpringStagesAndExistingSeasonDatesArePreserved() {
        val mixture = FolkFertilizers.tankMixes.single { it.id == FolkFertilizers.GREEN_CONE_ID }
        SeasonalCarePrograms.templates.forEach { original ->
            val updated = PlantCareCatalog.all().single { it.id == original.id }
            if (original.id !in SpringCarePrograms.woodyCrops) {
                assertEquals(original.steps.map { it.id }, updated.steps.map { it.id })
                assertEquals(original.openGroundStartOffsetDays, updated.openGroundStartOffsetDays)
            } else {
                assertEquals(7, updated.version)
                assertEquals(original.steps.size + 2, updated.steps.size)
                assertEquals(SpringCarePrograms.EARLY_STEP, ProgramProductCatalog.baseStepId(updated.steps[0].id))
                assertEquals(SpringCarePrograms.GREEN_STEP, updated.steps[1].id)
                assertEquals(mixture.name, updated.steps[1].productDescription)
                assertTrue(updated.steps[1].note.contains(mixture.ingredients))
                assertTrue(updated.steps.take(2).all { it.recurrence == null })
                original.steps.zip(updated.steps.drop(2)).forEach { (old, new) ->
                    assertEquals(old.id, new.id)
                    assertEquals(original.openGroundStartOffsetDays + old.offsetDays,
                        updated.openGroundStartOffsetDays + new.offsetDays)
                }
            }
        }
    }

    @Test fun springChoicesAreCropSpecificAndExpiredSpringWorkMovesToNextYear() {
        val apple = PlantCareCatalog.find("яблоня")!!
        val first = apple.steps.first()
        assertEquals(SpringCarePrograms.products.map { it.id }.toSet(),
            ProgramProductCatalog.alternatives("apple", first.id).map { it.id }.toSet())
        assertFalse(ProgramProductCatalog.alternatives("blueberry", first.id).any { it.id == "spring_profilaktin" })
        assertTrue(ProgramProductCatalog.alternatives("tomato", first.id).isEmpty())
        SpringCarePrograms.products.forEach { product ->
            assertTrue(ProgramReferenceCatalog.entries(ProductSection.TREATMENT).any { it.id == product.id })
        }
        val start = LocalDate.of(2026, 3, 1)
        val program = CareProgramGenerator().generate(apple, CareProgramContext(start, CultivationType.OPEN_GROUND, climate))
        val changed = program.withProduct(0, "spring_30_plus", start)
        assertTrue(changed.steps[0].note.contains("500 мл на 10 л"))
        assertFalse(changed.steps[0].note.contains("400 г"))
        assertEquals(program.steps[1], changed.steps[1])
        val resumed = CareProgramGenerator().generate(apple,
            CareProgramContext(LocalDate.of(2026, 6, 1), CultivationType.OPEN_GROUND, climate))
        val moved = resumed.steps.filter { ProgramProductCatalog.baseStepId(it.templateStepId) in
            setOf(SpringCarePrograms.EARLY_STEP, SpringCarePrograms.GREEN_STEP) }
        assertEquals(2, moved.size)
        assertTrue(moved.all { it.scheduledDate.year == 2027 })
    }
}
