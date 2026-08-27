package ru.samates.gardenspa.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime

data class GardenActivityDefinition(
    val code: String,
    val title: String,
    val met: Double,
    val description: String
)

object GardenActivities {
    const val SOURCE_URL = "https://pacompendium.com/lawn-garden/"

    val popular = listOf(
        GardenActivityDefinition("watering", "Полив сада", 4.0, "Стоя или в движении"),
        GardenActivityDefinition("weeding", "Прополка и рыхление", 3.8, "Лёгкая или умеренная нагрузка"),
        GardenActivityDefinition("planting", "Посадка растений", 4.3, "Посадка с наклонами и перемещением"),
        GardenActivityDefinition("digging", "Копка и работа лопатой", 5.0, "Копка, подготовка почвы или компоста"),
        GardenActivityDefinition("raking", "Сгребание травы и листьев", 3.8, "Работа граблями в умеренном темпе"),
        GardenActivityDefinition("mowing", "Стрижка газона", 5.0, "Ходьба с газонокосилкой"),
        GardenActivityDefinition("pruning", "Обрезка кустов и деревьев", 3.8, "Ручной секатор или ножницы"),
        GardenActivityDefinition("harvesting", "Сбор урожая", 3.0, "Сбор овощей, ягод или цветов"),
        GardenActivityDefinition("wheelbarrow", "Работа с тачкой", 4.8, "Перевозка земли, компоста или урожая"),
        GardenActivityDefinition("general", "Общие работы в саду", 3.8, "Смешанная работа умеренной интенсивности")
    )

    fun find(code: String): GardenActivityDefinition =
        popular.firstOrNull { it.code == code } ?: popular.last()
}

data class GardenWorkDraft(
    val activityCode: String,
    val minutes: Int
)

fun estimateGardenCalories(met: Double, weightKg: Double, minutes: Int): Double =
    met * weightKg * minutes.coerceAtLeast(0) / 60.0

fun estimateGardenCalories(
    work: List<GardenWorkDraft>,
    weightKg: Double
): Double = work.sumOf { item ->
    estimateGardenCalories(GardenActivities.find(item.activityCode).met, weightKg, item.minutes)
}

private val GARDEN_WORK_RESET_TIME: LocalTime = LocalTime.of(3, 0)

fun gardenWorkDate(now: LocalDateTime = LocalDateTime.now()): LocalDate =
    if (now.toLocalTime().isBefore(GARDEN_WORK_RESET_TIME)) {
        now.toLocalDate().minusDays(1)
    } else {
        now.toLocalDate()
    }

fun nextGardenWorkReset(now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
    val resetToday = now.toLocalDate().atTime(GARDEN_WORK_RESET_TIME).atZone(now.zone)
    return if (now.isBefore(resetToday)) resetToday else resetToday.plusDays(1)
}
