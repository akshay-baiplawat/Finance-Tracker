package com.example.financetracker.presentation.add_transaction

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LocalCafe
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financetracker.presentation.theme.DarkSurface
import com.example.financetracker.presentation.theme.DarkSurfaceHighlight
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextPrimary
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.presentation.theme.isDarkTheme
import com.example.financetracker.presentation.theme.selectionBorder
import com.example.financetracker.presentation.components.CategoryIconSmall
import com.example.financetracker.presentation.components.TransactionDatePicker
import com.example.financetracker.presentation.components.AttachmentPickerSection
import com.example.financetracker.presentation.components.AttachmentUiModel
import com.example.financetracker.presentation.components.AttachmentMenuButton
import com.example.financetracker.core.util.ValidationUtils
import com.example.financetracker.core.util.TransactionAttachmentUtil
import com.example.financetracker.core.util.CameraCaptureUtil
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionSheet(
    onDismiss: () -> Unit,
    // amount, categoryName, type, paymentType, note, timestamp, attachmentUris
    onAdd: (String, String, String, String, String, Long, List<Uri>) -> Unit,
    isTripMode: Boolean = false
) {
    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        AddTransactionContent(onDismiss, onAdd, isTripMode = isTripMode)
    }
}

@OptIn(ExperimentalMaterial3Api::class, kotlinx.coroutines.ExperimentalCoroutinesApi::class)
@Composable
fun AddTransactionContent(
    onDismiss: () -> Unit,
    onAdd: (String, String, String, String, String, Long, List<Uri>) -> Unit,
    isTripMode: Boolean = false,
    categoryViewModel: com.example.financetracker.presentation.category.CategoryViewModel = hiltViewModel(),
    dashboardViewModel: com.example.financetracker.presentation.dashboard.DashboardViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var selectedTimestamp by remember { mutableLongStateOf(System.currentTimeMillis()) }

    // Attachments state
    var selectedAttachments by remember { mutableStateOf<List<AttachmentUiModel>>(emptyList()) }

    // Camera capture state
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val remainingSlots = 5 - selectedAttachments.size
            val urisToAdd = uris.take(remainingSlots)
            val newAttachments = urisToAdd.mapNotNull { uri ->
                val fileName = TransactionAttachmentUtil.getFileName(context, uri) ?: "attachment"
                val mimeType = TransactionAttachmentUtil.getMimeType(context, uri) ?: "application/octet-stream"
                AttachmentUiModel(
                    filePath = "",
                    fileName = fileName,
                    mimeType = mimeType,
                    isNew = true,
                    uri = uri
                )
            }
            selectedAttachments = selectedAttachments + newAttachments
        }
    }

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && pendingCameraUri != null && pendingCameraFile != null) {
            val attachment = AttachmentUiModel(
                filePath = pendingCameraFile!!.absolutePath,
                fileName = pendingCameraFile!!.name,
                mimeType = "image/jpeg",
                isNew = true,
                uri = pendingCameraUri
            )
            selectedAttachments = selectedAttachments + attachment
        }
        pendingCameraFile = null
        pendingCameraUri = null
    }

    // Camera permission launcher
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted, launch camera
            CameraCaptureUtil.createImageFile(context)?.let { (file, uri) ->
                pendingCameraFile = file
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        }
    }

    // Function to handle camera capture
    fun launchCamera() {
        val hasCameraPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCameraPermission) {
            CameraCaptureUtil.createImageFile(context)?.let { (file, uri) ->
                pendingCameraFile = file
                pendingCameraUri = uri
                cameraLauncher.launch(uri)
            }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Function to launch file picker
    fun launchFilePicker() {
        val mimeTypes = arrayOf(
            "image/*",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
        )
        filePickerLauncher.launch(mimeTypes)
    }

    // Get wallets from dashboard
    val uiState by dashboardViewModel.uiState.collectAsState()
    val wallets = uiState.wallets.take(3)

    // Set default selected wallet
    var selectedWalletId by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(wallets) {
        if (selectedWalletId == null && wallets.isNotEmpty()) {
            selectedWalletId = wallets.first().id
        }
    }

    // Check theme for dynamic styling inside the sheet
    val isDark = isDarkTheme()
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                    text = "Add Transaction",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = TextPrimary,
                    modifier = Modifier.weight(1f)
                )
                // Attachment menu button in header
                AttachmentMenuButton(
                    attachmentCount = selectedAttachments.size,
                    maxAttachments = 5,
                    onTakePhoto = { launchCamera() },
                    onChooseFiles = { launchFilePicker() },
                    tint = TextPrimary
                )
            }
        }

        // Attachment preview row (if any attachments)
        if (selectedAttachments.isNotEmpty()) {
            AttachmentPickerSection(
                attachments = selectedAttachments,
                maxAttachments = 5,
                onAddAttachments = { /* Not used - adding via header icon */ },
                onRemoveAttachment = { index ->
                    selectedAttachments = selectedAttachments.toMutableList().apply { removeAt(index) }
                },
                onAttachmentClick = { /* Preview not needed during add */ },
                showAddButton = false,
                showLabel = true,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        // Scrollable form content
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            // Amount
            Text("Amount", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it },
                modifier = Modifier.fillMaxWidth(),
                textStyle = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                prefix = { Text("₹", style = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) },
                shape = RoundedCornerShape(16.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if(isDark) DarkSurfaceHighlight else Color.LightGray.copy(alpha=0.5f),
                    focusedBorderColor = FinosMint,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category Dropdown
            Text("Category", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))

            val categories by categoryViewModel.categories.collectAsState(initial = emptyList())
            
            // Effect to set default
            var selectedCategory by remember { mutableStateOf<com.example.financetracker.data.local.entity.CategoryEntity?>(null) }
            LaunchedEffect(categories, isTripMode) {
                if (selectedCategory == null && categories.isNotEmpty()) {
                    // If Trip Mode is ON, prefer "Trip" category for EXPENSE
                    selectedCategory = if (isTripMode) {
                        categories.find { it.name == "Trip" && it.type == "EXPENSE" }
                            ?: categories.find { it.name == "Uncategorized" && it.type == "EXPENSE" }
                            ?: categories.first()
                    } else {
                        // Normal Mode: prefer "Uncategorized" for EXPENSE
                        categories.find { it.name == "Uncategorized" && it.type == "EXPENSE" }
                            ?: categories.first()
                    }
                }
            }
            
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val displayText = if (selectedCategory != null) {
                    "${selectedCategory!!.name} (${selectedCategory!!.type.lowercase().replaceFirstChar { it.uppercase() }})"
                } else "Select Category"
                
                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = if(isDark) DarkSurfaceHighlight else Color.LightGray.copy(alpha=0.5f),
                        focusedBorderColor = FinosMint,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surface).heightIn(max = 300.dp)
                ) {
                    // Group by Type for better UX
                    val grouped = categories.groupBy { it.type }
                    
                    // Expense Section
                    if (grouped["EXPENSE"]?.isNotEmpty() == true) {
                        DropdownMenuItem(
                            text = { Text("EXPENSE", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark) },
                            onClick = { },
                            enabled = false
                        )
                        grouped["EXPENSE"]?.forEach { category ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIconSmall(
                                            categoryName = category.name,
                                            iconId = category.iconId,
                                            colorHex = category.colorHex
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(category.name, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = if(isDark) DarkSurfaceHighlight else Color.LightGray)
                    }

                    // Income Section
                    if (grouped["INCOME"]?.isNotEmpty() == true) {
                         DropdownMenuItem(
                            text = { Text("INCOME", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark) },
                            onClick = { },
                            enabled = false
                        )
                        grouped["INCOME"]?.forEach { category ->
                             DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        CategoryIconSmall(
                                            categoryName = category.name,
                                            iconId = category.iconId,
                                            colorHex = category.colorHex
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(category.name, color = MaterialTheme.colorScheme.onSurface)
                                    }
                                },
                                onClick = {
                                    selectedCategory = category
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Note / Merchant
            Text("Note / Merchant", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Manual Entry", color = TextSecondaryDark) },
                shape = RoundedCornerShape(16.dp),
                textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = if(isDark) DarkSurfaceHighlight else Color.LightGray.copy(alpha=0.5f),
                    focusedBorderColor = FinosMint,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                leadingIcon = { Icon(Icons.Outlined.Description, contentDescription = "Note", tint = TextSecondaryDark) }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction Date
            Text("Transaction Date", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))
            TransactionDatePicker(
                selectedTimestamp = selectedTimestamp,
                onDateSelected = { selectedTimestamp = it },
                label = ""
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Type (from user's wallets)
            Text("Payment Type", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))

            if (wallets.isEmpty()) {
                Text(
                    "No accounts found. Add a wallet first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // First row - up to 2 wallets
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        wallets.take(2).forEach { wallet ->
                            PaymentTypeChip(
                                text = wallet.name,
                                selected = selectedWalletId == wallet.id,
                                onClick = { selectedWalletId = wallet.id }
                            )
                        }
                    }
                    // Second row - 3rd wallet if exists
                    if (wallets.size > 2) {
                        PaymentTypeChip(
                            text = wallets[2].name,
                            selected = selectedWalletId == wallets[2].id,
                            onClick = { selectedWalletId = wallets[2].id }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Buttons
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = keyboardAwareOnClick,
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if(isDark) DarkSurfaceHighlight else Color.LightGray)
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
                }

                val selectedWallet = wallets.find { it.id == selectedWalletId }
                val isValidAmount = ValidationUtils.isValidAmount(amount)
                Button(
                    onClick = {
                        if (isValidAmount) {
                            val attachmentUris = selectedAttachments.mapNotNull { it.uri }
                            onAdd(
                                amount,
                                selectedCategory?.name ?: "Uncategorized",
                                selectedCategory?.type ?: "EXPENSE",
                                selectedWallet?.name ?: "Cash",
                                note.ifBlank { "Manual Entry" },
                                selectedTimestamp,
                                attachmentUris
                            )
                        }
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinosMint),
                    enabled = isValidAmount
                ) {
                    Text("Add", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            // Bottom padding to ensure content is above navigation bar
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PaymentTypeChip(text: String, selected: Boolean, onClick: () -> Unit) {
    val border = selectionBorder(selected)

    Box(
        modifier = Modifier
            .border(
                border = border,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (selected) FinosMint else TextSecondaryDark,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = text,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
