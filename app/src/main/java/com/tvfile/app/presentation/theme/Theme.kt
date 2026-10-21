package com.tvfile.app.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = TVAccentCyan,
    secondary = TVAccentBlue,
    background = TVBackground,
    surface = TVSurface,
    onPrimary = TVBackground,
    onBackground = TVTextPrimary,
    onSurface = TVTextPrimary
)

@Composable
fun TVFileTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
