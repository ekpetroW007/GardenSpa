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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import kotlinx.coroutines.flow.flowOf
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.domain.FolkFertilizerRecipe
import ru.samates.gardenspa.domain.FolkFertilizers
import ru.samates.gardenspa.domain.EradicationCare
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
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Forest900
import ru.samates.gardenspa.ui.theme.Danger
import java.time.LocalDate

@Composable
fun FolkRecipes(innerPadding: PaddingValues, tankMixes: Boolean = false, onBack: (() -> Unit)? = null,
    navController: NavController? = null) {
    val application = androidx.compose.ui.platform.LocalContext.current.applicationContext as BookeeperApp
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(application.repository))
    val drugs by drugsVm.drugs.collectAsState()
    val plantsVm: PlantsViewmodel = viewModel(factory = PlantsViewmodelFactory(application.repository))
    val plants by plantsVm.plants.collectAsState()
    var query by rememberSaveable(tankMixes) { mutableStateOf("") }
    var recipeToUseId by rememberSaveable(tankMixes) { mutableStateOf<String?>(null) }
    val recipeToUse = FolkFertilizers.tankMixes.firstOrNull { it.id == recipeToUseId }
    var selectedCardId by rememberSaveable(recipeToUseId) { mutableStateOf<String?>(null) }
    var selectedDateText by rememberSaveable(recipeToUseId) { mutableStateOf<String?>(null) }
    val parentEntry = remember(navController) { navController?.currentBackStackEntry }
    val createdCardId by (parentEntry?.savedStateHandle?.getStateFlow(AppDestinations.CREATED_PLANT_CARD_RESULT, "")
        ?: flowOf("")).collectAsState("")
    LaunchedEffect(createdCardId) {
        if (createdCardId.isNotBlank()) {
            if (recipeToUseId != null) selectedCardId = createdCardId
            parentEntry?.savedStateHandle?.set(AppDestinations.CREATED_PLANT_CARD_RESULT, "")
        }
    }
    var success by remember { mutableStateOf<String?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
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
        saveError?.let { message -> item { Text(message, color = Danger) } }
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
                onUse = {
                    parentEntry?.savedStateHandle?.set(AppDestinations.CREATED_PLANT_CARD_RESULT, "")
                    recipeToUseId = recipe.id
                    success = null
                },
                onAdd = {
                    drugsVm.addDrug(
                        name = recipe.name,
                        purpose = recipe.purposeForDrug(),
                        consumptionRate = recipe.consumptionRate,
                        applicationMethod = if (recipe.isTankMix) "TREATMENT" else "UNSPECIFIED",
                        onSaved = { success = "Сохранено в «Мои средства»: ${recipe.name}"; saveError = null },
                        onError = { saveError = it }
                    )
                }
            )
        }
    }
    recipeToUse?.let { recipe ->
        TankMixtureUseDialog(recipe, plants.toPlantCards(), selectedCardId, selectedDateText,
            onCardSelected = { selectedCardId = it }, onDateSelected = { selectedDateText = it.toString() },
            onAddPlant = navController?.let { navigation -> {
                parentEntry?.savedStateHandle?.set(AppDestinations.CREATED_PLANT_CARD_RESULT, "")
                navigation.navigate(AppDestinations.plantAdd(selectedDateText ?: LocalDate.now().toString()))
            } },
            onDismiss = { recipeToUseId = null },
            onSave = { card, date, onError ->
                plantsVm.useTankMixture(card.primary, recipe, date, onSaved = {
                    TreatmentReminderScheduler.refreshNow(application)
                    success = "Добавлено в календарь: ${card.primary.plantName}, ${date.toRussianDate()}"
                    recipeToUseId = null
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
    val exactRecipe = recipe.id in setOf(EradicationCare.AUTUMN_RECIPE_ID, EradicationCare.IRON_RECIPE_ID)
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
                if (!exactRecipe || recipe.ingredients.isNotBlank()) RecipeSection("Что понадобится", recipe.ingredients)
                if (!exactRecipe || recipe.preparation.isNotBlank()) RecipeSection("Как приготовить", recipe.preparation)
                if (!exactRecipe || recipe.consumptionRate.isNotBlank()) RecipeSection("Как использовать", recipe.consumptionRate)
                if (!exactRecipe || recipe.warning.isNotBlank()) RecipeSection(
                    "Меры осторожности",
                    recipe.warning.ifBlank { "Сначала проверьте состав на небольшом участке. Не смешивайте его с другими средствами без подтверждённой совместимости." }
                )
                if (!exactRecipe && recipe.sourceUrl.isNotBlank()) {
                    LinkifiedText(
                        "Источник: ${recipe.sourceName}\n${recipe.sourceUrl}",
                        color = Mist
                    )
                } else if (!exactRecipe) {
                    Text(recipe.sourceName, color = Mist)
                }
                if (!exactRecipe) Text("Справочник GardenSpa · сентябрь 2026", color = Mist)
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
private fun TankMixtureUseDialog(recipe: FolkFertilizerRecipe, plants: List<PlantCard>, cardId: String?, dateText: String?,
    onCardSelected: (String) -> Unit, onDateSelected: (LocalDate) -> Unit, onAddPlant: (() -> Unit)?, onDismiss: () -> Unit,
    onSave: (PlantCard, LocalDate, (String) -> Unit) -> Unit) {
    val selected = plants.firstOrNull { it.cardId == cardId }
    val selectedDate = dateText?.let(LocalDate::parse)
    var datePicker by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = { if (!saving) onDismiss() }, containerColor = Forest900,
        title = { Text("Использовать баковую смесь", color = Cream) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(recipe.name, color = Cream)
                SelectionMenu("Растение", selected?.let { "${it.primary.plantName} · ${it.primary.gardenName}" }.orEmpty(),
                    plants, { "${it.primary.plantName} · ${it.primary.gardenName}" }, { if (!saving) onCardSelected(it.cardId) },
                    addActionLabel = "+ Растение", onAddAction = onAddPlant?.let { add -> { if (!saving) add() } }, addActionFirst = true,
                    optionEnabled = { !saving })
                selected?.let { Text("${it.primary.plantName}\nСад: ${it.primary.gardenName}", color = Cream) }
                SelectionMenu("Дата", selectedDate?.toRussianDate().orEmpty(), emptyList<String>(), { it }, {},
                    onOpen = { if (!saving) datePicker = true })
                Text("Одна процедура. Перед применением сверьте состав, культуру и условия с рецептом и инструкциями средств.", color = Mist)
                error?.let { Text(it, color = Danger) }
            }
        },
        confirmButton = { TextButton(enabled = selected != null && selectedDate != null && !saving, onClick = {
            saving = true
            error = null
            onSave(requireNotNull(selected), requireNotNull(selectedDate)) { error = it; saving = false }
        }) { Text(if (saving) "Сохраняем…" else "Сохранить", color = Leaf300) } },
        dismissButton = { TextButton(enabled = !saving, onClick = onDismiss) { Text("Отмена", color = Mist) } })
    if (datePicker) GardenDatePickerDialog("Дата процедуры", selectedDate ?: LocalDate.now(), {
        if (it.isBefore(LocalDate.now())) error = "Выберите сегодняшнюю или будущую дату"
        else { onDateSelected(it); error = null }
        datePicker = false
    }, { datePicker = false }, confirmText = "Подтвердить дату")
}

@Composable
private fun RecipeSection(title: String, text: String) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, color = Mist, style = MaterialTheme.typography.labelLarge)
        Text(text, color = Cream)
    }
}
