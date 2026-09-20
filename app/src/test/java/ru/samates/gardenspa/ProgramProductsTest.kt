package ru.samates.gardenspa

import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.*

class ProgramProductsTest {
    private val date = LocalDate.of(2026, 9, 15)
    private fun plant(crop: String = "tomato", step: String = "fitosporin_spraying") = PlantEntity(
        id = 42, plantName = "Мой томат", taskName = "Исходная обработка", wateringInterval = 21,
        creationDate = date.toString(), drugId = null, gardenId = 7, drugName = "Фитоспорин", gardenName = "Дача",
        repeatType = "CUSTOM", repeatInterval = 21, repeatEndType = "COUNT", repeatCount = 2,
        plantCardId = "my-card", programId = crop, programVersion = 4, programStepId = step,
        programImportKey = "original-key", programNote = "Старая дозировка", photoUri = "file:///plant.jpg"
    )

    @Test fun diseaseAndPestFiltersAreIndependentAndDiagnosisCanBeSkipped() {
        val all = ProgramProductCatalog.treatments("cucumber", null, CultivationType.GREENHOUSE)
        assertTrue(all.any { it.id == "boverix" })
        assertTrue(all.any { it.id == "muchnistop" })
        assertFalse(all.any { it.manufacturer == "Аминосил" || it.id == "maxi_nutrition" })
        val mildew = ProgramProductCatalog.treatments("cucumber", "powdery_mildew", CultivationType.OPEN_GROUND)
        assertTrue(mildew.any { it.id == "muchnistop" })
        assertTrue(mildew.any { it.id == "biozashchitin" })
        assertFalse(ProgramProductCatalog.treatments("cucumber", "downy_mildew", CultivationType.OPEN_GROUND).any { it.id == "muchnistop" })
        assertTrue(ProgramProductCatalog.treatments("cucumber", "late_blight", CultivationType.OPEN_GROUND).isEmpty())
        assertTrue(ProgramProductCatalog.treatments("apple", null, CultivationType.OPEN_GROUND).any { it.id == "rayek_fruit" })
    }

    @Test fun whiteflyProductRequiresGreenhouseAndMatchingTarget() {
        assertFalse(ProgramProductCatalog.treatments("tomato", "whitefly", CultivationType.OPEN_GROUND).any { it.id == "boverix" })
        assertFalse(ProgramProductCatalog.treatments("tomato", "spider_mite", CultivationType.GREENHOUSE).any { it.id == "boverix" })
        assertThrows(NoSuchElementException::class.java) {
            plant().problemTreatment("whitefly", "boverix", CultivationType.OPEN_GROUND, date, 1)
        }
    }

    @Test fun unverifiedCandidatesCannotBecomeCalendarWork() {
        assertThrows(NoSuchElementException::class.java) {
            plant().problemTreatment("spider_mite", "maxi_mite", CultivationType.OPEN_GROUND, date, 1)
        }
        assertThrows(NoSuchElementException::class.java) {
            plant(step = "gumi_omi_feeding").withProgramProduct("maxi_nutrition", date, 1)
        }
    }

    @Test fun replacementKeepsIdentityGardenAndPhotoButNotOldDoseOrRepeat() {
        val old = plant()
        val next = old.withProgramProduct("silver_prevention", date.plusDays(1), 5)
        assertEquals(old.id, next.id)
        assertEquals(old.plantCardId, next.plantCardId)
        assertEquals(old.gardenId, next.gardenId)
        assertEquals(old.photoUri, next.photoUri)
        assertEquals(old.programImportKey, next.programImportKey)
        assertEquals("NONE", next.repeatType)
        assertNull(next.repeatCount)
        assertEquals(5, next.reminderDaysBefore)
        assertTrue(next.drugName.contains("Серебромедин"))
        assertFalse(next.programNote.contains("Старая дозировка"))
        assertEquals("silver_prevention", ProgramProductCatalog.productForStep(next.programStepId)?.id)
        assertTrue(next.occursOn(date.plusDays(1)))
        assertFalse(next.occursOn(date.plusDays(22)))
    }

    @Test fun conditionalWorkHasOwnIdAndNoteAndIsInSamePlantCard() {
        val old = plant()
        val new = old.problemTreatment(null, "biozashchitin", CultivationType.OPEN_GROUND, date, 0, "request-a")
        assertEquals(0, new.id)
        assertEquals(old.plantCardId, new.plantCardId)
        assertEquals(old.gardenId, new.gardenId)
        assertEquals(old.photoUri, new.photoUri)
        assertNotEquals(old.programImportKey, new.programImportKey)
        assertTrue(new.programNote.contains("не указаны пользователем"))
        assertNull(new.repeatCount)
        assertEquals(0, new.reminderDaysBefore)
        assertTrue(new.occursOn(date))
        assertFalse(new.occursOn(date.plusDays(7)))
        assertEquals("Старая дозировка", old.programNote)
    }

    @Test fun unsupportedAndWrongCropSelectionsAreRejected() {
        assertTrue(ProgramProductCatalog.alternatives("apple", "fitosporin_spraying").isEmpty())
        assertThrows(NoSuchElementException::class.java) {
            plant(crop = "cucumber", step = "gumi_omi_7_8_leaves").withProgramProduct("amino_tomato", date, 1)
        }
        assertThrows(NoSuchElementException::class.java) {
            plant().problemTreatment("unknown", "biozashchitin", CultivationType.OPEN_GROUND, date, 1)
        }
    }

    @Test fun alternativeDoesNotInheritFitosporinWeatherWindow() {
        val old = plant()
        val next = old.withProgramProduct("silver_prevention", date, 1)
        assertEquals(2, ScheduledTreatment(old, date, date, false, false).weatherLimits()?.requiredDryHoursAfter)
        assertNull(ScheduledTreatment(next, date, date, false, false).weatherLimits())
    }

    @Test fun previewReplacesOnlyChosenStepAndCanAddOptionalTreatment() {
        val originalStep = GeneratedCareStep("fitosporin_spraying", "Профилактика", date, date, date,
            CareRecurrence(RepeatType.CUSTOM, 21, 2), false, false, "Старое объяснение", "Фитоспорин", "Старая дозировка")
        val original = GeneratedCareProgram("instance", "tomato", 5, "Томат", CultivationType.GREENHOUSE,
            date, date, "Климат", null, listOf(originalStep, originalStep.copy(templateStepId = "other")))
        val changed = original.withProduct(0, "silver_prevention", date, 5)
        assertEquals(original.steps[1], changed.steps[1])
        assertNull(changed.steps[0].recurrence)
        assertEquals(5, changed.steps[0].reminderDaysBefore)
        assertFalse(changed.steps[0].note.contains("Старая дозировка"))
        assertThrows(IllegalArgumentException::class.java) { original.withProduct(0, "silver_prevention", date.minusDays(1)) }
        val withProblem = changed.withProblemTreatment("whitefly", "boverix", date, 0)
        assertEquals(3, withProblem.steps.size)
        assertEquals(changed.steps, withProblem.steps.take(2))
        assertTrue(withProblem.steps.last().note.contains("белокрылка"))
        assertNull(withProblem.steps.last().recurrence)
    }

    @Test fun catalogueHasUniqueIdsAndSourcesAndBothCropsGetAlternatives() {
        val products = ProgramProductCatalog.products
        assertEquals(products.size, products.map { it.id }.toSet().size)
        assertTrue(products.all { (it.sourceUrl.startsWith("https://") || it.manufacturer == "Рецепт пользователя") && it.instruction.isNotBlank() })
        assertTrue(products.all { it.manufacturer in ProgramProductCatalog.allowedManufacturers || it.manufacturer == "Рецепт пользователя" })
        for (crop in listOf("tomato", "cucumber")) {
            val template = PlantCareCatalog.all().first { it.id == crop }
            assertTrue(template.steps.count { step -> ProgramProductCatalog.alternatives(crop, step.id).any { it.unavailableReason == null } } >= 3)
        }
    }

    @Test fun everyGenericProgramAllowsAddingProductsAndOptionalProblemTreatment() {
        val crops = listOf("sweet-pepper", "eggplant", "zucchini", "pumpkin", "cabbage", "carrot", "beet", "onion", "garlic")
        crops.forEach { crop ->
            assertTrue(ProgramProductCatalog.supports(crop))
            assertTrue(ProgramProductCatalog.alternatives(crop, "preventive_disease_treatment").any { it.unavailableReason == null })
            assertTrue(ProgramProductCatalog.alternatives(crop, "pest_treatment_if_needed").any { it.unavailableReason == null })
            assertTrue(ProgramProductCatalog.treatments(crop, null, CultivationType.OPEN_GROUND).any { it.unavailableReason == null })
        }
        assertTrue(ProgramProductCatalog.treatments("sweet-pepper", null, CultivationType.GREENHOUSE).isNotEmpty())
        assertTrue(ProgramProductCatalog.treatments("carrot", null, CultivationType.GREENHOUSE).isEmpty())
    }

    @Test fun changingProductsOnMovedRepeatsPreservesDistinctOccurrenceKeys() {
        val originalRepeat = plant(step = "fitosporin_spraying:remaining:1")
        assertEquals(2, ScheduledTreatment(originalRepeat, date, date, false, false).weatherLimits()?.requiredDryHoursAfter)
        val first = plant(step = "fitosporin_spraying:remaining:1").withProgramProduct("silver_prevention", date, 1)
        assertNull(ScheduledTreatment(first, date, date, false, false).weatherLimits())
        val second = plant(step = "fitosporin_spraying:remaining:2").withProgramProduct("silver_prevention", date, 1)
        assertNotEquals(first.programStepId, second.programStepId)
        val changedAgain = first.withProgramProduct("biozashchitin_prevention", date, 1)
        assertTrue(changedAgain.programStepId!!.endsWith(":remaining:1"))
        assertEquals("biozashchitin_prevention", ProgramProductCatalog.productForStep(changedAgain.programStepId)?.id)
    }
}
