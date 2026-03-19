package com.example.financetracker.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.financetracker.presentation.theme.*
import com.example.financetracker.presentation.components.SyncDialog
import com.example.financetracker.presentation.components.DataManagementSheet
import com.example.financetracker.presentation.components.BatteryOptimizationDialog
import com.example.financetracker.presentation.components.ErrorSnackbarEffect
import com.example.financetracker.presentation.components.ErrorSnackbarHost
import com.example.financetracker.presentation.components.LogoutConfirmationDialog
import com.example.financetracker.presentation.components.LogoutDataChoiceDialog
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.core.util.BatteryOptimizationUtil
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.content.Intent
import android.net.Uri
import android.content.Context
import android.app.Activity
import android.content.ContextWrapper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onCategoriesClick: () -> Unit,
    onUserProfileClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val theme by viewModel.theme.collectAsState(initial = "system")
    val displaySize by viewModel.displaySize.collectAsState(initial = "medium")
    val systemDarkMode = isSystemInDarkTheme()
    val isDark = when (theme) {
        "dark" -> true
        "light" -> false
        else -> systemDarkMode // "system" follows device setting
    }
    val userName by viewModel.userName.collectAsState(initial = "")
    val userImagePath by viewModel.userImagePath.collectAsState(initial = null)

    val context = LocalContext.current
    val isSyncing by viewModel.isSyncing.collectAsState()
    val isExporting by viewModel.isExporting.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val isPrivacyMode by viewModel.isPrivacyMode.collectAsState()
    val isTripMode by viewModel.isTripMode.collectAsState()
    val isNotificationsEnabled by viewModel.isNotificationsEnabled.collectAsState()
    val isPeriodicSyncEnabled by viewModel.isPeriodicSyncEnabled.collectAsState()
    val isBatteryOptimizationPromptDismissed by viewModel.isBatteryOptimizationPromptDismissed.collectAsState()
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showSyncDialog by remember { mutableStateOf(false) }
    var showDataManagementSheet by remember { mutableStateOf(false) }
    var showBatteryOptimizationDialog by remember { mutableStateOf(false) }
    var showLogoutConfirmDialog by remember { mutableStateOf(false) }
    var showLogoutDataChoiceDialog by remember { mutableStateOf(false) }
    var isLoggingOut by remember { mutableStateOf(false) }

    // Show error snackbar
    ErrorSnackbarEffect(
        error = error,
        snackbarHostState = snackbarHostState,
        onErrorShown = { viewModel.clearError() }
    )

    // SMS Permission launcher
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSyncDialog = true
        } else {
            Toast.makeText(context, "Permission needed to read SMS", Toast.LENGTH_SHORT).show()
        }
    }

    // File picker for CSV import
    val csvImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importCsv(it) { count ->
                when {
                    count > 0 -> Toast.makeText(context, "Imported $count transactions successfully!", Toast.LENGTH_LONG).show()
                    count == 0 -> Toast.makeText(context, "No transactions found in file", Toast.LENGTH_SHORT).show()
                    else -> Toast.makeText(context, "Failed to import CSV", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val currentCurrency by viewModel.currency.collectAsState(initial = "INR")
    var showCurrencyPicker by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.syncResult.collect { msg ->
             Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
        }
    }

    // Sync Dialog
    if (showSyncDialog) {
        SyncDialog(
            onDismiss = { showSyncDialog = false },
            onSync = { startTime ->
                viewModel.triggerSync(startTime)
                showSyncDialog = false
            },
            isDark = isDark
        )
    }

    // Data Management Sheet
    if (showDataManagementSheet) {
        DataManagementSheet(
            onDismiss = { showDataManagementSheet = false },
            onExport = {
                viewModel.saveCsvToDevice { filename ->
                    if (filename != null) {
                        Toast.makeText(context, "Exported to Downloads/$filename", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "No transactions to export", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onImport = {
                csvImportLauncher.launch(arrayOf("text/csv", "text/comma-separated-values", "*/*"))
            },
            onClearData = {
                viewModel.wipeData {
                    Toast.makeText(context, "All data cleared successfully", Toast.LENGTH_SHORT).show()
                }
                showDataManagementSheet = false
            },
            isExporting = isExporting,
            isImporting = isImporting
        )
    }

    if (showCurrencyPicker) {
        CurrencyPickerSheet(
            currentCurrencyCode = currentCurrency,
            onDismiss = { showCurrencyPicker = false },
            onCurrencySelected = { code ->
                viewModel.updateCurrency(code)
            }
        )
    }

    // Battery Optimization Dialog
    if (showBatteryOptimizationDialog) {
        BatteryOptimizationDialog(
            onDismiss = { dontAskAgain ->
                showBatteryOptimizationDialog = false
                if (dontAskAgain) {
                    viewModel.setBatteryOptimizationPromptDismissed(true)
                }
            },
            onEnable = {
                showBatteryOptimizationDialog = false
                BatteryOptimizationUtil.openBatteryOptimizationSettings(context)
            },
            isDark = isDark
        )
    }

    // Logout Confirmation Dialog
    if (showLogoutConfirmDialog) {
        LogoutConfirmationDialog(
            onDismiss = { showLogoutConfirmDialog = false },
            onContinue = {
                showLogoutConfirmDialog = false
                showLogoutDataChoiceDialog = true
            },
            isDark = isDark
        )
    }

    // Logout Data Choice Dialog
    if (showLogoutDataChoiceDialog) {
        LogoutDataChoiceDialog(
            onDismiss = { showLogoutDataChoiceDialog = false },
            onKeepData = {
                showLogoutDataChoiceDialog = false
                isLoggingOut = true
                viewModel.logout(keepData = true) {
                    // Restart app to trigger fresh flow
                    restartApp(context)
                }
            },
            onDeleteData = {
                showLogoutDataChoiceDialog = false
                isLoggingOut = true
                viewModel.logout(keepData = false) {
                    // Restart app to trigger fresh flow
                    restartApp(context)
                }
            },
            isDark = isDark
        )
    }

    // Loading overlay while logging out
    if (isLoggingOut) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = FinosMint)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { ErrorSnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onUserProfileClick() },
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = FinosMint)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(Color.White.copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (userImagePath != null) {
                                AsyncImage(
                                    model = userImagePath,
                                    contentDescription = "Profile",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                val initials = userName
                                    .split(" ")
                                    .take(2)
                                    .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                                    .joinToString("")
                                    .ifEmpty { "?" }
                                Text(initials, style = MaterialTheme.typography.headlineMedium, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                userName.ifEmpty { "Set your name" },
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = Color.White
                            )
                            Text(
                                "Tap to edit profile",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.8f)
                            )
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = "Edit",
                            tint = Color.White
                        )
                    }
                }
            }

            // Settings Groups
            item {
                SettingsGroup("Privacy & Security") {
                    SettingsToggle(
                        "Privacy Mode",
                        "Ignore SMS transactions",
                        isPrivacyMode,
                        { viewModel.setPrivacyMode(it) }
                    )

                    SettingsToggle(
                        "Trip Mode",
                        "Default category for expenses becomes 'Trip'",
                        isTripMode,
                        { viewModel.setTripMode(it) }
                    )
                }
            }

            // Data Management Section - Now combined into one item
            item {
                SettingsGroup("Data Management") {
                    // Sync SMS Data
                    SettingsItem(
                        icon = Icons.Rounded.Refresh,
                        title = "Sync SMS Data",
                        subtitle = if (isSyncing) "Syncing..." else "Scan messages for transactions",
                        onClick = {
                            if (!isSyncing) {
                                if (androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.READ_SMS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                ) {
                                    showSyncDialog = true
                                } else {
                                    smsPermissionLauncher.launch(android.Manifest.permission.READ_SMS)
                                }
                            }
                        }
                    )

                    // Backup & Restore - Opens the DataManagementSheet
                    SettingsItem(
                        icon = Icons.Rounded.Storage,
                        title = "Backup & Restore",
                        subtitle = "Export, import, or clear your data",
                        onClick = { showDataManagementSheet = true }
                    )
                }
            }

            item {
                SettingsGroup("Preferences") {
                    SettingsToggle(
                        "Notifications",
                        "Get alerts for transactions",
                        isNotificationsEnabled,
                        { viewModel.setNotificationsEnabled(it) }
                    )

                    SettingsToggle(
                        "Auto SMS Sync",
                        "Sync SMS every 15 minutes",
                        isPeriodicSyncEnabled,
                        { enabled ->
                            viewModel.setPeriodicSyncEnabled(enabled)
                            // Show battery optimization dialog when enabling
                            if (enabled && !isBatteryOptimizationPromptDismissed &&
                                BatteryOptimizationUtil.shouldShowBatteryOptimizationPrompt(context)
                            ) {
                                showBatteryOptimizationDialog = true
                            }
                        }
                    )

                    // Battery Optimization Settings Item
                    val isBatteryOptimized = remember(context) {
                        BatteryOptimizationUtil.isIgnoringBatteryOptimizations(context)
                    }
                    SettingsItem(
                        icon = Icons.Rounded.BatteryAlert,
                        title = "Battery Optimization",
                        subtitle = if (isBatteryOptimized) "Unrestricted" else "Tap to optimize for background sync",
                        onClick = {
                            BatteryOptimizationUtil.openBatteryOptimizationSettings(context)
                        }
                    )

                    // Dark Mode is controlled by ViewModel
                    SettingsToggle(
                        title = "Dark Mode",
                        subtitle = if (theme == "system") "Following system setting" else "Manual override",
                        checked = isDark,
                        onCheckedChange = { isChecked ->
                             viewModel.updateTheme(if (isChecked) "dark" else "light")
                        }
                    )

                    // Display Size selector
                    DisplaySizeSelector(
                        currentSize = displaySize,
                        onSizeSelected = { viewModel.updateDisplaySize(it) }
                    )

                    SettingsItem(Icons.Rounded.Category, "Categories", "Manage expense & income", onClick = onCategoriesClick)
                    SettingsItem(
                        icon = Icons.Rounded.Language,
                        title = "Currency",
                        subtitle = try {
                            java.util.Currency.getInstance(currentCurrency).displayName
                        } catch(e: Exception) {
                            AppLogger.w("ProfileScreen", "Invalid currency code: $currentCurrency", e)
                            currentCurrency
                        },
                        onClick = { showCurrencyPicker = true }
                    )
                }
            }

            item {
                SettingsGroup("Support") {
                    SettingsItem(
                        Icons.AutoMirrored.Rounded.HelpOutline,
                        "Help Center",
                        "Contact us via email",
                        onClick = { openEmailForSupport(context) }
                    )
                    SettingsItem(Icons.Rounded.Description, "Privacy Policy", "View our data practices", onClick = onPrivacyPolicyClick)
                    SettingsItem(
                        Icons.AutoMirrored.Rounded.Logout,
                        "Log Out",
                        "Reset app to initial state",
                        isDestructive = true,
                        onClick = { showLogoutConfirmDialog = true }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(LocalDimensions.current.xl)) }
        }
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    val dims = LocalDimensions.current
    val styling = cardStyling()

    Column {
        Text(title, style = MaterialTheme.typography.titleSmall, color = TextSecondaryDark, modifier = Modifier.padding(bottom = dims.sm, start = dims.sm))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(dims.cardRadius),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = styling.elevation),
            border = styling.border
        ) {
            Column(modifier = Modifier.padding(vertical = dims.sm)) {
                content()
            }
        }
    }
}

@Composable
fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit = {}
) {
    val dims = LocalDimensions.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.lg, vertical = dims.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val bg = if(isDarkTheme()) FinosMint.copy(alpha=0.1f) else BrandBackground

            Icon(Icons.Rounded.Settings, contentDescription = null, tint = FinosMint, modifier = Modifier.size(dims.avatarSm).background(bg, CircleShape).padding(dims.sm))
            Spacer(modifier = Modifier.width(dims.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
            }
        }
        Spacer(modifier = Modifier.width(dims.sm))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = FinosMint)
        )
    }
}

@Composable
fun SettingsItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isDestructive: Boolean = false,
    onClick: () -> Unit = {}
) {
    val dims = LocalDimensions.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = dims.lg, vertical = dims.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
             val bg = if (isDestructive) FinosCoral.copy(alpha = 0.1f) else if(isDarkTheme()) MaterialTheme.colorScheme.background else BrandBackground

             Box(
                modifier = Modifier
                    .size(dims.avatarSm)
                    .background(bg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = if (isDestructive) FinosCoral else TextSecondaryDark, modifier = Modifier.size(dims.iconSm))
            }
            Spacer(modifier = Modifier.width(dims.md))
            Column {
                Text(title, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium), color = if(isDestructive) FinosCoral else MaterialTheme.colorScheme.onSurface)
                if (subtitle.isNotEmpty()) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondaryDark)
                }
            }
        }
        Icon(Icons.Rounded.ChevronRight, contentDescription = "Go", tint = TextSecondaryDark)
    }
}

private fun openEmailForSupport(context: android.content.Context) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf("support@example.com")) // TODO: Update with your actual email
        putExtra(Intent.EXTRA_SUBJECT, "FinanceTracker Support Request")
    }
    try {
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, "No email app found", Toast.LENGTH_SHORT).show()
    }
}

// SyncDialog moved to presentation/components/SyncDialog.kt

// Helper to find Activity from Context (handles ContextWrapper)
private fun Context.findActivity(): Activity? {
    var context = this
    while (context is ContextWrapper) {
        if (context is Activity) return context
        context = context.baseContext
    }
    return null
}

// Restart app completely to trigger fresh flow
private fun restartApp(context: Context) {
    val activity = context.findActivity() ?: return
    val intent = activity.intent
    activity.finish()
    activity.startActivity(intent)
}

@Composable
fun DisplaySizeSelector(
    currentSize: String,
    onSizeSelected: (String) -> Unit
) {
    val dims = LocalDimensions.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dims.lg, vertical = dims.md),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val bg = if(isDarkTheme()) FinosMint.copy(alpha=0.1f) else BrandBackground
            Icon(
                Icons.Rounded.TextFormat,
                contentDescription = null,
                tint = FinosMint,
                modifier = Modifier
                    .size(dims.avatarSm)
                    .background(bg, CircleShape)
                    .padding(dims.sm)
            )
            Spacer(modifier = Modifier.width(dims.md))
            Column {
                Text(
                    "Display Size",
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    DisplaySize.getFullLabel(currentSize),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            }
        }

        // Segmented buttons for size selection
        Row(
            horizontalArrangement = Arrangement.spacedBy(dims.xs)
        ) {
            DisplaySize.options.forEach { size ->
                val isSelected = size == currentSize
                val selectedBg = if (isDarkTheme()) FinosMint.copy(alpha = 0.2f) else FinosMint.copy(alpha = 0.15f)
                val unselectedBg = if (isDarkTheme()) DarkSurfaceHighlight else BrandBackground

                Surface(
                    modifier = Modifier
                        .clickable { onSizeSelected(size) },
                    shape = RoundedCornerShape(dims.chipRadius),
                    color = if (isSelected) selectedBg else unselectedBg,
                    border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, FinosMint) else null
                ) {
                    Text(
                        text = DisplaySize.getLabel(size),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (isSelected) FinosMint else TextSecondaryDark,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.padding(horizontal = dims.md - dims.xs, vertical = 6.dp)
                    )
                }
            }
        }
    }
}
