package ru.samates.gardenspa.presentation

import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import ru.samates.gardenspa.BookeeperApp
import ru.samates.gardenspa.data.database.entity.DrugEntity
import ru.samates.gardenspa.presentation.navigation.AppDestinations
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.DrugsViewmodel
import ru.samates.gardenspa.viewmodel.DrugsViewmodelFactory

@Composable
fun Drugs(navController: NavController, innerPadding: PaddingValues) {
    val application = LocalContext.current.applicationContext as BookeeperApp
    val drugsVm: DrugsViewmodel = viewModel(factory = DrugsViewmodelFactory(application.repository))
    val drugs by drugsVm.drugs.collectAsState()
    var query by remember { mutableStateOf("") }
    val filtered = drugs.filter {
        query.isBlank() || it.name.contains(query, true) || it.purpose.contains(query, true)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("Библиотека ухода", color = Mist)
                    Text("Препараты", style = MaterialTheme.typography.headlineLarge, color = Cream)
                }
                PrimaryAction("+ Добавить", { navController.navigate(AppDestinations.DRUG_ADD_ROUTE) })
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("Поиск по названию или назначению") },
                leadingIcon = { Text("⌕", color = Leaf300) },
                singleLine = true,
                colors = glassTextFieldColors(),
                shape = CompactGlassShape,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (filtered.isEmpty()) {
            item { EmptyGlassState("Ничего не найдено", "Измените запрос или добавьте новый препарат") }
        }
        items(filtered, key = { it.id }) { drug ->
            DrugCard(
                drug = drug,
                onOpen = {
                    navController.navigate(
                        "drugInfoScreen/${Uri.encode(drug.name)}/${Uri.encode(drug.purpose)}/${Uri.encode(drug.consumptionRate)}"
                    )
                },
                onDelete = { drugsVm.deleteDrug(drug.id) }
            )
        }
    }
}

@Composable
fun DrugCard(drug: DrugEntity, onOpen: () -> Unit, onDelete: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("◇", color = Leaf300, style = MaterialTheme.typography.headlineLarge)
            Column(Modifier.weight(1f)) {
                Text(drug.name, style = MaterialTheme.typography.titleLarge, color = Cream)
                Text(drug.purpose, color = Mist, maxLines = 2)
                Text("Норма: ${drug.consumptionRate}", color = Leaf300, modifier = Modifier.padding(top = 6.dp))
            }
            Text("×", color = Danger, modifier = Modifier.clickable(onClick = onDelete))
        }
    }
}
