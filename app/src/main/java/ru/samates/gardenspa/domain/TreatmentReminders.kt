package ru.samates.gardenspa.domain

import java.time.LocalTime
import java.time.ZonedDateTime
import ru.samates.gardenspa.data.database.entity.PlantEntity
import ru.samates.gardenspa.data.database.entity.ProcedureEntity

const val MAX_REMINDER_MINUTES = 365 * 24 * 60
val DEFAULT_PROCEDURE_TIME: LocalTime = LocalTime.of(9, 0)

enum class ReminderUnit(val minutes: Int, val title: String) {
    MINUTES(1, "минут"),
    HOURS(60, "часов"),
    DAYS(24 * 60, "дней"),
    WEEKS(7 * 24 * 60, "недель")
}

data class TreatmentReminderAlarm(
    val plantId: Int,
    val originalDate: String,
    val scheduledDate: String,
    val offsetMinutes: Int,
    val triggerAt: ZonedDateTime
)

fun encodeReminderOffsets(offsets: List<Int>): String = offsets.filter { it in 0..MAX_REMINDER_MINUTES }.distinct().sorted().joinToString(",").ifEmpty { "none" }

fun decodeReminderOffsets(value: String, legacyDaysBefore: Int = 1): List<Int> = when {
    value == "none" -> emptyList()
    value.isBlank() -> listOf(legacyDaysBefore.coerceAtLeast(0) * 24 * 60)
    else -> value.split(',').mapNotNull(String::toIntOrNull).filter { it in 0..MAX_REMINDER_MINUTES }.distinct().sorted()
}

fun customReminderMinutes(value: Int, unit: ReminderUnit): Int? {
    val minutes = value.toLong() * unit.minutes
    return if (value > 0 && minutes <= MAX_REMINDER_MINUTES) minutes.toInt() else null
}

fun reminderOffsetLabel(minutes: Int): String = when (minutes) {
    0 -> "В момент процедуры"
    10 -> "За 10 минут"
    30 -> "За 30 минут"
    60 -> "За 1 час"
    24 * 60 -> "За 1 день"
    7 * 24 * 60 -> "За 1 неделю"
    else -> {
        val unit = ReminderUnit.entries.lastOrNull { minutes % it.minutes == 0 } ?: ReminderUnit.MINUTES
        "За ${minutes / unit.minutes} ${unit.title}"
    }
}

fun nextTreatmentReminderAlarms(
    plants: List<PlantEntity>,
    procedures: List<ProcedureEntity>,
    now: ZonedDateTime = ZonedDateTime.now()
): List<TreatmentReminderAlarm> {
    val offsetsByPlant = plants.associate { it.id to decodeReminderOffsets(it.reminderOffsetsMinutes, it.reminderDaysBefore) }
    if (offsetsByPlant.values.all { it.isEmpty() }) return emptyList()
    val maxOffsetDays = (offsetsByPlant.values.flatten().maxOrNull() ?: 0) / (24 * 60) + 1
    val endDate = now.toLocalDate().plusDays((366 + maxOffsetDays).toLong())
    val alarms = mutableMapOf<Pair<Int, Int>, TreatmentReminderAlarm>()
    var date = now.toLocalDate()
    while (!date.isAfter(endDate)) {
        scheduledTreatmentsOn(plants, procedures, date).filterNot(ScheduledTreatment::completed).forEach { treatment ->
            offsetsByPlant[treatment.plant.id].orEmpty().forEach { offset ->
                val key = treatment.plant.id to offset
                val triggerAt = treatment.scheduledDate.atTime(DEFAULT_PROCEDURE_TIME).atZone(now.zone).minusMinutes(offset.toLong())
                if (triggerAt.isAfter(now) && key !in alarms) {
                    alarms[key] = TreatmentReminderAlarm(treatment.plant.id, treatment.originalDate.toString(), treatment.scheduledDate.toString(), offset, triggerAt)
                }
            }
        }
        date = date.plusDays(1)
    }
    return alarms.values.toList()
}
