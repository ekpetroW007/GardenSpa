package ru.samates.gardenspa.domain

const val NO_DRUG_REQUIRED_LABEL = "Препарат не требуется"

private const val LEGACY_NO_DRUG_REQUIRED_LABEL = "Не требуется"

fun String.toDrugDisplayName(): String =
    if (trim() == LEGACY_NO_DRUG_REQUIRED_LABEL) NO_DRUG_REQUIRED_LABEL else this

fun String.toDrugDisplayText(): String {
    val displayName = toDrugDisplayName()
    return if (displayName == NO_DRUG_REQUIRED_LABEL) displayName else "Препарат: $displayName"
}
