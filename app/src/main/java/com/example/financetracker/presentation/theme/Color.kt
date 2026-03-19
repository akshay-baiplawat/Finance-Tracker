package com.example.financetracker.presentation.theme

import androidx.compose.ui.graphics.Color

// --- Brand Identity ---
// --- Brand Identity (FinOS 3.0 Mint) ---
val FinosMint = Color(0xFF34D399) // Mint Green (Primary) ~ oklch(0.78 0.12 155)
val FinosGreen = Color(0xFF059669) // Deeper Green (Secondary) ~ oklch(0.55 0.14 155)
val FinosCoral = Color(0xFFF87171) // Coral/Peach (Destructive/Expense) ~ oklch(0.72 0.12 35)

val BrandPrimary = FinosMint // Alias for compatibility
val BrandSecondary = FinosGreen

val BrandBackground = Color(0xFFF8F9FA) // Off-White
val OffWhiteSurface = BrandBackground
val TextPrimary = Color(0xFF1A1C1E) // Soft Black

// Metric Colors
val ExpenseData = FinosCoral
val IncomeData = FinosGreen // Using Green for Income
val ErrorState = FinosCoral

// --- FinOS Dark Theme Palette ---
val DarkBackground = Color(0xFF0B0E14) // Deep Slate Black
val DarkSurface = Color(0xFF151A23)     // Dark Slate Surface
val DarkSurfaceHighlight = Color(0xFF2B323B) // Subtle Border
val DarkInputBackground = Color(0xFF0F131A) // Text Field Background
val DarkInputSurface = DarkInputBackground // Alias for backward compatibility

val FinosPrimaryDark = Color(0xFF4ADE80) // Light Emerald
val BrandAccentDark = Color(0xFF334155)  // Slate 700
val TextPrimaryDark = Color(0xFFF8FAFC)  // Slate 50
val TextSecondaryDark = Color(0xFF94A3B8) // Slate 400

val ExpenseDark = Color(0xFFF87171)      // Soft Red
val IncomeDark = FinosPrimaryDark        // Matching Primary
val WarningDark = Color(0xFFFBBF24)      // Amber

// Metrics
val ErrorDark = ExpenseDark

// --- Category Pastel Palette ---
val Melon = Color(0xFFFFB7B2)
val Peach = Color(0xFFFFDAC1)
val Lime = Color(0xFFE2F0CB)
val Mint = Color(0xFFB5EAD7)
val Periwinkle = Color(0xFFC7CEEA)
val Pink = Color(0xFFF8BBD0)
val Lavender = Color(0xFFD1C4E9)
val Sky = Color(0xFFB3E5FC)
val TeaGreen = Color(0xFFDCEDC8)
val Apricot = Color(0xFFFFE0B2)
val BlueGrey = Color(0xFFCFD8DC)
val Lemon = Color(0xFFF0F4C3)

val PastelPalette = listOf(
    Melon, Peach, Lime, Mint, Periwinkle, Pink, 
    Lavender, Sky, TeaGreen, Apricot, BlueGrey, Lemon
)

// Helper to get hex string for storage
fun Color.toHex(): String {
    val ulong = this.value
    // Format as #AARRGGBB
    return String.format("#%08X", ulong)
}
