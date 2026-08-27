package ru.samates.gardenspa.data.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "garden_work_entries",
    indices = [Index(value = ["work_date"])]
)
data class GardenWorkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(name = "work_date")
    val workDate: String,

    @ColumnInfo(name = "activity_code")
    val activityCode: String,

    @ColumnInfo(name = "activity_name")
    val activityName: String,

    @ColumnInfo(name = "minutes")
    val minutes: Int,

    @ColumnInfo(name = "met")
    val met: Double,

    @ColumnInfo(name = "weight_kg")
    val weightKg: Double,

    @ColumnInfo(name = "calories")
    val calories: Double
)
