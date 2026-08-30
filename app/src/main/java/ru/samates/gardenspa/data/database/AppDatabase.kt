package ru.samates.gardenspa.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.samates.gardenspa.data.database.dao.*
import ru.samates.gardenspa.data.database.entity.*
import ru.samates.gardenspa.domain.careTitleWithoutSeasonLabel

@Database(
    entities = [
        DrugEntity::class,
        GardenEntity::class,
        PlantEntity::class,
        TaskEntity::class,
        ProcedureEntity::class,
        GardenWorkEntity::class
    ],
    version = 17,
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
                    .addMigrations(*configuredMigrations())
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

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE garden ADD COLUMN location_name TEXT")
                database.execSQL("ALTER TABLE garden ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE garden ADD COLUMN longitude REAL")
                database.execSQL("ALTER TABLE garden ADD COLUMN elevation_meters INTEGER")
                database.execSQL("ALTER TABLE garden ADD COLUMN location_source TEXT")
                database.execSQL("ALTER TABLE garden ADD COLUMN location_accuracy_km REAL")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_safe_spring_day TEXT")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_safe_autumn_day TEXT")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_frost_free_days INTEGER")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_gdd_5 REAL")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_gdd_10 REAL")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_warm_precipitation REAL")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_winter_minimum_p10 REAL")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_confidence TEXT")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_source_years INTEGER")
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_updated_at TEXT")
                database.execSQL("ALTER TABLE plants ADD COLUMN photo_uri TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                consolidatePrograms(database)
            }
        }

        /**
         * Version 9 exists in two historical shapes: the current weather branch already has
         * the target tables, while older public builds have the pre-weather schema. Detect the
         * shape instead of deleting user data or guessing from the version number alone.
         */
        private val MIGRATION_9_17 = object : Migration(9, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                if (!hasColumn(database, "garden", "location_name")) {
                    migrateLegacyDatabase(database)
                }
            }
        }

        private val MIGRATION_10_17 = legacyMigrationTo17(10)
        private val MIGRATION_11_17 = legacyMigrationTo17(11)
        private val MIGRATION_12_17 = legacyMigrationTo17(12)
        private val MIGRATION_13_17 = legacyMigrationTo17(13)
        private val MIGRATION_14_17 = legacyMigrationTo17(14)
        private val MIGRATION_15_17 = legacyMigrationTo17(15)
        private val MIGRATION_16_17 = legacyMigrationTo17(16)

        internal fun configuredMigrations(): Array<Migration> = arrayOf(
            MIGRATION_1_2,
            MIGRATION_2_3,
            MIGRATION_3_4,
            MIGRATION_4_5,
            MIGRATION_5_6,
            MIGRATION_6_7,
            MIGRATION_7_8,
            MIGRATION_8_9,
            MIGRATION_9_17,
            MIGRATION_10_17,
            MIGRATION_11_17,
            MIGRATION_12_17,
            MIGRATION_13_17,
            MIGRATION_14_17,
            MIGRATION_15_17,
            MIGRATION_16_17
        )

        private fun legacyMigrationTo17(startVersion: Int) = object : Migration(startVersion, 17) {
            override fun migrate(database: SupportSQLiteDatabase) {
                migrateLegacyDatabase(database)
            }
        }

        private fun migrateLegacyDatabase(database: SupportSQLiteDatabase) {
            database.execSQL("PRAGMA defer_foreign_keys = ON")

            database.execSQL("ALTER TABLE procedure_history RENAME TO procedure_history_legacy")
            database.execSQL("DROP INDEX IF EXISTS index_procedure_history_plant_id")
            database.execSQL("DROP INDEX IF EXISTS index_procedure_history_plant_id_scheduled_date")
            database.execSQL("ALTER TABLE plants RENAME TO plants_legacy")
            database.execSQL("DROP INDEX IF EXISTS index_plants_program_import_key")
            database.execSQL("DROP INDEX IF EXISTS index_plants_drug_id")
            database.execSQL("DROP INDEX IF EXISTS index_plants_garden_id")
            database.execSQL("ALTER TABLE garden RENAME TO garden_legacy")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS garden (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    location_name TEXT,
                    latitude REAL,
                    longitude REAL,
                    elevation_meters INTEGER,
                    location_source TEXT,
                    location_accuracy_km REAL,
                    climate_safe_spring_day TEXT,
                    climate_safe_autumn_day TEXT,
                    climate_frost_free_days INTEGER,
                    climate_gdd_5 REAL,
                    climate_gdd_10 REAL,
                    climate_warm_precipitation REAL,
                    climate_winter_minimum_p10 REAL,
                    climate_confidence TEXT,
                    climate_source_years INTEGER,
                    climate_updated_at TEXT
                )
                """.trimIndent()
            )
            database.execSQL("INSERT INTO garden (id, name) SELECT id, name FROM garden_legacy")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS plants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    task TEXT NOT NULL,
                    wateringInterval INTEGER NOT NULL,
                    creationDate TEXT NOT NULL,
                    drug_id INTEGER,
                    garden_id INTEGER,
                    drugNameInPlant TEXT NOT NULL,
                    gardenNameInPlant TEXT NOT NULL,
                    repeat_type TEXT NOT NULL DEFAULT 'NONE',
                    repeat_interval INTEGER NOT NULL DEFAULT 1,
                    repeat_days_of_week TEXT NOT NULL DEFAULT '',
                    repeat_end_type TEXT NOT NULL DEFAULT 'NEVER',
                    repeat_end_date TEXT,
                    repeat_count INTEGER,
                    reminder_days_before INTEGER NOT NULL DEFAULT 1,
                    plant_card_id TEXT NOT NULL DEFAULT '',
                    program_id TEXT,
                    program_version INTEGER,
                    program_step_id TEXT,
                    program_import_key TEXT,
                    program_note TEXT NOT NULL DEFAULT '',
                    user_locked_date INTEGER NOT NULL DEFAULT 0,
                    photo_uri TEXT,
                    FOREIGN KEY(drug_id) REFERENCES drug(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(garden_id) REFERENCES garden(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            val legacyNote = if (hasColumn(database, "plants_legacy", "plant_details")) {
                """
                CASE
                    WHEN TRIM(COALESCE(program_note, '')) = '' THEN COALESCE(plant_details, '')
                    WHEN TRIM(COALESCE(plant_details, '')) = '' THEN program_note
                    ELSE program_note || char(10) || plant_details
                END
                """.trimIndent()
            } else {
                "program_note"
            }
            database.execSQL(
                """
                INSERT INTO plants (
                    id, name, task, wateringInterval, creationDate, drug_id, garden_id,
                    drugNameInPlant, gardenNameInPlant, repeat_type, repeat_interval,
                    repeat_days_of_week, repeat_end_type, repeat_end_date, repeat_count,
                    reminder_days_before, plant_card_id, program_id, program_version,
                    program_step_id, program_import_key, program_note, user_locked_date, photo_uri
                )
                SELECT
                    id, name, task, wateringInterval, creationDate, drug_id, garden_id,
                    drugNameInPlant, gardenNameInPlant, repeat_type, repeat_interval,
                    repeat_days_of_week, repeat_end_type, repeat_end_date, repeat_count,
                    reminder_days_before, plant_card_id, program_id, program_version,
                    program_step_id, program_import_key, $legacyNote, user_locked_date, NULL
                FROM plants_legacy
                """.trimIndent()
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_plants_program_import_key " +
                    "ON plants(program_import_key)"
            )
            database.execSQL("CREATE INDEX IF NOT EXISTS index_plants_drug_id ON plants(drug_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS index_plants_garden_id ON plants(garden_id)")

            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS procedure_history (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    plant_id INTEGER NOT NULL,
                    procedure_name TEXT NOT NULL,
                    scheduled_date TEXT NOT NULL,
                    rescheduled_date TEXT,
                    completed_date TEXT,
                    status TEXT NOT NULL,
                    note TEXT NOT NULL,
                    FOREIGN KEY(plant_id) REFERENCES plants(id) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO procedure_history (
                    id, plant_id, procedure_name, scheduled_date, rescheduled_date,
                    completed_date, status, note
                )
                SELECT id, plant_id, procedure_name, scheduled_date, rescheduled_date,
                    completed_date, status, note
                FROM procedure_history_legacy
                """.trimIndent()
            )
            database.execSQL(
                "CREATE INDEX IF NOT EXISTS index_procedure_history_plant_id " +
                    "ON procedure_history(plant_id)"
            )
            database.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS index_procedure_history_plant_id_scheduled_date " +
                    "ON procedure_history(plant_id, scheduled_date)"
            )

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

            if (hasColumn(database, "plants_legacy", "plant_details")) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS legacy_plant_metadata (
                        plant_id INTEGER PRIMARY KEY NOT NULL,
                        plant_details TEXT NOT NULL,
                        reminder_offsets_minutes TEXT
                    )
                    """.trimIndent()
                )
                val offsets = if (hasColumn(database, "plants_legacy", "reminder_offsets_minutes")) {
                    "reminder_offsets_minutes"
                } else {
                    "NULL"
                }
                database.execSQL(
                    "INSERT OR REPLACE INTO legacy_plant_metadata " +
                        "(plant_id, plant_details, reminder_offsets_minutes) " +
                        "SELECT id, plant_details, $offsets FROM plants_legacy"
                )
            }

            database.execSQL("DROP TABLE procedure_history_legacy")
            database.execSQL("DROP TABLE plants_legacy")
            database.execSQL("DROP TABLE garden_legacy")
            consolidatePrograms(database)
        }

        private fun hasColumn(
            database: SupportSQLiteDatabase,
            table: String,
            column: String
        ): Boolean = database.query("PRAGMA table_info(`$table`)").use { cursor ->
            val nameColumn = cursor.getColumnIndexOrThrow("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameColumn) == column) return@use true
            }
            false
        }

        private fun consolidatePrograms(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                DELETE FROM plants
                WHERE (program_id LIKE 'strawberry-%' AND program_step_id IN ('post_harvest_pruning', 'runner_pruning'))
                   OR (program_id = 'zucchini-vining' AND program_step_id = 'pruning_training')
                   OR (program_id = 'garlic-hardneck' AND program_step_id = 'scape_pruning')
                """.trimIndent()
            )
            database.execSQL(
                "UPDATE plants SET name = 'Яблоня', program_id = 'apple' " +
                    "WHERE program_id IN ('apple-standard-rootstock', 'apple-dwarf-rootstock')"
            )
            database.execSQL(
                "UPDATE plants SET name = 'Груша', program_id = 'pear' " +
                    "WHERE program_id IN ('pear-standard-rootstock', 'pear-dwarf-rootstock')"
            )
            database.execSQL(
                "UPDATE plants SET name = 'Земляника', program_id = 'garden-strawberry' " +
                    "WHERE program_id LIKE 'strawberry-%'"
            )
            database.execSQL(
                "UPDATE plants SET name = 'Кабачок', program_id = 'zucchini' " +
                    "WHERE program_id IN ('zucchini-bush', 'zucchini-vining')"
            )
            database.execSQL(
                "UPDATE plants SET name = 'Чеснок', program_id = 'garlic' " +
                    "WHERE program_id IN ('garlic-hardneck', 'garlic-softneck')"
            )
            cleanProgramTitles(database, "plants", "task", "program_id IS NOT NULL")
            cleanProgramTitles(
                database,
                "procedure_history",
                "procedure_name",
                "plant_id IN (SELECT id FROM plants WHERE program_id IS NOT NULL)"
            )
        }

        private fun cleanProgramTitles(
            database: SupportSQLiteDatabase,
            table: String,
            titleColumn: String,
            whereClause: String
        ) {
            database.query("SELECT id, $titleColumn FROM $table WHERE $whereClause").use { cursor ->
                val idColumn = cursor.getColumnIndexOrThrow("id")
                val valueColumn = cursor.getColumnIndexOrThrow(titleColumn)
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idColumn)
                    val title = cursor.getString(valueColumn)
                    val cleaned = careTitleWithoutSeasonLabel(title)
                    if (cleaned != title) {
                        database.execSQL(
                            "UPDATE $table SET $titleColumn = ? WHERE id = ?",
                            arrayOf(cleaned, id)
                        )
                    }
                }
            }
        }

    }
}
