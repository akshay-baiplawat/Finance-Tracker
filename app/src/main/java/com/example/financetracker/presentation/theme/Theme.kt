package com.example.financetracker.presentation.theme

import android.app.Activity
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

// --- Color Scheme ---
// We prioritize Light Mode for V2.0 MVP as per "Pastel" aesthetic vibes, 
// but define dark variants safely if needed (for now mapping same or safe defaults).
// The requirement explicitly mentions "BrandBackground = Off-White", implying light theme default.
// --- Color Schemes ---
private val LightColorScheme = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color(0xFF1A1C1E), // Soft Black on Mint
    secondary = BrandSecondary,
    onSecondary = Color.White,
    background = BrandBackground,
    surface = Color.White,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorState
)

private val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = FinosPrimaryDark,
    onPrimary = Color(0xFF0F172A), // Dark Slate Text on Green
    secondary = BrandAccentDark,
    onSecondary = TextPrimaryDark,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = ExpenseDark,
    onError = Color.White,
    outline = DarkSurfaceHighlight // Use outline for borders
)

// --- Shapes ---
val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp), // CardShape
    large = RoundedCornerShape(24.dp)   // SheetShape
)

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    displaySize: String = DisplaySize.MEDIUM,
    // Dynamic color is available on Android 12+, but we want to ENFORCE our Trust-First brand.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Calculate scale factor based on display size
    val scale = DisplaySize.getScale(displaySize)
    val dimensions = createDimensions(scale)
    val typography = scaledTypography(scale)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = android.graphics.Color.TRANSPARENT // Transparent for Edge-to-Edge
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    CompositionLocalProvider(
        LocalDimensions provides dimensions
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = Shapes,
            content = content
        )
    }
}
