package ru.samates.gardenspa

import java.time.LocalDate
import java.time.MonthDay
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.*

class ProgramRepeatTest {
    private val date = LocalDate.of(2026, 9, 27)
    private val row = PlantEntity(
        id = 12, plantName = "Томат", taskName = "Подкормить", wateringInterval = 1,
        creationDate = date.toString(), drugId = null, gardenId = null,
        drugName = "Средство", gardenName = "Сад", plantCardId = "card",
        programId = "tomato", programStepId = "nutrition"
    )
    private val step = GeneratedCareStep(
        "nutrition", "Подкормить", date, date, date, null, false, false,
        "Описание", "Средство", "Инструкция"
    )

    @Test fun selectedIntervalsPersistAsUnlimitedCalendarRulesAndCanBeDisabled() {
        listOf(7, 10, 14, 30).forEach { days ->
            val saved = row.withProgramRepeat(days)
            assertEquals("CUSTOM", saved.repeatType)
            assertEquals(days, saved.repeatInterval)
            assertEquals(days, saved.wateringInterval)
            assertEquals("NEVER", saved.repeatEndType)
            assertNull(saved.repeatCount)
            assertNull(saved.repeatEndDate)
            assertEquals(row.id, saved.id)
            assertEquals(row.creationDate, saved.creationDate)
            assertEquals(row.drugName, saved.drugName)
            assertTrue(saved.occursOn(date.plusDays(days.toLong())))
            assertTrue(saved.occursOn(date.plusDays(days * 200L)))
            assertFalse(saved.occursOn(date.plusDays(days + 1L)))

            val disabled = saved.withProgramRepeat(null)
            assertEquals("Без повтора", disabled.recurrenceDescription())
            assertTrue(disabled.occursOn(date))
            assertFalse(disabled.occursOn(date.plusDays(days.toLong())))
        }
        assertThrows(IllegalArgumentException::class.java) { row.withProgramRepeat(8) }
    }

    @Test fun explicitChoiceReplacesFiniteOrDateLimitedRuleWithoutChangingOtherWork() {
        val finite = row.copy(repeatType = "CUSTOM", repeatInterval = 21,
            repeatEndType = "COUNT", repeatCount = 3)
        val finiteStep = step.copy(recurrence = CareRecurrence(RepeatType.CUSTOM, 21, 3))
        assertEquals("Каждые 21 дн., 3 раз", finiteStep.programRepeatDescription())
        assertEquals(3, finiteStep.copy(title = "Новое название").recurrence?.count)
        assertEquals(3, finite.copy(taskName = "Новое название").repeatCount)
        assertNull(finite.withProgramRepeat(14).repeatCount)
        val chosen = finiteStep.withProgramRepeat(14)
        assertEquals("Каждые 14 дн.", chosen.programRepeatDescription())
        assertEquals(CareRecurrence(RepeatType.CUSTOM, 14, null), chosen.recurrence)
        assertEquals(step.note, chosen.note)
        assertEquals(step.productDescription, chosen.productDescription)
        assertEquals(step.scheduledDate, chosen.scheduledDate)
        assertNull(chosen.withProgramRepeat(null).recurrence)
        val limited = finite.copy(repeatEndType = "UNTIL_DATE", repeatEndDate = "2026-10-01")
        assertNull(limited.withProgramRepeat(30).repeatEndDate)
        assertEquals("NEVER", limited.withProgramRepeat(30).repeatEndType)
    }

    @Test fun alternativeProductsCanGetTheirOwnRepeatAfterOldScheduleIsReset() {
        val program = GeneratedCareProgram("card", "tomato", 1, "Томат", CultivationType.OPEN_GROUND,
            date, date, "Климат", null, listOf(step.copy(templateStepId = "fitosporin_spraying")))
        val product = ProgramProductCatalog.alternatives("tomato", "fitosporin_spraying")
            .first { it.unavailableReason == null }
        val changed = program.withProduct(0, product.id, date).steps.single()
        assertNull(changed.recurrence)
        val repeated = changed.withProgramRepeat(10)
        assertEquals(product.displayName, repeated.productDescription)
        assertEquals(10, repeated.recurrence?.interval)
        assertNull(repeated.recurrence?.count)
    }

    @Test fun unlimitedTemplateKeepsAnchorWhenStartingLateAndDoesNotExpandForever() {
        val climate = ClimateFingerprint(MonthDay.of(4, 25), MonthDay.of(10, 10), 168,
            2_600.0, 1_750.0, 420.0, -18.0, ClimateConfidence.HIGH, 20)
        val template = PlantCareTemplate("test", "Тест", emptySet(), 1,
            setOf(CultivationType.OPEN_GROUND), openGroundStartOffsetDays = -84,
            steps = listOf(CareStepTemplate("monthly", "Полив", offsetDays = 0,
                recurrence = CareRecurrence(RepeatType.MONTHLY, 1, null), note = "")))
        val generated = CareProgramGenerator().generate(template,
            CareProgramContext(LocalDate.of(2026, 2, 1), CultivationType.OPEN_GROUND, climate))
        assertEquals(1, generated.steps.size)
        assertEquals(LocalDate.of(2026, 1, 31), generated.steps.single().scheduledDate)
        assertNull(generated.steps.single().recurrence?.count)
    }
}
