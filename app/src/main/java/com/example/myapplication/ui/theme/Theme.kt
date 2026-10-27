package com.example.myapplication.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MintPrimary,
    onPrimary = Color.Black,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    background = WebDarkBackground,
    onBackground = TextWhite,
    surface = WebDarkSurface,
    onSurface = TextWhite,
    surfaceVariant = WebDarkSurfaceVariant,
    onSurfaceVariant = TextGray
)

private val LightColorScheme = lightColorScheme(
    primary = MintPrimary,
    onPrimary = Color.White,
    primaryContainer = MintContainer,
    onPrimaryContainer = OnMintContainer,
    background = WebLightBackground,
    onBackground = TextDark,
    surface = WebLightSurface,
    onSurface = TextDark,
    surfaceVariant = WebLightSurfaceVariant,
    onSurfaceVariant = TextLightGray
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
