package com.satvikm.quiet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val QuietDarkColors = darkColorScheme(
    background = QuietBlack,
    onBackground = QuietWhite,
    surface = QuietBlack,
    onSurface = QuietWhite,
)

private val QuietLightColors = lightColorScheme(
    background = QuietWhite,
    onBackground = QuietBlack,
    surface = QuietWhite,
    onSurface = QuietBlack,
)

@Composable
fun QuietTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) QuietDarkColors else QuietLightColors
    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
