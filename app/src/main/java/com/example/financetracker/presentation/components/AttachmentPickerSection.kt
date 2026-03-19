package com.example.financetracker.presentation.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.financetracker.core.util.TransactionAttachmentUtil
import com.example.financetracker.presentation.theme.FinosMint
import java.io.File

/**
 * UI model for displaying attachments in the picker section
 */
data class AttachmentUiModel(
    val id: Long = 0,
    val filePath: String,
    val fileName: String,
    val mimeType: String,
    val isNew: Boolean = false, // true if just added (not yet saved to DB)
    val uri: Uri? = null // Only set for new attachments
)

/**
 * Supported MIME types for the file picker
 */
private val SUPPORTED_MIME_TYPES = arrayOf(
    "image/*",
    "application/pdf",
    "application/msword",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
    "application/vnd.ms-excel",
    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
    "text/plain"
)

/**
 * Reusable component for selecting and displaying attachments in transaction sheets.
 *
 * @param attachments List of current attachments
 * @param maxAttachments Maximum number of attachments allowed
 * @param onAddAttachments Callback when new attachments are selected
 * @param onRemoveAttachment Callback when an attachment is removed (index-based)
 * @param onAttachmentClick Callback when an attachment is clicked to view
 * @param showAddButton Whether to show the add button (false when using header icon)
 * @param showLabel Whether to show the "Attachments (x/y)" label
 * @param modifier Additional modifiers
 */
@Composable
fun AttachmentPickerSection(
    attachments: List<AttachmentUiModel>,
    maxAttachments: Int = 5,
    onAddAttachments: (List<Uri>) -> Unit,
    onRemoveAttachment: (Int) -> Unit,
    onAttachmentClick: (Int) -> Unit,
    showAddButton: Boolean = true,
    showLabel: Boolean = true,
    modifier: Modifier = Modifier
) {
    val canAddMore = attachments.size < maxAttachments
    val remainingSlots = maxAttachments - attachments.size

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            // Limit to remaining slots
            val urisToAdd = uris.take(remainingSlots)
            onAddAttachments(urisToAdd)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (showLabel) {
            Text(
                text = "Attachments (${attachments.size}/$maxAttachments)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (attachments.isEmpty() && canAddMore && showAddButton) {
            // Empty state - show add button prominently
            OutlinedButton(
                onClick = { filePicker.launch(SUPPORTED_MIME_TYPES) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = FinosMint
                ),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(FinosMint.copy(alpha = 0.5f))
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.Add,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Attachments")
            }
        } else if (attachments.isNotEmpty()) {
            // Show attachments in a horizontal scroll
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 4.dp)
            ) {
                itemsIndexed(attachments) { index, attachment ->
                    AttachmentThumbnail(
                        attachment = attachment,
                        onClick = { onAttachmentClick(index) },
                        onRemove = { onRemoveAttachment(index) }
                    )
                }

                // Add button at the end if we can add more and showAddButton is true
                if (canAddMore && showAddButton) {
                    item {
                        AddAttachmentButton(
                            onClick = { filePicker.launch(SUPPORTED_MIME_TYPES) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Thumbnail display for a single attachment
 */
@Composable
private fun AttachmentThumbnail(
    attachment: AttachmentUiModel,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val isImage = TransactionAttachmentUtil.isImage(attachment.mimeType)

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable { onClick() }
    ) {
        if (isImage) {
            // Show image preview
            val imageModel = if (attachment.isNew && attachment.uri != null) {
                attachment.uri
            } else {
                File(attachment.filePath)
            }

            AsyncImage(
                model = imageModel,
                contentDescription = attachment.fileName,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            // Show file type icon
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = getIconForMimeType(attachment.mimeType),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = getColorForMimeType(attachment.mimeType)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = attachment.fileName,
                    style = MaterialTheme.typography.labelSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Remove button (top-right corner)
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .offset(x = 4.dp, y = (-4).dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.error)
                .clickable { onRemove() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Remove",
                modifier = Modifier.size(14.dp),
                tint = Color.White
            )
        }
    }
}

/**
 * Add button for adding more attachments
 */
@Composable
private fun AddAttachmentButton(
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = FinosMint.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Rounded.Add,
            contentDescription = "Add attachment",
            modifier = Modifier.size(32.dp),
            tint = FinosMint
        )
    }
}

/**
 * Get the appropriate icon for a MIME type
 */
private fun getIconForMimeType(mimeType: String): ImageVector {
    return when {
        mimeType.startsWith("image/") -> Icons.Rounded.Image
        mimeType == "application/pdf" -> Icons.Rounded.PictureAsPdf
        mimeType.contains("word") || mimeType.contains("document") -> Icons.Rounded.Description
        mimeType.contains("excel") || mimeType.contains("spreadsheet") -> Icons.Rounded.Description
        mimeType == "text/plain" -> Icons.Rounded.Description
        else -> Icons.Rounded.InsertDriveFile
    }
}

/**
 * Get the appropriate color for a MIME type
 */
private fun getColorForMimeType(mimeType: String): Color {
    return when {
        mimeType.startsWith("image/") -> Color(0xFF4CAF50) // Green
        mimeType == "application/pdf" -> Color(0xFFE53935) // Red
        mimeType.contains("word") || mimeType.contains("document") -> Color(0xFF2196F3) // Blue
        mimeType.contains("excel") || mimeType.contains("spreadsheet") -> Color(0xFF4CAF50) // Green
        mimeType == "text/plain" -> Color(0xFF757575) // Gray
        else -> Color(0xFF9E9E9E) // Gray
    }
}
