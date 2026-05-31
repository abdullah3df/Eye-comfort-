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
    primary = SleekPrimaryDark,
    secondary = SleekSecondaryDark,
    tertiary = SleekTertiaryDark,
    background = SleekBackgroundDark,
    surface = SleekSurfaceDark,
    primaryContainer = SleekAccentContainerDark,
    onPrimaryContainer = SleekOnAccentDark,
    onPrimary = SleekBackgroundDark,
    onSecondary = SleekOnBackgroundDark,
    onTertiary = SleekBackgroundDark,
    onBackground = SleekOnBackgroundDark,
    onSurface = SleekOnBackgroundDark
)

private val LightColorScheme = lightColorScheme(
    primary = SleekPrimaryLight,
    secondary = SleekSecondaryLight,
    tertiary = SleekTertiaryLight,
    background = SleekBackgroundLight,
    surface = SleekSurfaceLight,
    primaryContainer = SleekAccentContainerLight,
    onPrimaryContainer = SleekOnAccentLight,
    onPrimary = Color.White,
    onSecondary = SleekOnBackgroundLight,
    onTertiary = SleekOnBackgroundLight,
    onBackground = SleekOnBackgroundLight,
    onSurface = SleekOnBackgroundLight
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
