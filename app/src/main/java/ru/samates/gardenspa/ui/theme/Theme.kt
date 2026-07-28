package ru.samates.gardenspa.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val BookeeperColors = darkColorScheme(
    primary = Leaf300,
    onPrimary = Forest950,
    primaryContainer = Forest700,
    onPrimaryContainer = Leaf200,
    secondary = Moss500,
    onSecondary = Cream,
    background = Forest950,
    onBackground = Cream,
    surface = Forest900,
    onSurface = Cream,
    surfaceVariant = Forest800,
    onSurfaceVariant = Mist,
    outline = GlassStroke,
    error = Danger
)

@Composable
fun MyApplicationTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Forest950.toArgb()
            window.navigationBarColor = Forest950.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = false
                isAppearanceLightNavigationBars = false
            }
        }
    }
    MaterialTheme(
        colorScheme = BookeeperColors,
        typography = Typography,
        content = content
    )
}
