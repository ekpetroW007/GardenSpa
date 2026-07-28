package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

enum class RepeatType { NONE, DAILY, WEEKLY, MONTHLY, YEARLY, CUSTOM }
enum class RepeatEndType { NEVER, UNTIL_DATE, COUNT }

fun PlantEntity.occursOn(date: LocalDate): Boolean {
    val start = runCatching { LocalDate.parse(creationDate) }.getOrNull() ?: return false
    if (date.isBefore(start)) return false

    val endType = runCatching { RepeatEndType.valueOf(repeatEndType) }
        .getOrDefault(RepeatEndType.NEVER)
    if (endType == RepeatEndType.UNTIL_DATE) {
        val endDate = repeatEndDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (endDate != null && date.isAfter(endDate)) return false
    }

    val type = runCatching { RepeatType.valueOf(repeatType) }.getOrDefault(RepeatType.NONE)
    val interval = repeatInterval.coerceAtLeast(1)
    val matchesRule = when (type) {
        RepeatType.NONE -> date == start
        RepeatType.DAILY -> ChronoUnit.DAYS.between(start, date) % interval == 0L
        RepeatType.WEEKLY -> {
            val selectedDays = repeatDaysOfWeek.split(',')
                .mapNotNull { value -> value.toIntOrNull()?.let(DayOfWeek::of) }
                .ifEmpty { listOf(start.dayOfWeek) }
            val startWeek = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            val dateWeek = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ChronoUnit.WEEKS.between(startWeek, dateWeek) % interval == 0L &&
                date.dayOfWeek in selectedDays
        }
        RepeatType.MONTHLY -> {
            val months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), date.withDayOfMonth(1))
            months % interval == 0L && date.dayOfMonth == start.dayOfMonth.coerceAtMost(date.lengthOfMonth())
        }
        RepeatType.YEARLY -> {
            val years = date.year - start.year
            years % interval == 0 && date.month == start.month &&
                date.dayOfMonth == start.dayOfMonth.coerceAtMost(date.lengthOfMonth())
        }
        RepeatType.CUSTOM -> ChronoUnit.DAYS.between(start, date) % interval == 0L
    }
    if (!matchesRule) return false

    val maxCount = repeatCount
    if (endType != RepeatEndType.COUNT || maxCount == null || maxCount < 1) return true
    return occurrenceNumber(start, date, type, interval, repeatDaysOfWeek) <= maxCount
}

private fun occurrenceNumber(
    start: LocalDate,
    target: LocalDate,
    type: RepeatType,
    interval: Int,
    daysOfWeek: String
): Int {
    if (type == RepeatType.NONE) return 1
    if (type == RepeatType.DAILY || type == RepeatType.CUSTOM) {
        return (ChronoUnit.DAYS.between(start, target) / interval + 1).toInt()
    }
    var count = 0
    var cursor = start
    while (!cursor.isAfter(target)) {
        val matches = when (type) {
            RepeatType.WEEKLY -> {
                val selectedDays = daysOfWeek.split(',')
                    .mapNotNull { it.toIntOrNull()?.let(DayOfWeek::of) }
                    .ifEmpty { listOf(start.dayOfWeek) }
                val startWeek = start.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val cursorWeek = cursor.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                ChronoUnit.WEEKS.between(startWeek, cursorWeek) % interval == 0L &&
                    cursor.dayOfWeek in selectedDays
            }
            RepeatType.MONTHLY -> {
                val months = ChronoUnit.MONTHS.between(start.withDayOfMonth(1), cursor.withDayOfMonth(1))
                months % interval == 0L &&
                    cursor.dayOfMonth == start.dayOfMonth.coerceAtMost(cursor.lengthOfMonth())
            }
            RepeatType.YEARLY -> {
                val years = cursor.year - start.year
                years % interval == 0 && cursor.month == start.month &&
                    cursor.dayOfMonth == start.dayOfMonth.coerceAtMost(cursor.lengthOfMonth())
            }
            RepeatType.DAILY, RepeatType.CUSTOM, RepeatType.NONE -> false
        }
        if (matches) count++
        cursor = cursor.plusDays(1)
    }
    return count
}

fun PlantEntity.recurrenceDescription(): String {
    val interval = repeatInterval.coerceAtLeast(1)
    val base = when (runCatching { RepeatType.valueOf(repeatType) }.getOrDefault(RepeatType.NONE)) {
        RepeatType.NONE -> "Без повтора"
        RepeatType.DAILY -> if (interval == 1) "Ежедневно" else "Каждые $interval дн."
        RepeatType.WEEKLY -> if (interval == 1) "Еженедельно" else "Каждые $interval нед."
        RepeatType.MONTHLY -> if (interval == 1) "Ежемесячно" else "Каждые $interval мес."
        RepeatType.YEARLY -> if (interval == 1) "Ежегодно" else "Каждые $interval г."
        RepeatType.CUSTOM -> "Каждые $interval дн."
    }
    return when (runCatching { RepeatEndType.valueOf(repeatEndType) }.getOrDefault(RepeatEndType.NEVER)) {
        RepeatEndType.NEVER -> base
        RepeatEndType.UNTIL_DATE -> "$base, до ${repeatEndDate.orEmpty()}"
        RepeatEndType.COUNT -> "$base, ${repeatCount ?: 1} раз"
    }
}
