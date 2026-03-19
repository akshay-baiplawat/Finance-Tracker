package com.example.financetracker.presentation.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// --- Font Families ---
// Fallback to System Fonts to prevent "Could not load font" crash if Play Services are missing/offline
val Inter = FontFamily.SansSerif
val JetBrainsMono = FontFamily.Monospace

/*
// Keep these for when we have local .ttf files or working GMS
val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_bold, FontWeight.Bold)
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrains_mono, FontWeight.Medium) // Used for data
)
*/

// --- Base Font Sizes ---
private object BaseFontSizes {
    const val DISPLAY_LARGE = 36f
    const val TITLE_MEDIUM = 18f
    const val BODY_MEDIUM = 16f
    const val BODY_SMALL = 14f
    const val LABEL_LARGE = 14f
    const val HEADLINE_MEDIUM = 16f
}

/**
 * Creates scaled typography based on the display size scale factor.
 */
fun scaledTypography(scale: Float): Typography = Typography(
    // Large Headlines (Hero Cards)
    displayLarge = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Bold,
        fontSize = (BaseFontSizes.DISPLAY_LARGE * scale).sp,
        color = BrandBackground // Usually on dark background
    ),
    // Section Headers
    titleMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.SemiBold,
        fontSize = (BaseFontSizes.TITLE_MEDIUM * scale).sp,
        color = TextPrimary
    ),
    // Body Text
    bodyMedium = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = (BaseFontSizes.BODY_MEDIUM * scale).sp,
        color = TextPrimary
    ),
    bodySmall = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Normal,
        fontSize = (BaseFontSizes.BODY_SMALL * scale).sp,
        color = ExpenseData
    ),
    // Buttons / Labels
    labelLarge = TextStyle(
        fontFamily = Inter,
        fontWeight = FontWeight.Medium,
        fontSize = (BaseFontSizes.LABEL_LARGE * scale).sp
    ),
    // Data Values (Lists)
    headlineMedium = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = (BaseFontSizes.HEADLINE_MEDIUM * scale).sp,
        color = ExpenseData
    )
)

// --- Default Typography System (for backward compatibility) ---
val Typography = scaledTypography(1.0f)
