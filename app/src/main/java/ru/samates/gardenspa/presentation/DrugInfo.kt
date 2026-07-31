package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

@Composable
fun DrugInfo(
    navController: NavController,
    drugName: String?,
    purpose: String?,
    consumptionRate: String?
) {
    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Карточка препарата", "Безопасная памятка", onBack = { navController.popBackStack() })
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text("◇", color = Leaf300, style = MaterialTheme.typography.headlineLarge)
                        Text(drugName.orEmpty(), style = MaterialTheme.typography.headlineLarge, color = Cream)
                        Text("Назначение", color = Leaf300, style = MaterialTheme.typography.labelLarge)
                        Text(purpose.orEmpty().ifBlank { "Не указано" }, color = Cream)
                        Text("Норма расхода", color = Leaf300, style = MaterialTheme.typography.labelLarge)
                        Text(consumptionRate.orEmpty().ifBlank { "Не указана" }, color = Cream)
                    }
                }
                GlassCard(Modifier.fillMaxWidth()) {
                    Column {
                        Text("Перед применением", style = MaterialTheme.typography.titleLarge, color = Cream)
                        Text(
                            "Сверьтесь с официальной инструкцией производителя, используйте средства защиты и соблюдайте дозировку.",
                            color = Mist,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
                PrimaryAction("Готово", { navController.popBackStack() }, Modifier.fillMaxWidth())
            }
        }
    }
}
