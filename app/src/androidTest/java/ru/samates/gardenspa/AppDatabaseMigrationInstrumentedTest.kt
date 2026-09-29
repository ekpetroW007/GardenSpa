package ru.samates.gardenspa

import android.content.Context
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.samates.gardenspa.data.database.AppDatabase

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationInstrumentedTest {
    @Test fun migratesVersion19WithoutLosingArchivedWorkOrUserEdits() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration19-${UUID.randomUUID()}.db"
        Room.databaseBuilder(context, AppDatabase::class.java, name).build().let { current ->
            current.openHelper.writableDatabase
            current.close()
        }
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("PRAGMA foreign_keys = OFF")
            // Remove exactly the new columns so this fixture really has the version-19 tables.
            mapOf("plants" to listOf("selected_variant_id", "variant_choices", "user_edited", "program_baseline"),
                "procedure_history" to listOf("selected_variant_id", "variant_choices", "comment")).forEach { (table, columns) ->
                val sql = db.rawQuery("SELECT sql FROM sqlite_master WHERE type = 'table' AND name = ?", arrayOf(table)).use {
                    it.moveToFirst(); it.getString(0)
                }
                val indexes = db.rawQuery("SELECT sql FROM sqlite_master WHERE type = 'index' AND tbl_name = ? AND sql IS NOT NULL", arrayOf(table)).use { cursor ->
                    buildList { while (cursor.moveToNext()) add(cursor.getString(0)) }
                }
                val oldSql = columns.fold(sql) { value, column -> value.replace(Regex(",\\s*[`\"]?$column[`\"]?\\s+[^,)]*"), "") }
                db.execSQL("DROP TABLE $table")
                db.execSQL(oldSql)
                indexes.forEach(db::execSQL)
            }
            db.execSQL("DROP TABLE program_installations")
            db.execSQL("""INSERT INTO plants(id, name, task, wateringInterval, creationDate, drugNameInPlant,
                gardenNameInPlant, plant_card_id, program_id, program_version, program_step_id, user_locked_date)
                VALUES(1, 'Яблоня', 'Личная работа', 1, '2027-04-10', 'Моё средство', 'Сад', 'card19', 'apple', 10, 'my-work', 1)""")
            db.execSQL("""INSERT INTO procedure_history(plant_id, procedure_name, scheduled_date, completed_date,
                status, note, plant_card_id, drug_name, reminder_days_before)
                VALUES(1, 'Выполненная работа', '2026-04-10', '2026-04-10', 'COMPLETED', 'Архивная норма', 'card19', 'Старое средство', 3)""")
            db.version = 19
        }
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.configuredMigrations()).build()
        try {
            val plant = migrated.plantDao().getAllPlantsOnce().single()
            val archive = migrated.procedureDao().getAllProceduresOnce().single()
            assertEquals("Личная работа", plant.taskName)
            assertTrue(plant.userEdited)
            assertNull(plant.selectedVariantId)
            assertNull(plant.programBaseline)
            assertEquals("Архивная норма", archive.note)
            assertEquals("Старое средство", archive.drugName)
            assertEquals(3, archive.reminderDaysBefore)
            assertEquals("", archive.comment)
            assertEquals(1, migrated.procedureDao().updateCompletionComment(archive.id, "Дополненный архив"))
            assertEquals("Дополненный архив", migrated.procedureDao().procedureById(archive.id)?.comment)
        } finally { migrated.close(); context.deleteDatabase(name) }
    }

    @Test fun migratesVersion18ArchiveAndCanRestoreNextSeasonWithOriginalInstructions() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration18-${UUID.randomUUID()}.db"
        Room.databaseBuilder(context, AppDatabase::class.java, name).build().let { current ->
            current.openHelper.writableDatabase
            current.close()
        }
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("DROP TABLE procedure_history")
            db.execSQL("""CREATE TABLE procedure_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, plant_id INTEGER NOT NULL,
                procedure_name TEXT NOT NULL, scheduled_date TEXT NOT NULL, rescheduled_date TEXT,
                completed_date TEXT, status TEXT NOT NULL, note TEXT NOT NULL,
                plant_card_id TEXT NOT NULL DEFAULT '', drug_name TEXT NOT NULL DEFAULT '')""")
            db.execSQL("CREATE INDEX index_procedure_history_plant_id ON procedure_history(plant_id)")
            db.execSQL("CREATE INDEX index_procedure_history_plant_card_id ON procedure_history(plant_card_id)")
            db.execSQL("CREATE UNIQUE INDEX index_procedure_history_plant_id_scheduled_date ON procedure_history(plant_id, scheduled_date)")
            db.execSQL("""INSERT INTO plants(id, name, task, wateringInterval, creationDate,
                drugNameInPlant, gardenNameInPlant, plant_card_id, program_id, program_version,
                program_step_id, reminder_days_before)
                VALUES(1, 'Яблоня', 'Новая работа', 1, '2026-05-01', 'Новый препарат', 'Сад',
                'migration-card', 'apple', 10, 'green_cone_biological', 4)""")
            db.execSQL("""INSERT INTO procedure_history(plant_id, procedure_name, scheduled_date,
                completed_date, status, note, plant_card_id, drug_name)
                VALUES(1, 'Старая работа', '2026-05-01', '2026-05-01', 'COMPLETED',
                'Старая инструкция', 'migration-card', 'Старый препарат')""")
            db.version = 18
        }
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.configuredMigrations()).build()
        try {
            val history = migrated.procedureDao().getAllProceduresOnce().single()
            assertEquals("Старый препарат", history.drugName)
            assertEquals("Старая инструкция", history.note)
            assertEquals("apple", history.programId)
            assertEquals(4, history.reminderDaysBefore)
            migrated.procedureDao().scheduleNextSeason(history.id, java.time.LocalDate.of(2026, 9, 27))
            val next = migrated.plantDao().getAllPlantsOnce().single { it.id != 1 }
            assertEquals("2027-05-01", next.creationDate)
            assertEquals("Старая работа", next.taskName)
            assertEquals("Старый препарат", next.drugName)
            assertEquals("Старая инструкция", next.programNote)
            assertEquals(4, next.reminderDaysBefore)
            assertEquals(history, migrated.procedureDao().getAllProceduresOnce().single())
        } finally { migrated.close(); context.deleteDatabase(name) }
    }

    @Test
    fun migratesPublicVersion15WithoutLosingGardenPlantOrProcedure() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val databaseName = "migration-${UUID.randomUUID()}.db"
        createVersion15Database(context, databaseName)

        val database = Room.databaseBuilder(context, AppDatabase::class.java, databaseName)
            .addMigrations(*AppDatabase.configuredMigrations())
            .build()
        try {
            val gardens = database.gardenDao().getAllGardens().first()
            val plants = database.plantDao().getAllPlantsOnce()
            val procedures = database.procedureDao().getAllProceduresOnce()

            assertEquals(1, gardens.size)
            assertEquals("Сад для миграции", gardens.single().name)
            assertNull(gardens.single().latitude)
            assertEquals(1, plants.size)
            assertEquals(1, plants.single().gardenId)
            assertTrue(plants.single().programNote.contains("Старая заметка программы"))
            assertTrue(plants.single().programNote.contains("Пользовательское описание растения"))
            assertEquals(1, procedures.size)
            assertEquals("Полив", procedures.single().procedureName)
            assertEquals("Препарат", procedures.single().drugName)
            assertEquals("card-1", procedures.single().plantCardId)
            assertEquals("UNSPECIFIED", database.drugDao().getAllDrugs().first().single().applicationMethod)
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
    }

    @Test fun migratesVersion17AndKeepsCompletedProductWhenProcedureIsDeleted() = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration17-${UUID.randomUUID()}.db"
        val current = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
        current.openHelper.writableDatabase
        current.close()
        // Recreate the two changed tables exactly as in the preceding version.
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("PRAGMA foreign_keys = OFF")
            db.execSQL("DROP TABLE procedure_history")
            db.execSQL("DROP TABLE drug")
            db.execSQL("CREATE TABLE drug (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, target TEXT NOT NULL, amount TEXT NOT NULL)")
            db.execSQL("""CREATE TABLE procedure_history (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, plant_id INTEGER NOT NULL,
                procedure_name TEXT NOT NULL, scheduled_date TEXT NOT NULL, rescheduled_date TEXT,
                completed_date TEXT, status TEXT NOT NULL, note TEXT NOT NULL,
                FOREIGN KEY(plant_id) REFERENCES plants(id) ON UPDATE NO ACTION ON DELETE CASCADE)""")
            db.execSQL("CREATE INDEX index_procedure_history_plant_id ON procedure_history(plant_id)")
            db.execSQL("CREATE UNIQUE INDEX index_procedure_history_plant_id_scheduled_date ON procedure_history(plant_id, scheduled_date)")
            db.execSQL("INSERT INTO plants(id, name, task, wateringInterval, creationDate, drugNameInPlant, gardenNameInPlant, plant_card_id) VALUES(1, 'Яблоня', 'Опрыскать', 1, '2026-05-01', 'Зелёный конус. Биологические препараты', 'Сад', 'migration-card')")
            db.execSQL("INSERT INTO procedure_history(plant_id, procedure_name, scheduled_date, completed_date, status, note) VALUES(1, 'Опрыскать', '2026-05-01', '2026-05-01', 'COMPLETED', 'Старая инструкция')")
            db.version = 17
        }
        val migrated = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(*AppDatabase.configuredMigrations()).build()
        try {
            val plant = migrated.plantDao().getAllPlantsOnce().single()
            assertTrue(plant.drugName.startsWith("Баковая смесь"))
            val archive = migrated.procedureDao().getAllProceduresOnce().single()
            assertEquals("migration-card", archive.plantCardId)
            assertEquals("Зелёный конус. Биологические препараты", archive.drugName)
            migrated.plantDao().deletePlant(plant.id)
            assertEquals(archive, migrated.procedureDao().getAllProceduresOnce().single())
        } finally { migrated.close(); context.deleteDatabase(name) }
    }

    private fun createVersion15Database(context: Context, name: String) {
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL("PRAGMA foreign_keys = ON")
            database.execSQL(
                "CREATE TABLE drug (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, target TEXT NOT NULL, amount TEXT NOT NULL)"
            )
            database.execSQL(
                "CREATE TABLE garden (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                    "name TEXT NOT NULL, climate_data TEXT NOT NULL DEFAULT '')"
            )
            database.execSQL(
                """
                CREATE TABLE plants (
                    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    name TEXT NOT NULL,
                    plant_details TEXT NOT NULL DEFAULT '',
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
                    reminder_offsets_minutes TEXT NOT NULL DEFAULT '1440',
                    plant_card_id TEXT NOT NULL DEFAULT '',
                    program_id TEXT,
                    program_version INTEGER,
                    program_step_id TEXT,
                    program_import_key TEXT,
                    program_note TEXT NOT NULL DEFAULT '',
                    user_locked_date INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(drug_id) REFERENCES drug(id) ON UPDATE NO ACTION ON DELETE SET NULL,
                    FOREIGN KEY(garden_id) REFERENCES garden(id) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            database.execSQL("CREATE UNIQUE INDEX index_plants_program_import_key ON plants(program_import_key)")
            database.execSQL("CREATE INDEX index_plants_drug_id ON plants(drug_id)")
            database.execSQL("CREATE INDEX index_plants_garden_id ON plants(garden_id)")
            database.execSQL(
                "CREATE TABLE task (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL)"
            )
            database.execSQL(
                """
                CREATE TABLE procedure_history (
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
            database.execSQL("CREATE INDEX index_procedure_history_plant_id ON procedure_history(plant_id)")
            database.execSQL(
                "CREATE UNIQUE INDEX index_procedure_history_plant_id_scheduled_date " +
                    "ON procedure_history(plant_id, scheduled_date)"
            )
            database.execSQL(
                """
                CREATE TABLE folk_recipe (
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

            database.execSQL("INSERT INTO drug (id, name, target, amount) VALUES (1, 'Препарат', 'Цель', '10 мл')")
            database.execSQL("INSERT INTO garden (id, name, climate_data) VALUES (1, 'Сад для миграции', '')")
            database.execSQL(
                """
                INSERT INTO plants (
                    id, name, plant_details, task, wateringInterval, creationDate,
                    drug_id, garden_id, drugNameInPlant, gardenNameInPlant,
                    repeat_type, repeat_interval, repeat_days_of_week, repeat_end_type,
                    reminder_days_before, reminder_offsets_minutes, plant_card_id,
                    program_note, user_locked_date
                ) VALUES (
                    1, 'Томат', 'Пользовательское описание растения', 'Полив', 1, '2026-08-30',
                    1, 1, 'Препарат', 'Сад для миграции',
                    'NONE', 1, '', 'NEVER', 1, '1440,60', 'card-1',
                    'Старая заметка программы', 0
                )
                """.trimIndent()
            )
            database.execSQL(
                """
                INSERT INTO procedure_history (
                    id, plant_id, procedure_name, scheduled_date, completed_date, status, note
                ) VALUES (1, 1, 'Полив', '2026-08-30', NULL, 'PLANNED', 'Не потерять')
                """.trimIndent()
            )
            database.version = 15
        }
    }
}
