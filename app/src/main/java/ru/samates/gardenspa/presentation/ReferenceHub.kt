package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

@Composable
fun ReferenceHub(innerPadding: PaddingValues, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Полезная информация", color = Mist)
                Text("Справочник", style = MaterialTheme.typography.headlineLarge, color = Cream)
                Text("Выберите, что хотите найти", color = Mist)
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { onOpen("Препараты") }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Средства для обработки", color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text("Назначение, способ применения и ваши собственные средства", color = Mist)
                    Text("Открыть список  →", color = Leaf300)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { onOpen("Рецепты") }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Народные рецепты", color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text("Состав, приготовление, ограничения и меры осторожности", color = Mist)
                    Text("Открыть рецепты  →", color = Leaf300)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth()) {
                Text(
                    "Перед обработкой сверяйтесь с инструкцией выбранного средства. Не смешивайте составы, если совместимость не указана производителем.",
                    color = Mist
                )
            }
        }
    }
}
