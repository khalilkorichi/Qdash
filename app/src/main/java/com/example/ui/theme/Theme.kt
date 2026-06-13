package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

import com.example.ui.designsystem.tokens.ColorTokens

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF818CF8), // Vibrant Indigo-400 for dark mode buttons/actions
    onPrimary = Color(0xFF09090B), // Slate-950 for contrast on primary buttons
    primaryContainer = Color(0xFF312E81), // Deep Indigo-900 container
    onPrimaryContainer = Color(0xFFE0E7FF),
    secondary = Color(0xFF38BDF8), // Pastel Info/Secondary Blue
    tertiary = Color(0xFFFBBF24), // Pastel Warning/Savings Amber
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceVariant = CardDark,
    onBackground = Color(0xFFF4F4F5), // Off-white Zinc-50
    onSurface = Color(0xFFF4F4F5), // Off-white Zinc-50
    onSurfaceVariant = Color(0xFFA1A1AA), // Zinc-400 secondary text
    outline = ColorTokens.BorderDark,
    outlineVariant = ColorTokens.DividerDark,
    error = Color(0xFFF87171) // Pastel Danger/Error Red
)

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = CardLight,
    onPrimaryContainer = PrimaryDark,
    secondary = TransferBlue,
    tertiary = SavingsAmber,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceVariant = CardLight,
    onBackground = Color(0xFF0F0F1A),
    onSurface = Color(0xFF0F0F1A),
    onSurfaceVariant = Color(0xFF5A5A75),
    outline = ColorTokens.BorderLight,
    outlineVariant = ColorTokens.BorderLight,
    error = ExpenseRed
)

@Composable
fun KdachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Set false to ensure premium branding colors are consistent
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
