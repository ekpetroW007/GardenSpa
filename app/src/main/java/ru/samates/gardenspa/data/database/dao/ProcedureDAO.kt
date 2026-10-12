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
import ru.samates.gardenspa.domain.occursOn

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
        val previous = procedureForSchedule(procedure.plantId, procedure.scheduledDate)
        previous?.takeIf { it.status == "COMPLETED" }?.let { return it }
        val plant = requireNotNull(plantForHistory(procedure.plantId)) { "Растение уже удалено" }
        require(procedure.status != "COMPLETED" || !ProgramVariants.isVariantWork(plant) || plant.selectedVariantId != null) {
            "Сначала выберите вариант обработки и средства"
        }
        val snapshot = procedure.copy(
            id = previous?.id ?: procedure.id,
            rescheduledDate = procedure.rescheduledDate ?: procedure.scheduledDate.takeUnless {
                plant.occursOn(LocalDate.parse(it))
            },
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
            comment = procedure.comment.trim().ifBlank { previous?.comment.orEmpty() }
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

    @Update
    suspend fun updateProcedure(procedure: ProcedureEntity)

    @Insert
    suspend fun insertRestoredWork(plant: PlantEntity): Long

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
    suspend fun undoCompletion(procedureId: Int): ProcedureEntity {
        val archive = requireNotNull(procedureById(procedureId)) { "Работа больше не найдена" }
        if (archive.status != "COMPLETED") return archive
        val original = plantForHistory(archive.plantId)
        val sameRecipe = original != null && archive.procedureName == original.taskName &&
            archive.drugName == original.drugName && archive.note == original.programNote &&
            archive.programId == original.programId && archive.programStepId == original.programStepId &&
            archive.selectedVariantId == original.selectedVariantId && archive.variantChoices == original.variantChoices
        val restored = if (sameRecipe) requireNotNull(original) else {
            val cardId = archive.plantCardId.ifBlank { original?.resolvedCardId.orEmpty() }
            val card = requireNotNull(plantsForNextSeason().firstOrNull { it.resolvedCardId == cardId }) {
                "Карточка растения уже удалена. Вернуть работу в план нельзя."
            }
            val work = card.copy(
                id = if (original == null) archive.plantId else 0,
                plantCardId = cardId, taskName = archive.procedureName, creationDate = archive.scheduledDate,
                drugId = null, drugName = archive.drugName, programNote = archive.note,
                programId = archive.programId, programVersion = archive.programVersion, programStepId = archive.programStepId,
                programImportKey = "undo-completion:${archive.id}:${archive.plantId}",
                wateringInterval = 1, repeatType = "NONE", repeatInterval = 1, repeatDaysOfWeek = "",
                repeatEndType = "NEVER", repeatEndDate = null, repeatCount = null,
                reminderDaysBefore = archive.reminderDaysBefore,
                selectedVariantId = archive.selectedVariantId, variantChoices = archive.variantChoices,
                userLockedDate = true, userEdited = true, programBaseline = null
            )
            work.copy(id = insertRestoredWork(work).toInt())
        }
        // Keep the same history id so an independently saved next season stays linked after another completion.
        // A pinned occurrence also remains visible if the owner changed the original repeat or start date.
        val planned = archive.copy(plantId = restored.id, status = "PLANNED", completedDate = null,
            rescheduledDate = archive.rescheduledDate ?: archive.scheduledDate.takeUnless {
                restored.occursOn(LocalDate.parse(it))
            })
        updateProcedure(planned)
        if (planned.scheduledDate == restored.creationDate) {
            variantFollowupDateUpdates(restored, plantsForNextSeason(), getAllProceduresOnce(),
                LocalDate.parse(planned.rescheduledDate ?: planned.scheduledDate)).forEach { updateFollowup(it) }
        }
        return planned
    }

    @Transaction
    suspend fun undoChange(plantId: Int, scheduledDate: String) {
        val record = procedureForSchedule(plantId, scheduledDate) ?: return
        if (record.status == "COMPLETED") {
            undoCompletion(record.id)
            return
        }
        // Cancelling a postponement must not discard a retained comment or its next-season identity.
        val plant = plantForHistory(plantId)
        updateProcedure(record.copy(rescheduledDate = record.scheduledDate.takeUnless {
            plant?.occursOn(LocalDate.parse(it)) == true
        }))
        plant?.takeIf { it.creationDate == scheduledDate }?.let { parent ->
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
