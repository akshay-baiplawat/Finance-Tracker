package com.example.financetracker.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Utility class for camera capture operations.
 * Handles creating temporary files for camera photos and generating URIs via FileProvider.
 */
object CameraCaptureUtil {

    private const val FILE_PROVIDER_AUTHORITY = "com.example.financetracker.provider"
    private const val CAMERA_IMAGES_DIR = "camera_images"

    /**
     * Creates a temporary file for storing a camera-captured image.
     * Returns a Pair of (File, Uri) where Uri is suitable for camera intent.
     *
     * @param context Application context
     * @return Pair of (File, Uri) or null if creation fails
     */
    fun createImageFile(context: Context): Pair<File, Uri>? {
        return try {
            // Create a unique filename with timestamp
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val imageFileName = "IMG_${timeStamp}.jpg"

            // Get or create the camera images directory
            val storageDir = File(context.filesDir, CAMERA_IMAGES_DIR)
            if (!storageDir.exists()) {
                storageDir.mkdirs()
            }

            // Create the file
            val imageFile = File(storageDir, imageFileName)

            // Create content URI via FileProvider
            val imageUri = FileProvider.getUriForFile(
                context,
                FILE_PROVIDER_AUTHORITY,
                imageFile
            )

            Pair(imageFile, imageUri)
        } catch (e: Exception) {
            AppLogger.e("CameraCaptureUtil", "Failed to create image file", e)
            null
        }
    }

    /**
     * Deletes a camera image file if it exists.
     *
     * @param file The file to delete
     * @return true if deleted successfully, false otherwise
     */
    fun deleteImageFile(file: File): Boolean {
        return try {
            if (file.exists()) {
                file.delete()
            } else {
                true
            }
        } catch (e: Exception) {
            AppLogger.e("CameraCaptureUtil", "Failed to delete image file", e)
            false
        }
    }

    /**
     * Cleans up old camera images that were not used.
     * Call this periodically or on app startup to prevent storage bloat.
     *
     * @param context Application context
     * @param maxAgeMillis Maximum age of files to keep (default: 24 hours)
     */
    fun cleanupOldImages(context: Context, maxAgeMillis: Long = 24 * 60 * 60 * 1000) {
        try {
            val storageDir = File(context.filesDir, CAMERA_IMAGES_DIR)
            if (storageDir.exists()) {
                val now = System.currentTimeMillis()
                storageDir.listFiles()?.forEach { file ->
                    if (now - file.lastModified() > maxAgeMillis) {
                        file.delete()
                    }
                }
            }
        } catch (e: Exception) {
            AppLogger.e("CameraCaptureUtil", "Failed to cleanup old images", e)
        }
    }

    /**
     * Gets the URI for an existing camera image file.
     *
     * @param context Application context
     * @param file The image file
     * @return Content URI for the file
     */
    fun getUriForFile(context: Context, file: File): Uri {
        return FileProvider.getUriForFile(
            context,
            FILE_PROVIDER_AUTHORITY,
            file
        )
    }
}
