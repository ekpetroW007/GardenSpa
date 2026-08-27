package ru.samates.gardenspa

import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.notifications.TreatmentNotificationPublisher
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.notifications.isNotificationRevisionCurrent
import ru.samates.gardenspa.notifications.shouldPublishRegularReminder

class TreatmentNotificationPublisherTest {
    private val today = LocalDate.of(2026, 8, 25)

    @Test
    fun `configured reminder is preserved while weather-only warning can be removed`() {
        assertTrue(
            TreatmentNotificationPublisher.isRegularReminder(
                treatment(date = today.plusDays(1), reminderDaysBefore = 1),
                today
            )
        )
        assertFalse(
            TreatmentNotificationPublisher.isRegularReminder(
                treatment(date = today, reminderDaysBefore = 5),
                today
            )
        )
    }

    @Test
    fun `weather warning cannot overwrite the regular reminder`() {
        assertNotEquals(
            TreatmentReminderScheduler.notificationId(1, today.toString()),
            TreatmentReminderScheduler.weatherNotificationId(1, today.toString())
        )
    }

    @Test
    fun `stale receiver snapshot cannot republish a cancelled reminder`() {
        assertFalse(
            shouldPublishRegularReminder(
                observedRevision = 3,
                currentRevision = 4,
                isStillPending = true,
                weatherWarningActive = false
            )
        )
    }

    @Test
    fun `active weather warning owns the treatment notification`() {
        assertFalse(
            shouldPublishRegularReminder(
                observedRevision = 4,
                currentRevision = 4,
                isStillPending = true,
                weatherWarningActive = true
            )
        )
        assertTrue(
            shouldPublishRegularReminder(
                observedRevision = 4,
                currentRevision = 4,
                isStillPending = true,
                weatherWarningActive = false
            )
        )
    }

    @Test
    fun `weather result is rejected when data changes during forecast loading`() {
        assertFalse(isNotificationRevisionCurrent(observedRevision = 8, currentRevision = 9))
        assertTrue(isNotificationRevisionCurrent(observedRevision = 9, currentRevision = 9))
    }

    private fun treatment(date: LocalDate, reminderDaysBefore: Int): ScheduledTreatment =
        ScheduledTreatment(
            plant = PlantEntity(
                id = 1,
                plantName = "Томат",
                taskName = "Обработка",
                wateringInterval = 1,
                creationDate = date.toString(),
                drugId = null,
                gardenId = 1,
                drugName = "",
                gardenName = "Дача",
                reminderDaysBefore = reminderDaysBefore
            ),
            originalDate = date,
            scheduledDate = date,
            completed = false,
            rescheduled = false
        )
}
