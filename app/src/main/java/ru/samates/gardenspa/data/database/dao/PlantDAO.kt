package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ru.samates.gardenspa.data.database.entity.PlantEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PlantDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlant(plant: PlantEntity)

    @Update
    suspend fun updatePlant(plant: PlantEntity)

    @Query("SELECT * FROM plants")
    fun getAllPlants(): Flow<List<PlantEntity>>

    @Query("SELECT * FROM plants")
    suspend fun getAllPlantsOnce(): List<PlantEntity>

    @Query("SELECT * FROM plants WHERE id = :plantId")
    suspend fun getPlantById(plantId: Long): PlantEntity?

    @Query("DELETE FROM plants WHERE id = :id ")
    suspend fun deletePlant(id: Int)

    @Query("DELETE FROM plants WHERE plant_card_id = :cardId")
    suspend fun deletePlantCard(cardId: String)

    @Query("SELECT * FROM plants WHERE plant_card_id = :cardId ORDER BY id")
    suspend fun getPlantsForCardOnce(cardId: String): List<PlantEntity>

    @Query("UPDATE plants SET drugNameInPlant = :name WHERE drug_id = :drugId")
    suspend fun updateDrugName(drugId: Int, name: String)

    @Transaction
    suspend fun replacePlantCard(cardId: String, plants: List<PlantEntity>) {
        val existing = getPlantsForCardOnce(cardId)
        val retainedIds = plants.asSequence().map(PlantEntity::id).filter { it > 0 }.toSet()

        plants.forEach { plant ->
            if (plant.id > 0) {
                updatePlant(plant)
            } else {
                insertPlant(plant)
            }
        }
        existing.filterNot { it.id in retainedIds }.forEach { deletePlant(it.id) }
    }

}
