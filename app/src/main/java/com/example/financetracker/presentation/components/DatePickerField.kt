package com.example.financetracker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.financetracker.presentation.theme.FinosMint
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Reusable date picker field with quick selection options.
 *
 * @param label The label shown above the field
 * @param selectedDate The currently selected date as timestamp (millis), null for special options
 * @param quickOptionLabel Label for the quick option (e.g., "From Day One", "Till Now")
 * @param isQuickOptionSelected Whether the quick option is currently selected
 * @param onDateSelected Callback when a specific date is selected
 * @param onQuickOptionSelected Callback when the quick option is selected
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerField(
    label: String,
    selectedDate: Long?,
    quickOptionLabel: String,
    isQuickOptionSelected: Boolean,
    onDateSelected: (Long) -> Unit,
    onQuickOptionSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    val displayText = when {
        isQuickOptionSelected -> quickOptionLabel
        selectedDate != null -> {
            val localDate = Instant.ofEpochMilli(selectedDate)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
            localDate.format(dateFormatter)
        }
        else -> "Select date"
    }

    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Quick option chip
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                selected = isQuickOptionSelected,
                onClick = onQuickOptionSelected,
                label = { Text(quickOptionLabel) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = FinosMint.copy(alpha = 0.2f),
                    selectedLabelColor = FinosMint
                )
            )

            Text(
                text = "or",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Date picker field
        OutlinedTextField(
            value = if (isQuickOptionSelected) "" else displayText,
            onValueChange = {},
            readOnly = true,
            placeholder = {
                Text(
                    "Select specific date",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Outlined.CalendarToday,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true },
            enabled = false,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                disabledTextColor = MaterialTheme.colorScheme.onSurface,
                disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                disabledLeadingIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                disabledPlaceholderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        )

        // Clickable overlay for the disabled text field
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .offset(y = (-56).dp)
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateSelected(millis)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = FinosMint)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor = FinosMint,
                    todayDateBorderColor = FinosMint
                )
            )
        }
    }
}
