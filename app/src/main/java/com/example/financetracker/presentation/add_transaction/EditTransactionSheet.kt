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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financetracker.presentation.model.TransactionUiModel
import com.example.financetracker.presentation.model.AccountUiModel
import com.example.financetracker.presentation.theme.selectionBorder
import com.example.financetracker.presentation.theme.DarkSurface
import com.example.financetracker.presentation.theme.DarkSurfaceHighlight
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextPrimary
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.presentation.theme.isDarkTheme
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
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionSheet(
    transaction: TransactionUiModel,
    existingAttachments: List<AttachmentUiModel> = emptyList(),
    wallets: List<AccountUiModel> = emptyList(),
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit,
    onUpdate: (Long, Long, String, String, String, Long, Long, List<Uri>, List<Long>) -> Unit, // id, amount, note, category, type, timestamp, walletId, newAttachmentUris, deletedAttachmentIds
    onViewAttachment: (Int) -> Unit = {}, // Callback to view attachment at index
    categoryViewModel: com.example.financetracker.presentation.category.CategoryViewModel = hiltViewModel()
) {
    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = null
    ) {
        EditTransactionContent(
            transaction = transaction,
            existingAttachments = existingAttachments,
            wallets = wallets,
            onDismiss = onDismiss,
            onDelete = onDelete,
            onUpdate = onUpdate,
            onViewAttachment = onViewAttachment,
            categoryViewModel = categoryViewModel
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionContent(
    transaction: TransactionUiModel,
    existingAttachments: List<AttachmentUiModel>,
    wallets: List<AccountUiModel>,
    onDismiss: () -> Unit,
    onDelete: (Long) -> Unit,
    onUpdate: (Long, Long, String, String, String, Long, Long, List<Uri>, List<Long>) -> Unit,
    onViewAttachment: (Int) -> Unit,
    categoryViewModel: com.example.financetracker.presentation.category.CategoryViewModel
) {
    val context = LocalContext.current
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)

    // State
    // Format amount from Cents to Decimal String
    var amount by remember { mutableStateOf(String.format(Locale.US, "%.2f", transaction.amount / 100.0)) }
    var note by remember { mutableStateOf(transaction.title) } // This is 'merchant' or 'note' often mapped to title in UI model
    var selectedTimestamp by remember { mutableLongStateOf(transaction.timestamp) }
    var selectedWalletId by remember { mutableLongStateOf(transaction.walletId) }

    // Attachments state - track changes only, derive displayed list
    var newlyAddedAttachments by remember { mutableStateOf<List<AttachmentUiModel>>(emptyList()) }
    var deletedAttachmentIds by remember { mutableStateOf<List<Long>>(emptyList()) }

    // Derive the displayed attachments from existing + new - deleted
    val displayedAttachments by remember(existingAttachments, newlyAddedAttachments, deletedAttachmentIds) {
        derivedStateOf {
            val remaining = existingAttachments.filter { it.id !in deletedAttachmentIds }
            remaining + newlyAddedAttachments
        }
    }

    // Camera capture state
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var pendingCameraUri by remember { mutableStateOf<Uri?>(null) }

    // File picker launcher
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            val currentTotal = existingAttachments.size - deletedAttachmentIds.size + newlyAddedAttachments.size
            val remainingSlots = 5 - currentTotal
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
            newlyAddedAttachments = newlyAddedAttachments + newAttachments
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
            newlyAddedAttachments = newlyAddedAttachments + attachment
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

    // If SMS, some fields are read-only
    val isSms = transaction.smsId != null
    val isReadOnly = isSms

    // Category & Type Logic
    // We start with existing category/type
    // If user changes category, type auto-updates based on CategoryEntity
    val categories by categoryViewModel.categories.collectAsState(initial = emptyList())

    // Filter categories if SMS (Synced transactions should usually stick to their detected type)
    val visibleCategories = if (isSms) {
        categories.filter { it.type == transaction.type }
    } else {
        categories
    }

    var selectedCategoryName by remember { mutableStateOf(transaction.category) }
    var selectedType by remember { mutableStateOf(transaction.type) } // "EXPENSE" or "INCOME"

    // Check theme
    val isDark = isDarkTheme()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 32.dp)
    ) {
        // Header with attachment and delete buttons
        Box(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp)
        ) {
            Text(
                text = "Edit Transaction",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            Row(modifier = Modifier.align(Alignment.CenterEnd)) {
                // Attachment menu button
                AttachmentMenuButton(
                    attachmentCount = displayedAttachments.size,
                    maxAttachments = 5,
                    onTakePhoto = { launchCamera() },
                    onChooseFiles = { launchFilePicker() },
                    tint = MaterialTheme.colorScheme.onSurface
                )
                // Delete button
                IconButton(
                    onClick = { onDelete(transaction.id) }
                ) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }

        HorizontalDivider(thickness = 0.5.dp, color = if (isDark) DarkSurfaceHighlight else Color.LightGray.copy(alpha = 0.5f))

        // Attachment preview row (if any attachments)
        if (displayedAttachments.isNotEmpty()) {
            AttachmentPickerSection(
                attachments = displayedAttachments,
                maxAttachments = 5,
                onAddAttachments = { /* Not used - adding via header icon */ },
                onRemoveAttachment = { index ->
                    val attachmentToRemove = displayedAttachments[index]
                    if (attachmentToRemove.isNew) {
                        // Remove from newly added list
                        newlyAddedAttachments = newlyAddedAttachments.filter { it !== attachmentToRemove }
                    } else if (attachmentToRemove.id > 0) {
                        // Track deletion of existing attachment
                        deletedAttachmentIds = deletedAttachmentIds + attachmentToRemove.id
                    }
                },
                onAttachmentClick = { index ->
                    // Only allow viewing existing attachments (not new ones that aren't saved yet)
                    val attachment = displayedAttachments.getOrNull(index)
                    if (attachment != null && !attachment.isNew) {
                        // Find the original index among existing attachments only
                        val existingIndex = existingAttachments.indexOf(attachment)
                        if (existingIndex >= 0) {
                            onViewAttachment(existingIndex)
                        }
                    }
                },
                showAddButton = false,
                showLabel = true,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }

        Column(modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState())) {
            
            // Amount
            Text("Amount", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { char -> char.isDigit() || char == '.' }) amount = it },
                modifier = Modifier.fillMaxWidth(),
                readOnly = isReadOnly, // Block edit if SMS
                textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface),
                prefix = { Text("₹ ", style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = Color.Transparent, // No border for cleaner look
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )
            if (isReadOnly) {
                Text("Synced via SMS. Amount cannot be edited.", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.padding(start = 16.dp))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Category Dropdown (Fully Editable for BOTH)
            Text("Category", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))

            var expanded by remember { mutableStateOf(false) }
            
            // Derive Full Entity for display logic
            val currentCategoryEntity = categories.find { it.name == selectedCategoryName && it.type == selectedType } 
                                     ?: categories.find { it.name == selectedCategoryName }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val displayText = if (currentCategoryEntity != null) {
                    "${currentCategoryEntity.name} (${currentCategoryEntity.type.lowercase().replaceFirstChar { it.uppercase() }})"
                } else selectedCategoryName
                
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
                     val grouped = visibleCategories.groupBy { it.type }
                    
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
                                    selectedCategoryName = category.name
                                    selectedType = category.type // Auto update type
                                    expanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = if(isDark) DarkSurfaceHighlight else Color.LightGray)
                    }

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
                                    selectedCategoryName = category.name
                                    selectedType = category.type // Auto update type
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
                readOnly = false, 
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
            if (isReadOnly) {
                Text("Synced via SMS.", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.padding(start = 16.dp))
            }

             Spacer(modifier = Modifier.height(24.dp))

            // Date - Editable for manual transactions, read-only for SMS
            Text("Date", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))

            if (isSms) {
                // SMS transactions: show read-only date
                OutlinedTextField(
                    value = DateTimeFormatter.ofPattern("dd MMM yyyy").format(transaction.date),
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    textStyle = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if(isDark) DarkSurfaceHighlight else Color.LightGray.copy(alpha=0.5f),
                        disabledLeadingIconColor = TextSecondaryDark
                    ),
                    leadingIcon = { Icon(Icons.Outlined.CalendarToday, contentDescription = "Transaction date") }
                )
                Text("Synced via SMS. Date cannot be edited.", style = MaterialTheme.typography.labelSmall, color = TextSecondaryDark, modifier = Modifier.padding(start = 16.dp))
            } else {
                // Manual transactions: editable date picker
                TransactionDatePicker(
                    selectedTimestamp = selectedTimestamp,
                    onDateSelected = { selectedTimestamp = it },
                    label = ""
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Type (Wallet Selection)
            Text("Payment Type", style = MaterialTheme.typography.bodyMedium, color = TextSecondaryDark)
            Spacer(modifier = Modifier.height(8.dp))

            if (wallets.isEmpty()) {
                Text(
                    "No accounts found.",
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

            // Save Button
            Row(modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { keyboardAwareOnClick() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = TextSecondaryDark),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if(isDark) DarkSurfaceHighlight else Color.LightGray)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                val isValidAmount = isReadOnly || ValidationUtils.isValidAmount(amount)
                Button(
                    onClick = {
                        if (!isValidAmount) return@Button
                        // Parse amount safely after validation
                        val parsedAmount = (amount.toDouble() * 100).toLong()
                        // Collect new attachment URIs from newlyAddedAttachments
                        val newAttachmentUris = newlyAddedAttachments.mapNotNull { it.uri }
                        // Type is selectedType
                        // Note is note
                        // Category is selectedCategoryName
                        onUpdate(
                            transaction.id,
                            parsedAmount,
                            note,
                            selectedCategoryName,
                            selectedType,
                            selectedTimestamp,
                            selectedWalletId,
                            newAttachmentUris,
                            deletedAttachmentIds
                        )
                    },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = FinosMint),
                    enabled = isValidAmount
                ) {
                    Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
