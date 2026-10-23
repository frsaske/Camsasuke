package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SasukeDarkColorScheme = darkColorScheme(
    primary = TextPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkCardPrimary,
    onPrimaryContainer = TextPrimary,
    secondary = AccentBlue,
    onSecondary = Color.White,
    secondaryContainer = DarkCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = SlateBlue,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkCardSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCardElevated,
    onSurfaceVariant = TextSecondary,
    outline = DarkBorder,
    outlineVariant = DarkBorderSubtle,
    error = RecordRed,
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
