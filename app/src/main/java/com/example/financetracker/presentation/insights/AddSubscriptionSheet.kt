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
import androidx.compose.material.icons.rounded.DateRange
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import com.example.financetracker.presentation.model.SubscriptionUiModel
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextPrimary
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick
import java.time.format.DateTimeFormatter
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionSheet(
    existingSubscription: SubscriptionUiModel? = null,
    onDismiss: () -> Unit,
    onSave: (Long?, String, Long, String, Long, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        AddSubscriptionContent(existingSubscription, onDismiss, onSave)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddSubscriptionContent(
    existingSubscription: SubscriptionUiModel? = null,
    onDismiss: () -> Unit,
    onSave: (Long?, String, Long, String, Long, String) -> Unit
) {
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    var merchant by remember { mutableStateOf(existingSubscription?.merchantName ?: "") }
    var amount by remember { mutableStateOf(existingSubscription?.amount?.div(100)?.toString() ?: "") }
    var selectedColor by remember { mutableStateOf(existingSubscription?.colorHex ?: "#FF5722") }
    
    // Fix Date Initialization
    val initialDate = remember {
         if (existingSubscription != null) {
              java.time.Instant.ofEpochMilli(existingSubscription.nextDueDate)
                  .atZone(java.time.ZoneId.systemDefault())
                  .toLocalDate()
         } else {
             LocalDate.now().plusMonths(1)
         }
    }
    // Track selected date
    var selectedDate by remember { mutableStateOf(initialDate) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Frequency
    var expandedFreq by remember { mutableStateOf(false) }
    var frequency by remember { mutableStateOf(existingSubscription?.frequency ?: "Monthly") }
    val frequencies = listOf("Weekly", "Monthly", "Yearly")

    val colors = listOf(
        "#FF5722", // Deep Orange
        "#F44336", // Red
        "#E91E63", // Pink
        "#9C27B0", // Purple
        "#2196F3", // Blue
        "#4CAF50", // Green
        "#FFC107"  // Amber
    )
    
     val isDark = MaterialTheme.colorScheme.surface == com.example.financetracker.presentation.theme.DarkSurface
     val highlightColor = if(isDark) com.example.financetracker.presentation.theme.DarkSurfaceHighlight else Color.LightGray.copy(alpha=0.5f)
     val scrollState = rememberScrollState()

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        selectedDate = java.time.Instant.ofEpochMilli(millis).atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                    }
                    showDatePicker = false
                }) {
                    Text("OK", color = FinosMint)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondaryDark)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

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
                    if (existingSubscription == null) "New Subscription" else "Edit Subscription",
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
            
            // Amount Input (Prominent)
             Column {
                Text("Amount", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { input ->
                        if (input.matches(Regex("^\\d*\\.?\\d{0,2}$"))) {
                            amount = input
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                     prefix = { Text("₹", style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = highlightColor,
                        focusedBorderColor = FinosMint,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Merchant Input
             Column {
                Text("Service Name", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    placeholder = { Text("e.g. Netflix") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = highlightColor,
                        focusedBorderColor = FinosMint,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )
            }

            // Frequency & Date Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Frequency Dropdown
                Box(modifier = Modifier.weight(1f)) {
                     Column {
                        Text("Frequency", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = frequency,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = {
                                IconButton(onClick = { expandedFreq = true }) {
                                    Icon(Icons.Rounded.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth().clickable { expandedFreq = true },
                            enabled = false, 
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                disabledBorderColor = highlightColor,
                                disabledLabelColor = TextSecondaryDark,
                                disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                            ),
                             textStyle = MaterialTheme.typography.bodyLarge
                        )
                    }
                    // Invisible clickable overlay
                    Box(modifier = Modifier.matchParentSize().clickable { expandedFreq = true })
                    
                    DropdownMenu(
                        expanded = expandedFreq,
                        onDismissRequest = { expandedFreq = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        frequencies.forEach { freq ->
                            DropdownMenuItem(
                                text = { Text(freq) },
                                onClick = {
                                    frequency = freq
                                    expandedFreq = false
                                }
                            )
                        }
                    }
                }
                
                // Date
                Box(modifier = Modifier.weight(1f)) {
                     Column {
                        Text("Next Due", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = selectedDate.format(DateTimeFormatter.ofPattern("MMM dd")),
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { Icon(Icons.Rounded.DateRange, contentDescription = null) },
                            modifier = Modifier.fillMaxWidth().clickable { showDatePicker = true },
                            enabled = false, 
                            shape = RoundedCornerShape(16.dp),
                             colors = OutlinedTextFieldDefaults.colors(
                                    disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                    disabledBorderColor = highlightColor,
                                    disabledLabelColor = TextSecondaryDark,
                                    disabledTrailingIconColor = MaterialTheme.colorScheme.onSurface
                                ),
                             textStyle = MaterialTheme.typography.bodyLarge
                        )
                     }
                     // Invisible clickable overlay for date
                     Box(modifier = Modifier.matchParentSize().clickable { showDatePicker = true })
                }
            }

            // Color Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Label Color", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
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

            Spacer(modifier = Modifier.height(8.dp))

            // Save Button
            Button(
                onClick = {
                    val amountVal = amount.toDoubleOrNull() ?: 0.0
                    if (merchant.isNotBlank() && amountVal > 0) {
                         val limitCents = (amountVal * 100).toLong()
                         val dateMillis = selectedDate.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                         // ID is passed if existing, logic handled by caller
                        onSave(existingSubscription?.id, merchant, limitCents, frequency, dateMillis, selectedColor)
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
                    if (existingSubscription == null || existingSubscription.isDetected) "Confirm Subscription"
                    else "Save Changes",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // Bottom spacing for keyboard
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
