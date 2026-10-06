package ru.samates.gardenspa

import org.junit.Assert.*
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.*

class ProgramVariantsTest {
    private fun work(step: String = EradicationCare.EARLY_STEP) = PlantEntity(
        id = 1, plantName = "Яблоня", taskName = "Обработка", wateringInterval = 1, creationDate = "2027-04-04",
        drugId = null, gardenId = null, drugName = "", gardenName = "Сад", plantCardId = "apple-card",
        programId = "apple", programVersion = EradicationCare.PROGRAM_VERSION, programStepId = step)

    @Test fun legacySpringChoiceChangesFollowupAndPreservesOnlySelectedRecipe() {
        val young = work().copy(programVersion = 20).withProgramVariant("urea_young", "")
        assertTrue(young.drugName.contains("300 г"))
        assertEquals("2027-04-11", ProgramVariants.followupFor(young)?.creationDate)
        val mature = young.withProgramVariant("urea_mature", "")
        assertNull(ProgramVariants.followupFor(mature))
        assertFalse(mature.drugName.contains("300 г"))
        val copper = mature.withProgramVariant("copper", "copper_source=copper_young")
        assertTrue(copper.drugName.contains("100 г"))
        assertFalse(copper.programNote.contains("300 г"))
        assertFalse(copper.programNote.contains("250 мл"))
        assertEquals("2027-04-11", ProgramVariants.followupFor(copper)?.creationDate)
    }

    @Test fun autumnStoresOneComponentFromEachGroupAndResetsAllForNextSeason() {
        val selected = work(EradicationCare.AUTUMN_MIX_STEP)
            .withProgramVariant("autumn_main", "insecticide=coragen|fungicide=raek")
        assertTrue(selected.drugName.contains("Кораген"))
        assertTrue(selected.drugName.contains("Раёк"))
        assertFalse(selected.drugName.contains("Актара"))
        assertFalse(selected.drugName.contains("Скор"))
        assertFalse(selected.drugName.contains("Топаз"))
        assertTrue(selected.drugName.contains("Эпин, 1 мл"))
        val next = selected.resetProgramVariantForNextSeason()
        assertNull(next.selectedVariantId)
        assertEquals("", next.variantChoices)
        assertTrue(ProgramVariants.isVariantWork(next))
        assertTrue(EradicationCare.autumnComponents.single { it.id == "insecticide" }.options.any { it.id == "coragen" })
        assertTrue(EradicationCare.autumnComponents.single { it.id == "fungicide" }.options.any { it.id == "raek" })
    }

    @Test fun personalRecipeRoundTripsUnicodeNewlinesAndSeparatorsAndCanRestoreOriginal() {
        val variant = EradicationCare.springVariants.single { it.id == "copper" }
        val original = work().withProgramVariant(variant.id, "copper_source=bordeaux_liquid")
        val product = "Бордоская жидкость — моя дозировка = 120 мл/10 л\nПометка | для яблони № 1"
        val note = "Личная инструкция: объём = 10 л | только этот вариант.\nВторая строка: ёлка, йод, +5…+10 °C."
        val encoded = ProgramVariants.editRecipe(variant.id, original.variantChoices, product, note)
        val restored = original.withProgramVariant(variant.id, ProgramVariants.encode(ProgramVariants.choices(encoded)))

        assertTrue(ProgramVariants.hasCustomRecipe(restored.variantChoices))
        assertEquals("bordeaux_liquid", ProgramVariants.choices(restored.variantChoices)["copper_source"])
        assertEquals(product, restored.drugName)
        assertEquals(note, restored.programNote)
        assertEquals(product, ProgramVariants.selectedProduct(variant, restored.variantChoices))
        assertEquals(note, ProgramVariants.selectedNote(variant, restored.variantChoices))

        val defaultChoices = ProgramVariants.originalRecipe(restored.variantChoices)
        val reset = restored.withProgramVariant(variant.id, defaultChoices)
        assertFalse(ProgramVariants.hasCustomRecipe(defaultChoices))
        assertEquals(original.variantChoices, defaultChoices)
        assertEquals(original.drugName, reset.drugName)
        assertEquals(original.programNote, reset.programNote)
    }

    @Test fun nextSeasonClearsPersonalRecipeAndReopensCurrentCatalogVariants() {
        val selected = work().copy(programVersion = 20).withProgramVariant("copper",
            ProgramVariants.editRecipe("copper", "copper_source=bordeaux_liquid",
                "Мой состав = 100 мл | вода\n10 л", "Моя инструкция\nСохранить = да | заметка"))
        val next = selected.resetProgramVariantForNextSeason()
        val template = PlantCareCatalog.all().single { it.id == "apple" }
        val step = template.steps.single { it.id == EradicationCare.EARLY_STEP }

        assertNull(next.selectedVariantId)
        assertEquals("", next.variantChoices)
        assertFalse(ProgramVariants.hasCustomRecipe(next.variantChoices))
        assertTrue(ProgramVariants.isVariantWork(next))
        assertEquals(template.version, next.programVersion)
        assertEquals(step.productDescription.orEmpty(), next.drugName)
        assertEquals(step.note, next.programNote)
        assertTrue(ProgramVariants.hasCustomRecipe(selected.variantChoices))
    }

    @Test fun version20RowsStaySelectableAndVersion21KeepsSeparatePestWorkForEveryChoice() {
        assertTrue(EradicationCare.PROGRAM_VERSION >= 21)
        listOf(EradicationCare.EARLY_STEP, EradicationCare.AUTUMN_MIX_STEP).forEach { step ->
            assertTrue(ProgramVariants.isVariantWork(work(step).copy(programVersion = 20)))
        }
        listOf("urea_young", "urea_mature", "copper").forEach { variantId ->
            val selected = work().copy(programVersion = 21).withProgramVariant(variantId, "")
            val followup = requireNotNull(ProgramVariants.followupFor(selected))
            assertEquals("2027-04-11", followup.creationDate)
            assertEquals(EradicationCare.PEST_STEP, ProgramVariants.baseStep(followup.programStepId))
            assertNull(followup.selectedVariantId)
            assertEquals("", followup.variantChoices)
        }
    }

    @Test fun historyYearsAreScopedToPlantAndWork() {
        val history = listOf(
            ProcedureEntity(plantId = 1, plantCardId = "apple-card", procedureName = "Обработка", scheduledDate = "2026-04-04",
                completedDate = "2026-04-05", status = "COMPLETED", programStepId = EradicationCare.EARLY_STEP, selectedVariantId = "copper"),
            ProcedureEntity(plantId = 2, plantCardId = "other-card", procedureName = "Обработка", scheduledDate = "2025-04-04",
                completedDate = "2025-04-05", status = "COMPLETED", programStepId = EradicationCare.EARLY_STEP, selectedVariantId = "copper"))
        assertEquals(mapOf("copper" to listOf(2026)), ProgramVariants.completedYears(work(), history))
    }

    @Test fun protectedLegacyRowsKeepTheirOriginalSingleProductWorkflow() {
        assertFalse(ProgramVariants.isVariantWork(work().copy(programVersion = 10, userEdited = true)))
        val current = work().withProgramVariant("urea_young", "").copy(programVersion = 19)
        assertTrue(ProgramVariants.isVariantWork(current))
    }

    @Test fun activeRepeatDoesNotReplaceNextSeasonsOpenVariantChoice() {
        val selected = work().withProgramVariant("urea_mature", "").copy(repeatType = "DAILY", repeatEndType = "NEVER")
        val archive = ProcedureEntity(plantId = selected.id, procedureName = selected.taskName,
            scheduledDate = selected.creationDate, status = "COMPLETED", drugName = selected.drugName,
            note = selected.programNote, selectedVariantId = selected.selectedVariantId)
        assertNull(archive.nextSeasonScheduledDate(listOf(selected), listOf(archive), java.time.LocalDate.of(2027, 4, 4)))
    }
}
