package com.example.financetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.core.util.CategoryIconUtil
import com.example.financetracker.presentation.theme.DarkSurface
import com.example.financetracker.presentation.theme.DarkSurfaceHighlight
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.isDarkTheme

// ============== ICON PICKER ==============

/**
 * Icon picker dialog for selecting category icons.
 *
 * @param selectedIconId Currently selected icon ID
 * @param onIconSelected Callback when an icon is selected
 * @param onDismiss Callback to dismiss the picker
 */
@Composable
fun IconPickerDialog(
    selectedIconId: Int,
    onIconSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isDarkTheme()
    val icons = CategoryIconUtil.availableIcons

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Select Icon",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(icons) { iconItem ->
                        val isSelected = iconItem.id == selectedIconId
                        IconPickerItem(
                            iconItem = iconItem,
                            isSelected = isSelected,
                            onClick = {
                                onIconSelected(iconItem.id)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun IconPickerItem(
    iconItem: CategoryIconUtil.IconItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val isDark = isDarkTheme()
    val backgroundColor = when {
        isSelected -> FinosMint.copy(alpha = 0.2f)
        isDark -> DarkSurfaceHighlight
        else -> Color.LightGray.copy(alpha = 0.1f)
    }
    val borderColor = if (isSelected) FinosMint else Color.Transparent

    Box(
        modifier = Modifier
            .size(52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = iconItem.icon,
            contentDescription = iconItem.name,
            tint = if (isSelected) FinosMint else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
    }
}

/**
 * Icon picker button that shows current icon and opens picker on click.
 */
@Composable
fun IconPickerButton(
    currentIconId: Int,
    colorHex: String,
    enabled: Boolean = true,
    onIconSelected: (Int) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    val icon = CategoryIconUtil.getIconById(currentIconId)
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        AppLogger.w("IconPicker", "Invalid color hex in IconPickerButton: $colorHex", e)
        FinosMint
    }

    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.15f))
            .then(
                if (enabled) {
                    Modifier.clickable { showPicker = true }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Category Icon",
            tint = color,
            modifier = Modifier.size(28.dp)
        )
    }

    if (showPicker && enabled) {
        IconPickerDialog(
            selectedIconId = currentIconId,
            onIconSelected = onIconSelected,
            onDismiss = { showPicker = false }
        )
    }
}

// ============== COLOR PICKER ==============

/**
 * Available colors for category customization
 */
object CategoryColors {
    val availableColors = listOf(
        "#EF4444", // Red
        "#F97316", // Orange
        "#F59E0B", // Amber
        "#EAB308", // Yellow
        "#84CC16", // Lime
        "#22C55E", // Green
        "#10B981", // Emerald (FinosMint-like)
        "#14B8A6", // Teal
        "#06B6D4", // Cyan
        "#0EA5E9", // Sky
        "#3B82F6", // Blue
        "#6366F1", // Indigo
        "#8B5CF6", // Violet
        "#A855F7", // Purple
        "#D946EF", // Fuchsia
        "#EC4899", // Pink
        "#F43F5E", // Rose
        "#78716C", // Stone
        "#6B7280", // Gray
        "#1F2937"  // Dark Gray
    )
}

/**
 * Color picker dialog for selecting category colors.
 *
 * @param selectedColorHex Currently selected color hex string
 * @param onColorSelected Callback when a color is selected
 * @param onDismiss Callback to dismiss the picker
 */
@Composable
fun ColorPickerDialog(
    selectedColorHex: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Select Color",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))

                LazyVerticalGrid(
                    columns = GridCells.Fixed(5),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(CategoryColors.availableColors) { colorHex ->
                        val isSelected = colorHex.equals(selectedColorHex, ignoreCase = true)
                        ColorPickerItem(
                            colorHex = colorHex,
                            isSelected = isSelected,
                            onClick = {
                                onColorSelected(colorHex)
                                onDismiss()
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPickerItem(
    colorHex: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val color = try {
        Color(android.graphics.Color.parseColor(colorHex))
    } catch (e: Exception) {
        AppLogger.w("IconPicker", "Invalid color hex in ColorPickerItem: $colorHex", e)
        FinosMint
    }

    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Rounded.Check,
                contentDescription = "Selected",
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

/**
 * Color picker button that shows current color and opens picker on click.
 */
@Composable
fun ColorPickerButton(
    currentColorHex: String,
    enabled: Boolean = true,
    onColorSelected: (String) -> Unit
) {
    var showPicker by remember { mutableStateOf(false) }

    val color = try {
        Color(android.graphics.Color.parseColor(currentColorHex))
    } catch (e: Exception) {
        AppLogger.w("IconPicker", "Invalid color hex in ColorPickerButton: $currentColorHex", e)
        FinosMint
    }

    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.outline,
                shape = CircleShape
            )
            .then(
                if (enabled) {
                    Modifier.clickable { showPicker = true }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        // Empty - just show the color
    }

    if (showPicker && enabled) {
        ColorPickerDialog(
            selectedColorHex = currentColorHex,
            onColorSelected = onColorSelected,
            onDismiss = { showPicker = false }
        )
    }
}

// Helper function to calculate luminance
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
