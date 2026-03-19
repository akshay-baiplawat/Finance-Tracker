package com.example.financetracker.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.presentation.theme.DarkSurface
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.presentation.theme.isDarkTheme
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick
import java.util.Currency
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerSheet(
    currentCurrencyCode: String,
    onDismiss: () -> Unit,
    onCurrencySelected: (String) -> Unit
) {
    val isDark = isDarkTheme()
    val containerColor = if (isDark) DarkSurface else MaterialTheme.colorScheme.surface
    val contentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.Black

    val currencies = remember {
        try {
            Currency.getAvailableCurrencies()
                .sortedBy { it.currencyCode }
                .toList()
        } catch (e: Exception) {
            AppLogger.w("CurrencyPickerSheet", "Failed to load available currencies", e)
            emptyList()
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    val filteredCurrencies = remember(searchQuery, currencies) {
        if (searchQuery.isBlank()) currencies
        else currencies.filter {
            it.currencyCode.contains(searchQuery, ignoreCase = true) ||
            it.displayName.contains(searchQuery, ignoreCase = true) ||
            it.symbol.contains(searchQuery, ignoreCase = true)
        }
    }

    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = containerColor,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp)
        ) {
            Text(
                "Select Currency",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                placeholder = { Text("Search currency") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondaryDark) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinosMint,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Divider(color = MaterialTheme.colorScheme.outlineVariant)
            
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(filteredCurrencies) { currency ->
                    val isSelected = currency.currencyCode == currentCurrencyCode
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onCurrencySelected(currency.currencyCode)
                                onDismiss()
                            }
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "${currency.currencyCode} (${currency.symbol})",
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                color = if (isSelected) FinosMint else contentColor
                            )
                            Text(
                                text = currency.displayName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondaryDark
                            )
                        }
                        
                        if (isSelected) {
                            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = FinosMint)
                        }
                    }
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), modifier = Modifier.padding(horizontal = 24.dp))
                }
            }
        }
    }
}
