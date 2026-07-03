package com.spmods.sinkey.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.spmods.sinkey.data.ThemeMode

// SinKey brand palette — warm cream / gold / maroon, inspired by a
// Sri Lankan lotus-and-brass aesthetic rather than a stock Material look.
val Gold = Color(0xFFC79A3E)
val Maroon = Color(0xFF7A2038)
val CreamBg = Color(0xFFF4F2ED)
val CreamSurface = Color(0xFFFFFFFF)
val CreamText = Color(0xFF241C14)
val CreamSub = Color(0xFF86776A)

val NightBg = Color(0xFF15130F)
val NightSurface = Color(0xFF221D16)
val NightText = Color(0xFFF2EDE4)
val NightSub = Color(0xFF9A8D7C)

private val LightColors = lightColorScheme(
    primary = Maroon,
    secondary = Gold,
    background = CreamBg,
    surface = CreamSurface,
    onBackground = CreamText,
    onSurface = CreamText,
    onPrimary = Color.White,
    onSecondary = Color.White
)

private val DarkColors = darkColorScheme(
    primary = Gold,
    secondary = Maroon,
    background = NightBg,
    surface = NightSurface,
    onBackground = NightText,
    onSurface = NightText,
    onPrimary = Color.Black,
    onSecondary = Color.White
)

@Composable
fun SinKeyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDark = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val colors = if (useDark) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = MaterialTheme.typography,
        content = content
    )
}
