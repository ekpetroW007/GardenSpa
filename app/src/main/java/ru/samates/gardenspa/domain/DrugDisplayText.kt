package ru.samates.gardenspa.domain

const val NO_DRUG_REQUIRED_LABEL = "Препарат не требуется"
const val NOT_SELECTED_LABEL = "Не выбрано"

private const val LEGACY_NO_DRUG_REQUIRED_LABEL = "Не требуется"
private const val LEGACY_NOT_SELECTED_LABEL = "Не выбран"

fun String.toDrugDisplayName(): String = when (trim()) {
    LEGACY_NO_DRUG_REQUIRED_LABEL -> NO_DRUG_REQUIRED_LABEL
    LEGACY_NOT_SELECTED_LABEL -> NOT_SELECTED_LABEL
    else -> this
}

fun String.toDrugDisplayText(): String {
    val displayName = toDrugDisplayName()
    return if (displayName == NO_DRUG_REQUIRED_LABEL || displayName == NOT_SELECTED_LABEL) displayName else "Препарат: $displayName"
}
