package com.example.myapplication.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.myapplication.data.database.entity.GardenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenDAO {
    @Query("DELETE FROM garden WHERE id = :id ")
    suspend fun deleteGarden(id: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarden(garden: GardenEntity)

    @Query("SELECT * FROM garden")
    fun getAllGardens(): Flow<List<GardenEntity>>
}