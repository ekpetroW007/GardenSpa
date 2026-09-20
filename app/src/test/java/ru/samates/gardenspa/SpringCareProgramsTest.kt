package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.domain.*

class SpringCareProgramsTest {
    private val climate = ClimateFingerprint(MonthDay.of(4, 25), MonthDay.of(10, 10), 168,
        2600.0, 1750.0, 420.0, -18.0, ClimateConfidence.HIGH, 20)

    @Test fun woodyProgramsSeparateDiseaseAndPestsAndPreserveRetainedSeasonDates() {
        SeasonalCarePrograms.templates.forEach { original ->
            val updated = PlantCareCatalog.all().single { it.id == original.id }
            val retained = original.steps.filterNot { it.title.contains("осмотреть", true) || it.id == "after_flowering" }
            if (original.id !in SpringCarePrograms.woodyCrops) {
                assertFalse(updated.steps.any { it.id == SpringCarePrograms.EARLY_STEP })
                assertEquals(original.openGroundStartOffsetDays, updated.openGroundStartOffsetDays)
            } else {
                assertTrue(updated.version >= 10)
                val early = updated.steps.single { it.id == SpringCarePrograms.EARLY_STEP }
                val pests = updated.steps.single { it.id == SpringCarePrograms.PEST_STEP }
                val green = updated.steps.single { it.id == SpringCarePrograms.GREEN_STEP }
                assertEquals("Обработать от болезней до распускания почек", early.title)
                assertEquals("Бордосская смесь", early.productDescription)
                assertEquals(7, pests.offsetDays - early.offsetDays)
                assertEquals(7, green.offsetDays - pests.offsetDays)
                assertTrue(listOf(early, pests, green).all { it.note.contains("+3…+5") && it.recurrence == null })
                assertEquals(CareAnchor.SAFE_AUTUMN_DATE, updated.steps.single { it.id.startsWith(SpringCarePrograms.AUTUMN_STEP) }.anchor)
                retained.forEach { old ->
                    val new = updated.steps.single { it.id == old.id }
                    assertEquals(original.openGroundStartOffsetDays + old.offsetDays, updated.openGroundStartOffsetDays + new.offsetDays)
                }
            }
            assertTrue(updated.steps.none { it.id == "after_flowering" })
        }
    }

    @Test fun replacementRetainsPhaseAndDoesNotCarryOldProductInstructions() {
        val apple = PlantCareCatalog.find("яблоня")!!
        assertEquals(SpringCarePrograms.diseaseIds, ProgramProductCatalog.alternatives("apple", SpringCarePrograms.EARLY_STEP).map { it.id }.toSet())
        assertEquals(SpringCarePrograms.pestIds, ProgramProductCatalog.alternatives("apple", SpringCarePrograms.PEST_STEP).map { it.id }.toSet())
        assertTrue(ProgramProductCatalog.alternatives("tomato", SpringCarePrograms.EARLY_STEP).isEmpty())
        val start = LocalDate.of(2026, 3, 1)
        val program = CareProgramGenerator().generate(apple, CareProgramContext(start, CultivationType.OPEN_GROUND, climate))
        val pestIndex = program.steps.indexOfFirst { it.templateStepId == SpringCarePrograms.PEST_STEP }
        val changed = program.withProduct(pestIndex, "spring_profilaktin_bio", start.plusDays(7))
        assertEquals(program.steps[pestIndex].title, changed.steps[pestIndex].title)
        assertTrue(changed.steps[pestIndex].note.contains("0,5 л/10 л"))
        assertFalse(changed.steps[pestIndex].note.contains("500 мл на 10 л"))
        assertEquals(program.steps.filterIndexed { index, _ -> index != pestIndex }, changed.steps.filterIndexed { index, _ -> index != pestIndex })
        assertThrows(NoSuchElementException::class.java) { program.withProduct(0, "spring_30_plus", start) }
        val resumed = CareProgramGenerator().generate(apple, CareProgramContext(LocalDate.of(2026, 6, 1), CultivationType.OPEN_GROUND, climate))
        val moved = resumed.steps.filter { ProgramProductCatalog.baseStepId(it.templateStepId) in
            setOf(SpringCarePrograms.EARLY_STEP, SpringCarePrograms.PEST_STEP, SpringCarePrograms.GREEN_STEP) }
        assertEquals(3, moved.size)
        assertTrue(moved.all { it.scheduledDate.year == 2027 })
    }
}
