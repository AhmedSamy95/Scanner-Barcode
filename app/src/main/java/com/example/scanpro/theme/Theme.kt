package com.example.scanpro.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryAccent,
    onPrimary = OnPrimaryDark,
    secondary = SecondaryAccent,
    onSecondary = OnSecondaryDark,
    tertiary = TertiaryAccent,
    error = ErrorColor,
    background = DarkBackground,
    onBackground = OnBackgroundDark,
    surface = DarkSurface,
    onSurface = OnSurfaceDark,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = LightSurface,
    secondary = SecondaryLight,
    onSecondary = LightSurface,
    tertiary = TertiaryAccent,
    error = ErrorColor,
    background = LightBackground,
    onBackground = OnBackgroundLight,
    surface = LightSurface,
    onSurface = OnSurfaceLight,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = OnSurfaceVariantLight
)

@Composable
fun ScanProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // We prefer dark theme by default, but honor user preference if they have darkTheme system configuration.
    // For this scan app, the dark design scheme looks exceptionally good.
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
