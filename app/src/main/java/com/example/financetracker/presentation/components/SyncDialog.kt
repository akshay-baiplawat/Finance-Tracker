package com.example.financetracker.presentation.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.financetracker.presentation.theme.DarkSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncDialog(onDismiss: () -> Unit, onSync: (Long) -> Unit, isDark: Boolean) {
    var showDatePicker by remember { mutableStateOf(false) }
    
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
             selectableDates = object : SelectableDates {
                 override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                     return utcTimeMillis <= System.currentTimeMillis()
                 }
             }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { date ->
                        onSync(date)
                    }
                    showDatePicker = false
                    onDismiss()
                }) { Text("Confirm") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Back") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    } else {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text("Sync Transactions") },
            text = { 
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    // Force colors: White for Dark Mode, standard onSurface (Black) for Light Mode
                    val textColor = if (isDark) Color.White else MaterialTheme.colorScheme.onSurface
                    val borderColor = if (isDark) Color.White.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                    Text("Select time range to scan:", color = textColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    val buttonColors = ButtonDefaults.outlinedButtonColors(contentColor = textColor)
                    val buttonBorder = androidx.compose.foundation.BorderStroke(1.dp, borderColor)

                    OutlinedButton(
                        onClick = { 
                            val cal = java.util.Calendar.getInstance()
                            cal.add(java.util.Calendar.DAY_OF_YEAR, -15)
                            onSync(cal.timeInMillis)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = buttonColors,
                        border = buttonBorder
                    ) { Text("Last 15 Days", color = textColor) }
                    
                    OutlinedButton(
                        onClick = {
                            val cal = java.util.Calendar.getInstance()
                            cal.add(java.util.Calendar.DAY_OF_YEAR, -30)
                            onSync(cal.timeInMillis)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = buttonColors,
                        border = buttonBorder
                    ) { Text("Last 30 Days", color = textColor) }
                    
                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = buttonColors,
                        border = buttonBorder
                    ) { Text("Custom Date...", color = textColor) }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        )
    }
}
