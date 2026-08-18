package ru.samates.gardenspa.domain

enum class ProcedureStep { PLANT, PROGRAM, GARDEN, PROCEDURE, DRUG, REMINDERS }

fun procedureSteps(hasReadyProgram: Boolean, useReadyProgram: Boolean): List<ProcedureStep> = when {
    hasReadyProgram && useReadyProgram -> listOf(ProcedureStep.PLANT, ProcedureStep.PROGRAM, ProcedureStep.GARDEN, ProcedureStep.REMINDERS)
    hasReadyProgram -> listOf(ProcedureStep.PLANT, ProcedureStep.PROGRAM, ProcedureStep.GARDEN, ProcedureStep.PROCEDURE, ProcedureStep.DRUG, ProcedureStep.REMINDERS)
    else -> listOf(ProcedureStep.PLANT, ProcedureStep.GARDEN, ProcedureStep.PROCEDURE, ProcedureStep.DRUG, ProcedureStep.REMINDERS)
}
