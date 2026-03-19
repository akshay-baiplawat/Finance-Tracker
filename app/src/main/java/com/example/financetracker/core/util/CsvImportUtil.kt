package com.example.financetracker.core.util

import android.content.Context
import android.net.Uri
import com.example.financetracker.domain.model.ImportedTransactionData
import com.example.financetracker.data.local.entity.TransactionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Locale
import javax.inject.Inject

class CsvImportUtil @Inject constructor(
    @ApplicationContext private val context: Context
) {

    suspend fun parseCsv(uri: Uri): List<ImportedTransactionData> = withContext(Dispatchers.IO) {
        val results = mutableListOf<ImportedTransactionData>()
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Skip header
                    val header = reader.readLine()
                    // Validate header? "Date,Merchant,Amount,Type,Category"
                    // Relax validation for now, just assume format.

                    var line: String?
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

                    while (reader.readLine().also { line = it } != null) {
                        try {
                            // Simple CSV splitting (ignoring quoted commas for V1, assuming ExportUtil format)
                            // ExportUtil escapes quotes as "".
                            // We need a proper CSV parser or a smart split.
                            // Regex for split: ,(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$) handles quoted commas.
                            val tokens = line!!.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)".toRegex())
                            
                            if (tokens.size >= 5) {
                                val dateStr = tokens[0]
                                val merchant = unescapeCsv(tokens[1])
                                val amountDouble = tokens[2].toDoubleOrNull() ?: 0.0
                                val typeStr = tokens[3]
                                val category = unescapeCsv(tokens[4])

                                val date = dateFormat.parse(dateStr)?.time ?: System.currentTimeMillis()
                                val amountPaisa = (amountDouble * 100).toLong()
                                val type = when (typeStr.uppercase()) {
                                    "EXPENSE", "DEBIT" -> TransactionType.EXPENSE
                                    "INCOME", "CREDIT" -> TransactionType.INCOME
                                    "TRANSFER" -> TransactionType.TRANSFER
                                    else -> TransactionType.EXPENSE
                                }

                                results.add(ImportedTransactionData(date, merchant, amountPaisa, type, category))
                            }
                        } catch (e: Exception) {
                            e.printStackTrace() // Skip bad lines
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext results
    }

    private fun unescapeCsv(value: String): String {
        var result = value
        if (result.startsWith("\"") && result.endsWith("\"")) {
            result = result.substring(1, result.length - 1)
        }
        return result.replace("\"\"", "\"")
    }
}
