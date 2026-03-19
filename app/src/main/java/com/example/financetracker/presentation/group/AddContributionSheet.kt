package com.example.financetracker.presentation.group

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.Close
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
import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.presentation.components.TransactionDatePicker
import com.example.financetracker.presentation.components.AttachmentPickerSection
import com.example.financetracker.presentation.components.AttachmentUiModel
import com.example.financetracker.presentation.components.AttachmentMenuButton
import com.example.financetracker.presentation.theme.FinosMint
import com.example.financetracker.core.util.TransactionAttachmentUtil
import com.example.financetracker.core.util.CameraCaptureUtil
import com.example.financetracker.presentation.components.KeyboardAwareModalBottomSheet
import com.example.financetracker.presentation.components.rememberKeyboardAwareOnClick
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContributionSheet(
    sheetState: SheetState,
    groupId: Long,
    members: List<PersonEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (amount: Long, memberId: Long?, isSelf: Boolean, note: String, timestamp: Long) -> Unit
) {
    KeyboardAwareModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        AddContributionContent(
            members = members,
            currencySymbol = currencySymbol,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

@Composable
private fun AddContributionContent(
    members: List<PersonEntity>,
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (Long, Long?, Boolean, String, Long) -> Unit
) {
    val context = LocalContext.current
    val keyboardAwareOnClick = rememberKeyboardAwareOnClick(onDismiss)
    var amount by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var contributedBySelf by remember { mutableStateOf(true) }
    var contributedByPersonId by remember { mutableStateOf<Long?>(null) }
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

    val allParticipants = remember(members) {
        listOf(null to "You") + members.map { it.id to it.name }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Header with attachment icon
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
                text = "Add Contribution",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Center)
            )

            // Attachment button (right)
            AttachmentMenuButton(
                attachmentCount = selectedAttachments.size,
                maxAttachments = 5,
                onTakePhoto = { launchCamera() },
                onChooseFiles = { launchFilePicker() },
                tint = Color.White,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
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
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }

        // Scrollable form content
        Column(
            modifier = Modifier
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

            Spacer(modifier = Modifier.height(24.dp))

            // Contributed By
            Text(
                text = "Contributed By",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(allParticipants) { (personId, name) ->
                    val isSelected = (personId == null && contributedBySelf) ||
                            (personId != null && contributedByPersonId == personId && !contributedBySelf)

                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (personId == null) {
                                contributedBySelf = true
                                contributedByPersonId = null
                            } else {
                                contributedBySelf = false
                                contributedByPersonId = personId
                            }
                        },
                        label = { Text(name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = FinosMint.copy(alpha = 0.2f),
                            selectedLabelColor = FinosMint
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Note (optional)
            Text(
                text = "Note (Optional)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                placeholder = { Text("e.g., Monthly contribution") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = FinosMint,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Contribution Date
            Text(
                text = "Contribution Date",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(8.dp))

            TransactionDatePicker(
                selectedTimestamp = selectedTimestamp,
                onDateSelected = { selectedTimestamp = it },
                label = ""
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    val amountPaisa = ((amount.toDoubleOrNull() ?: 0.0) * 100).toLong()
                    onSave(amountPaisa, contributedByPersonId, contributedBySelf, note, selectedTimestamp)
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
                    text = "Add Contribution",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
