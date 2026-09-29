package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
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
            onDismiss = { comment ->
                saving = true
                viewmodel.updateCompletionComment(completed.id, comment,
                    onSaved = { saving = false; completedId = null; error = null },
                    onError = { saving = false; error = it.message ?: "Не удалось сохранить комментарий" })
            },
            onConfirm = { comment ->
                saving = true
                error = null
                viewmodel.updateCompletionComment(completed.id, comment, onSaved = {
                viewmodel.scheduleNextSeason(completed.id,
                    onSaved = {
                        TreatmentReminderScheduler.refreshNow(context)
                        saving = false
                        completedId = null
                    },
                    onError = { saving = false; error = it.message ?: "Не удалось сохранить работу. Попробуйте ещё раз." })
                }, onError = { saving = false; error = it.message ?: "Не удалось сохранить комментарий" })
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
    onDismiss: (String) -> Unit,
    onConfirm: (String) -> Unit
) {
    var comment by rememberSaveable(procedure.id) { mutableStateOf(procedure.comment) }
    AlertDialog(
        onDismissRequest = { if (!saving) onDismiss(comment) },
        containerColor = Forest900,
        title = { Text("Сохранить работу на следующий год?", color = Cream) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(procedure.procedureName, color = Cream)
                Text("Дата: ${procedure.nextSeasonDate().toRussianDate()}", color = Leaf300)
                Text("Запись о выполнении останется в архиве. Добавить такую же работу в план на указанную дату?", color = Mist)
                Text("Если выбрать «Нет», её можно будет сохранить на следующий сезон из архива растения.", color = Mist)
                if (procedure.selectedVariantId != null) {
                    Text("В следующем году будут доступны все варианты и новый выбор препаратов. Выполненный вариант останется отмечен в истории.", color = Mist)
                }
                OutlinedTextField(value = comment, onValueChange = { comment = it.take(2000) },
                    label = { Text("Комментарий к выполненной работе") }, enabled = !saving,
                    minLines = 2, maxLines = 4, colors = glassTextFieldColors())
                error?.let { Text(it, color = Danger) }
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(comment) }, enabled = !saving) { Text(if (saving) "Сохраняем…" else "Да, сохранить", color = Leaf300) } },
        dismissButton = { TextButton(onClick = { onDismiss(comment) }, enabled = !saving) { Text("Нет, только в архив", color = Cream) } }
    )
}
