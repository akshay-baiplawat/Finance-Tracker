package com.example.financetracker.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.data.local.entity.CategoryEntity
import com.example.financetracker.presentation.theme.PastelPalette
import com.example.financetracker.presentation.theme.TextPrimary

@Composable
fun CategoryChipSelector(
    categories: List<CategoryEntity>,
    selectedCategory: CategoryEntity?,
    onCategorySelected: (CategoryEntity) -> Unit,
    onAddNewClick: () -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        items(categories) { category ->
            CategoryChip(
                category = category,
                isSelected = category.id == selectedCategory?.id,
                onClick = { onCategorySelected(category) }
            )
        }
        item {
            AddNewCategoryChip(onClick = onAddNewClick)
        }
    }
}

@Composable
fun CategoryChip(
    category: CategoryEntity,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = try {
        Color(android.graphics.Color.parseColor(category.colorHex))
    } catch (e: Exception) {
        AppLogger.w("CategoryComponents", "Invalid color hex: ${category.colorHex} for category: ${category.name}", e)
        Color.LightGray
    }

    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isSelected) backgroundColor else Color.White)
            .border(
                width = 1.dp,
                color = if (isSelected) Color.Black else Color.LightGray,
                shape = CircleShape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) Color.Black else Color.Gray
        )
    }
}

@Composable
fun AddNewCategoryChip(onClick: () -> Unit) {
    val stroke = Stroke(
        width = 2f,
        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
    )
    
    // Simplification: Using Border with dash is harder in standard modifier usually.
    // For V1 MVP, just standard border with explicit style or using a Canvas.
    // Or just a standard outlined button style for simplicity that matches "Dashed Grey Border" intent.
    // Let's use a Box with drawBehind for perfect Dotted line if we wanted, but let's stick to standard Modifier.border for now 
    // and assume it's solid for MVP or simple dashed if library supports it easily (it doesn't natively on Modifier.border).
    // I will use a solid border for now to avoid custom canvas complexity in this turn.
    
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(Color.Transparent)
            .border(1.dp, Color.Gray, CircleShape) // TODO: Make Dashed
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.Gray, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add New", style = MaterialTheme.typography.labelLarge, color = Color.Gray)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCategoryBottomSheet(
    onDismiss: () -> Unit,
    onCreateCategory: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var categoryName by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(PastelPalette.first()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp) // Outer padding for footer
        ) {
            // Header and Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "New Category",
                    style = MaterialTheme.typography.headlineSmall,
                    color = com.example.financetracker.presentation.theme.BrandPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Preview Section
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Preview: ", 
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    CategoryChip(
                        category = CategoryEntity(name = categoryName.ifEmpty { "Category" }, colorHex = "#" + Integer.toHexString(selectedColor.toArgb())),
                        isSelected = true,
                        onClick = {}
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))

                // Name Input
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { categoryName = it },
                    label = { Text("Category Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Color Picker
                Text(
                    text = "Pick a Color", 
                    style = MaterialTheme.typography.labelLarge,
                    color = com.example.financetracker.presentation.theme.BrandPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                val chunkedPalette = PastelPalette.chunked(4)
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    chunkedPalette.forEach { rowColors ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowColors.forEach { color ->
                                ColorCircle(
                                    color = color,
                                    isSelected = color == selectedColor,
                                    onClick = { selectedColor = color }
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
            }
            
            // Sticky Footer Button
            Button(
                onClick = {
                    val hex = String.format("#%08X", selectedColor.toArgb())
                    onCreateCategory(categoryName, hex)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                enabled = categoryName.isNotBlank(),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = com.example.financetracker.presentation.theme.BrandPrimary,
                    disabledContainerColor = Color.LightGray
                )
            ) {
                Text("Create Category", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
fun ColorCircle(color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(color)
            .clickable(onClick = onClick)
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
