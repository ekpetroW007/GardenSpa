package ru.samates.gardenspa.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.GlassStroke
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

private data class NavigationEntry(
    val route: String,
    val label: String,
    val symbol: String,
    val description: String
)

@Composable
fun AppButtonBar(selectedScreen: String = "Сегодня", onClick: (String) -> Unit) {
    val entries = listOf(
        NavigationEntry("Сегодня", "Сегодня", "⌂", "Дела на сегодня"),
        NavigationEntry("Сады", "Мой сад", "♧", "Мои сады и растения"),
        NavigationEntry("Календарь", "Календарь", "▦", "Календарь работ"),
        NavigationEntry("Справочник", "Справочник", "☷", "Средства и народные рецепты")
    )
    val normalizedSelection = when (selectedScreen) {
        "Препараты", "Рецепты" -> "Справочник"
        "Мои сады" -> "Сады"
        "Главная", "Профиль" -> "Сегодня"
        else -> selectedScreen
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
        color = Color(0xF21B3B31),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 8.dp
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 6.dp).selectableGroup(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            entries.forEach { item ->
                val selected = normalizedSelection == item.route
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 64.dp)
                        .semantics { contentDescription = item.description }
                        .selectable(
                            selected = selected,
                            role = Role.Tab,
                            onClick = { onClick(item.route) }
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        item.symbol,
                        color = if (selected) Leaf300 else Mist,
                        fontSize = 26.sp,
                        lineHeight = 28.sp
                    )
                    Text(
                        item.label,
                        color = if (selected) Cream else Mist,
                        fontSize = 13.sp,
                        lineHeight = 17.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}
