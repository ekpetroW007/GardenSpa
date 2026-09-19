package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import ru.samates.gardenspa.data.database.entity.DrugEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DrugDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDrug(drug: DrugEntity): Long

    @Query("SELECT * FROM drug WHERE name = :name COLLATE NOCASE AND target = :purpose AND amount = :rate AND application_method = :method LIMIT 1")
    suspend fun findSaved(name: String, purpose: String, rate: String, method: String): DrugEntity?

    @Transaction
    suspend fun saveToMyProducts(drug: DrugEntity): DrugEntity {
        val clean = drug.copy(id = 0, name = drug.name.trim(), purpose = drug.purpose.trim(), consumptionRate = drug.consumptionRate.trim())
        require(clean.name.isNotBlank()) { "Укажите название средства" }
        return findSaved(clean.name, clean.purpose, clean.consumptionRate, clean.applicationMethod)
            ?: clean.copy(id = insertDrug(clean).toInt())
    }

    @Update
    suspend fun updateDrug(drug: DrugEntity)

    @Query("SELECT * FROM drug")
    fun getAllDrugs(): Flow<List<DrugEntity>>

    @Query("DELETE FROM drug WHERE id = :id ")
    suspend fun deleteDrug(id: Int)
}
