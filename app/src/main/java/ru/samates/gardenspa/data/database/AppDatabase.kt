package ru.samates.gardenspa.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.samates.gardenspa.data.database.dao.*
import ru.samates.gardenspa.data.database.entity.*

@Database(
    entities = [
        DrugEntity::class,
        GardenEntity::class,
        PlantEntity::class,
        TaskEntity::class,
        ProcedureEntity::class,
        GardenWorkEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun drugDao(): DrugDAO
    abstract fun gardenDao(): GardenDAO
    abstract fun plantDao(): PlantDAO
    abstract fun taskDao(): TaskDAO
    abstract fun procedureDao(): ProcedureDAO
    abstract fun gardenWorkDao(): GardenWorkDAO

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "bookeper_database"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE plants ADD COLUMN repeat_type TEXT NOT NULL DEFAULT 'NONE'")
                database.execSQL("ALTER TABLE plants ADD COLUMN repeat_interval INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE plants ADD COLUMN repeat_days_of_week TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN repeat_end_type TEXT NOT NULL DEFAULT 'NEVER'")
                database.execSQL("ALTER TABLE plants ADD COLUMN repeat_end_date TEXT")
                database.execSQL("ALTER TABLE plants ADD COLUMN repeat_count INTEGER")
                database.execSQL(
                    "UPDATE plants SET repeat_type = 'DAILY', " +
                        "repeat_interval = CASE WHEN wateringInterval < 1 THEN 1 ELSE wateringInterval END"
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS procedure_history (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        plant_id INTEGER NOT NULL,
                        procedure_name TEXT NOT NULL,
                        scheduled_date TEXT NOT NULL,
                        completed_date TEXT,
                        status TEXT NOT NULL,
                        note TEXT NOT NULL,
                        FOREIGN KEY(plant_id) REFERENCES plants(id) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL("CREATE INDEX IF NOT EXISTS index_procedure_history_plant_id ON procedure_history(plant_id)")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_procedure_history_plant_id_scheduled_date ON procedure_history(plant_id, scheduled_date)")
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE procedure_history ADD COLUMN rescheduled_date TEXT")
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE plants ADD COLUMN plant_card_id TEXT NOT NULL DEFAULT ''")
                database.execSQL("UPDATE plants SET plant_card_id = 'legacy-' || id")
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS garden_work_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        work_date TEXT NOT NULL,
                        activity_code TEXT NOT NULL,
                        activity_name TEXT NOT NULL,
                        minutes INTEGER NOT NULL,
                        met REAL NOT NULL,
                        weight_kg REAL NOT NULL,
                        calories REAL NOT NULL
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_garden_work_entries_work_date " +
                        "ON garden_work_entries(work_date)"
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE plants ADD COLUMN reminder_days_before INTEGER NOT NULL DEFAULT 1"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE plants ADD COLUMN program_id TEXT")
                database.execSQL("ALTER TABLE plants ADD COLUMN program_version INTEGER")
                database.execSQL("ALTER TABLE plants ADD COLUMN program_step_id TEXT")
                database.execSQL("ALTER TABLE plants ADD COLUMN program_import_key TEXT")
                database.execSQL("ALTER TABLE plants ADD COLUMN program_note TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE plants ADD COLUMN user_locked_date INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_plants_program_import_key " +
                        "ON plants(program_import_key)"
                )
            }
        }

    }
}
