package com.example.financetracker.presentation.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Display size scale factors.
 * Large (1.0x) = current app sizing
 * Medium (0.92x) = recommended default
 */
object DisplaySize {
    const val EXTRA_SMALL = "extra_small"
    const val SMALL = "small"
    const val MEDIUM = "medium"
    const val LARGE = "large"

    fun getScale(size: String): Float = when (size) {
        EXTRA_SMALL -> 0.70f
        SMALL -> 0.80f
        MEDIUM -> 0.90f
        else -> 1.0f // LARGE = current behavior
    }

    val options = listOf(EXTRA_SMALL, SMALL, MEDIUM, LARGE)

    fun getLabel(size: String): String = when (size) {
        EXTRA_SMALL -> "XS"
        SMALL -> "S"
        MEDIUM -> "M"
        else -> "L"
    }

    fun getFullLabel(size: String): String = when (size) {
        EXTRA_SMALL -> "Extra Small"
        SMALL -> "Small"
        MEDIUM -> "Medium"
        else -> "Large"
    }
}

/**
 * Scaled dimensions for the app.
 * All values are based on the scale factor.
 */
data class Dimensions(
    // Spacing
    val xs: Dp,       // 4.dp base - fine borders, small gaps
    val sm: Dp,       // 8.dp base - small spacing
    val md: Dp,       // 12.dp base - medium spacing
    val lg: Dp,       // 16.dp base - standard padding
    val xl: Dp,       // 20.dp base - large padding
    val xxl: Dp,      // 24.dp base - hero padding
    val xxxl: Dp,     // 32.dp base - extra large

    // Icons
    val iconXs: Dp,   // 16.dp base
    val iconSm: Dp,   // 20.dp base
    val iconMd: Dp,   // 24.dp base
    val iconLg: Dp,   // 32.dp base
    val iconXl: Dp,   // 48.dp base
    val iconXxl: Dp,  // 64.dp base

    // Components
    val buttonHeight: Dp,     // 56.dp base
    val cardRadius: Dp,       // 20.dp base
    val heroRadius: Dp,       // 24.dp base
    val chipRadius: Dp,       // 8.dp base
    val fabSize: Dp,          // 64.dp base
    val fabClearance: Dp,     // 80.dp base - bottom padding for FAB
    val statusBarSpace: Dp,   // 48.dp base

    // Avatars
    val avatarXs: Dp,  // 28.dp base
    val avatarSm: Dp,  // 36.dp base
    val avatarMd: Dp,  // 48.dp base
    val avatarLg: Dp,  // 64.dp base

    // Progress/Indicators
    val progressHeight: Dp,   // 8.dp base
    val dividerHeight: Dp,    // 1.dp base
    val indicatorHeight: Dp,  // 4.dp base
    val tabIndicatorHeight: Dp, // 3.dp base

    // Special sizes
    val donutChartSize: Dp,   // 100.dp base
    val searchBarHeight: Dp,  // 48.dp base
    val textFieldHeight: Dp,  // 56.dp base
)

/**
 * Creates scaled dimensions based on the scale factor.
 */
fun createDimensions(scale: Float): Dimensions = Dimensions(
    // Spacing
    xs = (4 * scale).dp,
    sm = (8 * scale).dp,
    md = (12 * scale).dp,
    lg = (16 * scale).dp,
    xl = (20 * scale).dp,
    xxl = (24 * scale).dp,
    xxxl = (32 * scale).dp,

    // Icons
    iconXs = (16 * scale).dp,
    iconSm = (20 * scale).dp,
    iconMd = (24 * scale).dp,
    iconLg = (32 * scale).dp,
    iconXl = (48 * scale).dp,
    iconXxl = (64 * scale).dp,

    // Components
    buttonHeight = (56 * scale).dp,
    cardRadius = (20 * scale).dp,
    heroRadius = (24 * scale).dp,
    chipRadius = (8 * scale).dp,
    fabSize = (64 * scale).dp,
    fabClearance = (80 * scale).dp,
    statusBarSpace = (48 * scale).dp,

    // Avatars
    avatarXs = (28 * scale).dp,
    avatarSm = (36 * scale).dp,
    avatarMd = (48 * scale).dp,
    avatarLg = (64 * scale).dp,

    // Progress/Indicators
    progressHeight = (8 * scale).dp,
    dividerHeight = 1.dp, // Keep 1dp for crisp lines
    indicatorHeight = (4 * scale).dp,
    tabIndicatorHeight = (3 * scale).dp,

    // Special sizes
    donutChartSize = (100 * scale).dp,
    searchBarHeight = (48 * scale).dp,
    textFieldHeight = (56 * scale).dp,
)

/**
 * CompositionLocal for accessing dimensions throughout the app.
 */
val LocalDimensions = staticCompositionLocalOf { createDimensions(1f) }
