package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun AuraEQTheme(
    accentTheme: AccentTheme = AccentTheme.NEON_CYAN,
    content: @Composable () -> Unit
) {
    val darkColorScheme = darkColorScheme(
        primary = accentTheme.primary,
        onPrimary = AmoledBackground,
        primaryContainer = accentTheme.primary.copy(alpha = 0.2f),
        onPrimaryContainer = accentTheme.primary,
        secondary = accentTheme.secondary,
        onSecondary = AmoledBackground,
        secondaryContainer = accentTheme.secondary.copy(alpha = 0.2f),
        onSecondaryContainer = accentTheme.secondary,
        tertiary = AmberAccent,
        onTertiary = AmoledBackground,
        background = AmoledBackground,
        onBackground = TextPrimary,
        surface = AmoledSurface,
        onSurface = TextPrimary,
        surfaceVariant = AmoledSurfaceVariant,
        onSurfaceVariant = TextSecondary,
        outline = AmoledBorder,
        outlineVariant = AmoledBorder.copy(alpha = 0.5f)
    )

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = AmoledBackground.toArgb()
            window.navigationBarColor = AmoledBackground.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = Typography,
        content = content
    )
}
