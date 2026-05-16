package com.devcraft.jsmart.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val JSmartColorScheme = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Cream,
    primaryContainer = TealDark,
    onPrimaryContainer = TealLight,
    secondary = CharcoalMedium,
    onSecondary = Cream,
    background = Cream,
    onBackground = CharcoalDark,
    surface = Cream,
    onSurface = CharcoalDark,
    surfaceVariant = SurfaceContainer,
    onSurfaceVariant = CharcoalMedium,
    outline = OutlineColor,
    error = ErrorRed,
)

@Composable
fun JSmartTheme(content: @Composable () -> Unit) {
    val colorScheme = JSmartColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = TealPrimary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}