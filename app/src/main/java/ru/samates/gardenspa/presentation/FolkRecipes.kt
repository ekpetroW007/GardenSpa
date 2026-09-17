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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
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
import ru.samates.gardenspa.viewmodel.PlantsViewmodel
import ru.samates.gardenspa.viewmodel.PlantsViewmodelFactory
import ru.samates.gardenspa.domain.toPlantCards
import ru.samates.gardenspa.domain.PlantCard
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Danger
import java.time.LocalDate

@Composable
fun FolkRecipes(innerPadding: PaddingValues, tankMixes: Boolean = false, onBack: (() -> Unit)? = null) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as BookeeperApp
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(application.repository))
    val drugs by drugsVm.drugs.collectAsState()
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(application.repository))
    val plants by plantsVm.plants.collectAsState()
    var query by rememberSaveable(tankMixes) { mutableStateOf("") }
    var recipeToUse by remember { mutableStateOf<FolkFertilizerRecipe?>(null) }
    var success by remember { mutableStateOf<String?>(null) }
    val filteredRecipes = (if (tankMixes) FolkFertilizers.tankMixes else FolkFertilizers.recipes).filter { recipe ->
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
            ScreenHeader(if (tankMixes) "Баковые смеси" else "Народные рецепты",
                if (tankMixes) "Составы для совместного применения средств" else "Справочные варианты подкормок и ухода", onBack)
        }
        success?.let { message -> item { Text(message, color = Leaf300) } }
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
                onUse = { recipeToUse = recipe; success = null },
                onAdd = {
                    drugsVm.addDrug(
                        name = recipe.name,
                        purpose = recipe.purposeForDrug(),
                        consumptionRate = recipe.consumptionRate,
                        applicationMethod = if (recipe.isTankMix) "TREATMENT" else "UNSPECIFIED"
                    )
                }
            )
        }
    }
    recipeToUse?.let { recipe ->
        TankMixtureUseDialog(recipe, plants.toPlantCards(), onDismiss = { recipeToUse = null },
            onSave = { card, date, onError ->
                plantsVm.useTankMixture(card.primary, recipe, date, onSaved = {
                    TreatmentReminderScheduler.refreshNow(application)
                    success = "Добавлено в календарь: ${card.primary.plantName}, ${date.toRussianDate()}"
                    recipeToUse = null
                }, onError = onError)
            })
    }
}

@Composable
private fun FolkRecipeCard(
    recipe: FolkFertilizerRecipe,
    alreadyAdded: Boolean,
    onUse: () -> Unit,
    onAdd: () -> Unit
) {
    var expanded by remember(recipe.id) { mutableStateOf(false) }
    GlassCard(Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(recipe.name, color = Cream, style = MaterialTheme.typography.titleLarge)
            Text(recipe.purpose, color = Leaf300)
            if (recipe.isTankMix) PrimaryAction("Использовать", onUse, Modifier.fillMaxWidth())
            SecondaryAction(
                text = if (expanded) "Скрыть рецепт" else "Показать рецепт",
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            )
            if (expanded) {
                RecipeSection("Что понадобится", recipe.ingredients)
                RecipeSection("Как приготовить", recipe.preparation)
                RecipeSection("Как использовать", recipe.consumptionRate)
                RecipeSection(
                    "Меры осторожности",
                    recipe.warning.ifBlank { "Сначала проверьте состав на небольшом участке. Не смешивайте его с другими средствами без подтверждённой совместимости." }
                )
                if (recipe.sourceUrl.isNotBlank()) {
                    LinkifiedText(
                        "Источник: ${recipe.sourceName}\n${recipe.sourceUrl}",
                        color = Mist
                    )
                } else {
                    Text(recipe.sourceName, color = Mist)
                }
                Text("Справочник GardenSpa · сентябрь 2026", color = Mist)
                PrimaryAction(
                    text = if (alreadyAdded) "Уже добавлено" else "Добавить в мои средства",
                    onClick = onAdd,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !alreadyAdded
                )
            }
        }
    }
}

@Composable
private fun TankMixtureUseDialog(recipe: FolkFertilizerRecipe, plants: List<PlantCard>, onDismiss: () -> Unit,
    onSave: (PlantCard, LocalDate, (String) -> Unit) -> Unit) {
    var cardId by rememberSaveable(recipe.id) { mutableStateOf<String?>(null) }
    val selected = plants.firstOrNull { it.cardId == cardId }
    var dateText by rememberSaveable(recipe.id) { mutableStateOf(LocalDate.now().toString()) }
    var datePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, containerColor = Forest900,
        title = { Text("Использовать баковую смесь", color = Cream) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(recipe.name, color = Cream)
                if (plants.isEmpty()) Text("Сначала добавьте растение в Мой сад", color = Mist)
                else SelectionMenu("Растение", selected?.let { "${it.primary.plantName} · ${it.primary.gardenName}" } ?: "Выберите растение",
                    plants, { "${it.primary.plantName} · ${it.primary.gardenName}" }, { cardId = it.cardId })
                selected?.let { Text("${it.primary.plantName}\nСад: ${it.primary.gardenName}", color = Cream) }
                SecondaryAction("Дата: ${LocalDate.parse(dateText).toRussianDate()}", { datePicker = true },
                    Modifier.fillMaxWidth(), enabled = !saving)
                Text("Одна процедура. Перед применением сверьте состав, культуру и условия с рецептом и инструкциями средств.", color = Mist)
                error?.let { Text(it, color = Danger) }
            }
        },
        confirmButton = { TextButton(enabled = selected != null && !saving, onClick = {
            saving = true
            error = null
            onSave(requireNotNull(selected), LocalDate.parse(dateText)) { error = it; saving = false }
        }) { Text(if (saving) "Сохраняем…" else "Добавить в календарь", color = Leaf300) } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена", color = Mist) } })
    if (datePicker) GardenDatePickerDialog("Дата процедуры", LocalDate.parse(dateText), {
        if (it.isBefore(LocalDate.now())) error = "Выберите сегодняшнюю или будущую дату"
        else { dateText = it.toString(); error = null }
        datePicker = false
    }, { datePicker = false })
}

@Composable
private fun RecipeSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Mist, style = MaterialTheme.typography.labelLarge)
        Text(text, color = Cream)
    }
}
