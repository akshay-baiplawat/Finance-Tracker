package com.example.financetracker.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.financetracker.presentation.theme.DarkSurfaceHighlight
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.presentation.theme.isDarkTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Date picker field for transaction entry.
 * Shows the selected date and opens a Material3 DatePickerDialog when clicked.
 *
 * @param selectedTimestamp The currently selected date as timestamp (millis)
 * @param onDateSelected Callback when a date is selected, returns timestamp in millis
 * @param modifier Optional modifier for the component
 * @param label Optional label text (defaults to "Transaction Date")
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionDatePicker(
    selectedTimestamp: Long,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Transaction Date"
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val isDark = isDarkTheme()

    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy") }

    val displayText = remember(selectedTimestamp) {
        val localDate = Instant.ofEpochMilli(selectedTimestamp)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        localDate.format(dateFormatter)
    }

    OutlinedTextField(
        value = displayText,
        onValueChange = {},
        readOnly = true,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = "Select date",
                tint = TextSecondaryDark
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDatePicker = true },
        enabled = false,
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = if (isDark) DarkSurfaceHighlight else Color.LightGray.copy(alpha = 0.5f),
            disabledLeadingIconColor = TextSecondaryDark,
            disabledLabelColor = TextSecondaryDark
        )
    )

    // Invisible clickable overlay since disabled TextField doesn't receive clicks
    if (!showDatePicker) {
        androidx.compose.foundation.layout.Box(
            modifier = modifier
                .fillMaxWidth()
                .clickable { showDatePicker = true }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedTimestamp
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedDateMillis ->
                            // Preserve the time of day from the original timestamp
                            val originalInstant = Instant.ofEpochMilli(selectedTimestamp)
                            val originalTime = originalInstant.atZone(ZoneId.systemDefault()).toLocalTime()

                            val selectedDate = Instant.ofEpochMilli(selectedDateMillis)
                                .atZone(ZoneId.systemDefault())
                                .toLocalDate()

                            // Combine selected date with original time (or current time for new transactions)
                            val combinedDateTime = selectedDate.atTime(originalTime)
                                .atZone(ZoneId.systemDefault())
                                .toInstant()
                                .toEpochMilli()

                            onDateSelected(combinedDateTime)
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
