package ru.samates.gardenspa.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import ru.samates.gardenspa.data.database.entity.GardenWorkEntity

@Dao
interface GardenWorkDAO {
    @Query("SELECT * FROM garden_work_entries ORDER BY work_date DESC, id ASC")
    fun getAllEntries(): Flow<List<GardenWorkEntity>>

    @Query("SELECT * FROM garden_work_entries WHERE work_date = :date ORDER BY id ASC")
    suspend fun getEntriesForDate(date: String): List<GardenWorkEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntries(entries: List<GardenWorkEntity>)

    @Query("DELETE FROM garden_work_entries WHERE work_date = :date")
    suspend fun deleteEntriesForDate(date: String)

    @Transaction
    suspend fun replaceEntriesForDate(date: String, entries: List<GardenWorkEntity>) {
        deleteEntriesForDate(date)
        if (entries.isNotEmpty()) insertEntries(entries)
    }
}
