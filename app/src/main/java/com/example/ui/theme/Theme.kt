package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SasukeDarkColorScheme = darkColorScheme(
    primary = SasukeSilver,
    onPrimary = SasukeBlack,
    primaryContainer = SasukeSurfaceVariant,
    onPrimaryContainer = SasukeSilver,
    secondary = SasukeAmber,
    onSecondary = SasukeBlack,
    secondaryContainer = SasukeSurface,
    onSecondaryContainer = SasukeAmber,
    tertiary = SasukeCyan,
    onTertiary = SasukeBlack,
    background = SasukeBlack,
    onBackground = SasukeSilver,
    surface = SasukeCharcoal,
    onSurface = SasukeSilver,
    surfaceVariant = SasukeSurfaceVariant,
    onSurfaceVariant = SasukeMuted,
    outline = SasukeBorder,
    outlineVariant = GlassBorder,
    error = SasukeRed,
    onError = Color.White
)

@Composable
fun SasukeRetroCamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SasukeDarkColorScheme,
        typography = Typography,
        content = content
    )
}
