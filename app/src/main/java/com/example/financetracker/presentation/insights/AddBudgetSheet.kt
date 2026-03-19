package com.example.financetracker.presentation.insights

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextPrimary
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheet(
    existingBudget: com.example.financetracker.presentation.model.BudgetUiModel? = null,
    categories: List<com.example.financetracker.data.local.entity.CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Long?, String, Long, String, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        AddBudgetContent(existingBudget, categories, onDismiss, onSave)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetContent(
    existingBudget: com.example.financetracker.presentation.model.BudgetUiModel? = null,
    categories: List<com.example.financetracker.data.local.entity.CategoryEntity> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (Long?, String, Long, String, Boolean) -> Unit
) {
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    var category by remember { mutableStateOf(existingBudget?.categoryName ?: "") }
    var amount by remember { mutableStateOf(existingBudget?.limitAmount?.div(100)?.toString() ?: "") }
    var selectedColor by remember { mutableStateOf(existingBudget?.colorHex ?: "#4CAF50") }
    var isRolloverEnabled by remember { mutableStateOf(existingBudget?.rolloverEnabled ?: false) }
    
    var expanded by remember { mutableStateOf(false) }

    val colors = listOf(
        "#4CAF50", // Green
        "#2196F3", // Blue
        "#FFC107", // Amber
        "#9C27B0", // Purple
        "#F44336", // Red
        "#00BCD4", // Cyan
        "#FF9800"  // Orange
    )

    val isDark = MaterialTheme.colorScheme.surface == com.example.financetracker.presentation.theme.DarkSurface
    val highlightColor = if(isDark) com.example.financetracker.presentation.theme.DarkSurfaceHighlight else Color.LightGray.copy(alpha=0.5f)
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header - Fixed (not scrollable)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FinosMint)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = keyboardAwareOnClick,
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White, CircleShape)
                ) {
                    Icon(
                        androidx.compose.material.icons.Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    if (existingBudget == null) "New Monthly Budget" else "Edit Budget",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color.Black
                )
            }
        }

        // Scrollable form content
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            
            // Amount Input
            Column {
                Text("Monthly Limit", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.all { char -> char.isDigit() }) amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    prefix = { Text("₹", style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = highlightColor,
                        focusedBorderColor = FinosMint,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Category Input (Dropdown)
            Column {
                Text("Category", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = { },
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        colors = OutlinedTextFieldDefaults.colors(
                            unfocusedBorderColor = highlightColor,
                            focusedBorderColor = FinosMint,
                            focusedTextColor = MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                        ),
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        textStyle = MaterialTheme.typography.bodyLarge
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface).heightIn(max = 300.dp)
                    ) {
                        val filteredCategories = categories.filter { it.type == "EXPENSE" }
                        
                        // Option 1: All Expenses
                        DropdownMenuItem(
                            text = { Text("All Expenses", fontWeight = FontWeight.Bold) },
                            onClick = { 
                                category = "All Expenses"
                                selectedColor = "#F44336" // Red for generic expense
                                expanded = false 
                            }
                        )
                        HorizontalDivider(thickness = 0.5.dp, color = highlightColor)

                        if (filteredCategories.isEmpty()) {
                                DropdownMenuItem(
                                text = { Text("No categories found") },
                                onClick = { expanded = false }
                            )
                        } else {
                            filteredCategories.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.name) },
                                    onClick = { 
                                        category = cat.name
                                        selectedColor = cat.colorHex // Auto-select color
                                        expanded = false 
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Color Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Color Tag", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    colors.forEach { colorHex ->
                        val color = Color(android.graphics.Color.parseColor(colorHex))
                        val isSelected = selectedColor == colorHex
                        
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = colorHex }
                                .border(
                                    if (isSelected) 3.dp else 0.dp,
                                    if (isSelected) MaterialTheme.colorScheme.onSurface else Color.Transparent, 
                                    CircleShape
                                )
                        )
                    }
                }
            }

            // Rollover Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha=0.3f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Rollover Budget", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Unused budget will carry over to next month.", 
                        style = MaterialTheme.typography.bodySmall, 
                        color = TextSecondaryDark
                    )
                }
                Switch(
                    checked = isRolloverEnabled,
                    onCheckedChange = { isRolloverEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FinosMint)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val limit = amount.toLongOrNull() ?: 0L
                    if (category.isNotBlank() && limit > 0) {
                        onSave(existingBudget?.id, category, limit * 100, selectedColor, isRolloverEnabled) // Convert to Paisa
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinosMint)
            ) {
                Text(
                    if (existingBudget == null) "Create Budget" else "Save Changes",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // Bottom spacing for keyboard
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
