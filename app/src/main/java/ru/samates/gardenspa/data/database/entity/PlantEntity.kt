package ru.samates.gardenspa.data.database.entity


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "plants",
    foreignKeys = [
        ForeignKey(
            entity = DrugEntity::class,
            parentColumns = ["id"],
            childColumns = ["drug_id"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = GardenEntity::class,
            parentColumns = ["id"],
            childColumns = ["garden_id"],
            onDelete = ForeignKey.SET_NULL
        )
    ]
)
data class PlantEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "name")
    val plantName: String,

    @ColumnInfo(name = "task")
    val taskName: String,

    @ColumnInfo(name = "wateringInterval")
    val wateringInterval: Int,

    @ColumnInfo(name = "creationDate")
    val creationDate: String,

    @ColumnInfo(name = "drug_id", index = true)
    val drugId: Int?,

    @ColumnInfo(name = "garden_id", index = true)
    val gardenId: Int?,

    @ColumnInfo(name = "drugNameInPlant")
    val drugName: String,

    @ColumnInfo(name = "gardenNameInPlant")
    val gardenName: String,

    @ColumnInfo(name = "repeat_type", defaultValue = "'NONE'")
    val repeatType: String = "NONE",

    @ColumnInfo(name = "repeat_interval", defaultValue = "1")
    val repeatInterval: Int = 1,

    @ColumnInfo(name = "repeat_days_of_week", defaultValue = "''")
    val repeatDaysOfWeek: String = "",

    @ColumnInfo(name = "repeat_end_type", defaultValue = "'NEVER'")
    val repeatEndType: String = "NEVER",

    @ColumnInfo(name = "repeat_end_date")
    val repeatEndDate: String? = null,

    @ColumnInfo(name = "repeat_count")
    val repeatCount: Int? = null,

    @ColumnInfo(name = "reminder_days_before", defaultValue = "1")
    val reminderDaysBefore: Int = 1,

    @ColumnInfo(name = "plant_card_id", defaultValue = "''")
    val plantCardId: String = "",
)

val PlantEntity.resolvedCardId: String
    get() = plantCardId.ifBlank { "legacy-$id" }
