package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.resolvedCardId
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import kotlinx.coroutines.flow.Flow
import ru.samates.gardenspa.domain.nextSeasonDate
import ru.samates.gardenspa.domain.nextSeasonImportKey
import ru.samates.gardenspa.domain.nextSeasonScheduledDate
import java.time.LocalDate

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
        val snapshot = procedure.copy(
            plantCardId = plant.resolvedCardId,
            drugName = plant.drugName,
            note = procedure.note.ifBlank { plant.programNote },
            programId = plant.programId,
            programVersion = plant.programVersion,
            programStepId = plant.programStepId,
            reminderDaysBefore = plant.reminderDaysBefore
        )
        return snapshot.copy(id = insertProcedure(snapshot).toInt())
    }

    @Query("SELECT * FROM procedure_history WHERE id = :id")
    suspend fun procedureById(id: Int): ProcedureEntity?

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
        insertNextSeasonWork(current.copy(
            id = 0, taskName = archive.procedureName, creationDate = date.toString(),
            drugId = null, drugName = archive.drugName, programNote = archive.note,
            plantCardId = archive.plantCardId,
            wateringInterval = 1, repeatType = "NONE", repeatInterval = 1, repeatDaysOfWeek = "",
            repeatEndType = "NEVER", repeatEndDate = null, repeatCount = null,
            reminderDaysBefore = archive.reminderDaysBefore,
            programId = archive.programId, programVersion = archive.programVersion,
            programStepId = archive.programStepId, programImportKey = archive.nextSeasonImportKey,
            userLockedDate = true
        ))
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
