package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.domain.FolkFertilizerRecipe
import ru.samates.gardenspa.domain.FolkFertilizers
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.DrugsViewmodel
import ru.samates.gardenspa.viewmodel.DrugsViewmodelFactory

@Composable
fun FolkRecipes(innerPadding: PaddingValues) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as BookeeperApp
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(application.repository))
    val drugs by drugsVm.drugs.collectAsState()
    var query by remember { mutableStateOf("") }
    val filteredRecipes = FolkFertilizers.recipes.filter { recipe ->
        query.isBlank() ||
            recipe.name.contains(query, ignoreCase = true) ||
            recipe.purpose.contains(query, ignoreCase = true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Рецепты",
                    color = Cream,
                    style = MaterialTheme.typography.headlineLarge,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    "Проверенные народные рецепты и баковые смеси",
                    color = Mist
                )
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск по названию или назначению") },
                leadingIcon = { Text("⌕", color = Leaf300) },
                keyboardOptions = SentenceKeyboardOptions,
                singleLine = true,
                colors = glassTextFieldColors(),
                shape = CompactGlassShape,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filteredRecipes.isEmpty()) {
            item { EmptyGlassState("Ничего не найдено", "Измените поисковый запрос") }
        }
        items(filteredRecipes, key = FolkFertilizerRecipe::id) { recipe ->
            val added = drugs.any { it.name.equals(recipe.name, ignoreCase = true) }
            FolkRecipeCard(
                recipe = recipe,
                alreadyAdded = added,
                onAdd = {
                    drugsVm.addDrug(
                        name = recipe.name,
                        purpose = recipe.purposeForDrug(),
                        consumptionRate = recipe.consumptionRate
                    )
                }
            )
        }
    }
}

@Composable
private fun FolkRecipeCard(
    recipe: FolkFertilizerRecipe,
    alreadyAdded: Boolean,
    onAdd: () -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(recipe.name, color = Cream, style = MaterialTheme.typography.titleLarge)
            Text(recipe.purpose, color = Leaf300)
            RecipeSection("Что понадобится", recipe.ingredients)
            RecipeSection("Как приготовить", recipe.preparation)
            RecipeSection("Норма расхода", recipe.consumptionRate)
            PrimaryAction(
                text = if (alreadyAdded) "Уже добавлено" else "Добавить в «Препараты»",
                onClick = onAdd,
                modifier = Modifier.fillMaxWidth(),
                enabled = !alreadyAdded
            )
        }
    }
}

@Composable
private fun RecipeSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Mist, style = MaterialTheme.typography.labelLarge)
        Text(text, color = Cream)
    }
}
