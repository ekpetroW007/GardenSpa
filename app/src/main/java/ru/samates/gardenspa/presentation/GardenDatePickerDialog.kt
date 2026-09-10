package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import java.time.LocalDate
import java.time.YearMonth
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest950
import ru.samates.gardenspa.ui.theme.Leaf300

@Composable
internal fun GardenDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedDate by remember(initialDate) { mutableStateOf(initialDate) }
    var month by remember(initialDate) { mutableStateOf(YearMonth.from(initialDate)) }
    val windowHeight = LocalWindowInfo.current.containerSize.height
    val maxHeight = with(LocalDensity.current) { windowHeight.toDp() * .9f }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(
            color = Forest950,
            shape = GlassShape,
            modifier = Modifier.fillMaxWidth(.94f).heightIn(max = maxHeight)
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(title, color = Cream, style = MaterialTheme.typography.titleLarge)
                Text(selectedDate.toRussianDate(), color = Leaf300)
                Column(Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState())) {
                    MonthCalendar(
                        month = month,
                        selectedDate = selectedDate,
                        datesWithTasks = emptySet(),
                        onPreviousMonth = { month = month.minusMonths(1) },
                        onNextMonth = { month = month.plusMonths(1) },
                        onDateSelected = { selectedDate = it; month = YearMonth.from(it) },
                        showTaskIndicators = false
                    )
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecondaryAction("Отмена", onDismiss, Modifier.weight(1f))
                    PrimaryAction("Выбрать дату", { onDateSelected(selectedDate) }, Modifier.weight(1f))
                }
            }
        }
    }
}
