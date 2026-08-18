package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ru.samates.gardenspa.data.database.entity.DrugEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrugDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrug(drug: DrugEntity)

    @Update
    suspend fun updateDrug(drug: DrugEntity)

    @Query("SELECT * FROM drug")
    fun getAllDrugs(): Flow<List<DrugEntity>>

    @Query("UPDATE plants SET drugNameInPlant = 'Не выбрано' WHERE drug_id = :id")
    suspend fun clearPlantReferences(id: Int)

    @Query("DELETE FROM drug WHERE id = :id")
    suspend fun deleteDrugRow(id: Int)

    @Transaction
    suspend fun deleteDrug(id: Int) {
        clearPlantReferences(id)
        deleteDrugRow(id)
    }
}
