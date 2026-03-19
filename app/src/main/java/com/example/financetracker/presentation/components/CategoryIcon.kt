package com.example.financetracker.presentation.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.core.util.CategoryIconUtil
import com.example.financetracker.presentation.theme.DarkSurface
import com.example.financetracker.presentation.theme.DarkSurfaceHighlight
import com.example.financetracker.presentation.theme.BrandBackground
import com.example.financetracker.presentation.theme.isDarkTheme

/**
 * Reusable composable for displaying category icons consistently across the app.
 *
 * @param categoryName The name of the category (used for fallback icon mapping)
 * @param iconId The iconId stored in the category entity
 * @param colorHex The color hex code for the category (used for background tint)
 * @param size The size of the icon container
 * @param useRoundedCorners Whether to use rounded corners (true) or circle shape (false)
 * @param modifier Additional modifiers
 */
@Composable
fun CategoryIcon(
    categoryName: String,
    iconId: Int,
    colorHex: String,
    size: Dp = 48.dp,
    useRoundedCorners: Boolean = true,
    modifier: Modifier = Modifier
) {
    val isDark = isDarkTheme()

    // Parse color from hex, with fallback
    val categoryColor = try {
        Color(AndroidColor.parseColor(colorHex))
    } catch (e: Exception) {
        AppLogger.w("CategoryIcon", "Invalid color hex: $colorHex, using fallback", e)
        MaterialTheme.colorScheme.primary
    }

    // Get the appropriate icon
    val icon = CategoryIconUtil.getIconForCategory(categoryName, iconId)

    // Calculate icon size (roughly 50% of container)
    val iconSize = size * 0.5f

    val shape = if (useRoundedCorners) RoundedCornerShape(12.dp) else CircleShape
    val backgroundColor = if (isDark) {
        categoryColor.copy(alpha = 0.15f)
    } else {
        categoryColor.copy(alpha = 0.12f)
    }

    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = categoryName,
            tint = categoryColor,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Compact version for use in dropdowns and lists
 */
@Composable
fun CategoryIconSmall(
    categoryName: String,
    iconId: Int,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    CategoryIcon(
        categoryName = categoryName,
        iconId = iconId,
        colorHex = colorHex,
        size = 32.dp,
        useRoundedCorners = false,
        modifier = modifier
    )
}
