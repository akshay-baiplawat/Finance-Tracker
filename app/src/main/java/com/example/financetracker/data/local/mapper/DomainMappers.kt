package com.example.financetracker.data.local.mapper

import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.data.local.entity.WalletEntity
import com.example.financetracker.presentation.model.AccountUiModel
import com.example.financetracker.presentation.model.PersonUiModel
import com.example.financetracker.presentation.model.TransactionUiModel
import com.example.financetracker.core.util.CurrencyFormatter
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.graphics.Color as AndroidColor
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

// Helper for Currency
fun Long.formatAsCurrency(currencyCode: String = "INR"): String {
    return CurrencyFormatter.format(this, currencyCode)
}

// Wallet to AccountUiModel
fun WalletEntity.toAccountUiModel(currencyCode: String): AccountUiModel {
    val parsedColor = try {
        Color(AndroidColor.parseColor(this.colorHex))
    } catch (e: Exception) {
        Color.Gray // Fallback
    }
    
    return AccountUiModel(
        id = this.id,
        name = this.name,
        balanceFormatted = this.balance.formatAsCurrency(currencyCode),
        rawBalance = this.balance,
        type = this.type,
        color = parsedColor
    )
}

// Transaction to TransactionUiModel
fun TransactionEntity.toTransactionUiModel(currencyCode: String): TransactionUiModel {
    val amountFormatted = if (this.type == "INCOME") "+${this.amount.formatAsCurrency(currencyCode)}" else "-${this.amount.formatAsCurrency(currencyCode)}"
    val amountColor = if (this.type == "INCOME") AndroidColor.parseColor("#4CAF50") else AndroidColor.parseColor("#F44336") // Green/Red
    
    val date = Instant.ofEpochMilli(this.timestamp).atZone(ZoneId.systemDefault())
    val formattedDate = DateTimeFormatter.ofPattern("dd MMM, hh:mm a").format(date)

    return TransactionUiModel(
        id = this.id,
        title = this.note,
        amount = this.amount,
        amountFormatted = amountFormatted,
        amountColor = amountColor,
        date = date.toLocalDate(),
        dateFormatted = formattedDate,
        category = this.categoryName,
        type = this.type
    )
}

// Person to PersonUiModel
fun com.example.financetracker.data.local.entity.PersonEntity.toPersonUiModel(currencyCode: String): com.example.financetracker.presentation.model.PersonUiModel {
    // Logic for Status Text and Color
    // If balance > 0 -> "Owes you" (Green)
    // If balance < 0 -> "You owe" (Orange)
    // If 0 -> "Settled" (Grey)
    
    val statusText = when {
        this.currentBalance > 0 -> "Owes you"
        this.currentBalance < 0 -> "You owe"
        else -> "Settled"
    }
    
    val statusColor = when {
        this.currentBalance > 0 -> com.example.financetracker.presentation.theme.FinosMint
        this.currentBalance < 0 -> androidx.compose.ui.graphics.Color(0xFFFF9800) // Orange
        else -> com.example.financetracker.presentation.theme.TextSecondaryDark
    }

    return com.example.financetracker.presentation.model.PersonUiModel(
        id = this.id,
        name = this.name,
        balanceFormatted = this.currentBalance.formatAsCurrency(currencyCode),
        balance = this.currentBalance,
        statusText = statusText,
        statusColor = statusColor
    )
}
