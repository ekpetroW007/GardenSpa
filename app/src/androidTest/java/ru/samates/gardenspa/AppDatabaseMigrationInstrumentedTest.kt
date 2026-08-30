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
        } finally {
            database.close()
            context.deleteDatabase(databaseName)
        }
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
