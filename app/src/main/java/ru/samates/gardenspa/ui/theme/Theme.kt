package ru.samates.gardenspa.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
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

val LocalHighContrast = staticCompositionLocalOf { false }

private fun scaledTypography(scale: Float) = Typography.copy(
    displayLarge = Typography.displayLarge.copy(fontSize = Typography.displayLarge.fontSize * scale, lineHeight = Typography.displayLarge.lineHeight * scale),
    headlineLarge = Typography.headlineLarge.copy(fontSize = Typography.headlineLarge.fontSize * scale, lineHeight = Typography.headlineLarge.lineHeight * scale),
    headlineMedium = Typography.headlineMedium.copy(fontSize = Typography.headlineMedium.fontSize * scale, lineHeight = Typography.headlineMedium.lineHeight * scale),
    titleLarge = Typography.titleLarge.copy(fontSize = Typography.titleLarge.fontSize * scale, lineHeight = Typography.titleLarge.lineHeight * scale),
    titleMedium = Typography.titleMedium.copy(fontSize = Typography.titleMedium.fontSize * scale, lineHeight = Typography.titleMedium.lineHeight * scale),
    bodyLarge = Typography.bodyLarge.copy(fontSize = Typography.bodyLarge.fontSize * scale, lineHeight = Typography.bodyLarge.lineHeight * scale),
    bodyMedium = Typography.bodyMedium.copy(fontSize = Typography.bodyMedium.fontSize * scale, lineHeight = Typography.bodyMedium.lineHeight * scale),
    labelLarge = Typography.labelLarge.copy(fontSize = Typography.labelLarge.fontSize * scale, lineHeight = Typography.labelLarge.lineHeight * scale),
    labelMedium = Typography.labelMedium.copy(fontSize = Typography.labelMedium.fontSize * scale, lineHeight = Typography.labelMedium.lineHeight * scale)
)

@Composable
fun MyApplicationTheme(
    largeInterface: Boolean = false,
    highContrast: Boolean = false,
    content: @Composable () -> Unit
) {
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
    androidx.compose.runtime.CompositionLocalProvider(LocalHighContrast provides highContrast) {
        MaterialTheme(
            colorScheme = BookeeperColors,
            typography = scaledTypography(if (largeInterface) 1.14f else 1f),
            content = content
        )
    }
}
