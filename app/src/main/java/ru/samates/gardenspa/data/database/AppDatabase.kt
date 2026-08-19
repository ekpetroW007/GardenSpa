package ru.samates.gardenspa.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import ru.samates.gardenspa.data.database.dao.*
import ru.samates.gardenspa.data.database.entity.*
import ru.samates.gardenspa.domain.FolkFertilizerRecipe
import ru.samates.gardenspa.domain.FolkFertilizers
import ru.samates.gardenspa.domain.ReadyProgramDrugCatalog

@Database(
    entities = [
        DrugEntity::class,
        GardenEntity::class,
        PlantEntity::class,
        TaskEntity::class,
        ProcedureEntity::class,
        FolkFertilizerRecipe::class
    ],
    version = 15,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun drugDao(): DrugDAO
    abstract fun gardenDao(): GardenDAO
    abstract fun plantDao(): PlantDAO
    abstract fun taskDao(): TaskDAO
    abstract fun procedureDao(): ProcedureDAO
    abstract fun folkRecipeDao(): FolkRecipeDAO

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
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13,
                        MIGRATION_13_14,
                        MIGRATION_14_15
                    )
                    .addCallback(DEFAULT_DATA_CALLBACK)
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
                database.execSQL("ALTER TABLE plants ADD COLUMN plant_details TEXT NOT NULL DEFAULT ''")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "UPDATE plants SET repeat_end_type = 'UNTIL_DATE', repeat_end_date = date('now', '-1 day') " +
                        "WHERE program_id IS NOT NULL AND program_step_id NOT LIKE '%pruning%' " +
                        "AND drugNameInPlant IN ('Препарат не требуется', 'Не требуется')"
                )
            }
        }

        private val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE garden ADD COLUMN climate_data TEXT NOT NULL DEFAULT ''")
                database.execSQL("DROP TABLE IF EXISTS garden_work_entries")
            }
        }

        private val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE plants ADD COLUMN reminder_offsets_minutes TEXT NOT NULL DEFAULT '1440'")
                database.execSQL("UPDATE plants SET reminder_offsets_minutes = CAST(reminder_days_before * 1440 AS TEXT)")
            }
        }

        private val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                createRecipeTable(database)
                insertDefaultRecipes(database)
            }
        }

        private val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                insertRecipes(database, FolkFertilizers.previousPhotoRecipes)
            }
        }

        private val MIGRATION_13_14 = object : Migration(13, 14) {
            override fun migrate(database: SupportSQLiteDatabase) {
                (FolkFertilizers.previousPhotoRecipes + FolkFertilizers.homePhotoRecipes).map(FolkFertilizerRecipe::id).distinct().forEach { id ->
                    database.execSQL("DELETE FROM folk_recipe WHERE id = ?", arrayOf(id))
                }
                insertRecipes(database, FolkFertilizers.homePhotoRecipes)
            }
        }

        private val MIGRATION_14_15 = object : Migration(14, 15) {
            override fun migrate(database: SupportSQLiteDatabase) {
                insertDefaultDrugs(database)
            }
        }

        private val DEFAULT_DATA_CALLBACK = object : Callback() {
            override fun onCreate(database: SupportSQLiteDatabase) {
                super.onCreate(database)
                insertDefaultRecipes(database)
                insertDefaultDrugs(database)
            }
        }

        private fun createRecipeTable(database: SupportSQLiteDatabase) {
            database.execSQL(
                """
                CREATE TABLE IF NOT EXISTS folk_recipe (
                    id TEXT NOT NULL PRIMARY KEY,
                    name TEXT NOT NULL,
                    purpose TEXT NOT NULL,
                    ingredients TEXT NOT NULL,
                    preparation TEXT NOT NULL,
                    consumptionRate TEXT NOT NULL,
                    warning TEXT NOT NULL,
                    sourceName TEXT NOT NULL,
                    sourceUrl TEXT NOT NULL
                )
                """.trimIndent()
            )
        }

        private fun insertDefaultRecipes(database: SupportSQLiteDatabase) {
            insertRecipes(database, FolkFertilizers.recipes)
        }

        private fun insertDefaultDrugs(database: SupportSQLiteDatabase) {
            ReadyProgramDrugCatalog.defaultDrugs.forEach { drug ->
                database.execSQL(
                    "INSERT INTO drug (name, target, amount) SELECT ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM drug WHERE name = ? COLLATE NOCASE)",
                    arrayOf(drug.name, drug.purpose, drug.consumptionRate, drug.name)
                )
            }
        }

        private fun insertRecipes(database: SupportSQLiteDatabase, recipes: List<FolkFertilizerRecipe>) {
            recipes.forEach { recipe ->
                database.execSQL(
                    "INSERT OR IGNORE INTO folk_recipe (id, name, purpose, ingredients, preparation, consumptionRate, warning, sourceName, sourceUrl) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    arrayOf(recipe.id, recipe.name, recipe.purpose, recipe.ingredients, recipe.preparation, recipe.consumptionRate, recipe.warning, recipe.sourceName, recipe.sourceUrl)
                )
            }
        }

    }
}
