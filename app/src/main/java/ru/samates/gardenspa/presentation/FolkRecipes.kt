package ru.samates.gardenspa.presentation

import androidx.compose.foundation.clickable
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.samates.gardenspa.domain.FolkFertilizerRecipe
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

@Composable
fun FolkRecipeCard(recipe: FolkFertilizerRecipe, alreadyAdded: Boolean, onAdd: () -> Unit, onEdit: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(recipe.name, color = Cream, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                Text("✎", color = Leaf300, style = MaterialTheme.typography.titleLarge, modifier = Modifier.clickable(onClick = onEdit))
            }
            Text(recipe.purpose, color = Leaf300)
            RecipeSection("Что понадобится", recipe.ingredients)
            RecipeSection("Как приготовить", recipe.preparation)
            RecipeSection("Норма расхода", recipe.consumptionRate)
            if (recipe.warning.isNotBlank()) RecipeSection("Важно", recipe.warning)
            PrimaryAction(
                text = if (alreadyAdded) "Уже добавлено" else "Добавить как препарат",
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = !alreadyAdded
            )
        }
    }
}

@Composable
fun RecipeEditorDialog(
    recipe: FolkFertilizerRecipe,
    onDismiss: () -> Unit,
    onSave: (FolkFertilizerRecipe) -> Unit,
    onDelete: () -> Unit
) {
    var name by remember(recipe.id) { mutableStateOf(recipe.name) }
    var purpose by remember(recipe.id) { mutableStateOf(recipe.purpose) }
    var ingredients by remember(recipe.id) { mutableStateOf(recipe.ingredients) }
    var preparation by remember(recipe.id) { mutableStateOf(recipe.preparation) }
    var consumptionRate by remember(recipe.id) { mutableStateOf(recipe.consumptionRate) }
    var warning by remember(recipe.id) { mutableStateOf(recipe.warning) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text("Редактировать рецепт") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 520.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                RecipeField("Название", name, { name = it }, singleLine = true)
                RecipeField("Назначение", purpose, { purpose = it })
                RecipeField("Ингредиенты", ingredients, { ingredients = it })
                RecipeField("Приготовление", preparation, { preparation = it })
                RecipeField("Норма расхода", consumptionRate, { consumptionRate = it })
                RecipeField("Предупреждение", warning, { warning = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        recipe.copy(
                            name = name.trim(),
                            purpose = purpose.trim(),
                            ingredients = ingredients.trim(),
                            preparation = preparation.trim(),
                            consumptionRate = consumptionRate.trim(),
                            warning = warning.trim()
                        )
                    )
                }
            ) {
                Text("Сохранить", color = Leaf300)
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onDelete) { Text("Удалить", color = Danger) }
                TextButton(onClick = onDismiss) { Text("Отмена", color = Mist) }
            }
        }
    )
}

@Composable
private fun RecipeField(label: String, value: String, onValueChange: (String) -> Unit, singleLine: Boolean = false) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        keyboardOptions = SentenceKeyboardOptions,
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        colors = glassTextFieldColors(),
        shape = CompactGlassShape,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun RecipeSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Mist, style = MaterialTheme.typography.labelLarge)
        Text(text, color = Cream)
    }
}
