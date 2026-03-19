package com.example.financetracker.presentation.wallet

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.financetracker.presentation.model.AccountUiModel
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextPrimary
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.core.util.ValidationUtils
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddWalletSheet(
    existingWallet: AccountUiModel? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        AddWalletContent(existingWallet, onDismiss, onSave)
    }
}

@Composable
fun AddWalletContent(
    existingWallet: AccountUiModel? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, String) -> Unit
) {
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    var name by remember { mutableStateOf(existingWallet?.name ?: "") }
    var amount by remember { mutableStateOf(existingWallet?.rawBalance?.let { kotlin.math.abs(it / 100.0).toString() } ?: "") }
    var type by remember { mutableStateOf(existingWallet?.type ?: "BANK") }

    val walletTypes = listOf("BANK", "CASH", "CREDIT", "INVESTMENT")
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
                    if (existingWallet == null) "New Account" else "Edit Account",
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
                Text("Current Balance", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
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

            // Name Input
            Column {
                Text("Account Name", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. HDFC Bank") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = highlightColor,
                        focusedBorderColor = FinosMint
                    )
                )
            }

            // Type Selection (Chips)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Account Type", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween 
                    // Simple logic to wrap is complex in Row, using simple map for now or FlowRow if available
                ) {
                   // Using a simple scrollable row is safer
                }
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(walletTypes.size) { index ->
                        val t = walletTypes[index]
                        val isSelected = type == t
                        FilterChip(
                            selected = isSelected,
                            onClick = { type = t },
                            label = { Text(t) },
                            leadingIcon = if (isSelected) {
                                { Icon(Icons.Rounded.Check, contentDescription = "Selected", modifier = Modifier.size(16.dp)) }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = FinosMint,
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))

            // Validate inputs
            val isValidName = ValidationUtils.isValidWalletName(name)
            val isValidAmount = amount.isBlank() || ValidationUtils.isValidAmount(amount)
            val canSave = isValidName && isValidAmount

            // Save Button
            Button(
                onClick = {
                    if (canSave) {
                         onSave(name.trim(), amount.ifBlank { "0" }, type)
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
                    if (existingWallet == null) "Create Account" else "Save Changes",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color.White
                )
            }

            // Bottom spacing for keyboard
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
