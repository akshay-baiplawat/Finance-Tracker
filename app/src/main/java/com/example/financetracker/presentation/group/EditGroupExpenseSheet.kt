package com.example.financetracker.presentation.group

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.financetracker.core.util.CameraCaptureUtil
import com.example.financetracker.core.util.TransactionAttachmentUtil
import com.example.financetracker.data.local.entity.CategoryEntity
import com.example.financetracker.presentation.category.CategoryViewModel
import com.example.financetracker.presentation.components.AttachmentMenuButton
import com.example.financetracker.presentation.components.AttachmentPickerSection
import com.example.financetracker.presentation.components.AttachmentUiModel
import com.example.financetracker.presentation.components.CategoryIconSmall
import com.example.financetracker.presentation.components.TransactionDatePicker
import com.example.financetracker.presentation.model.AccountUiModel
import com.example.financetracker.presentation.model.GroupExpenseUiModel
import com.example.financetracker.presentation.theme.DarkSurfaceHighlight
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.presentation.theme.TextSecondaryDark
import com.example.financetracker.presentation.theme.isDarkTheme
import com.example.financetracker.presentation.theme.selectionBorder
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGroupExpenseSheet(
    sheetState: SheetState,
    expense: GroupExpenseUiModel,
    isGeneralGroup: Boolean,
    existingAttachments: List<AttachmentUiModel> = emptyList(),
    wallets: List<AccountUiModel> = emptyList(),
    currentWalletId: Long = 0L,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (amount: Long, note: String, categoryName: String, timestamp: Long, newAttachmentUris: List<Uri>, deletedAttachmentIds: List<Long>) -> Unit,
    onSaveGeneral: ((amount: Long, note: String, categoryName: String, transactionType: String, walletId: Long, timestamp: Long, newAttachmentUris: List<Uri>, deletedAttachmentIds: List<Long>) -> Unit)?,
    onDelete: () -> Unit,
    onViewAttachment: (index: Int) -> Unit = {}
) {
    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        EditGroupExpenseContent(
            expense = expense,
            isGeneralGroup = isGeneralGroup,
            existingAttachments = existingAttachments,
            wallets = wallets,
            currentWalletId = currentWalletId,
            currencySymbol = currencySymbol,
            onDismiss = onDismiss,
            onSave = onSave,
            onSaveGeneral = onSaveGeneral,
            onDelete = onDelete,
            onViewAttachment = onViewAttachment
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGroupExpenseContent(
    expense: GroupExpenseUiModel,
    isGeneralGroup: Boolean,
    existingAttachments: List<AttachmentUiModel>,
    wallets: List<AccountUiModel>,
    currentWalletId: Long,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Long, String, String, Long, List<Uri>, List<Long>) -> Unit,
    onSaveGeneral: ((Long, String, String, String, Long, Long, List<Uri>, List<Long>) -> Unit)?,
    onDelete: () -> Unit,
    onViewAttachment: (index: Int) -> Unit,
    categoryViewModel: CategoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)

    // Initialize state from existing expense
    var amount by remember { mutableStateOf(String.format(Locale.US, "%.2f", expense.amount / 100.0)) }
    var note by remember { mutableStateOf(expense.note) }
    var selectedTimestamp by remember { mutableLongStateOf(expense.timestamp) }
    var selectedWalletId by remember { mutableLongStateOf(if (currentWalletId > 0) currentWalletId else wallets.firstOrNull()?.id ?: 0L) }

    // Attachment tracking
    var newlyAddedAttachments by remember { mutableStateOf<List<AttachmentUiModel>>(emptyList()) }
    var deletedAttachmentIds by remember { mutableStateOf<List<Long>>(emptyList()) }

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

    // Category selection
    val categories by categoryViewModel.categories.collectAsState(initial = emptyList())
    var selectedCategory by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Set initial category when categories load
    LaunchedEffect(categories, expense.categoryName) {
        if (selectedCategory == null && categories.isNotEmpty()) {
            selectedCategory = categories.find {
                it.name == expense.categoryName && it.type == expense.transactionType
            } ?: categories.find { it.name == expense.categoryName }
        }
    }

    // Check theme for dynamic styling
    val isDark = isDarkTheme()

    val scrollState = rememberScrollState()

    // Delete confirmation dialog
    var showDeleteConfirmation by remember { mutableStateOf(false) }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete Transaction", color = MaterialTheme.colorScheme.onSurface) },
            text = {
                Text(
                    "Are you sure you want to delete this transaction? This action cannot be undone.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel", color = FinosMint)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header with close button, attachment button and delete button
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(FinosMint)
                .padding(16.dp)
        ) {
            // Close button (left)
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.2f))
                    .clickable(onClick = keyboardAwareOnClick)
                    .align(Alignment.CenterStart),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }

            // Title (center)
            Text(
                text = if (isGeneralGroup) "Edit Transaction" else "Edit Expense",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )

            // Right side: Attachment button + Delete button
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Attachment button
                AttachmentMenuButton(
                    attachmentCount = displayedAttachments.size,
                    maxAttachments = 5,
                    onTakePhoto = { launchCamera() },
                    onChooseFiles = { launchFilePicker() },
                    tint = Color.White
                )

                // Delete button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f))
                        .clickable(onClick = { showDeleteConfirmation = true }),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = Color.White
                    )
                }
            }
        }

        // Attachment preview section (if any attachments)
        if (displayedAttachments.isNotEmpty()) {
            AttachmentPickerSection(
                attachments = displayedAttachments,
                maxAttachments = 5,
                onAddAttachments = { /* Not used - adding via header icon */ },
                onRemoveAttachment = { index ->
                    val attachmentToRemove = displayedAttachments[index]
                    if (attachmentToRemove.isNew) {
                        newlyAddedAttachments = newlyAddedAttachments.filter { it !== attachmentToRemove }
                    } else if (attachmentToRemove.id > 0) {
                        deletedAttachmentIds = deletedAttachmentIds + attachmentToRemove.id
                    }
                },
                onAttachmentClick = { index ->
                    // Only allow viewing existing attachments (not new ones that aren't saved yet)
                    val attachment = displayedAttachments.getOrNull(index)
                    if (attachment != null && !attachment.isNew) {
                        val existingIndex = existingAttachments.indexOf(attachment)
                        if (existingIndex >= 0) {
                            onViewAttachment(existingIndex)
                        }
                    }
                },
                showAddButton = false,
                showLabel = true,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Form content
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Amount
            Text(
                text = "Amount",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = amount,
                onValueChange = { if (it.all { c -> c.isDigit() || c == '.' }) amount = it },
                placeholder = { Text("0.00") },
                prefix = {
                    Text(
                        currencySymbol,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinosMint,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            // Info text for non-general groups
            if (!isGeneralGroup) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Splits will be automatically recalculated",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Description
            Text(
                text = "Description",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("What was this expense for?") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinosMint,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Category
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = categoryExpanded,
                onExpandedChange = { categoryExpanded = !categoryExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                val displayText = selectedCategory?.name ?: expense.categoryName

                OutlinedTextField(
                    value = displayText,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = FinosMint,
                        unfocusedBorderColor = if (isDark) DarkSurfaceHighlight else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                ExposedDropdownMenu(
                    expanded = categoryExpanded,
                    onDismissRequest = { categoryExpanded = false },
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.surface)
                        .heightIn(max = 300.dp)
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
                                    categoryExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                        HorizontalDivider(thickness = 0.5.dp, color = if (isDark) DarkSurfaceHighlight else Color.LightGray)
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
                                    categoryExpanded = false
                                },
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Transaction Date
            Text(
                text = "Transaction Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            TransactionDatePicker(
                selectedTimestamp = selectedTimestamp,
                onDateSelected = { selectedTimestamp = it },
                label = ""
            )

            // Payment Type for GENERAL groups
            if (isGeneralGroup && wallets.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Payment Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // First row - up to 2 wallets
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        wallets.take(2).forEach { wallet ->
                            PaymentTypeChipGroup(
                                text = wallet.name,
                                selected = selectedWalletId == wallet.id,
                                onClick = { selectedWalletId = wallet.id }
                            )
                        }
                    }
                    // Second row - 3rd wallet if exists
                    if (wallets.size > 2) {
                        PaymentTypeChipGroup(
                            text = wallets[2].name,
                            selected = selectedWalletId == wallets[2].id,
                            onClick = { selectedWalletId = wallets[2].id }
                        )
                    }
                }
            }

            // Show read-only info for SPLIT_EXPENSE groups
            if (!isGeneralGroup) {
                Spacer(modifier = Modifier.height(24.dp))

                // Paid By (read-only)
                Text(
                    text = "Paid By",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = expense.paidByName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (isDark) DarkSurfaceHighlight else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Payer cannot be changed",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Split Type (read-only)
                Text(
                    text = "Split Type",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = expense.splitType.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (isDark) DarkSurfaceHighlight else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Split type cannot be changed",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondaryDark
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    val amountPaisa = ((amount.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    val categoryName = selectedCategory?.name ?: expense.categoryName
                    val transactionType = selectedCategory?.type ?: expense.transactionType
                    val newAttachmentUris = newlyAddedAttachments.mapNotNull { it.uri }

                    if (isGeneralGroup && onSaveGeneral != null) {
                        onSaveGeneral(
                            amountPaisa,
                            note.ifBlank { "Transaction" },
                            categoryName,
                            transactionType,
                            selectedWalletId,
                            selectedTimestamp,
                            newAttachmentUris,
                            deletedAttachmentIds
                        )
                    } else {
                        onSave(
                            amountPaisa,
                            note.ifBlank { "Expense" },
                            categoryName,
                            selectedTimestamp,
                            newAttachmentUris,
                            deletedAttachmentIds
                        )
                    }
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = amount.isNotBlank() && (amount.toDoubleOrNull() ?: 0.0) > 0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = FinosMint,
                    disabledContainerColor = FinosMint.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun PaymentTypeChipGroup(text: String, selected: Boolean, onClick: () -> Unit) {
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
