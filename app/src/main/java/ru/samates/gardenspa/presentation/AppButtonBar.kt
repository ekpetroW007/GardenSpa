package ru.samates.gardenspa.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.GlassStroke
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist

private enum class NavigationIcon { HOME, GARDEN, CALENDAR, REFERENCE }

private data class NavigationEntry(val label: String, val icon: NavigationIcon)

@Composable
fun AppButtonBar(selectedScreen: String = "Главная", onClick: (String) -> Unit) {
    val entries = listOf(
        NavigationEntry("Главная", NavigationIcon.HOME),
        NavigationEntry("Мои сады", NavigationIcon.GARDEN),
        NavigationEntry("Календарь", NavigationIcon.CALENDAR),
        NavigationEntry("Справочник", NavigationIcon.REFERENCE)
    )
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp),
        color = Color(0xE61B3B31),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, GlassStroke),
        shadowElevation = 8.dp
    ) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            entries.forEach { item ->
                val selected = selectedScreen == item.label || (selectedScreen == "Профиль" && item.label == "Главная")
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(58.dp)
                        .clickable { onClick(item.label) },
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    NavigationGlyph(item.icon, selected)
                    Text(
                        if (item.label == "Мои сады") "Сады" else item.label,
                        color = if (selected) Cream else Mist,
                        fontSize = 10.sp,
                        lineHeight = 12.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun NavigationGlyph(icon: NavigationIcon, selected: Boolean) {
    val iconColor = if (selected) Leaf300 else Mist
    Canvas(Modifier.size(36.dp)) {
        val center = Offset(size.width / 2f, size.height / 2f)
        if (selected) {
            drawCircle(
                color = Color(0x2ECCE8A7),
                radius = size.minDimension / 2f,
                center = center
            )
        }

        val unit = size.minDimension / 36f
        val stroke = Stroke(
            width = 2.4f * unit,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round
        )
        when (icon) {
            NavigationIcon.HOME -> {
                val house = Path().apply {
                    moveTo(center.x - 9f * unit, center.y - 1f * unit)
                    lineTo(center.x, center.y - 9f * unit)
                    lineTo(center.x + 9f * unit, center.y - 1f * unit)
                    lineTo(center.x + 7f * unit, center.y - 1f * unit)
                    lineTo(center.x + 7f * unit, center.y + 9f * unit)
                    lineTo(center.x - 7f * unit, center.y + 9f * unit)
                    lineTo(center.x - 7f * unit, center.y - 1f * unit)
                }
                drawPath(house, color = iconColor, style = stroke)
            }
            NavigationIcon.GARDEN -> {
                drawCircle(iconColor, 4.5f * unit, Offset(center.x, center.y - 5f * unit), style = stroke)
                drawCircle(iconColor, 4.5f * unit, Offset(center.x - 5f * unit, center.y), style = stroke)
                drawCircle(iconColor, 4.5f * unit, Offset(center.x + 5f * unit, center.y), style = stroke)
                drawLine(iconColor, Offset(center.x, center.y + 4f * unit), Offset(center.x, center.y + 9f * unit), stroke.width, StrokeCap.Round)
                drawLine(iconColor, Offset(center.x - 5f * unit, center.y + 9f * unit), Offset(center.x + 5f * unit, center.y + 9f * unit), stroke.width, StrokeCap.Round)
            }
            NavigationIcon.CALENDAR -> {
                drawRoundRect(
                    color = iconColor,
                    topLeft = Offset(center.x - 8f * unit, center.y - 8f * unit),
                    size = Size(16f * unit, 16f * unit),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * unit),
                    style = stroke
                )
                drawLine(iconColor, Offset(center.x, center.y - 8f * unit), Offset(center.x, center.y + 8f * unit), stroke.width)
            }
            NavigationIcon.REFERENCE -> {
                val diamond = Path().apply {
                    moveTo(center.x, center.y - 9f * unit)
                    lineTo(center.x + 9f * unit, center.y)
                    lineTo(center.x, center.y + 9f * unit)
                    lineTo(center.x - 9f * unit, center.y)
                    close()
                }
                drawPath(diamond, color = iconColor, style = stroke)
            }
        }
    }
}
