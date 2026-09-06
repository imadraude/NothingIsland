package com.nothingisland.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val NothingBlack = Color(0xFF000000)
val NothingDarkSurface = Color(0xFF101010)
val NothingCardBorder = Color(0xFF222222)
val NothingRed = Color(0xFFD71920)
val NothingWhite = Color(0xFFEEEEEE)
val NothingGrey = Color(0xFF888888)
val NothingSubtleGrey = Color(0xFF2E2E32)

private val DarkColorScheme = darkColorScheme(
    primary = NothingRed,
    onPrimary = NothingWhite,
    secondary = NothingGrey,
    background = NothingBlack,
    surface = NothingDarkSurface,
    onSurface = NothingWhite,
    outline = NothingCardBorder
)

@Composable
fun NothingIslandTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
