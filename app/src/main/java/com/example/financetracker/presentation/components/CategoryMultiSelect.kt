package com.example.financetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.data.local.entity.CategoryEntity
import com.example.financetracker.presentation.theme.FinosMint

/**
 * Multi-select category picker with horizontal scrolling chips.
 *
 * @param categories List of all available categories
 * @param selectedCategoryIds Set of currently selected category IDs
 * @param onSelectionChanged Callback when selection changes
 * @param modifier Modifier for the component
 */
@Composable
fun CategoryMultiSelect(
    categories: List<CategoryEntity>,
    selectedCategoryIds: Set<Long>,
    onSelectionChanged: (Set<Long>) -> Unit,
    modifier: Modifier = Modifier
) {
    // Group categories by type for better organization
    val expenseCategories = categories.filter { it.type == "EXPENSE" }
    val incomeCategories = categories.filter { it.type == "INCOME" }

    Column(modifier = modifier) {
        // Expense categories section
        if (expenseCategories.isNotEmpty()) {
            Text(
                text = "Expenses",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(expenseCategories) { category ->
                    CategoryMultiSelectChip(
                        category = category,
                        isSelected = selectedCategoryIds.contains(category.id),
                        onToggle = {
                            val newSelection = if (selectedCategoryIds.contains(category.id)) {
                                selectedCategoryIds - category.id
                            } else {
                                selectedCategoryIds + category.id
                            }
                            onSelectionChanged(newSelection)
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        // Income categories section
        if (incomeCategories.isNotEmpty()) {
            Text(
                text = "Income",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(incomeCategories) { category ->
                    CategoryMultiSelectChip(
                        category = category,
                        isSelected = selectedCategoryIds.contains(category.id),
                        onToggle = {
                            val newSelection = if (selectedCategoryIds.contains(category.id)) {
                                selectedCategoryIds - category.id
                            } else {
                                selectedCategoryIds + category.id
                            }
                            onSelectionChanged(newSelection)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryMultiSelectChip(
    category: CategoryEntity,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val categoryColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        AppLogger.w("CategoryMultiSelect", "Invalid color hex: ${category.colorHex} for category: ${category.name}", e)
        FinosMint
    }

    FilterChip(
        selected = isSelected,
        onClick = onToggle,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                CategoryIconSmall(
                    categoryName = category.name,
                    iconId = category.iconId,
                    colorHex = category.colorHex,
                    size = 18.dp
                )
                Text(category.name)
            }
        },
        leadingIcon = if (isSelected) {
            {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = categoryColor.copy(alpha = 0.2f),
            selectedLabelColor = categoryColor,
            selectedLeadingIconColor = categoryColor
        )
    )
}

/**
 * Small category icon for use in chips and compact displays.
 */
@Composable
private fun CategoryIconSmall(
    categoryName: String,
    iconId: Int,
    colorHex: String,
    size: androidx.compose.ui.unit.Dp = 18.dp
) {
    CategoryIcon(
        categoryName = categoryName,
        iconId = iconId,
        colorHex = colorHex,
        size = size,
        useRoundedCorners = true
    )
}
