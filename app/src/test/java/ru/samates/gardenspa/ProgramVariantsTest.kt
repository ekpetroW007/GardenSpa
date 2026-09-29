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

    @Test fun springChoiceChangesFollowupAndPreservesOnlySelectedRecipe() {
        val young = work().withProgramVariant("urea_young", "")
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
        assertEquals(2, EradicationCare.autumnComponents.first().options.size)
        assertEquals(3, EradicationCare.autumnComponents.last().options.size)
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
