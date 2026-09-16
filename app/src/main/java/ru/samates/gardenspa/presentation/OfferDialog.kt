package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ru.samates.gardenspa.domain.OfferDocument
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Forest900

@Composable
internal fun OfferDialog(document: OfferDocument, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Surface(Modifier.fillMaxSize().padding(10.dp), color = Forest900, shape = GlassShape) {
            Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Договор оферты", color = Cream, style = MaterialTheme.typography.headlineMedium)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    SelectionContainer { Text(document.text, color = Cream) }
                }
                // Reading or closing the document must never accept it on the user's behalf.
                SecondaryAction("Закрыть документ", onDismiss, Modifier.fillMaxWidth())
            }
        }
    }
}
