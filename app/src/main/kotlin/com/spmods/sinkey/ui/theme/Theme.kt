package com.spmods.sinkey.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.spmods.sinkey.data.ThemeMode

// SinKey brand palette — violet / lilac, the same colours as the About Developer
// page, applied to the whole app (light + dark).
val BrandPrimary = Color(0xFF6D28F2)   // violet — buttons, headers, highlights (light theme primary)
val BrandAccent = Color(0xFFB093FF)    // lilac  — soft accents, gradients (dark theme primary family)

// Light
val LightBg = Color(0xFFF8F6FC)
val LightSurface = Color(0xFFFCFBFE)
val LightText = Color(0xFF0A0D2C)
val LightSub = Color(0xFF5B5C9A)

// Dark
val NightBg = Color(0xFF0F0C1D)
val NightSurface = Color(0xFF181331)
val NightText = Color(0xFFF2EEFF)
val NightSub = Color(0xFFB9B6E0)

private val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4DAFB),
    onPrimaryContainer = Color(0xFF2A0E7A),
    inversePrimary = Color(0xFFA78BFA),
    secondary = Color(0xFF8B62F5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFEFE8FD),
    onSecondaryContainer = Color(0xFF3A17B8),
    tertiary = Color(0xFFE0498A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFCE8F0),
    onTertiaryContainer = Color(0xFF5C1233),
    background = LightBg,
    onBackground = LightText,
    surface = LightSurface,
    onSurface = LightText,
    surfaceVariant = Color(0xFFEFE8FD),
    onSurfaceVariant = LightSub,
    surfaceTint = BrandPrimary,
    inverseSurface = Color(0xFF2C2145),
    inverseOnSurface = Color(0xFFF2EEFF),
    outline = Color(0xFFB3A6E0),
    outlineVariant = Color(0xFFE8DEFB),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFE9E4F6),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF4F0FC),
    surfaceContainer = Color(0xFFEFEAFB),
    surfaceContainerHigh = Color(0xFFE9E2F9),
    surfaceContainerHighest = Color(0xFFE3DBF7)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA78BFA),
    onPrimary = Color(0xFF1E0B5C),
    primaryContainer = Color(0xFF34285F),
    onPrimaryContainer = Color(0xFFE6DEFF),
    inversePrimary = BrandPrimary,
    secondary = BrandAccent,
    onSecondary = Color(0xFF24106A),
    secondaryContainer = Color(0xFF2F2457),
    onSecondaryContainer = Color(0xFFE0D6FF),
    tertiary = Color(0xFFF06BA3),
    onTertiary = Color(0xFF4A0D2B),
    tertiaryContainer = Color(0xFF5C1F3E),
    onTertiaryContainer = Color(0xFFFFD9E8),
    background = NightBg,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = Color(0xFF251D47),
    onSurfaceVariant = NightSub,
    surfaceTint = Color(0xFFA78BFA),
    inverseSurface = Color(0xFFEDE8FF),
    inverseOnSurface = Color(0xFF241C3A),
    outline = Color(0xFF6E5FB0),
    outlineVariant = Color(0xFF2D2452),
    surfaceBright = Color(0xFF2A2152),
    surfaceDim = NightBg,
    surfaceContainerLowest = Color(0xFF0B0916),
    surfaceContainerLow = Color(0xFF140F28),
    surfaceContainer = NightSurface,
    surfaceContainerHigh = Color(0xFF211A41),
    surfaceContainerHighest = Color(0xFF2A2150)
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
