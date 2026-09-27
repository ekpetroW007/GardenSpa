package ru.samates.gardenspa.domain

import ru.samates.gardenspa.data.database.entity.PlantEntity

val programRepeatIntervals: List<Int?> = listOf(null, 7, 10, 14, 30)

/** An explicit interval choice repeats until the user disables it. */
fun GeneratedCareStep.withProgramRepeat(intervalDays: Int?): GeneratedCareStep {
    require(intervalDays in programRepeatIntervals)
    return copy(recurrence = intervalDays?.let { CareRecurrence(RepeatType.CUSTOM, it, null) })
}

fun PlantEntity.withProgramRepeat(intervalDays: Int?): PlantEntity {
    require(intervalDays in programRepeatIntervals)
    return copy(
        repeatType = if (intervalDays == null) RepeatType.NONE.name else RepeatType.CUSTOM.name,
        repeatInterval = intervalDays ?: 1,
        wateringInterval = intervalDays ?: 1,
        repeatDaysOfWeek = "",
        repeatEndType = RepeatEndType.NEVER.name,
        repeatEndDate = null,
        repeatCount = null,
        userLockedDate = true
    )
}

fun GeneratedCareStep.programRepeatDescription(): String {
    val repeat = recurrence ?: return "Без повтора"
    val interval = repeat.interval.coerceAtLeast(1)
    val base = when (repeat.type) {
        RepeatType.NONE -> return "Без повтора"
        RepeatType.DAILY, RepeatType.CUSTOM -> "Каждые $interval дн."
        RepeatType.WEEKLY -> "Каждые $interval нед."
        RepeatType.MONTHLY -> "Каждые $interval мес."
        RepeatType.YEARLY -> "Каждые $interval г."
    }
    return repeat.count?.let { "$base, $it раз" } ?: base
}
