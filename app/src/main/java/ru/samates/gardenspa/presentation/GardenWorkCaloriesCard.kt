package ru.samates.gardenspa.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import kotlin.math.roundToInt
import ru.samates.gardenspa.data.database.entity.GardenWorkEntity
import ru.samates.gardenspa.domain.GardenActivities
import ru.samates.gardenspa.domain.GardenWorkDraft
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

private data class GardenWorkInput(
    val key: Long,
    val activityCode: String,
    val minutes: String
)

@Composable
fun GardenWorkCaloriesCard(
    date: LocalDate,
    entries: List<GardenWorkEntity>,
    savedWeightKg: Double,
    onSave: (Double, List<GardenWorkDraft>) -> Unit
) {
    var editingOverride by remember(date) { mutableStateOf<Boolean?>(null) }
    val editing = editingOverride ?: entries.isEmpty()
    var weightText by remember(date) { mutableStateOf(formatWeight(savedWeightKg)) }
    var rows by remember(date) {
        mutableStateOf(listOf(GardenWorkInput(1L, GardenActivities.popular.first().code, "30")))
    }

    LaunchedEffect(entries, editingOverride) {
        if (editingOverride != true && entries.isNotEmpty()) {
            val loadedWeight = entries.first().weightKg
            weightText = formatWeight(loadedWeight)
            rows = entries.mapIndexed { index, entry ->
                GardenWorkInput(
                    key = entry.id.toLong().takeIf { it != 0L } ?: index.toLong(),
                    activityCode = entry.activityCode,
                    minutes = entry.minutes.toString()
                )
            }
        }
    }

    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Сколько энергии вы потратили?", color = Cream, style = MaterialTheme.typography.titleLarge)
            if (editing) {
                Text("Чем вы сегодня занимались в саду?", color = Mist)
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { value ->
                        weightText = value.filter { it.isDigit() || it == ',' || it == '.' }.take(6)
                    },
                    label = { Text("Ваш вес, кг") },
                    supportingText = { Text("Нужен для приблизительного расчёта", color = Mist) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = glassTextFieldColors(),
                    shape = CompactGlassShape,
                    modifier = Modifier.fillMaxWidth()
                )
                rows.forEach { row ->
                    GardenWorkInputRow(
                        row = row,
                        canRemove = rows.size > 1,
                        onActivityChanged = { code ->
                            rows = rows.map { if (it.key == row.key) it.copy(activityCode = code) else it }
                        },
                        onMinutesChanged = { minutes ->
                            rows = rows.map { if (it.key == row.key) it.copy(minutes = minutes) else it }
                        },
                        onRemove = { rows = rows.filterNot { it.key == row.key } }
                    )
                }
                SecondaryAction(
                    text = "+ Добавить ещё работу",
                    onClick = {
                        val nextKey = (rows.maxOfOrNull { it.key } ?: 0L) + 1L
                        rows = rows + GardenWorkInput(nextKey, GardenActivities.popular.first().code, "30")
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                val weightKg = weightText.replace(',', '.').toDoubleOrNull()
                val drafts = rows.mapNotNull { row ->
                    row.minutes.toIntOrNull()?.takeIf { it > 0 }?.let { minutes ->
                        GardenWorkDraft(row.activityCode, minutes)
                    }
                }
                PrimaryAction(
                    text = "Рассчитать",
                    onClick = {
                        val validWeight = weightKg ?: return@PrimaryAction
                        onSave(validWeight, drafts)
                        editingOverride = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = weightKg != null && weightKg in 25.0..300.0 && drafts.size == rows.size
                )
            } else {
                val totalCalories = entries.sumOf { it.calories }.roundToInt()
                Text("≈ $totalCalories ккал", color = Leaf300, style = MaterialTheme.typography.headlineLarge)
                entries.forEach { entry ->
                    Text(
                        "${entry.activityName}: ${entry.minutes} мин · ≈ ${entry.calories.roundToInt()} ккал",
                        color = Cream
                    )
                }
                Text(
                    "Это ориентировочная оценка по MET: фактический расход зависит от темпа и особенностей организма.",
                    color = Mist,
                    style = MaterialTheme.typography.bodySmall
                )
                SecondaryAction(
                    text = "Изменить работы",
                    onClick = { editingOverride = true },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun GardenWorkInputRow(
    row: GardenWorkInput,
    canRemove: Boolean,
    onActivityChanged: (String) -> Unit,
    onMinutesChanged: (String) -> Unit,
    onRemove: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            GardenActivityMenu(
                selectedCode = row.activityCode,
                onSelected = onActivityChanged,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = row.minutes,
                onValueChange = { onMinutesChanged(it.filter(Char::isDigit).take(4)) },
                label = { Text("Минут") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = glassTextFieldColors(),
                shape = CompactGlassShape,
                modifier = Modifier.width(104.dp)
            )
        }
        if (canRemove) {
            TextButton(onClick = onRemove, modifier = Modifier.padding(start = 2.dp)) {
                Text("Удалить эту работу", color = Danger)
            }
        }
    }
}

@Composable
private fun GardenActivityMenu(
    selectedCode: String,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = GardenActivities.find(selectedCode)
    Box(modifier) {
        OutlinedTextField(
            value = selected.title,
            onValueChange = {},
            readOnly = true,
            label = { Text("Вид работы") },
            trailingIcon = { Text("⌄", color = Leaf300) },
            colors = glassTextFieldColors(),
            shape = CompactGlassShape,
            modifier = Modifier.fillMaxWidth()
        )
        Box(Modifier.matchParentSize().clickable { expanded = true })
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            GardenActivities.popular.forEach { activity ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(activity.title)
                            Text(activity.description, color = Mist, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        onSelected(activity.code)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatWeight(weightKg: Double): String =
    if (weightKg % 1.0 == 0.0) weightKg.toInt().toString() else weightKg.toString()
