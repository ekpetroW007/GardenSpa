package ru.samates.gardenspa.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import ru.samates.gardenspa.domain.CultivationType
import ru.samates.gardenspa.domain.PRODUCT_LABEL_NOTICE
import ru.samates.gardenspa.domain.ProgramProductCatalog
import ru.samates.gardenspa.domain.PlantCareCatalog
import ru.samates.gardenspa.domain.SINGLE_PRODUCT_WORK_NOTICE
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

/** A null step opens the optional diagnosis flow, not an automatic treatment recommendation. */
@Composable
internal fun ProgramProductDialog(
    programId: String,
    stepId: String? = null,
    initialDate: LocalDate = LocalDate.now(),
    minimumDate: LocalDate = LocalDate.now(),
    initialCultivation: CultivationType? = null,
    initialReminder: Int = 1,
    saving: Boolean = false,
    error: String? = null,
    onDismiss: () -> Unit,
    onConfirm: (productId: String, problemId: String?, cultivation: CultivationType, date: LocalDate, reminder: Int) -> Unit
) {
    val afterInspection = stepId == null
    var choosingProduct by rememberSaveable { mutableStateOf(!afterInspection) }
    var problemId by rememberSaveable { mutableStateOf<String?>(null) }
    val cultivations = PlantCareCatalog.all().firstOrNull { it.id == programId }?.supportedCultivationTypes
        ?: CultivationType.entries.toSet()
    var cultivationName by rememberSaveable { mutableStateOf((initialCultivation ?: cultivations.singleOrNull())?.name) }
    val cultivation = cultivationName?.let(CultivationType::valueOf)
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    var dateText by rememberSaveable { mutableStateOf(maxOf(initialDate, minimumDate).toString()) }
    var reminder by rememberSaveable { mutableStateOf(initialReminder) }
    var confirmed by rememberSaveable { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }
    var productQuery by rememberSaveable { mutableStateOf("") }
    val scroll = rememberScrollState()
    val options = if (afterInspection) {
        cultivation?.let { ProgramProductCatalog.treatments(programId, problemId, it) }.orEmpty()
    } else ProgramProductCatalog.alternatives(programId, stepId)
    val selected = options.firstOrNull { it.id == selectedId }

    LaunchedEffect(choosingProduct, selectedId) { scroll.scrollTo(0) }

    Dialog(onDismissRequest = { if (!saving) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(10.dp), color = Forest900, shape = GlassShape) {
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    if (afterInspection) "После осмотра" else "Препараты-аналоги",
                    color = Cream, style = MaterialTheme.typography.headlineMedium
                )
                Column(Modifier.weight(1f).verticalScroll(scroll), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!choosingProduct) {
                        Text("Если вы уже подтвердили болезнь или вредителя, можно добавить отдельную обработку.", color = Mist)
                        Text("Где растёт растение?", color = Leaf300)
                        cultivations.forEach { type ->
                            ProductChoice(type.displayName, cultivation == type, !saving && initialCultivation == null) {
                                cultivationName = type.name
                                selectedId = null
                                confirmed = false
                            }
                        }
                        if (initialCultivation != null) Text("Условия выращивания взяты из выбранной программы.", color = Mist)
                        Text("Болезнь или вредитель (необязательно)", color = Leaf300)
                        ProductChoice("Не указывать / пропустить", problemId == null, !saving) { problemId = null }
                        ProgramProductCatalog.problemsFor(programId).forEach { problem ->
                            ProductChoice("${problem.kind.label}: ${problem.label}", problemId == problem.id, !saving) {
                                problemId = problem.id
                            }
                        }
                    } else if (selected == null) {
                        if (afterInspection) {
                            Text(cultivation?.displayName.orEmpty(), color = Leaf300)
                            Text(ProgramProductCatalog.problemsFor(programId).firstOrNull { it.id == problemId }?.label
                                ?: "Каталог без подбора по диагнозу", color = Mist)
                        } else {
                            Text("Выберите один вариант по задаче", color = Mist)
                        }
                        OutlinedTextField(value = productQuery, onValueChange = { productQuery = it },
                            placeholder = { Text("Название или производитель") }, singleLine = true,
                            colors = glassTextFieldColors(), modifier = Modifier.fillMaxWidth())
                        if (options.isEmpty()) {
                            Text("Для этого случая пока нет проверенных вариантов. Уточните диагноз и регламент у агронома или производителя.", color = Cream)
                        }
                        val filteredOptions = options.filter { productQuery.isBlank() || it.displayName.contains(productQuery.trim(), ignoreCase = true) }
                        if (options.isNotEmpty() && filteredOptions.isEmpty()) {
                            Text("Ничего не найдено. Измените название или производителя.", color = Mist)
                        }
                        filteredOptions.forEach { product ->
                            GlassCard(Modifier.fillMaxWidth()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(product.name, color = Cream, style = MaterialTheme.typography.titleMedium)
                                    Text(product.manufacturer, color = Leaf300)
                                    Text(product.purpose, color = Mist)
                                    if (afterInspection && problemId == null) {
                                        Text("Задачи: " + ProgramProductCatalog.problems.filter { it.id in product.problems }
                                            .joinToString { it.label }, color = Mist)
                                    }
                                    product.unavailableReason?.let { Text(it, color = Danger) }
                                    SecondaryAction("Подробнее: ${product.name}", {
                                        selectedId = product.id
                                        confirmed = false
                                    }, Modifier.fillMaxWidth(), enabled = !saving)
                                }
                            }
                        }
                    } else {
                        Text(selected.displayName, color = Leaf300, style = MaterialTheme.typography.titleLarge)
                        Text(selected.purpose, color = Cream)
                        LinkifiedText(selected.instruction, color = Cream)
                        LinkifiedText("Инструкция производителя: ${selected.sourceUrl}", color = Leaf300)
                        selected.unavailableReason?.let { Text(it, color = Danger) }
                        Text("Одна обработка · без автоматического погодного окна", color = Mist)
                        ExpandableInfo("Правила обработки", "$SINGLE_PRODUCT_WORK_NOTICE\n$PRODUCT_LABEL_NOTICE")
                        if (afterInspection && problemId == null) {
                            Text("Диагноз не указан — сверьте цель обработки", color = Danger)
                        }
                        if (selected.unavailableReason == null) {
                            Text("Не раньше ${minimumDate.toRussianDate()}", color = Mist)
                            SecondaryAction("Дата: ${LocalDate.parse(dateText).toRussianDate()}", { datePickerOpen = true },
                                Modifier.fillMaxWidth(), enabled = !saving)
                            Text("Напоминание", color = Leaf300)
                            listOf(0 to "В день процедуры", 1 to "За 1 день", 5 to "За 5 дней").forEach { (days, label) ->
                                ProductChoice(label, reminder == days, !saving) { reminder = days }
                            }
                            Row(Modifier.fillMaxWidth().clickable(enabled = !saving, role = Role.Checkbox) { confirmed = !confirmed },
                                verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = confirmed, onCheckedChange = null, enabled = !saving)
                                Text("Я сверил(а) этикетку: средство подходит для моей культуры, цели и условий", color = Cream,
                                    modifier = Modifier.weight(1f))
                            }
                        }
                    }
                    error?.let { Text(it, color = Danger) }
                }
                if (!choosingProduct) {
                    PrimaryAction("Перейти к препаратам", { choosingProduct = true }, Modifier.fillMaxWidth(),
                        enabled = cultivation != null && !saving)
                } else if (selected != null) {
                    PrimaryAction(if (saving) "Сохраняем…" else if (afterInspection) "Добавить обработку" else "Выбрать этот препарат", {
                        onConfirm(selected.id, problemId, cultivation ?: CultivationType.OPEN_GROUND, LocalDate.parse(dateText), reminder)
                    }, Modifier.fillMaxWidth(), enabled = !saving && confirmed && selected.unavailableReason == null)
                    SecondaryAction("К списку препаратов", { selectedId = null; confirmed = false }, Modifier.fillMaxWidth(), enabled = !saving)
                } else if (afterInspection) {
                    SecondaryAction("Изменить проблему или грунт", { choosingProduct = false }, Modifier.fillMaxWidth(), enabled = !saving)
                }
                SecondaryAction("Отмена", onDismiss, Modifier.fillMaxWidth(), enabled = !saving)
            }
        }
    }
    if (datePickerOpen) {
        GardenDatePickerDialog("Дата обработки", LocalDate.parse(dateText), { date ->
            dateText = maxOf(date, minimumDate).toString()
            datePickerOpen = false
        }, { datePickerOpen = false })
    }
}

@Composable
private fun ProductChoice(text: String, selected: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(enabled = enabled, role = Role.RadioButton, onClick = onClick).padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = null, enabled = enabled)
        Text(text, color = if (selected) Leaf300 else Cream, modifier = Modifier.weight(1f))
    }
}
