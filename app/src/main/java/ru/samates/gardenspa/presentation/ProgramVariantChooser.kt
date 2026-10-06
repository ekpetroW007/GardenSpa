package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import ru.samates.gardenspa.domain.EradicationCare
import ru.samates.gardenspa.domain.ProgramVariants
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

@Composable
internal fun ProgramVariantChooser(
    stepId: String?,
    selectedVariantId: String?,
    variantChoices: String,
    onSelect: (String, String) -> Unit,
    enabled: Boolean = true,
    completed: Boolean = false,
    completedYears: Map<String, List<Int>> = emptyMap(),
    onComplete: (() -> Unit)? = null
) {
    var editingRecipe by rememberSaveable(stepId, selectedVariantId) { mutableStateOf(false) }
    var editedProduct by rememberSaveable(stepId, selectedVariantId) { mutableStateOf("") }
    var editedNote by rememberSaveable(stepId, selectedVariantId) { mutableStateOf("") }
    var chooseAnother by rememberSaveable(stepId, selectedVariantId) { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ProgramVariants.baseStep(stepId) == EradicationCare.EARLY_STEP) {
            Text("Строго по спящим почкам, при устойчивой температуре +3…+5 °C и выше; учитывайте требования выбранного средства.", color = Leaf300)
            Text("Чередуйте карбамид и медь по годам, учитывая прошлые обработки и состояние сада. Выберите один вариант.", color = Mist)
        } else {
            Text(EradicationCare.AUTUMN_CONDITIONS, color = Leaf300)
        }
        EradicationCare.variantsFor(ProgramVariants.baseStep(stepId)).forEach { variant ->
            val selected = selectedVariantId == variant.id
            val customized = selected && ProgramVariants.hasCustomRecipe(variantChoices)
            var expandedManually by rememberSaveable(stepId, variant.id, selectedVariantId) { mutableStateOf(false) }
            var draftChoices by rememberSaveable(stepId, variant.id, variantChoices) {
                mutableStateOf(ProgramVariants.encode(ProgramVariants.normalizedChoices(variant.id, if (selected) variantChoices else "")))
            }
            val expanded = selected || selectedVariantId == null || expandedManually || chooseAnother
            GlassCard(Modifier.fillMaxWidth().alpha(if (selectedVariantId != null && !selected) 0.65f else 1f)
                .testTag("variant:${variant.id}")) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(variant.title, color = Cream, style = MaterialTheme.typography.titleMedium)
                    completedYears[variant.id]?.forEach { year -> Text("Выполнено в $year году", color = Leaf300) }
                    if (selectedVariantId != null && !selected) {
                        TextButton(onClick = { expandedManually = !expandedManually }) {
                            Text(if (expanded) "Свернуть" else "Развернуть", color = Leaf300)
                        }
                    }
                    if (expanded) {
                        Text(if (customized) ProgramVariants.selectedProduct(variant, variantChoices) else variant.product, color = Leaf300)
                        ExpandableInfo("Инструкция", if (customized) ProgramVariants.selectedNote(variant, variantChoices) else variant.note)
                        if (!customized) EradicationCare.componentsFor(variant.id).forEach { group ->
                            val chosenId = ProgramVariants.normalizedChoices(variant.id, draftChoices)[group.id]
                            val chosen = group.options.single { it.id == chosenId }
                            if (!completed && enabled) {
                                SelectionMenu(label = group.title, value = chosen.product, options = group.options,
                                    optionLabel = { it.product + it.unavailableReason?.let { reason -> " — $reason" }.orEmpty() },
                                    optionEnabled = { it.unavailableReason == null }, onSelected = { option ->
                                        draftChoices = ProgramVariants.encode(ProgramVariants.choices(draftChoices) + (group.id to option.id))
                                        if (selected) onSelect(variant.id, draftChoices)
                                    })
                            } else Text("${group.title}: ${chosen.product}", color = Leaf300)
                            ExpandableInfo("Применение выбранного средства", chosen.note)
                        }
                        if (variant.kind == "autumn_mix" && !customized) {
                            Text(EradicationCare.AUTUMN_FIXED_INGREDIENTS, color = Leaf300)
                            Text(EradicationCare.AUTUMN_ADJUVANT_NOTICE, color = Mist)
                        }
                        if (selected && !completed) {
                            SecondaryAction("Изменить состав и инструкцию", {
                                editedProduct = ProgramVariants.selectedProduct(variant, variantChoices)
                                editedNote = ProgramVariants.selectedNote(variant, variantChoices)
                                editingRecipe = true
                            }, Modifier.fillMaxWidth(), enabled = enabled)
                            if (customized) TextButton(onClick = {
                                onSelect(variant.id, ProgramVariants.originalRecipe(variantChoices))
                            }, enabled = enabled) { Text("Вернуть исходный состав", color = Leaf300) }
                            if (EradicationCare.variantsFor(ProgramVariants.baseStep(stepId)).size > 1) {
                                TextButton(onClick = { chooseAnother = !chooseAnother }) {
                                    Text(if (chooseAnother) "Свернуть другие варианты" else "Изменить вариант", color = Leaf300)
                                }
                            }
                        }
                        if (selected && completed) {
                            Row { TwigCheckMark(); Text("Выполнено", color = Leaf300) }
                        } else if (selected && onComplete != null) {
                            PrimaryAction("Выполнено", onComplete, Modifier.fillMaxWidth(), enabled = enabled)
                        } else if (selected) {
                            Text("Выбрано", color = Leaf300)
                        } else {
                            SecondaryAction("Выбрать", { onSelect(variant.id, draftChoices) },
                                Modifier.fillMaxWidth().testTag("choose-variant:${variant.id}"), enabled = enabled && !completed)
                        }
                    }
                }
            }
        }
    }
    if (editingRecipe && selectedVariantId != null) {
        AlertDialog(
            onDismissRequest = { editingRecipe = false }, containerColor = Forest900,
            title = { Text("Изменить выбранный вариант", color = Cream) },
            text = {
                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(editedProduct, { editedProduct = it.take(4000) },
                        label = { Text("Состав и дозировки") }, modifier = Modifier.fillMaxWidth(),
                        colors = glassTextFieldColors(), minLines = 2)
                    OutlinedTextField(editedNote, { editedNote = it.take(20000) },
                        label = { Text("Инструкция") }, modifier = Modifier.fillMaxWidth(),
                        colors = glassTextFieldColors(), minLines = 4)
                }
            },
            confirmButton = { TextButton(onClick = {
                onSelect(selectedVariantId, ProgramVariants.editRecipe(selectedVariantId, variantChoices, editedProduct, editedNote))
                editingRecipe = false
            }, enabled = enabled && editedProduct.isNotBlank() && editedNote.isNotBlank()) { Text("Сохранить", color = Leaf300) } },
            dismissButton = { TextButton(onClick = { editingRecipe = false }) { Text("Отмена", color = Mist) } }
        )
    }
}
