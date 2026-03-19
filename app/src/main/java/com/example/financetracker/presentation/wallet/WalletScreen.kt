package com.example.financetracker.presentation.wallet

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState 
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp


import com.example.financetracker.presentation.theme.*
import com.example.financetracker.presentation.components.NotificationIconWithBadge
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost

@Composable
fun WalletScreen(
    onSettingsClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    viewModel: WalletViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val unreadNotificationCount by viewModel.unreadNotificationCount.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddWallet by remember { mutableStateOf(false) }
    var editingWallet by remember { mutableStateOf<com.example.financetracker.presentation.model.AccountUiModel?>(null) }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = uiState.error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    if (showAddWallet) {
        AddWalletSheet(
            onDismiss = { showAddWallet = false },
            onSave = { name, amount, type ->
                viewModel.addWallet(name, amount, type)
                showAddWallet = false
            }
        )
    }

    if (editingWallet != null) {
        AddWalletSheet(
            existingWallet = editingWallet,
            onDismiss = { editingWallet = null },
            onSave = { name, amount, type ->
                viewModel.updateWallet(editingWallet!!.id, name, amount, type)
                editingWallet = null
            }
        )
    }

    Scaffold(
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) }
    ) { padding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(top = 48.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    text = "My Accounts",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.align(Alignment.Center)
                )
                Row(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationIconWithBadge(
                        unreadCount = unreadNotificationCount,
                        onClick = onNotificationClick
                    )
                    IconButton(
                        onClick = onSettingsClick,
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface, CircleShape)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Direct content - no tab switching
        MyAccountsContent(
            uiState,
            onAddClick = { showAddWallet = true },
            onEdit = { editingWallet = it },
            onDelete = { viewModel.deleteWallet(it.id) }
        )
    }
    } // Scaffold
}

@Composable
fun MyAccountsContent(
    uiState: com.example.financetracker.presentation.model.WalletUiState, 
    onAddClick: () -> Unit,
    onEdit: (com.example.financetracker.presentation.model.AccountUiModel) -> Unit,
    onDelete: (com.example.financetracker.presentation.model.AccountUiModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // 1. Summary Cards (Assets vs Liabilities)
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Assets", uiState.totalAssets, FinosMint, Modifier.weight(1f))
                SummaryCard("Liabilities", uiState.totalLiabilities, FinosCoral, Modifier.weight(1f))
            }
        }
        
        // 3. Accounts List
        item { Text("Your Accounts", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground) }
        
        val allWallets = uiState.assetWallets + uiState.liabilityWallets
        items(allWallets) { account ->
            AccountRow(account, onEdit = { onEdit(account) }, onDelete = { onDelete(account) })
        }
        
        item {
             OutlinedButton(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50), // Pill Shape
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha=0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add account")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Account", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun PeopleContent(
    uiState: com.example.financetracker.presentation.model.WalletUiState, 
    onAddClick: () -> Unit,
    onEdit: (com.example.financetracker.presentation.model.PersonUiModel) -> Unit,
    onDelete: (com.example.financetracker.presentation.model.PersonUiModel) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("They Owe You", uiState.totalOwedToYouFormatted, FinosMint, Modifier.weight(1f)) 
                SummaryCard("You Owe Them", uiState.totalOwedByYouFormatted, Color(0xFFFF9800), Modifier.weight(1f), isYellow = true)
            }
        }
        
        item { Text("People", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onBackground) }
        
        if (uiState.people.isEmpty()) {
            item { Text("No contacts added yet.", color = TextSecondaryDark) }
        } else {
            items(uiState.people) { person ->
                PersonRow(person, onEdit = { onEdit(person) }, onDelete = { onDelete(person) })
            }
        }
        
         item {
             OutlinedButton(
                onClick = onAddClick,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50), // Pill Shape
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha=0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add person")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Person", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, amount: String, color: Color, modifier: Modifier = Modifier, isYellow: Boolean = false) {
    val bgColor = if(isYellow) Color(0xFFFFF9C4).copy(alpha=0.15f) else color.copy(alpha = 0.15f)
    val textColor = if(isYellow) Color(0xFFFF9800) else color
    val isDark = isDarkTheme()

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.AutoMirrored.Rounded.TrendingUp, contentDescription = null, tint = textColor, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(title, style = MaterialTheme.typography.bodyMedium, color = if(isDark) TextSecondaryDark else Color.DarkGray)
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(amount, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), color = textColor)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountRow(
    account: com.example.financetracker.presentation.model.AccountUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val styling = cardStyling()
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
        border = styling.border,
        modifier = Modifier.combinedClickable(
            onClick = { },
            onLongClick = { showMenu = true }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(account.color, CircleShape) 
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                val icon = when {
                    account.type == "Investment" -> Icons.AutoMirrored.Rounded.TrendingUp
                    account.type == "Cash" -> Icons.Rounded.AccountBalanceWallet
                    else -> Icons.Rounded.AccountBalanceWallet 
                }
                Icon(icon, contentDescription = "Account type", tint = Color.Black) 
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(account.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(account.type, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
            }
            Text(account.balanceFormatted, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.width(8.dp))
            
            Box {
                Icon(
                    Icons.Rounded.MoreHoriz, 
                    contentDescription = "Options", 
                    tint = TextSecondaryDark, 
                    modifier = Modifier.size(20.dp).clickable { showMenu = true }
                )
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    DropdownMenuItem(
                        text = { Text("Edit") },
                        onClick = { showMenu = false; onEdit() }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = Color.Red) },
                        onClick = { showMenu = false; onDelete() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PersonRow(
    person: com.example.financetracker.presentation.model.PersonUiModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val styling = cardStyling()
    var showMenu by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
        border = styling.border,
        modifier = Modifier.combinedClickable(
            onClick = { },
            onLongClick = { showMenu = true }
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
             Box(
                modifier = Modifier.size(48.dp).background(MaterialTheme.colorScheme.background, CircleShape), // Background color in circle
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Person, contentDescription = "Person", tint = TextSecondaryDark)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(person.name, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onSurface)
                Text(person.statusText, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                val amountText = if(person.balance == 0L) "Settled" else (if(person.balance > 0) "+" else "") + person.balanceFormatted
                
                if (person.balance == 0L) {
                     Box(modifier = Modifier.background(BrandAccentDark.copy(alpha = 0.5f), RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)) {
                         Text("Settled", style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                     }
                } else {
                    Text(
                        amountText, 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = if(person.balance > 0) FinosMint else if(person.balance < 0) Color(0xFFE65100) else TextSecondaryDark
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))
                
                Box {
                    Icon(
                        Icons.Rounded.MoreHoriz, 
                        contentDescription = "Options", 
                        tint = TextSecondaryDark, 
                        modifier = Modifier.size(20.dp).clickable { showMenu = true }
                    )
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit") },
                            onClick = { showMenu = false; onEdit() }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = Color.Red) },
                            onClick = { showMenu = false; onDelete() }
                        )
                    }
                }
            }
        }
    }
}
