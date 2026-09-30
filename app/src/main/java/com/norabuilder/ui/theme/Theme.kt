package com.norabuilder.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF6EE7B7),
    surface = Color(0xFF121214),
    background = Color(0xFF0A0A0B),
    surfaceVariant = Color(0xFF1E1E20)
)

@Composable
fun NoraTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColorScheme, typography = Typography(), content = content)
}
