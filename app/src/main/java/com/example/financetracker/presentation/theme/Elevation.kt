package com.example.financetracker.presentation.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Centralized elevation values for consistent card depth across the app.
 * In dark mode, elevations are 0.dp with borders instead for visual separation.
 */
object FinosElevation {
    /** Major hero cards (Net Worth, Income/Spent) */
    val Hero: Dp = 2.dp

    /** Standard cards (wallets, budgets, settings groups) */
    val Card: Dp = 1.dp

    /** List item cards (transaction rows) */
    val ListItem: Dp = 0.5.dp

    /** Flat surface (dark mode default, FAB) */
    val None: Dp = 0.dp
}

/**
 * Centralized border width values for consistent styling.
 */
object FinosBorderWidth {
    /** Standard border (most common) */
    val Standard: Dp = 1.dp

    /** Selected/active state border */
    val Selected: Dp = 2.dp

    /** Emphasized selection (color picker) */
    val Emphasized: Dp = 3.dp
}

/**
 * Combined elevation and border styling for cards.
 * Simplifies passing both values to Card composables.
 */
data class CardStyling(
    val elevation: Dp,
    val border: BorderStroke?
)

/**
 * Detects if the current theme is dark mode by comparing surface color.
 * This is the canonical way to check dark mode in this app.
 */
@Composable
fun isDarkTheme(): Boolean {
    return MaterialTheme.colorScheme.surface == DarkSurface
}

/**
 * Returns appropriate CardStyling for hero-level cards (Net Worth, Income/Spent).
 * - Light mode: 2.dp elevation, no border
 * - Dark mode: 0.dp elevation, 1.dp outline border
 */
@Composable
fun heroCardStyling(): CardStyling {
    val isDark = isDarkTheme()
    return CardStyling(
        elevation = if (isDark) FinosElevation.None else FinosElevation.Hero,
        border = if (isDark) BorderStroke(FinosBorderWidth.Standard, MaterialTheme.colorScheme.outline) else null
    )
}

/**
 * Returns appropriate CardStyling for standard cards (wallets, settings, budgets).
 * - Light mode: 1.dp elevation, no border
 * - Dark mode: 0.dp elevation, 1.dp outline border
 */
@Composable
fun cardStyling(): CardStyling {
    val isDark = isDarkTheme()
    return CardStyling(
        elevation = if (isDark) FinosElevation.None else FinosElevation.Card,
        border = if (isDark) BorderStroke(FinosBorderWidth.Standard, MaterialTheme.colorScheme.outline) else null
    )
}

/**
 * Returns appropriate CardStyling for list item cards (transaction rows).
 * - Light mode: 0.5.dp elevation, no border
 * - Dark mode: 0.dp elevation, 1.dp highlight border
 */
@Composable
fun listItemCardStyling(): CardStyling {
    val isDark = isDarkTheme()
    return CardStyling(
        elevation = if (isDark) FinosElevation.None else FinosElevation.ListItem,
        border = if (isDark) BorderStroke(FinosBorderWidth.Standard, DarkSurfaceHighlight) else null
    )
}

/**
 * Returns appropriate border for standard cards.
 * - Light mode: null (no border)
 * - Dark mode: 1.dp outline border
 */
@Composable
fun standardBorder(): BorderStroke? {
    val isDark = isDarkTheme()
    return if (isDark) BorderStroke(FinosBorderWidth.Standard, MaterialTheme.colorScheme.outline) else null
}

/**
 * Returns appropriate border color for input fields and outlined buttons.
 * - Light mode: LightGray with 50% alpha
 * - Dark mode: DarkSurfaceHighlight
 */
@Composable
fun inputBorderColor(): Color {
    return if (isDarkTheme()) DarkSurfaceHighlight else Color.LightGray.copy(alpha = 0.5f)
}

/**
 * Returns border for selected/active state items.
 * @param isSelected Whether the item is currently selected
 * @param selectedColor Color to use when selected (default: FinosMint)
 */
@Composable
fun selectionBorder(
    isSelected: Boolean,
    selectedColor: Color = FinosMint
): BorderStroke {
    val isDark = isDarkTheme()
    return when {
        isSelected -> BorderStroke(FinosBorderWidth.Selected, selectedColor)
        isDark -> BorderStroke(FinosBorderWidth.Standard, DarkSurfaceHighlight)
        else -> BorderStroke(FinosBorderWidth.Standard, Color.LightGray.copy(alpha = 0.5f))
    }
}

/**
 * Returns border for accent/alert cards (Trip banner, warnings).
 * @param accentColor The accent color for the border
 * @param alpha Alpha value for the border color (default: 0.3f)
 */
fun accentBorder(
    accentColor: Color,
    alpha: Float = 0.3f
): BorderStroke {
    return BorderStroke(FinosBorderWidth.Standard, accentColor.copy(alpha = alpha))
}

/**
 * Returns destructive action border (delete confirmations).
 */
fun destructiveBorder(): BorderStroke {
    return BorderStroke(FinosBorderWidth.Standard, FinosCoral.copy(alpha = 0.3f))
}
