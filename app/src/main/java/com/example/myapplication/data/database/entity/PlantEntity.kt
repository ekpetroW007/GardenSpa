package com.example.myapplication.data.database.entity


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
)