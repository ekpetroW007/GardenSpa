package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import ru.samates.gardenspa.domain.ProgramRecipes
import ru.samates.gardenspa.domain.ProgramProductCatalog
import ru.samates.gardenspa.domain.toDrugDisplayName
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

/** A recipe shares its composition with the catalog; personal means keep their saved text. */
@Composable
internal fun ProgramProductSummary(
    stepId: String?, productName: String, note: String, workTitle: String,
    personal: Boolean = stepId.orEmpty().contains("personal:")
) {
    val savedPersonal = personal || stepId.orEmpty().contains("personal:")
    val recipe = if (savedPersonal) null else ProgramRecipes.recipeFor(stepId, productName)
    val name = recipe?.name ?: (if (savedPersonal) null else ProgramProductCatalog.productForStep(stepId)?.displayName)
        ?: productName.toDrugDisplayName()
    if (workTitle !in setOf(name, "Применить: $name", "Использовать: $name")) Text(name, color = Leaf300)
    val composition = if (savedPersonal) note else recipe?.let(ProgramRecipes::composition)
    if (!composition.isNullOrBlank()) RecipeComposition(composition)
}

@Composable
internal fun RecipeComposition(text: String, modifier: Modifier = Modifier) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var hasMore by remember(text) { mutableStateOf(false) }
    Column(modifier) {
        Text(text, color = Mist, maxLines = if (expanded) Int.MAX_VALUE else 5,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.testTag("recipe-composition"),
            onTextLayout = { if (!expanded) hasMore = it.hasVisualOverflow })
        if (hasMore || expanded) TextButton(onClick = { expanded = !expanded }) {
            Text(if (expanded) "Свернуть состав" else "Показать весь состав", color = Leaf300)
        }
    }
}
