package com.example.financetracker.core.util

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import java.io.File

object TransactionAttachmentUtil {

    private const val ATTACHMENTS_DIR = "transaction_attachments"
    private const val MAX_ATTACHMENTS = 5

    fun getAttachmentsDirectory(context: Context): File {
        return File(context.filesDir, ATTACHMENTS_DIR).apply {
            if (!exists()) mkdirs()
        }
    }

    fun generateFileName(transactionId: Long, index: Int, originalName: String): String {
        val timestamp = System.currentTimeMillis()
        val sanitizedName = originalName.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        return "transaction_${transactionId}_${index}_${timestamp}_$sanitizedName"
    }

    fun copyFileToInternalStorage(
        context: Context,
        uri: Uri,
        transactionId: Long,
        index: Int
    ): AttachmentFileInfo? {
        return try {
            val originalFileName = getFileName(context, uri) ?: "attachment"
            val mimeType = getMimeType(context, uri) ?: "application/octet-stream"
            val newFileName = generateFileName(transactionId, index, originalFileName)

            val attachmentsDir = getAttachmentsDirectory(context)
            val destinationFile = File(attachmentsDir, newFileName)

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                destinationFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            } ?: return null

            AttachmentFileInfo(
                filePath = destinationFile.absolutePath,
                fileName = originalFileName,
                mimeType = mimeType
            )
        } catch (e: Exception) {
            AppLogger.e("TransactionAttachmentUtil", "Failed to copy file to internal storage", e)
            null
        }
    }

    fun deleteFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            AppLogger.e("TransactionAttachmentUtil", "Failed to delete file: $filePath", e)
            false
        }
    }

    fun getMimeType(context: Context, uri: Uri): String? {
        return context.contentResolver.getType(uri)
            ?: MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            )
    }

    fun getFileName(context: Context, uri: Uri): String? {
        var fileName: String? = null

        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex)
                    }
                }
            }
        }

        if (fileName == null) {
            fileName = uri.path?.substringAfterLast('/')
        }

        return fileName
    }

    fun isImage(mimeType: String): Boolean {
        return mimeType.startsWith("image/")
    }

    fun isPdf(mimeType: String): Boolean {
        return mimeType == "application/pdf"
    }

    fun isDocument(mimeType: String): Boolean {
        return mimeType in listOf(
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain",
            "text/csv",
            "application/rtf"
        )
    }

    fun canAddMoreAttachments(currentCount: Int): Boolean {
        return currentCount < MAX_ATTACHMENTS
    }

    fun getMaxAttachments(): Int = MAX_ATTACHMENTS

    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "")
    }

    fun getIconTypeForMimeType(mimeType: String): AttachmentIconType {
        return when {
            isImage(mimeType) -> AttachmentIconType.IMAGE
            isPdf(mimeType) -> AttachmentIconType.PDF
            isDocument(mimeType) -> AttachmentIconType.DOCUMENT
            else -> AttachmentIconType.FILE
        }
    }
}

data class AttachmentFileInfo(
    val filePath: String,
    val fileName: String,
    val mimeType: String
)

enum class AttachmentIconType {
    IMAGE,
    PDF,
    DOCUMENT,
    FILE
}
