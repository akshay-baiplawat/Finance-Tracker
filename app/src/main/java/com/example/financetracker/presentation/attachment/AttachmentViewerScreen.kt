package com.example.financetracker.presentation.attachment

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.example.financetracker.core.util.TransactionAttachmentUtil
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import com.example.financetracker.presentation.theme.FinosMint
import java.io.File

/**
 * Full-screen viewer for transaction attachments.
 * Supports images with swipe and pinch-to-zoom, and opens external apps for PDFs/documents.
 *
 * @param attachments List of attachments to display
 * @param initialIndex Starting index for the viewer
 * @param onBack Callback when the back button is pressed
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AttachmentViewerScreen(
    attachments: List<TransactionAttachmentEntity>,
    initialIndex: Int = 0,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(initialPage = initialIndex.coerceIn(0, attachments.lastIndex.coerceAtLeast(0))) {
        attachments.size
    }

    val currentAttachment = attachments.getOrNull(pagerState.currentPage)
    val isImage = currentAttachment?.let { TransactionAttachmentUtil.isImage(it.mimeType) } ?: false

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = currentAttachment?.fileName ?: "Attachment",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1
                        )
                        if (attachments.size > 1) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${attachments.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    if (currentAttachment != null && !isImage) {
                        IconButton(onClick = {
                            openFileInExternalApp(context, currentAttachment)
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.OpenInNew,
                                contentDescription = "Open in external app"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = Color.Black
    ) { padding ->
        if (attachments.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No attachments",
                    color = Color.White
                )
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) { page ->
                val attachment = attachments[page]

                if (TransactionAttachmentUtil.isImage(attachment.mimeType)) {
                    ZoomableImage(
                        filePath = attachment.filePath,
                        contentDescription = attachment.fileName
                    )
                } else {
                    NonImageAttachmentView(
                        attachment = attachment,
                        onOpenExternal = {
                            openFileInExternalApp(context, attachment)
                        }
                    )
                }
            }
        }
    }
}

/**
 * Zoomable image composable with pinch-to-zoom and pan support
 */
@Composable
private fun ZoomableImage(
    filePath: String,
    contentDescription: String
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTransformGestures { _, pan, zoom, _ ->
                    scale = (scale * zoom).coerceIn(0.5f, 5f)

                    // Only allow panning when zoomed in
                    if (scale > 1f) {
                        val maxX = (scale - 1) * size.width / 2
                        val maxY = (scale - 1) * size.height / 2
                        offset = Offset(
                            x = (offset.x + pan.x).coerceIn(-maxX, maxX),
                            y = (offset.y + pan.y).coerceIn(-maxY, maxY)
                        )
                    } else {
                        offset = Offset.Zero
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = File(filePath),
            contentDescription = contentDescription,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offset.x,
                    translationY = offset.y
                ),
            contentScale = ContentScale.Fit
        )
    }
}

/**
 * View for non-image attachments (PDFs, documents)
 */
@Composable
private fun NonImageAttachmentView(
    attachment: TransactionAttachmentEntity,
    onOpenExternal: () -> Unit
) {
    val isPdf = TransactionAttachmentUtil.isPdf(attachment.mimeType)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isPdf) Icons.Rounded.PictureAsPdf else Icons.Rounded.Description,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = if (isPdf) Color(0xFFE53935) else Color(0xFF2196F3)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = attachment.fileName,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when {
                isPdf -> "PDF Document"
                attachment.mimeType.contains("word") -> "Word Document"
                attachment.mimeType.contains("excel") || attachment.mimeType.contains("spreadsheet") -> "Spreadsheet"
                attachment.mimeType == "text/plain" -> "Text File"
                else -> "Document"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onOpenExternal,
            colors = ButtonDefaults.buttonColors(
                containerColor = FinosMint
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.OpenInNew,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open with External App")
        }
    }
}

/**
 * Opens a file in an external app using FileProvider
 */
private fun openFileInExternalApp(
    context: android.content.Context,
    attachment: TransactionAttachmentEntity
) {
    try {
        val file = File(attachment.filePath)
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            file
        )

        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, attachment.mimeType)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Open with"))
    } catch (e: Exception) {
        // Handle error - could show a toast
        e.printStackTrace()
    }
}
