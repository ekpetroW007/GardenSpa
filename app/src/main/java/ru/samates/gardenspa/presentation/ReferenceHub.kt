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
fun ReferenceHub(innerPadding: PaddingValues, onBack: (() -> Unit)? = null, onOpen: (String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(innerPadding),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ScreenHeader("Справочник", "Выберите, что хотите найти", onBack)
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { onOpen("Мои средства") }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Мои средства", color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text("Ваши препараты и сохранённые баковые смеси и народные рецепты", color = Mist)
                    Text("Открыть мои средства  →", color = Leaf300)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { onOpen("Препараты") }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ru.samates.gardenspa.domain.ProductSection.TREATMENT.title, color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text("Опрыскивание и другие процедуры по листьям и побегам", color = Mist)
                    Text("Открыть список  →", color = Leaf300)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { onOpen("Удобрения") }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ru.samates.gardenspa.domain.ProductSection.FERTILIZER.title, color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text("Полив под корень, внесение в почву и обработка корней", color = Mist)
                    Text("Открыть удобрения  →", color = Leaf300)
                }
            }
        }
        item {
            GlassCard(Modifier.fillMaxWidth(), onClick = { onOpen("Баковые смеси") }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Баковые смеси", color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text("Составы смесей, приготовление и применение", color = Mist)
                    Text("Открыть смеси  →", color = Leaf300)
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
    }
}
