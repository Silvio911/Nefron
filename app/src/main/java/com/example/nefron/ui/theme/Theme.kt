package com.example.nefron.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary            = NefronBlue,
    onPrimary          = Color.White,
    primaryContainer   = NefronBlueLight,
    onPrimaryContainer = NefronBlueDeep,
    secondary          = NefronBlueDark,
    onSecondary        = Color.White,
    secondaryContainer = NefronBlueLight,
    onSecondaryContainer = NefronBlueDeep,
    background         = NefronSurface,
    surface            = NefronSurface,
    onBackground       = NefronBlueDeep,
    onSurface          = NefronBlueDeep,
    surfaceVariant     = NefronBlueLight,
    onSurfaceVariant   = NefronBlueDark,
)

private val DarkColorScheme = darkColorScheme(
    primary            = NefronBlueLight,
    onPrimary          = NefronBlueDeep,
    primaryContainer   = NefronBlueDark,
    onPrimaryContainer = NefronBlueLight,
    secondary          = NefronBlue,
    onSecondary        = Color.White,
    secondaryContainer = NefronBlueDark,
    onSecondaryContainer = NefronBlueLight,
    background         = NefronSurfaceDark,
    surface            = NefronSurfaceDark,
    onBackground       = NefronBlueLight,
    onSurface          = NefronBlueLight,
    surfaceVariant     = Color(0xFF243545),
    onSurfaceVariant   = NefronBlueLight,
)

@Composable
fun NefronTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = Typography,
        content     = content
    )
}
