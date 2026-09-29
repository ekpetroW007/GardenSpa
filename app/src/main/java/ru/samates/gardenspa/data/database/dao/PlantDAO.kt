package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ru.samates.gardenspa.data.database.entity.PlantEntity
import kotlinx.coroutines.flow.Flow
import ru.samates.gardenspa.data.database.entity.GardenEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.data.database.entity.ProgramInstallationEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.domain.ProgramUpdates
import ru.samates.gardenspa.domain.ProgramVariants
import ru.samates.gardenspa.domain.withProgramVariant
import ru.samates.gardenspa.domain.withProgramRepeat
import ru.samates.gardenspa.domain.programFingerprint
import ru.samates.gardenspa.domain.effectiveVariantDate
import ru.samates.gardenspa.domain.variantFollowupDateUpdates
import ru.samates.gardenspa.domain.isUnchangedVariantFollowup
import java.time.LocalDate

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

    @Query("SELECT * FROM plants WHERE program_import_key = :key")
    suspend fun getPlantByImportKey(key: String): PlantEntity?

    @Query("SELECT * FROM program_installations WHERE plant_card_id = :cardId")
    suspend fun getProgramInstallation(cardId: String): ProgramInstallationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProgramInstallation(installation: ProgramInstallationEntity)

    @Query("DELETE FROM program_installations WHERE plant_card_id = :cardId")
    suspend fun deleteProgramInstallation(cardId: String)

    @Query("SELECT * FROM procedure_history")
    suspend fun programUpdateHistory(): List<ProcedureEntity>

    @Query("SELECT * FROM garden")
    suspend fun programUpdateGardens(): List<GardenEntity>

    @Transaction
    suspend fun importProgram(installation: ProgramInstallationEntity, plants: List<PlantEntity>) {
        require(plants.all { it.plantCardId == installation.plantCardId })
        replacePlantCard(installation.plantCardId, plants)
        saveProgramInstallation(installation)
        getPlantsForCardOnce(installation.plantCardId).filter { it.selectedVariantId != null }.forEach { selected ->
            selectProgramVariant(selected.id, requireNotNull(selected.selectedVariantId), selected.variantChoices)
        }
    }

    @Transaction
    suspend fun refreshInstalledPrograms(today: LocalDate = LocalDate.now()): Int {
        val history = programUpdateHistory()
        val gardens = programUpdateGardens().associateBy { it.id }
        var changed = 0
        getAllPlantsOnce().groupBy { it.resolvedCardId }.forEach { (cardId, rows) ->
            val plan = ProgramUpdates.plan(rows, history, gardens[rows.first().gardenId], getProgramInstallation(cardId), today)
                ?: return@forEach
            plan.updates.forEach { updatePlant(it) }
            plan.removals.forEach { deletePlant(it) }
            plan.additions.forEach { insertPlant(it) }
            saveProgramInstallation(plan.installation)
            changed += plan.updates.size + plan.removals.size + plan.additions.size
        }
        return changed
    }

    @Transaction
    suspend fun selectProgramVariant(plantId: Int, variantId: String, choices: String) {
        val plant = requireNotNull(getPlantById(plantId.toLong())) { "Работа уже удалена" }
        require(completionCount(plantId) == 0) { "Работа уже выполнена. Выберите новый вариант в работе следующего сезона." }
        val selected = plant.withProgramVariant(variantId, choices).copy(userEdited = true)
        updatePlant(selected)
        val prefix = "variant-followup:${plant.id}:"
        val history = programUpdateHistory()
        val desired = ProgramVariants.followupFor(selected.copy(creationDate = selected.effectiveVariantDate(history).toString()))?.let {
            it.copy(id = 0, programImportKey = "$prefix$variantId:${it.creationDate}", userEdited = true,
                programBaseline = null, selectedVariantId = null, variantChoices = "")
        }?.let { it.copy(programBaseline = it.programFingerprint()) }
        val previous = getPlantsForCardOnce(plant.resolvedCardId).filter { it.programImportKey?.startsWith(prefix) == true }
        val existingDesired = desired?.let { target -> previous.firstOrNull { it.programImportKey == target.programImportKey }
            ?: previous.firstOrNull { it.programImportKey?.startsWith("$prefix$variantId:") == true && it.creationDate == target.creationDate } }
        val oldDefinition = ProgramVariants.followupFor(plant)
        previous.filter { it.id != existingDesired?.id && historyCount(it.id) == 0 && it.isUnchangedVariantFollowup(oldDefinition) }
            .forEach { deletePlant(it.id) }
        if (desired != null) {
            val existing = existingDesired
            if (existing == null) insertPlant(desired)
            else if (historyCount(existing.id) == 0 && existing.isUnchangedVariantFollowup(desired))
                updatePlant(desired.copy(id = existing.id, programImportKey = existing.programImportKey))
        }
    }

    @Transaction
    suspend fun updateProgramRepeat(plantId: Int, intervalDays: Int?) {
        val current = requireNotNull(getPlantById(plantId.toLong())) { "Работа изменилась или удалена. Откройте карточку заново." }
        require(current.programId != null) { "Это не работа готовой программы" }
        updatePlant(current.withProgramRepeat(intervalDays).copy(userEdited = true))
    }

    @Transaction
    suspend fun replaceImportedProgramCard(expected: List<PlantEntity>, replacement: List<PlantEntity>) {
        val cardId = expected.firstOrNull()?.resolvedCardId ?: return
        require(getPlantsForCardOnce(cardId).sortedBy { it.id } == expected.sortedBy { it.id }) {
            "Программа изменилась. Откройте редактирование заново, чтобы сохранить новые настройки."
        }
        replacePlantCard(cardId, replacement)
    }

    @Query("DELETE FROM plants WHERE id = :id ")
    suspend fun deletePlant(id: Int)

    @Query("DELETE FROM plants WHERE plant_card_id = :cardId OR ('legacy-' || id) = :cardId")
    suspend fun deleteCardRows(cardId: String)

    @Query("DELETE FROM procedure_history WHERE plant_card_id = :cardId OR plant_id IN (SELECT id FROM plants WHERE plant_card_id = :cardId)")
    suspend fun deleteCardHistory(cardId: String)

    @Transaction
    suspend fun deletePlantCard(cardId: String) {
        deleteProgramInstallation(cardId)
        deleteCardHistory(cardId)
        deleteCardRows(cardId)
    }

    @Query("SELECT * FROM plants WHERE plant_card_id = :cardId ORDER BY id")
    suspend fun getPlantsForCardOnce(cardId: String): List<PlantEntity>

    @Query("SELECT COUNT(*) FROM procedure_history WHERE plant_id = :id")
    suspend fun historyCount(id: Int): Int

    @Query("SELECT COUNT(*) FROM procedure_history WHERE plant_id = :id AND status = 'COMPLETED'")
    suspend fun completionCount(id: Int): Int

    @Transaction
    suspend fun replaceUnusedProgramProduct(expected: PlantEntity, replacement: PlantEntity) {
        val current = getPlantById(expected.id.toLong())
        require(current == expected) { "Карточка изменилась. Откройте выбор препарата заново." }
        require(historyCount(expected.id) == 0) {
            "У работы уже есть выполнения или переносы. Чтобы сохранить историю, оставьте её без изменений и добавьте отдельную обработку после осмотра."
        }
        require(replacement.id == expected.id && replacement.plantCardId == expected.plantCardId)
        updatePlant(replacement.copy(userEdited = true))
    }

    @Transaction
    suspend fun addProgramTreatment(expected: PlantEntity, treatment: PlantEntity) {
        require(getPlantById(expected.id.toLong()) == expected) {
            "Карточка изменилась или удалена. Откройте её заново."
        }
        require(treatment.id == 0 && treatment.plantCardId == expected.plantCardId.ifBlank { "legacy-${expected.id}" })
        insertPlant(treatment.copy(userEdited = true))
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
        val history = programUpdateHistory()
        plants.filter { replacement -> existing.any { it.id == replacement.id && it.creationDate != replacement.creationDate } }
            .forEach { parent ->
                variantFollowupDateUpdates(parent, getPlantsForCardOnce(cardId), history, parent.effectiveVariantDate(history))
                    .forEach { updatePlant(it) }
            }
    }

}
