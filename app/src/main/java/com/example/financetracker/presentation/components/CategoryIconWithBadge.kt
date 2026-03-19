package com.example.financetracker.presentation.components

import android.graphics.Color as AndroidColor
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.financetracker.presentation.theme.FinosMint

/**
 * Wrapper composable that renders CategoryIcon with an optional paperclip badge
 * to indicate the presence of attachments.
 *
 * @param categoryName The name of the category
 * @param iconId The iconId stored in the category entity
 * @param colorHex The color hex code for the category
 * @param size The size of the icon container
 * @param useRoundedCorners Whether to use rounded corners (true) or circle shape (false)
 * @param attachmentCount Number of attachments (badge shown if > 0)
 * @param modifier Additional modifiers
 */
@Composable
fun CategoryIconWithBadge(
    categoryName: String,
    iconId: Int,
    colorHex: String,
    size: Dp = 48.dp,
    useRoundedCorners: Boolean = true,
    attachmentCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        // Main category icon
        CategoryIcon(
            categoryName = categoryName,
            iconId = iconId,
            colorHex = colorHex,
            size = size,
            useRoundedCorners = useRoundedCorners
        )

        // Attachment badge (paperclip icon) at top-right
        if (attachmentCount > 0) {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .clip(CircleShape)
                    .background(Color.White)
                    .border(1.dp, Color.LightGray, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.AttachFile,
                    contentDescription = "$attachmentCount attachments",
                    tint = FinosMint,
                    modifier = Modifier.size(10.dp)
                )
            }
        }
    }
}

/**
 * Small version of CategoryIconWithBadge for compact layouts
 */
@Composable
fun CategoryIconWithBadgeSmall(
    categoryName: String,
    iconId: Int,
    colorHex: String,
    attachmentCount: Int = 0,
    modifier: Modifier = Modifier
) {
    CategoryIconWithBadge(
        categoryName = categoryName,
        iconId = iconId,
        colorHex = colorHex,
        size = 32.dp,
        useRoundedCorners = false,
        attachmentCount = attachmentCount,
        modifier = modifier
    )
}
