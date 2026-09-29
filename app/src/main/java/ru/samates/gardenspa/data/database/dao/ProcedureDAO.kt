package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import kotlinx.coroutines.flow.Flow
import ru.samates.gardenspa.domain.nextSeasonDate
import ru.samates.gardenspa.domain.nextSeasonImportKey
import ru.samates.gardenspa.domain.nextSeasonScheduledDate
import java.time.LocalDate
import ru.samates.gardenspa.domain.resetProgramVariantForNextSeason
import ru.samates.gardenspa.domain.ProgramVariants
import ru.samates.gardenspa.domain.variantFollowupDateUpdates

@Dao
interface ProcedureDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcedure(procedure: ProcedureEntity): Long

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun plantForHistory(id: Int): PlantEntity?

    @Query("SELECT * FROM procedure_history WHERE plant_id = :id AND scheduled_date = :date")
    suspend fun procedureForSchedule(id: Int, date: String): ProcedureEntity?

    @Transaction
    suspend fun saveProcedureWithSnapshot(procedure: ProcedureEntity): ProcedureEntity {
        // A repeated tap or a delayed notification must not replace an archived completion.
        procedureForSchedule(procedure.plantId, procedure.scheduledDate)?.takeIf { it.status == "COMPLETED" }
            ?.let { return it }
        val plant = requireNotNull(plantForHistory(procedure.plantId)) { "Растение уже удалено" }
        require(procedure.status != "COMPLETED" || !ProgramVariants.isVariantWork(plant) || plant.selectedVariantId != null) {
            "Сначала выберите вариант обработки и средства"
        }
        val snapshot = procedure.copy(
            procedureName = plant.taskName,
            plantCardId = plant.resolvedCardId,
            drugName = plant.drugName,
            note = procedure.note.ifBlank { plant.programNote },
            programId = plant.programId,
            programVersion = plant.programVersion,
            programStepId = plant.programStepId,
            reminderDaysBefore = plant.reminderDaysBefore,
            selectedVariantId = plant.selectedVariantId,
            variantChoices = plant.variantChoices,
            comment = procedure.comment.trim()
        )
        val saved = snapshot.copy(id = insertProcedure(snapshot).toInt())
        if (saved.status == "PLANNED" && saved.scheduledDate == plant.creationDate) {
            variantFollowupDateUpdates(plant, plantsForNextSeason(), getAllProceduresOnce(),
                LocalDate.parse(saved.rescheduledDate ?: saved.scheduledDate)).forEach { updateFollowup(it) }
        }
        return saved
    }

    @Update
    suspend fun updateFollowup(plant: PlantEntity)

    @Query("SELECT * FROM procedure_history WHERE id = :id")
    suspend fun procedureById(id: Int): ProcedureEntity?

    @Query("UPDATE procedure_history SET comment = :comment WHERE id = :id AND status = 'COMPLETED'")
    suspend fun updateCompletionComment(id: Int, comment: String): Int

    @Query("SELECT * FROM plants")
    suspend fun plantsForNextSeason(): List<PlantEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNextSeasonWork(plant: PlantEntity): Long

    @Query("SELECT * FROM plants WHERE program_import_key = :key")
    suspend fun nextSeasonWork(key: String): PlantEntity?

    @Query("DELETE FROM plants WHERE program_import_key = :key")
    suspend fun deleteNextSeasonWork(key: String)

    @Query("SELECT COUNT(*) FROM procedure_history WHERE plant_id = :plantId")
    suspend fun historyCount(plantId: Int): Int

    @Transaction
    suspend fun scheduleNextSeason(procedureId: Int, today: LocalDate = LocalDate.now()): LocalDate {
        val archive = requireNotNull(procedureById(procedureId)) { "Работа больше не найдена в архиве" }
        require(archive.status == "COMPLETED") { "Сначала отметьте работу выполненной" }
        val plants = plantsForNextSeason()
        archive.nextSeasonScheduledDate(plants, getAllProceduresOnce(), today)?.let { return it }
        val current = requireNotNull(plants.firstOrNull { it.resolvedCardId == archive.plantCardId }) {
            "Растение уже удалено. Сохранить работу на следующий сезон нельзя."
        }
        val date = archive.nextSeasonDate(today)
        val next = current.copy(
            id = 0, taskName = archive.procedureName, creationDate = date.toString(),
            drugId = null, drugName = archive.drugName, programNote = archive.note,
            plantCardId = archive.plantCardId,
            wateringInterval = 1, repeatType = "NONE", repeatInterval = 1, repeatDaysOfWeek = "",
            repeatEndType = "NEVER", repeatEndDate = null, repeatCount = null,
            reminderDaysBefore = archive.reminderDaysBefore,
            programId = archive.programId, programVersion = archive.programVersion,
            programStepId = archive.programStepId, programImportKey = archive.nextSeasonImportKey,
            userLockedDate = true, userEdited = true, programBaseline = null,
            selectedVariantId = archive.selectedVariantId, variantChoices = archive.variantChoices
        )
        insertNextSeasonWork(if (archive.selectedVariantId != null) next.resetProgramVariantForNextSeason() else next)
        return LocalDate.parse(requireNotNull(nextSeasonWork(archive.nextSeasonImportKey)).creationDate)
    }

    @Transaction
    suspend fun undoChange(plantId: Int, scheduledDate: String) {
        procedureForSchedule(plantId, scheduledDate)?.let { archive ->
            nextSeasonWork(archive.nextSeasonImportKey)?.let { next ->
                require(historyCount(next.id) == 0) {
                    "У работы следующего сезона уже есть выполнение или перенос. Сначала отмените его."
                }
                deleteNextSeasonWork(archive.nextSeasonImportKey)
            }
        }
        deleteForSchedule(plantId, scheduledDate)
        plantForHistory(plantId)?.takeIf { it.creationDate == scheduledDate }?.let { parent ->
            variantFollowupDateUpdates(parent, plantsForNextSeason(), getAllProceduresOnce(), LocalDate.parse(parent.creationDate))
                .forEach { updateFollowup(it) }
        }
    }

    @Query("SELECT * FROM procedure_history ORDER BY scheduled_date DESC")
    fun getAllProcedures(): Flow<List<ProcedureEntity>>

    @Query("SELECT * FROM procedure_history ORDER BY scheduled_date DESC")
    suspend fun getAllProceduresOnce(): List<ProcedureEntity>

    @Query("DELETE FROM procedure_history WHERE plant_id = :plantId AND scheduled_date = :scheduledDate")
    suspend fun deleteForSchedule(plantId: Int, scheduledDate: String)

    @Query("SELECT * FROM procedure_history WHERE plant_id = :plantId ORDER BY scheduled_date DESC")
    fun getProceduresForPlant(plantId: Int): Flow<List<ProcedureEntity>>
}
