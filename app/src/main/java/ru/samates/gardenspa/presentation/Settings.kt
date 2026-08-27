package ru.samates.gardenspa.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import ru.samates.gardenspa.notifications.GardenWorkReminderScheduler
import ru.samates.gardenspa.notifications.TreatmentReminderScheduler
import ru.samates.gardenspa.ui.theme.Cream
import ru.samates.gardenspa.ui.theme.Leaf300
import ru.samates.gardenspa.ui.theme.Mist
import ru.samates.gardenspa.viewmodel.UserViewModel

@Composable
fun SettingsScreen(navController: NavController, userViewModel: UserViewModel) {
    val context = LocalContext.current
    val largeInterface by userViewModel.largeInterface.collectAsState()
    val highContrast by userViewModel.highContrast.collectAsState()
    var calorieReminder by remember { mutableStateOf(GardenWorkReminderScheduler.isEnabled(context)) }
    var notificationAllowed by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        )
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationAllowed = granted
        if (granted) TreatmentReminderScheduler.refreshNow(context)
    }

    BotanicalBackground {
        Column(Modifier.fillMaxSize()) {
            ScreenHeader("Настройки", "Сделайте приложение удобным для себя", onBack = { navController.popBackStack() })
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    SettingsToggleCard(
                        title = "Крупный интерфейс",
                        description = "Увеличивает текст и подписи внутри GardenSpa",
                        checked = largeInterface,
                        onCheckedChange = userViewModel::setLargeInterface
                    )
                }
                item {
                    SettingsToggleCard(
                        title = "Повышенный контраст",
                        description = "Карточки и границы становятся заметнее на ярком свету",
                        checked = highContrast,
                        onCheckedChange = userViewModel::setHighContrast
                    )
                }
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Напоминания об уходе", color = Cream, style = MaterialTheme.typography.titleLarge)
                            Text(
                                if (notificationAllowed) "Разрешены на этом устройстве" else "Пока выключены. Включите их, чтобы не пропускать работы.",
                                color = Mist
                            )
                            PrimaryAction(
                                if (notificationAllowed) "Открыть настройки уведомлений" else "Включить напоминания",
                                onClick = {
                                    if (!notificationAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        })
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
                item {
                    SettingsToggleCard(
                        title = "Вечерний подсчёт садовой активности",
                        description = "Необязательное напоминание в 20:00. По умолчанию выключено.",
                        checked = calorieReminder,
                        onCheckedChange = { enabled ->
                            calorieReminder = enabled
                            GardenWorkReminderScheduler.setEnabled(context, enabled)
                        }
                    )
                }
                item {
                    GlassCard(Modifier.fillMaxWidth()) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Данные и приватность", color = Cream, style = MaterialTheme.typography.titleLarge)
                            Text("Сады и календарь хранятся на устройстве и доступны без интернета. Интернет нужен для обновления погоды.", color = Mist)
                            Text("Местоположение используется только для расчёта сроков ухода.", color = Leaf300)
                        }
                    }
                }
                item {
                    EmptyGlassState(
                        "Нужна помощь?",
                        "На каждом сложном экране есть пояснения. Все опасные действия требуют подтверждения."
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = Cream, style = MaterialTheme.typography.titleLarge)
                Text(description, color = Mist)
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
