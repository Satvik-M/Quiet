package com.satvikm.quiet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily

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
    fontFamily: FontFamily = FontFamily.SansSerif,
    fontScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) QuietDarkColors else QuietLightColors
    val typography = remember(fontFamily, fontScale) { scaledTypography(fontFamily, fontScale) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        content = content,
    )
}

private fun scaledTypography(fontFamily: FontFamily, scale: Float): Typography {
    fun TextStyle.scaled() = copy(fontFamily = fontFamily, fontSize = fontSize * scale, lineHeight = lineHeight * scale)
    val base = Typography()
    return Typography(
        displayLarge = base.displayLarge.scaled(),
        displayMedium = base.displayMedium.scaled(),
        displaySmall = base.displaySmall.scaled(),
        headlineLarge = base.headlineLarge.scaled(),
        headlineMedium = base.headlineMedium.scaled(),
        headlineSmall = base.headlineSmall.scaled(),
        titleLarge = base.titleLarge.scaled(),
        titleMedium = base.titleMedium.scaled(),
        titleSmall = base.titleSmall.scaled(),
        bodyLarge = base.bodyLarge.scaled(),
        bodyMedium = base.bodyMedium.scaled(),
        bodySmall = base.bodySmall.scaled(),
        labelLarge = base.labelLarge.scaled(),
        labelMedium = base.labelMedium.scaled(),
        labelSmall = base.labelSmall.scaled(),
    )
}
