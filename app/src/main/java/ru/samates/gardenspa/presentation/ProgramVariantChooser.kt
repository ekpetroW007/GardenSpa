package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (ProgramVariants.baseStep(stepId) == EradicationCare.EARLY_STEP) {
            Text("Строго по спящим почкам, при устойчивой температуре +3…+5 °C и выше; учитывайте требования выбранного средства.", color = Leaf300)
            Text("Чередуйте карбамид и медь по годам, учитывая прошлые обработки и состояние сада. Выберите один вариант.", color = Mist)
        } else {
            Text("После сбора урожая, до листопада, при температуре не ниже +15 °C. Проверьте культуру и совместимость компонентов по этикеткам.", color = Leaf300)
        }
        EradicationCare.variantsFor(ProgramVariants.baseStep(stepId)).forEach { variant ->
            val selected = selectedVariantId == variant.id
            var expandedManually by rememberSaveable(stepId, variant.id, selectedVariantId) { mutableStateOf(false) }
            var draftChoices by rememberSaveable(stepId, variant.id, variantChoices) {
                mutableStateOf(ProgramVariants.encode(ProgramVariants.normalizedChoices(variant.id, if (selected) variantChoices else "")))
            }
            val expanded = selected || selectedVariantId == null || expandedManually
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
                        Text(variant.product, color = Leaf300)
                        ExpandableInfo("Инструкция и ограничения", variant.note)
                        EradicationCare.componentsFor(variant.id).forEach { group ->
                            val chosenId = ProgramVariants.normalizedChoices(variant.id, draftChoices)[group.id]
                            val chosen = group.options.single { it.id == chosenId }
                            if (!completed && enabled) {
                                SelectionMenu(label = group.title, value = chosen.product, options = group.options,
                                    optionLabel = { it.product }, onSelected = { option ->
                                        draftChoices = ProgramVariants.encode(ProgramVariants.choices(draftChoices) + (group.id to option.id))
                                        if (selected) onSelect(variant.id, draftChoices)
                                    })
                            } else Text("${group.title}: ${chosen.product}", color = Leaf300)
                            ExpandableInfo("Применение выбранного средства", chosen.note)
                        }
                        if (variant.kind == "autumn_mix") {
                            Text(EradicationCare.AUTUMN_FIXED_INGREDIENTS, color = Leaf300)
                            Text(EradicationCare.AUTUMN_ADJUVANT_NOTICE, color = Mist)
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
}
