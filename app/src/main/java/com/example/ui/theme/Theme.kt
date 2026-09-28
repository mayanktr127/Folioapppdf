package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = CranberryPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = CranberryPale,
    onPrimaryContainer = CranberryPressed,
    secondary = CranberryPressed,
    onSecondary = SurfaceWhite,
    secondaryContainer = CranberryPale,
    onSecondaryContainer = CranberryPressed,
    tertiary = AccentBlue,
    background = CanvasBackground,
    onBackground = InkPrimary,
    surface = SurfaceWhite,
    onSurface = InkPrimary,
    surfaceVariant = CanvasBackground,
    onSurfaceVariant = InkSecondary,
    outline = BorderLight,
    outlineVariant = BorderLight
)

private val DarkColorScheme = darkColorScheme(
    primary = CranberryPrimary,
    onPrimary = SurfaceWhite,
    primaryContainer = CranberryPressed,
    onPrimaryContainer = CranberryPale,
    secondary = CranberryPrimary,
    onSecondary = SurfaceWhite,
    secondaryContainer = CranberryDark,
    onSecondaryContainer = CranberryPale,
    tertiary = AccentBlue,
    background = DarkBackground,
    onBackground = DarkInkPrimary,
    surface = DarkSurface,
    onSurface = DarkInkPrimary,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkInkSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorder
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CranberryPrimary.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
