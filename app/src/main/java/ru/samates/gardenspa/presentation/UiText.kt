package ru.samates.gardenspa.presentation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val russianDateFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.forLanguageTag("ru"))
private val russianDayMonthFormatter = DateTimeFormatter.ofPattern("d MMMM", Locale.forLanguageTag("ru"))

fun LocalDate.toRussianDate(includeYear: Boolean = true): String =
    format(if (includeYear) russianDateFormatter else russianDayMonthFormatter)

fun String.toRussianDateOrSelf(includeYear: Boolean = true): String =
    runCatching { LocalDate.parse(this).toRussianDate(includeYear) }.getOrDefault(this)

fun plantCountText(count: Int): String {
    val ending = when {
        count % 100 in 11..14 -> "растений"
        count % 10 == 1 -> "растение"
        count % 10 in 2..4 -> "растения"
        else -> "растений"
    }
    return "$count $ending"
}

fun gardenCountText(count: Int): String {
    val ending = when {
        count % 100 in 11..14 -> "садов"
        count % 10 == 1 -> "сад"
        count % 10 in 2..4 -> "сада"
        else -> "садов"
    }
    return "$count $ending"
}
