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

    @Query("DELETE FROM plants WHERE plant_card_id = :cardId OR ('legacy-' || id) = :cardId")
    suspend fun deleteCardRows(cardId: String)

    @Query("DELETE FROM procedure_history WHERE plant_card_id = :cardId OR plant_id IN (SELECT id FROM plants WHERE plant_card_id = :cardId)")
    suspend fun deleteCardHistory(cardId: String)

    @Transaction
    suspend fun deletePlantCard(cardId: String) {
        deleteCardHistory(cardId)
        deleteCardRows(cardId)
    }

    @Query("SELECT * FROM plants WHERE plant_card_id = :cardId ORDER BY id")
    suspend fun getPlantsForCardOnce(cardId: String): List<PlantEntity>

    @Query("SELECT COUNT(*) FROM procedure_history WHERE plant_id = :id")
    suspend fun historyCount(id: Int): Int

    @Transaction
    suspend fun replaceUnusedProgramProduct(expected: PlantEntity, replacement: PlantEntity) {
        val current = getPlantById(expected.id.toLong())
        require(current == expected) { "Карточка изменилась. Откройте выбор препарата заново." }
        require(historyCount(expected.id) == 0) {
            "У работы уже есть выполнения или переносы. Чтобы сохранить историю, оставьте её без изменений и добавьте отдельную обработку после осмотра."
        }
        require(replacement.id == expected.id && replacement.plantCardId == expected.plantCardId)
        updatePlant(replacement)
    }

    @Transaction
    suspend fun addProgramTreatment(expected: PlantEntity, treatment: PlantEntity) {
        require(getPlantById(expected.id.toLong()) == expected) {
            "Карточка изменилась или удалена. Откройте её заново."
        }
        require(treatment.id == 0 && treatment.plantCardId == expected.plantCardId.ifBlank { "legacy-${expected.id}" })
        insertPlant(treatment)
    }

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
