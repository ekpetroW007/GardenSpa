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

@Dao
interface ProcedureDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcedure(procedure: ProcedureEntity)

    @Query("SELECT * FROM plants WHERE id = :id")
    suspend fun plantForHistory(id: Int): PlantEntity?

    @Query("SELECT * FROM procedure_history WHERE plant_id = :id AND scheduled_date = :date")
    suspend fun procedureForSchedule(id: Int, date: String): ProcedureEntity?

    @Transaction
    suspend fun saveProcedureWithSnapshot(procedure: ProcedureEntity) {
        val plant = requireNotNull(plantForHistory(procedure.plantId)) { "Растение уже удалено" }
        // A repeated tap or a delayed notification must not replace an archived completion.
        if (procedureForSchedule(procedure.plantId, procedure.scheduledDate)?.status == "COMPLETED") return
        insertProcedure(procedure.copy(
            plantCardId = plant.resolvedCardId,
            drugName = plant.drugName,
            note = procedure.note.ifBlank { plant.programNote }
        ))
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
