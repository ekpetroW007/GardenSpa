package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.samates.gardenspa.data.database.entity.ProcedureEntity
import ru.samates.gardenspa.domain.ScheduledTreatment
import ru.samates.gardenspa.domain.nextSeasonDate
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.ProceduresViewmodel

/** Completion is saved before the optional next-season question is shown. */
@Composable
internal fun rememberCompleteWorkAction(
    viewmodel: ProceduresViewmodel,
    onCompleted: (ScheduledTreatment) -> Unit = {}
): (ScheduledTreatment) -> Unit {
    val context = LocalContext.current.applicationContext
    val history by viewmodel.procedures.collectAsState()
    var completedId by rememberSaveable { mutableStateOf<Int?>(null) }
    var completing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val completed = history.firstOrNull { it.id == completedId }
    if (completed != null) {
        NextSeasonDialog(completed, saving, error,
            onDismiss = { completedId = null; error = null },
            onConfirm = {
                saving = true
                error = null
                viewmodel.scheduleNextSeason(completed.id,
                    onSaved = {
                        TreatmentReminderScheduler.refreshNow(context)
                        saving = false
                        completedId = null
                    },
                    onError = { saving = false; error = it.message ?: "Не удалось сохранить работу. Попробуйте ещё раз." })
            })
    } else if (error != null) {
        AlertDialog(onDismissRequest = { error = null }, containerColor = Forest900,
            title = { Text("Не удалось сохранить выполнение", color = Cream) },
            text = { Text(error.orEmpty(), color = Mist) },
            confirmButton = { TextButton(onClick = { error = null }) { Text("Понятно", color = Leaf300) } })
    }
    return { treatment ->
        if (!completing) {
            completing = true
            error = null
            viewmodel.markCompleted(treatment.plant.id, treatment.plant.taskName,
                treatment.originalDate, treatment.scheduledDate,
                onCompleted = { archived ->
                    completing = false
                    completedId = archived.id
                    TreatmentReminderScheduler.cancelTreatmentNotification(context, treatment.plant.id, treatment.originalDate.toString())
                    TreatmentReminderScheduler.refreshNow(context)
                    onCompleted(treatment)
                },
                onError = { completing = false; error = it.message ?: "Попробуйте отметить выполнение ещё раз." })
        }
    }
}

@Composable
internal fun NextSeasonDialog(
    procedure: ProcedureEntity,
    saving: Boolean,
    error: String?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = Forest900,
        title = { Text("Сохранить работу на следующий год?", color = Cream) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(procedure.procedureName, color = Cream)
                Text("Дата: ${procedure.nextSeasonDate().toRussianDate()}", color = Leaf300)
                Text("Запись о выполнении останется в архиве. Добавить такую же работу в план на указанную дату?", color = Mist)
                Text("Если выбрать «Нет», её можно будет сохранить на следующий сезон из архива растения.", color = Mist)
                error?.let { Text(it, color = Danger) }
            }
        },
        confirmButton = { TextButton(onClick = onConfirm, enabled = !saving) { Text(if (saving) "Сохраняем…" else "Да, сохранить", color = Leaf300) } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Нет, только в архив", color = Cream) } }
    )
}
