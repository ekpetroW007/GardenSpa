package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ru.samates.gardenspa.data.database.entity.GardenEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GardenDAO {
    @Query("UPDATE plants SET gardenNameInPlant = 'Не выбрано' WHERE garden_id = :id")
    suspend fun clearPlantReferences(id: Int)

    @Query("DELETE FROM garden WHERE id = :id")
    suspend fun deleteGardenRow(id: Int)

    @Transaction
    suspend fun deleteGarden(id: Int) {
        clearPlantReferences(id)
        deleteGardenRow(id)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGarden(garden: GardenEntity)

    @Query("UPDATE garden SET climate_data = :climateData WHERE id = :id")
    suspend fun updateClimate(id: Int, climateData: String)

    @Query("SELECT * FROM garden")
    fun getAllGardens(): Flow<List<GardenEntity>>
}
