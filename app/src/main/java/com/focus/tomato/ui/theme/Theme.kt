package com.focus.tomato.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import android.app.Activity

private val LightScheme = lightColorScheme(
    primary = Tomato,
    onPrimary = Color.White,
    primaryContainer = TomatoSoft,
    onPrimaryContainer = Color(0xFF5B1A12),
    secondary = Teal,
    onSecondary = Color.White,
    secondaryContainer = TealSoft,
    onSecondaryContainer = Color(0xFF0D463F),
    tertiary = Amber,
    onTertiary = Color(0xFF3C2A00),
    tertiaryContainer = AmberSoft,
    onTertiaryContainer = Color(0xFF4A3400),
    background = Cream,
    onBackground = Ink,
    surface = Color(0xFFFFFDFB),
    onSurface = Ink,
    surfaceVariant = CreamDeep,
    onSurfaceVariant = InkSoft,
    outline = Color(0xFFD8CFC3),
    outlineVariant = Color(0xFFEAE1D5),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFF9A8A),
    onPrimary = Color(0xFF4A1009),
    primaryContainer = Color(0xFF6E241A),
    onPrimaryContainer = Color(0xFFFFDAD2),
    secondary = Color(0xFF6FD5C8),
    onSecondary = Color(0xFF003730),
    secondaryContainer = Color(0xFF1C514A),
    onSecondaryContainer = Color(0xFFD3F5EF),
    tertiary = Color(0xFFF5C96A),
    onTertiary = Color(0xFF3C2A00),
    tertiaryContainer = Color(0xFF5A4300),
    onTertiaryContainer = Color(0xFFFFE9B7),
    background = Night,
    onBackground = NightInk,
    surface = NightSurface,
    onSurface = NightInk,
    surfaceVariant = NightSurfaceVariant,
    onSurfaceVariant = Color(0xFFC4C0B8),
    outline = Color(0xFF4B505C),
    outlineVariant = Color(0xFF343945),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val TomatoTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.5).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 38.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 32.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp
    )
)

val TomatoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun TomatoTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val activity = view.context as? Activity
            if (activity != null) {
                val controller = WindowCompat.getInsetsController(activity.window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkScheme else LightScheme,
        typography = TomatoTypography,
        shapes = TomatoShapes,
        content = content
    )
}
