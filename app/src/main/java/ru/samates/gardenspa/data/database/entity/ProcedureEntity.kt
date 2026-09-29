package ru.samates.gardenspa.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "procedure_history",
    indices = [
        Index(value = ["plant_id"]),
        Index(value = ["plant_card_id"]),
        Index(value = ["plant_id", "scheduled_date"], unique = true)
    ]
)
data class ProcedureEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "plant_id")
    val plantId: Int,

    @ColumnInfo(name = "procedure_name")
    val procedureName: String,

    @ColumnInfo(name = "scheduled_date")
    val scheduledDate: String,

    @ColumnInfo(name = "rescheduled_date")
    val rescheduledDate: String? = null,

    @ColumnInfo(name = "completed_date")
    val completedDate: String? = null,

    @ColumnInfo(name = "status")
    val status: String = "PLANNED",

    @ColumnInfo(name = "note")
    val note: String = "",

    @ColumnInfo(name = "plant_card_id", defaultValue = "''")
    val plantCardId: String = "",

    @ColumnInfo(name = "drug_name", defaultValue = "''")
    val drugName: String = "",

    @ColumnInfo(name = "program_id")
    val programId: String? = null,

    @ColumnInfo(name = "program_version")
    val programVersion: Int? = null,

    @ColumnInfo(name = "program_step_id")
    val programStepId: String? = null,

    @ColumnInfo(name = "reminder_days_before", defaultValue = "1")
    val reminderDaysBefore: Int = 1,

    @ColumnInfo(name = "selected_variant_id")
    val selectedVariantId: String? = null,

    @ColumnInfo(name = "variant_choices", defaultValue = "''")
    val variantChoices: String = "",

    @ColumnInfo(name = "comment", defaultValue = "''")
    val comment: String = ""
)
