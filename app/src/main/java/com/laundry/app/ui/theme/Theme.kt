package com.laundry.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = LaundryBlue,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    primaryContainer = LaundryBlueLight,
    secondary = TealCyan,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    background = BackgroundDefault,
    surface = androidx.compose.ui.graphics.Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = androidx.compose.ui.graphics.Color(0xFFBA1A1A)
)

private val DarkColorScheme = darkColorScheme(
    primary = LaundryBlueLight,
    onPrimary = LaundryBlueDark,
    background = TextPrimary,
    surface = TextPrimary,
    onBackground = BackgroundDefault,
    onSurface = BackgroundDefault
)

@Composable
fun LaundryInventorySystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
