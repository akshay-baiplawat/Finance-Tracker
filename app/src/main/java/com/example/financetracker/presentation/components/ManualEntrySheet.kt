package com.example.financetracker.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.financetracker.data.local.entity.TransactionType
import com.example.financetracker.presentation.theme.BrandPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    onDismiss: () -> Unit,
    onSave: (amount: Long, merchant: String, type: String, categoryId: Long) -> Unit,
    onCreateCategory: (String, String) -> Unit,
    categories: List<com.example.financetracker.data.local.entity.CategoryEntity>
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var amountText by remember { mutableStateOf("") }
    var merchantText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<com.example.financetracker.data.local.entity.CategoryEntity?>(null) }
    var type by remember { mutableStateOf(TransactionType.DEBIT) }
    
    var showCreateCategorySheet by remember { mutableStateOf(false) }

    if (showCreateCategorySheet) {
        CreateCategoryBottomSheet(
            onDismiss = { showCreateCategorySheet = false },
            onCreateCategory = { name, color ->
                onCreateCategory(name, color)
                showCreateCategorySheet = false
            }
        )
    }

    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp) // Outer padding for the footer area
        ) {
            // Header and Scrollable Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false) // Allow wrapping but respect max height
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                Text(
                    text = "Add Transaction",
                    style = MaterialTheme.typography.headlineSmall,
                    color = BrandPrimary
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Type Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val inactiveColor = if (androidx.compose.foundation.isSystemInDarkTheme()) androidx.compose.ui.graphics.Color.DarkGray else androidx.compose.ui.graphics.Color.LightGray
                    
                    // Expense Button
                    Button(
                        onClick = { type = TransactionType.DEBIT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == TransactionType.DEBIT) BrandPrimary else inactiveColor
                        )
                    ) {
                        Text("Expense", color = androidx.compose.ui.graphics.Color.White)
                    }
                    
                    // Income Button
                    Button(
                        onClick = { type = TransactionType.CREDIT },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (type == TransactionType.CREDIT) com.example.financetracker.presentation.theme.IncomeData else inactiveColor
                        )
                    ) {
                        Text("Income", color = androidx.compose.ui.graphics.Color.White)
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                val textFieldColors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) com.example.financetracker.presentation.theme.DarkInputSurface else androidx.compose.ui.graphics.Color.Transparent,
                    unfocusedContainerColor = if (androidx.compose.foundation.isSystemInDarkTheme()) com.example.financetracker.presentation.theme.DarkInputSurface else androidx.compose.ui.graphics.Color.Transparent,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Merchant Field
                OutlinedTextField(
                    value = merchantText,
                    onValueChange = { merchantText = it },
                    label = { Text("Merchant / Description") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = textFieldColors
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selector
                Text("Category", style = MaterialTheme.typography.labelLarge, color = BrandPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                
                CategoryChipSelector(
                    categories = categories,
                    selectedCategory = selectedCategory,
                    onCategorySelected = { selectedCategory = it },
                    onAddNewClick = { showCreateCategorySheet = true } 
                )
                
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Sticky Footer Button
            Button(
                onClick = {
                    val amountDouble = amountText.toDoubleOrNull()
                    if (amountDouble != null && merchantText.isNotBlank()) {
                         val amountPaisa = (amountDouble * 100).toLong()
                         onSave(
                             amountPaisa, 
                             merchantText, 
                             if (type == TransactionType.DEBIT) "EXPENSE" else "INCOME", 
                             selectedCategory?.id ?: 1L
                         )
                         onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp) // Match horizontal padding
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text("Save Transaction", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
