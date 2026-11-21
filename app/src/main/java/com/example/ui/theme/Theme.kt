package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class TideExtendedColors(
    val risingAccent: Color,
    val fallingAccent: Color,
    val highTideBadge: Color,
    val lowTideBadge: Color,
    val waveLine: Color,
    val waveFillTop: Color,
    val waveFillBottom: Color,
    val chartGrid: Color,
)

val LocalTideExtendedColors = staticCompositionLocalOf {
    TideExtendedColors(
        risingAccent = ReefTeal,
        fallingAccent = CoralLow,
        highTideBadge = TideCyan,
        lowTideBadge = SunlitAmber,
        waveLine = ReefTeal,
        waveFillTop = ReefTeal.copy(alpha = 0.35f),
        waveFillBottom = ReefTeal.copy(alpha = 0.02f),
        chartGrid = OutlineDark,
    )
}

private val DarkTideColorScheme = darkColorScheme(
    primary = ReefTeal,
    onPrimary = AbyssNavy,
    primaryContainer = OceanElevated,
    onPrimaryContainer = MistCyan,
    secondary = TideCyan,
    onSecondary = AbyssNavy,
    secondaryContainer = Color(0xFF0E3C52),
    onSecondaryContainer = MistCyan,
    tertiary = SunlitAmber,
    onTertiary = AbyssNavy,
    background = AbyssNavy,
    onBackground = DarkTextPrimary,
    surface = DeepOcean,
    onSurface = DarkTextPrimary,
    surfaceVariant = OceanSlate,
    onSurfaceVariant = DarkTextSecondary,
    outline = OutlineDark,
    error = CoralLow,
    onError = AbyssNavy,
)

private val LightTideColorScheme = lightColorScheme(
    primary = LagoonBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFCFF2FA),
    onPrimaryContainer = AbyssNavy,
    secondary = TideCyan,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDF5F9),
    onSecondaryContainer = OceanSlate,
    tertiary = Color(0xFFD97706),
    onTertiary = Color.White,
    background = FoamWhite,
    onBackground = CoastalTextPrimary,
    surface = Color.White,
    onSurface = CoastalTextPrimary,
    surfaceVariant = SeaMist,
    onSurfaceVariant = CoastalTextSecondary,
    outline = OutlineLight,
    error = Color(0xFFD9483B),
    onError = Color.White,
)

private val TideShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun TidePulseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkTideColorScheme else LightTideColorScheme
    val extendedColors = if (darkTheme) {
        TideExtendedColors(
            risingAccent = ReefTeal,
            fallingAccent = CoralLow,
            highTideBadge = ReefTeal,
            lowTideBadge = SunlitAmber,
            waveLine = ReefTeal,
            waveFillTop = ReefTeal.copy(alpha = 0.34f),
            waveFillBottom = DeepOcean.copy(alpha = 0.04f),
            chartGrid = OutlineDark.copy(alpha = 0.65f),
        )
    } else {
        TideExtendedColors(
            risingAccent = Color(0xFF008B8B),
            fallingAccent = Color(0xFFD9483B),
            highTideBadge = LagoonBlue,
            lowTideBadge = Color(0xFFD97706),
            waveLine = LagoonBlue,
            waveFillTop = TideCyan.copy(alpha = 0.32f),
            waveFillBottom = FoamWhite.copy(alpha = 0.04f),
            chartGrid = OutlineLight.copy(alpha = 0.8f),
        )
    }

    CompositionLocalProvider(LocalTideExtendedColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TideTypography,
            shapes = TideShapes,
            content = content,
        )
    }
}
