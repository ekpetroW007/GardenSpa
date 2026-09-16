package ru.samates.gardenspa.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.semantics.Role
import ru.samates.gardenspa.domain.OfferDocument
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Danger
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.UserViewModel

@Composable
fun Registration(userViewModel: UserViewModel) {
    val state by userViewModel.registrationState.collectAsState()
    val saving by userViewModel.registrationSaving.collectAsState()
    val error by userViewModel.registrationError.collectAsState()
    RegistrationForm(state?.name.orEmpty(), userViewModel.offer, saving, error, userViewModel::registerUser)
}

@Composable
internal fun RegistrationForm(
    initialName: String,
    document: OfferDocument,
    saving: Boolean,
    error: String?,
    onContinue: (String, Boolean) -> Unit
) {
    var login by rememberSaveable { mutableStateOf(initialName) }
    var accepted by rememberSaveable(document.sha256) { mutableStateOf(false) }
    var offerOpen by rememberSaveable { mutableStateOf(false) }
    BotanicalBackground {
        Column(
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding()
                .verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("GardenSpa", color = Cream, style = MaterialTheme.typography.displayLarge)
            Text("Уход за садом — спокойно и вовремя", color = Mist)
            Spacer(Modifier.height(36.dp))
            GlassCard(Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Как к вам обращаться?", color = Cream, style = MaterialTheme.typography.titleLarge)
                    Text(if (initialName.isBlank()) "Добавьте имя и примите условия использования."
                        else "Для продолжения примите условия. Ваши сады и история сохранятся.", color = Mist)
                    OutlinedTextField(
                        value = login,
                        onValueChange = { login = it },
                        label = { Text("Имя или псевдоним") },
                        enabled = !saving,
                        keyboardOptions = SentenceKeyboardOptions,
                        singleLine = true,
                        colors = glassTextFieldColors(),
                        shape = CompactGlassShape,
                        modifier = Modifier.fillMaxWidth()
                    )
                    SecondaryAction("Договор оферты", { offerOpen = true }, Modifier.fillMaxWidth())
                    Row(Modifier.fillMaxWidth().toggleable(value = accepted, enabled = !saving,
                        role = Role.Checkbox, onValueChange = { accepted = it }),
                        verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = accepted, onCheckedChange = null, enabled = !saving)
                        Text("Я принимаю Договор оферты", color = Cream, modifier = Modifier.weight(1f))
                    }
                    error?.let { Text(it, color = Danger) }
                    PrimaryAction(
                        if (saving) "Сохраняем…" else "Продолжить",
                        onClick = { onContinue(login.trim(), accepted) },
                        enabled = login.isNotBlank() && accepted && !saving,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
    if (offerOpen) OfferDialog(document) { offerOpen = false }
}
