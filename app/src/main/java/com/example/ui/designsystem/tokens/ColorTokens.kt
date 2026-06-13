package com.example.ui.designsystem.tokens

import androidx.compose.ui.graphics.Color

object ColorTokens {
    // Notion Sleek Monochrome Brand Colors
    val Primary = Color(0xFF191919)       // Notion Deep Charcoal/Black
    val PrimaryDark = Color(0xFF0F0F0F)   // True Deep Black
    val PrimaryLight = Color(0xFF2E2E2E)  // Dark Gray Accent

    // Notion Calm Semantic Accents (Vibrant & high contrast under both themes)
    val Success = Color(0xFF22C55E)       // Vibrant Emerald Green
    val Danger = Color(0xFFEF4444)        // Vibrant Rose Red
    val Warning = Color(0xFFF59E0B)       // Vibrant Amber Orange
    val Info = Color(0xFF3B82F6)          // Vibrant Blue

    // Light Theme Palette (Warm Notion White documents)
    val BackgroundLight = Color(0xFFFBFBFA)      // Warm Paper White
    val SurfaceLight = Color(0xFFFFFFFF)         // Clean Card Surface
    val CardLight = Color(0xFFFFFFFF)
    val TextPrimaryLight = Color(0xFF1A1A1A)     // Ink Black
    val TextSecondaryLight = Color(0xFF6A6A65)   // Slate Gray
    val BorderLight = Color(0xFFE9E9E6)          // Thin Paper Gray Border

    // Dark Theme Palette (Sleek Dark Mode Notion)
    val BackgroundDark = Color(0xFF09090B)       // Premium dark slate background
    val SurfaceDark = Color(0xFF121214)          // Premium zinc dark surface
    val CardDark = Color(0xFF1C1C1F)             // Zinc dark card background
    val ElevatedSurfaceDark = Color(0xFF252528)  // Elevated zinc dark surface for dialogs/sheets
    val TextPrimaryDark = Color(0xFFF4F4F5)      // Off-white for high-contrast primary text
    val TextSecondaryDark = Color(0xFF94A3B8)    // Slate gray for secondary text
    val TextMutedDark = Color(0xFF64748B)        // Muted gray text
    val BorderDark = Color(0xFF27272A)           // Contrast border
    val DividerDark = Color(0xFF1E1E21)          // Darker divider line

    // Pastel/Lighter Semantic Colors specifically for Dark Theme
    val SuccessDark = Color(0xFF4ADE80)
    val DangerDark = Color(0xFFF87171)
    val WarningDark = Color(0xFFFBBF24)
    val InfoDark = Color(0xFF38BDF8)
    
    val PositiveContainerDark = Color(0xFF064E3B)
    val NegativeContainerDark = Color(0xFF7F1D1D)

    // Neutrals
    val TextGray = Color(0xFF8E8EA8)
    val Overlay = Color(0xFF0F0F0F).copy(alpha = 0.4f)
}
