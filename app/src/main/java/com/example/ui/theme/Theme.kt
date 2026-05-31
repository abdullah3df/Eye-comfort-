package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = SageTealPrimaryDark,
    secondary = SageTealSecondaryDark,
    tertiary = SageTealTertiaryDark,
    background = deepForestBackgroundDark,
    surface = deepForestSurfaceDark,
    primaryContainer = deepForestSurfaceDark,
    onPrimaryContainer = deepForestOnPrimaryContainer,
    onPrimary = deepForestBackgroundDark,
    onSecondary = deepForestOnBackgroundDark,
    onTertiary = deepForestBackgroundDark,
    onBackground = deepForestOnBackgroundDark,
    onSurface = deepForestOnBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = SageTealPrimaryLight,
    secondary = SageTealSecondaryLight,
    tertiary = SageTealTertiaryLight,
    background = creamBackgroundLight,
    surface = creamSurfaceLight,
    primaryContainer = creamSurfaceLight,
    onPrimaryContainer = creamOnPrimaryContainer,
    onPrimary = Color.White,
    onSecondary = creamOnBackgroundLight,
    onTertiary = creamOnBackgroundLight,
    onBackground = creamOnBackgroundLight,
    onSurface = creamOnBackgroundLight
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Disable dynamic color by default to guarantee our eye-comfort, blue-light-reducing colors are used
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
