package ru.samates.gardenspa.domain

import java.time.LocalDate

data class PlantTaskDraft(
    val name: String,
    val startDate: LocalDate,
    val id: Int = 0
)
