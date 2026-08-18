package ru.samates.gardenspa

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.samates.gardenspa.domain.ProcedureStep
import ru.samates.gardenspa.domain.procedureSteps

class ProcedureFlowTest {
    @Test
    fun manualAndReadyProgramFlowsUseRequestedOrder() {
        assertEquals(
            listOf(ProcedureStep.PLANT, ProcedureStep.GARDEN, ProcedureStep.PROCEDURE, ProcedureStep.DRUG, ProcedureStep.REMINDERS),
            procedureSteps(hasReadyProgram = false, useReadyProgram = false)
        )
        assertEquals(
            listOf(ProcedureStep.PLANT, ProcedureStep.PROGRAM, ProcedureStep.GARDEN, ProcedureStep.REMINDERS),
            procedureSteps(hasReadyProgram = true, useReadyProgram = true)
        )
    }
}
