package com.example.financetracker.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 1. Design System Data
object CategoryDesignSystem {
    val DeepForestGreen = Color(0xFF0F3D3E)
    val OffWhiteSurface = Color(0xFFF8F9FA)

    val PastelPalette = listOf(
        Color(0xFFFFB7B2), Color(0xFFFFDAC1), Color(0xFFE2F0CB), Color(0xFFB5EAD7),
        Color(0xFFC7CEEA), Color(0xFFF8BBD0), Color(0xFFD1C4E9), Color(0xFFB3E5FC),
        Color(0xFFDCEDC8), Color(0xFFFFE0B2), Color(0xFFCFD8DC), Color(0xFFF0F4C3)
    )

    // Using System fonts as fallback if R.font is missing, but keeping structure as if they exist
    val interFontFamily = FontFamily.SansSerif
}

data class Category(
    val id: String,
    val name: String,
    val color: Color
)

// 2. Component A: The "Smart Chip"
@Composable
fun CategoryChip(
    label: String,
    color: Color?,
    isSelected: Boolean,
    isAddButton: Boolean = false,
    onClick: () -> Unit
) {
    val height = 40.dp
    val shape = CircleShape

    if (isAddButton) {
        Box(
            modifier = Modifier
                .height(height)
                .clip(shape)
                .clickable(onClick = onClick)
                .drawBehind {
                    drawRoundRect(
                        color = Color.LightGray,
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(height.toPx() / 2)
                    )
                }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add New",
                tint = Color.Gray
            )
        }
    } else {
        val backgroundColor = if (isSelected) color ?: Color.White else Color.White
        val borderColor = if (isSelected) Color.Black else Color.LightGray
        val textColor = if (isSelected) Color.Black else Color.DarkGray
        val borderStroke = BorderStroke(1.dp, borderColor)

        Surface(
            onClick = onClick,
            modifier = Modifier.height(height),
            shape = shape,
            color = backgroundColor,
            border = borderStroke,
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Text(
                    text = label,
                    color = textColor,
                    fontFamily = CategoryDesignSystem.interFontFamily,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 14.sp
                )
            }
        }
    }
}

// 3. Component B: The "Smart Chip Selector" Row
@Composable
fun ChipSelectorRow(
    categories: List<Category>,
    selectedId: String?,
    onAddClicked: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        items(categories) { category ->
            CategoryChip(
                label = category.name,
                color = category.color,
                isSelected = category.id == selectedId,
                onClick = { onCategorySelected(category.id) }
            )
        }
        item {
            CategoryChip(
                label = "",
                color = null,
                isSelected = false,
                isAddButton = true,
                onClick = onAddClicked
            )
        }
    }
}

// 4. Component C: The "Create Category" Bottom Sheet
@Composable
fun CreateCategorySheet(
    onDismiss: () -> Unit,
    onCreateClicked: (String, Color) -> Unit
) {
    var textInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(CategoryDesignSystem.PastelPalette.first()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CategoryDesignSystem.OffWhiteSurface)
            .padding(24.dp)
            .padding(bottom = 32.dp) // Extra bottom padding
    ) {
        Text(
            text = "New Category",
            fontFamily = CategoryDesignSystem.interFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Live Preview Section
        Text(
            text = "Preview:",
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            CategoryChip(
                label = if (textInput.isBlank()) "Category Name" else textInput,
                color = selectedColor,
                isSelected = true,
                onClick = {}
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Input Field
        OutlinedTextField(
            value = textInput,
            onValueChange = { textInput = it },
            placeholder = { Text("Category Name (e.g., Gym)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = {
                if (textInput.isNotBlank()) onCreateClicked(textInput, selectedColor)
            })
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Color Picker Grid
        Text(
            text = "Pick a color:",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.height(200.dp) // Fixed height for grid in sheet
        ) {
            items(CategoryDesignSystem.PastelPalette) { color ->
                ColorOption(
                    color = color,
                    isSelected = color == selectedColor,
                    onClick = { selectedColor = color }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Primary Action Button
        Button(
            onClick = {
                if (textInput.isNotBlank()) {
                    onCreateClicked(textInput, selectedColor)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = CategoryDesignSystem.DeepForestGreen),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Create Category",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun ColorOption(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(color)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.Black
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CategoryComponentsPreview() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text("Selector Row:", fontWeight = FontWeight.Bold)
        ChipSelectorRow(
            categories = listOf(
                Category("1", "Food", CategoryDesignSystem.PastelPalette[0]),
                Category("2", "Travel", CategoryDesignSystem.PastelPalette[3]),
                Category("3", "Entertainment", CategoryDesignSystem.PastelPalette[6])
            ),
            selectedId = "2",
            onAddClicked = {},
            onCategorySelected = {}
        )

        Spacer(modifier = Modifier.height(32.dp))
        Text("Create Sheet:", fontWeight = FontWeight.Bold)
        CreateCategorySheet(onDismiss = {}, onCreateClicked = { _, _ -> })
    }
}
