package com.example.financetracker.core.util

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.financetracker.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CsvExportUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        /** Number of transactions to fetch per chunk during streaming export */
        private const val CHUNK_SIZE = 1000
    }

    suspend fun generateCsv(transactions: List<TransactionEntity>): Uri? = withContext(Dispatchers.IO) {
        try {
            val fileName = "finance_export_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            
            FileWriter(file).use { writer ->
                // Header
                writer.append("Date,Merchant,Amount,Type,Category\n")
                
                // Rows
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                transactions.forEach { item ->
                    val date = dateFormat.format(Date(item.timestamp))
                    val merchant = escapeCsv(item.note) // Note is Merchant/Description
                    val amountDecimal = item.amount / 100.0
                    val type = item.type
                    val category = escapeCsv(item.categoryName)
                    
                    writer.append("$date,$merchant,$amountDecimal,$type,$category\n")
                }
            }
            
            // Get URI using FileProvider
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                file
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun saveToDownloads(transactions: List<TransactionEntity>): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "finance_export_${System.currentTimeMillis()}.csv"

            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outputStream))

                    // Header
                    writer.write("Date,Merchant,Amount,Type,Category,Source")
                    writer.newLine()

                    // Rows
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    transactions.forEach { item ->
                        val date = dateFormat.format(Date(item.timestamp))
                        val merchant = escapeCsv(item.note)
                        val amount = (item.amount / 100.0).toString()
                        val type = item.type
                        val category = escapeCsv(item.categoryName)
                        val source = if (item.note.contains("SMS:")) "SMS" else "MANUAL"

                        writer.write("$date,$merchant,$amount,$type,$category,$source")
                        writer.newLine()
                    }

                    writer.flush()
                }
                fileName // Return filename on success
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Streaming export that fetches transactions in chunks to prevent OOM on large datasets.
     * Each chunk is written to file before fetching the next, keeping memory usage flat.
     *
     * @param getChunk Function to fetch a chunk of transactions (limit, offset) -> List
     * @param totalCount Total number of transactions to export
     * @return Filename on success, null on failure
     */
    suspend fun saveToDownloadsStreaming(
        getChunk: suspend (limit: Int, offset: Int) -> List<TransactionEntity>,
        totalCount: Int
    ): String? = withContext(Dispatchers.IO) {
        try {
            val fileName = "finance_export_${System.currentTimeMillis()}.csv"

            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/csv")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    val writer = java.io.BufferedWriter(java.io.OutputStreamWriter(outputStream))
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    // Write header
                    writer.write("Date,Merchant,Amount,Type,Category,Source")
                    writer.newLine()

                    // Stream chunks
                    var offset = 0
                    while (offset < totalCount) {
                        val chunk = getChunk(CHUNK_SIZE, offset)
                        if (chunk.isEmpty()) break

                        // Write chunk to file
                        chunk.forEach { item ->
                            val date = dateFormat.format(Date(item.timestamp))
                            val merchant = escapeCsv(item.note)
                            val amount = (item.amount / 100.0).toString()
                            val type = item.type
                            val category = escapeCsv(item.categoryName)
                            val source = if (item.note.contains("SMS:")) "SMS" else "MANUAL"

                            writer.write("$date,$merchant,$amount,$type,$category,$source")
                            writer.newLine()
                        }

                        // Flush after each chunk to free memory
                        writer.flush()
                        offset += CHUNK_SIZE
                    }
                }
                fileName
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun escapeCsv(value: String): String {
        var result = value

        // Protect against CSV formula injection
        // Cells starting with =, +, -, @, tab, or carriage return can be interpreted as formulas
        val formulaChars = setOf('=', '+', '-', '@', '\t', '\r')
        if (result.isNotEmpty() && result[0] in formulaChars) {
            result = "'$result"  // Prefix with single quote to prevent formula execution
        }

        if (result.contains(",") || result.contains("\"") || result.contains("\n") || result.contains("'")) {
            result = result.replace("\"", "\"\"")
            result = "\"$result\""
        }
        return result
    }
}
