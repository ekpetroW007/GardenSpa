package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProcedureDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProcedure(procedure: ProcedureEntity)

    @Query("SELECT * FROM procedure_history ORDER BY scheduled_date DESC")
    fun getAllProcedures(): Flow<List<ProcedureEntity>>

    @Query("SELECT * FROM procedure_history ORDER BY scheduled_date DESC")
    suspend fun getAllProceduresOnce(): List<ProcedureEntity>

    @Query("SELECT * FROM procedure_history WHERE plant_id = :plantId ORDER BY scheduled_date DESC")
    fun getProceduresForPlant(plantId: Int): Flow<List<ProcedureEntity>>
}
