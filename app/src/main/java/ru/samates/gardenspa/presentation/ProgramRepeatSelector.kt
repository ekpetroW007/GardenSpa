package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import ru.samates.gardenspa.domain.programRepeatIntervals
import ru.samates.gardenspa.ui.theme.Leaf300

@Composable
internal fun ProgramRepeatSelector(
    description: String,
    onSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier) {
        TextButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = "Повтор работы: $description" }
        ) {
            Text(description, color = Leaf300)
            Text(" ▾", color = Leaf300)
        }
        DropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            programRepeatIntervals.forEach { days ->
                DropdownMenuItem(
                    text = { Text(days?.let { "$it дней" } ?: "Без повтора") },
                    onClick = { expanded = false; onSelected(days) }
                )
            }
        }
    }
}
