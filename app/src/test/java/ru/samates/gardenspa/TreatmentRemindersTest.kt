package ru.samates.gardenspa

import java.time.ZoneId
import java.time.ZonedDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.ReminderUnit
import ru.samates.gardenspa.domain.customReminderMinutes
import ru.samates.gardenspa.domain.decodeReminderOffsets
import ru.samates.gardenspa.domain.encodeReminderOffsets
import ru.samates.gardenspa.domain.nextTreatmentReminderAlarms

class TreatmentRemindersTest {
    @Test
    fun customOffsetsRoundTripAndKeepMultipleNotifications() {
        assertEquals(listOf(10, 60, 1_440), decodeReminderOffsets(encodeReminderOffsets(listOf(1_440, 10, 60, 10))))
        assertEquals(180, customReminderMinutes(3, ReminderUnit.HOURS))
        assertNull(customReminderMinutes(366, ReminderUnit.DAYS))
    }

    @Test
    fun nextAlarmUsesNineAmProcedureTime() {
        val plant = PlantEntity(id = 7, plantName = "Томат", taskName = "Полив", wateringInterval = 1, creationDate = "2026-08-20", drugId = null, gardenId = null, drugName = "", gardenName = "Сад", reminderOffsetsMinutes = "30")
        val now = ZonedDateTime.of(2026, 8, 18, 12, 0, 0, 0, ZoneId.of("Europe/Moscow"))

        assertEquals(ZonedDateTime.of(2026, 8, 20, 8, 30, 0, 0, now.zone), nextTreatmentReminderAlarms(listOf(plant), emptyList(), now).single().triggerAt)
    }
}
