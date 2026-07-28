package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.samates.gardenspa.data.database.entity.DrugEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrugDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrug(drug: DrugEntity)

    @Query("SELECT * FROM drug")
    fun getAllDrugs(): Flow<List<DrugEntity>>

    @Query("DELETE FROM drug WHERE id = :id ")
    suspend fun deleteDrug(id: Int)
}