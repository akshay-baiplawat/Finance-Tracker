package com.example.financetracker.presentation.wallet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetracker.presentation.model.PersonUiModel
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextPrimary
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.core.util.ValidationUtils
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonSheet(
    existingPerson: PersonUiModel? = null,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        AddPersonContent(existingPerson, onDismiss, onSave)
    }
}

@Composable
fun AddPersonContent(
    existingPerson: PersonUiModel? = null,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    var name by remember { mutableStateOf(existingPerson?.name ?: "") }
    var amount by remember { mutableStateOf(existingPerson?.balance?.let { kotlin.math.abs(it / 100.0).toString() } ?: "") }
    // Logic: + means they owe you (Default), - means you owe them?
    // Let's use a Chip/Toggle for "Who owes who?"
    // Current Wallet logic: + Positive Balance = They Owe You, - Negative = You Owe Them.
    
    var oweType by remember { mutableStateOf(if ((existingPerson?.balance ?: 0L) >= 0) "THEY_OWE" else "I_OWE") }

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
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    if (existingPerson == null) "New Contact" else "Edit Contact",
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
                Text("Balance Amount", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { if (it.matches(Regex("^\\d*\\.?\\d{0,2}$"))) amount = it },
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                    prefix = { Text("₹", style = androidx.compose.ui.text.TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = highlightColor,
                        focusedBorderColor = FinosMint
                    )
                )
            }
            
            // Owe Type Toggle
             Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                 FilterChip(
                    selected = oweType == "THEY_OWE",
                    onClick = { oweType = "THEY_OWE" },
                    label = { Text("They Owe Me") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = FinosMint,
                        selectedLabelColor = Color.White
                    )
                )
                FilterChip(
                    selected = oweType == "I_OWE",
                    onClick = { oweType = "I_OWE" },
                    label = { Text("I Owe Them") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF44336), // Red for debt
                        selectedLabelColor = Color.White
                    )
                )
            }

            // Name Input
            Column {
                Text("Name", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. John Doe") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = highlightColor,
                        focusedBorderColor = FinosMint
                    )
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Validate inputs
            val isValidName = ValidationUtils.isValidPersonName(name)
            val rawAmountForValidation = amount.removePrefix("-")
            val isValidAmount = amount.isBlank() || ValidationUtils.isValidAmount(rawAmountForValidation)
            val canSave = isValidName && isValidAmount

            // Save Button
            Button(
                onClick = {
                    if (canSave) {
                         // Logic transfer
                         val rawAmount = amount.ifBlank { "0" }
                         val typePrefix = if(oweType == "I_OWE") "-" else "" // if I owe, it's negative
                         val finalAmountString = typePrefix + rawAmount.removePrefix("-")
                         onSave(name.trim(), finalAmountString)
                         onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = FinosMint),
                enabled = canSave
            ) {
                Text(
                    if (existingPerson == null) "Add Person" else "Save Changes",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // Bottom spacing for keyboard
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
