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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
    var drugForActions by remember { mutableStateOf<DrugEntity?>(null) }
    var drugPendingDelete by remember { mutableStateOf<DrugEntity?>(null) }
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
                onManage = { drugForActions = drug }
            )
        }
    }

    drugForActions?.let { drug ->
        DrugActionsDialog(
            drug = drug,
            onDismiss = { drugForActions = null },
            onEdit = {
                drugForActions = null
                navController.navigate(AppDestinations.drugEdit(drug.id))
            },
            onDelete = {
                drugForActions = null
                drugPendingDelete = drug
            }
        )
    }

    drugPendingDelete?.let { drug ->
        DeleteConfirmationDialog(
            itemName = drug.name,
            onConfirm = {
                drugsVm.deleteDrug(drug.id)
                drugPendingDelete = null
            },
            onDismiss = { drugPendingDelete = null }
        )
    }
}

@Composable
fun DrugCard(drug: DrugEntity, onOpen: () -> Unit, onManage: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth(), onClick = onOpen) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("◇", color = Leaf300, style = MaterialTheme.typography.headlineLarge)
            Column(Modifier.weight(1f)) {
                Text(drug.name, style = MaterialTheme.typography.titleLarge, color = Cream)
                Text(drug.purpose, color = Mist, maxLines = 2)
                Text("Норма: ${drug.consumptionRate}", color = Leaf300, modifier = Modifier.padding(top = 6.dp))
            }
            Text(
                "✎",
                color = Leaf300,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.clickable(onClick = onManage)
            )
        }
    }
}

@Composable
private fun DrugActionsDialog(
    drug: DrugEntity,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ru.samates.gardenspa.ui.theme.Forest900,
        titleContentColor = Cream,
        textContentColor = Cream,
        title = { Text(drug.name) },
        text = { Text("Что вы хотите сделать с препаратом?") },
        confirmButton = {
            TextButton(onClick = onEdit) {
                Text("Редактировать", color = Leaf300)
            }
        },
        dismissButton = {
            TextButton(onClick = onDelete) {
                Text("Удалить", color = Danger)
            }
        }
    )
}
