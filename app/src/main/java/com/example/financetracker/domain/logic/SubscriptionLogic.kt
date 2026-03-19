package com.example.financetracker.domain.logic

import com.example.financetracker.presentation.model.TransactionUiModel
import javax.inject.Inject
import java.util.concurrent.TimeUnit
import kotlin.math.abs

data class DetectedSubscription(
    val merchant: String,
    val amount: Long, // In cents (positive)
    val frequency: String, // "Monthly"
    val nextDueDate: Long,
    val probability: Float // 0.0 to 1.0 confidence
)

class SubscriptionLogic @Inject constructor() {

    fun detectSubscriptions(transactions: List<TransactionUiModel>): List<DetectedSubscription> {
        // 1. Filter for expenses only
        val expenses = transactions.filter { it.amount < 0 }

        // 2. Group by Merchant (normalized) and Amount
        val grouped = expenses.groupBy { 
            Pair(it.merchant.lowercase().trim(), abs(it.amount)) // Use amount in UI model (Double) or Entity (Long)?
            // UI Model amount is Double. We should arguably use Entity for precision, but UI Model is what we likely have in ViewModel.
            // Let's assume UI Model for now, or convert to cents key.
            // Pair(merchant, amount)
        }

        val results = mutableListOf<DetectedSubscription>()

        grouped.forEach { (key, txList) ->
            val (merchantName, amountDouble) = key
            
            // Need at least 2 occurrences to establish a pattern
            if (txList.size >= 2) {
                // Sort by date descending (newest first)
                val sorted = txList.sortedByDescending { it.timestamp }
                
                // Check intervals
                var isMonthly = true
                var totalDaysDiff = 0L
                var intervalCount = 0

                for (i in 0 until sorted.size - 1) {
                    val current = sorted[i]
                    val next = sorted[i + 1] // older
                    
                    val diffMs = current.timestamp - next.timestamp
                    val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs)
                    
                    // Monthly is roughly 28-32 days. 
                    // Let's be lenient: 25 to 35 days.
                    if (diffDays !in 25..35) {
                        isMonthly = false
                        // Could check for weekly/yearly later
                    } else {
                        totalDaysDiff += diffDays
                        intervalCount++
                    }
                }

                // If patterns match (mostly monthly)
                if (isMonthly || (intervalCount > 0 && intervalCount >= sorted.size - 2)) {
                    // Calculate next due date
                    // Add ~30 days to the most recent transaction
                    val lastTx = sorted.first()
                    // Average interval or just 30 days?
                    val avgInterval = if (intervalCount > 0) totalDaysDiff / intervalCount else 30
                    val nextDue = lastTx.timestamp + TimeUnit.DAYS.toMillis(avgInterval)
                    
                    // Only include if "next due" is in future or recent past (active sub)
                    // If next due was 6 months ago, it's likely cancelled.
                    // Let's say if nextDue > (Now - 45 days)
                    val cutOff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(45)
                    
                    if (nextDue > cutOff) {
                         results.add(
                            DetectedSubscription(
                                merchant = sorted.first().merchant, // Use original casing from most recent
                                amount = (abs(amountDouble) * 100).toLong(), // Convert back to cents for precision if needed? 
                                // Actually, if UI model is Double, let's keep it consistent or use cents for storage.
                                // Let's use cents for safe math.
                                frequency = "Monthly",
                                nextDueDate = nextDue,
                                probability = 0.8f + (0.1f * intervalCount).coerceAtMost(0.2f)
                            )
                        )
                    }
                }
            }
        }

        return results.sortedBy { it.nextDueDate }
    }
}
