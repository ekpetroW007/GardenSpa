package ru.samates.gardenspa.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/** The catalog steps already offered for a card, including ones the owner later deleted. */
@Entity(tableName = "program_installations")
data class ProgramInstallationEntity(
    @PrimaryKey @ColumnInfo(name = "plant_card_id") val plantCardId: String,
    @ColumnInfo(name = "template_id") val templateId: String,
    @ColumnInfo(name = "applied_version") val appliedVersion: Int,
    @ColumnInfo(name = "start_date") val startDate: String,
    @ColumnInfo(name = "cultivation_type") val cultivationType: String,
    @ColumnInfo(name = "southern_region") val southernRegion: Boolean,
    @ColumnInfo(name = "known_step_ids") val knownStepIds: String,
    @ColumnInfo(name = "inferred_context", defaultValue = "0") val inferredContext: Boolean = false
)
