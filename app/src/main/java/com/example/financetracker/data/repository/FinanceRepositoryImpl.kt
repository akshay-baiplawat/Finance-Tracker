package com.example.financetracker.data.repository

import androidx.compose.ui.graphics.Color
import androidx.room.withTransaction
import com.example.financetracker.data.local.AppDatabase
import com.example.financetracker.data.local.FinanceDao
import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.domain.repository.FinanceRepository
import com.example.financetracker.presentation.model.AccountUiModel
import com.example.financetracker.presentation.model.MonthlyReport
import com.example.financetracker.presentation.model.TransactionUiModel
import com.example.financetracker.domain.repository.UserPreferencesRepository
import com.example.financetracker.core.util.CurrencyFormatter
import com.example.financetracker.core.util.DateUtil
import com.example.financetracker.core.parser.CategoryInferrer
import com.example.financetracker.core.parser.MerchantNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CoroutineScope
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import com.example.financetracker.data.local.dao.GroupDao
import com.example.financetracker.data.local.dao.NotificationDao
import com.example.financetracker.data.local.dao.TransactionAttachmentDao
import com.example.financetracker.di.ApplicationScope

class FinanceRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val financeDao: FinanceDao,
    private val groupDao: GroupDao,
    private val notificationDao: NotificationDao,
    private val transactionAttachmentDao: TransactionAttachmentDao,
    private val database: AppDatabase,
    private val userPreferencesRepository: UserPreferencesRepository,
    @ApplicationScope private val applicationScope: CoroutineScope
) : FinanceRepository {

    // Cached categories flow - shared across all transaction queries to prevent N+1 queries
    // Uses stateIn with Eagerly + empty initial list to ensure combine() doesn't block
    private val cachedCategories = financeDao.getAllCategories()
        .stateIn(applicationScope, SharingStarted.Eagerly, emptyList())

    // Cached attachment counts flow - shared across all transaction queries
    private val cachedAttachmentCounts = transactionAttachmentDao.getAttachmentCounts()
        .map { counts -> counts.associate { it.transactionId to it.count } }
        .stateIn(applicationScope, SharingStarted.Eagerly, emptyMap())

    override fun getNetWorth(): Flow<Long> {
        return financeDao.getTotalNetWorth()
    }

    override fun getRecentTransactions(): Flow<List<TransactionUiModel>> {
        return combine(
            financeDao.getRecentTransactions(),
            userPreferencesRepository.currency,
            cachedCategories,
            cachedAttachmentCounts
        ) { entities, currency, categories, attachmentCounts ->
            val categoryMap = categories.associateBy { it.name }
            entities.map { entity ->
                mapToTransactionUiModel(entity, currency, categoryMap, attachmentCounts)
            }
        }
    }

    override fun searchTransactions(query: String): Flow<List<TransactionUiModel>> {
        val flow = if (query.isBlank()) {
            financeDao.getAllTransactions()
        } else {
            financeDao.searchTransactions(query)
        }

        return combine(flow, userPreferencesRepository.currency, cachedCategories, cachedAttachmentCounts) { entities, currency, categories, attachmentCounts ->
            val categoryMap = categories.associateBy { it.name }
            entities.map { mapToTransactionUiModel(it, currency, categoryMap, attachmentCounts) }
        }
    }

    override fun getAllWallets(): Flow<List<AccountUiModel>> {
        return combine(financeDao.getAllWallets(), userPreferencesRepository.currency) { entities, currency ->
            entities.map { entity ->
                AccountUiModel(
                    id = entity.id,
                    name = entity.name,
                    balanceFormatted = formatCurrency(entity.balance, currency),
                    rawBalance = entity.balance,
                    type = entity.type, // e.g., "CASH", "BANK"
                    color = Color(android.graphics.Color.parseColor(entity.colorHex))
                )
            }
        }
    }

    override fun getAllTransactions(): Flow<List<TransactionUiModel>> {
        return combine(
            financeDao.getAllTransactions(),
            userPreferencesRepository.currency,
            cachedCategories,
            cachedAttachmentCounts
        ) { entities, currency, categories, attachmentCounts ->
            val categoryMap = categories.associateBy { it.name }
            entities.map { mapToTransactionUiModel(it, currency, categoryMap, attachmentCounts) }
        }
    }

    override fun getTransactionsBetween(start: Long, end: Long): Flow<List<TransactionUiModel>> {
        return combine(
            financeDao.getTransactionsBetween(start, end),
            userPreferencesRepository.currency,
            cachedCategories,
            cachedAttachmentCounts
        ) { entities, currency, categories, attachmentCounts ->
            val categoryMap = categories.associateBy { it.name }
            entities.map { entity ->
                mapToTransactionUiModel(entity, currency, categoryMap, attachmentCounts)
            }
        }
    }



    override fun getAllPeople(): Flow<List<com.example.financetracker.data.local.entity.PersonEntity>> {
        return financeDao.getAllPeople()
    }

    override fun getCategoryBreakdown(): Flow<List<com.example.financetracker.data.local.CategoryTuple>> {
        return financeDao.getCategoryBreakdown()
    }

    override fun getIncomeBreakdown(): Flow<List<com.example.financetracker.data.local.CategoryTuple>> {
        return financeDao.getIncomeCategoryBreakdown()
    }

    override fun getMonthlyCashFlow(monthStart: Long, monthEnd: Long): Flow<MonthlyReport> {
        return combine(
            financeDao.getMonthlyIncome(monthStart, monthEnd),
            financeDao.getMonthlyExpense(monthStart, monthEnd)
        ) { income, expense ->
            MonthlyReport(
                totalIncome = income,
                totalExpense = expense,
                netSavings = income - expense
            )
        }
    }

    override fun getAllBudgets(): Flow<List<com.example.financetracker.data.local.entity.BudgetEntity>> {
        return financeDao.getAllBudgets()
    }

    override suspend fun addPerson(name: String, amount: Long) {
        financeDao.insertPerson(
            com.example.financetracker.data.local.entity.PersonEntity(
                name = name,
                currentBalance = amount
            )
        )
    }

    override suspend fun addTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            // 1. Insert
            financeDao.insertTransaction(transaction)

            // 2. Update Wallet Balance
            val amount = transaction.amount
            val type = transaction.type
            
            when (type) {
                "EXPENSE" -> {
                    // Subtract from source
                    financeDao.updateWalletBalance(transaction.walletId, -amount)
                }
                "INCOME" -> {
                    // Add to source
                    financeDao.updateWalletBalance(transaction.walletId, amount)
                }
                "TRANSFER" -> {
                    // Subtract from source (Assumes walletId is source)
                    financeDao.updateWalletBalance(transaction.walletId, -amount)
                    // TODO: Handle destination wallet if destWalletId exists
                }
            }

            // 3. Update Person Balance (Debt Logic)
            transaction.personId?.let { personId ->
                val debtImpact = if (type == "EXPENSE") amount else -amount
                // If I paid (EXPENSE), they owe me (+). If they paid me back (INCOME), debt decreases (-).
                financeDao.updatePersonBalance(personId, debtImpact)
            }
        }
    }

    override suspend fun updateTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            // 1. Fetch Original
            val original = financeDao.getTransactionById(transaction.id) ?: return@withTransaction

            // 2. Revert Original Impact
            // Revert Wallet
            when (original.type) {
                "EXPENSE" -> financeDao.updateWalletBalance(original.walletId, original.amount) // Add back
                "INCOME" -> financeDao.updateWalletBalance(original.walletId, -original.amount) // Subtract
                "TRANSFER" -> financeDao.updateWalletBalance(original.walletId, original.amount) // Add back
            }
             // Revert Person
            original.personId?.let { personId ->
                val revertDebt = if (original.type == "EXPENSE") -original.amount else original.amount
                financeDao.updatePersonBalance(personId, revertDebt)
            }

            // 3. Update Transaction (use @Update to avoid CASCADE delete on attachments)
            financeDao.updateTransactionEntity(transaction)

            // 4. Apply New Impact
            // Apply Wallet
            when (transaction.type) {
                "EXPENSE" -> financeDao.updateWalletBalance(transaction.walletId, -transaction.amount)
                "INCOME" -> financeDao.updateWalletBalance(transaction.walletId, transaction.amount)
                "TRANSFER" -> financeDao.updateWalletBalance(transaction.walletId, -transaction.amount)
            }
            // Apply Person
            transaction.personId?.let { personId ->
                val debtImpact = if (transaction.type == "EXPENSE") transaction.amount else -transaction.amount
                financeDao.updatePersonBalance(personId, debtImpact)
            }
        }
    }

    override suspend fun getTransactionById(id: Long): TransactionEntity? {
        return financeDao.getTransactionById(id)
    }

    override suspend fun addBudget(category: String, limit: Long, color: String, rolloverEnabled: Boolean) {
        val budget = com.example.financetracker.data.local.entity.BudgetEntity(
            categoryName = category,
            limitAmount = limit,
            colorHex = color,
            rolloverEnabled = rolloverEnabled,
            createdTimestamp = System.currentTimeMillis()
        )
        financeDao.insertBudget(budget)
    }

    override suspend fun deleteBudget(id: Long) {
        financeDao.deleteBudget(id)
    }

    override suspend fun updateBudget(id: Long, category: String, limit: Long, color: String, rolloverEnabled: Boolean) {
        financeDao.updateBudgetDetails(id, category, limit, color, rolloverEnabled)
    }

    override suspend fun addWallet(name: String, type: String, amount: Long, color: String): Long {
         val wallet = com.example.financetracker.data.local.entity.WalletEntity(
             name = name,
             type = type,
             balance = amount,
             colorHex = color,
             accountNumber = "N/A"
         )
         return financeDao.insertWallet(wallet)
    }

    override suspend fun deleteWallet(id: Long) {
        financeDao.deleteWallet(id)
    }

    override suspend fun deletePerson(id: Long) {
        financeDao.deletePerson(id)
    }

    override suspend fun updateWallet(id: Long, name: String, type: String, amount: Long, color: String) {
        val wallet = com.example.financetracker.data.local.entity.WalletEntity(
             id = id,
             name = name,
             type = type,
             balance = amount,
             colorHex = color,
             accountNumber = "N/A"
         )
         financeDao.insertWallet(wallet)
    }

    override suspend fun updatePerson(id: Long, name: String, amount: Long) {
        val person = com.example.financetracker.data.local.entity.PersonEntity(
            id = id,
            name = name,
            currentBalance = amount
        )
        financeDao.insertPerson(person)
    }

    override suspend fun syncTransactions(startTime: Long): Int {
        // Check if Privacy Mode is enabled - skip SMS sync if so
        if (userPreferencesRepository.isPrivacyMode.first()) {
            return 0
        }

        val smsReader = com.example.financetracker.domain.SmsReader(context)
        val transactions = smsReader.getTransactions(startTime)

        if (transactions.isEmpty()) {
            return 0
        }

        val wallets = financeDao.getAllWalletsList() // Helper to find a valid wallet
        val defaultWalletId = if (wallets.isNotEmpty()) {
            wallets[0].id
        } else {
            // Create default wallet to avoid Foreign Key crash
            val defaultWallet = com.example.financetracker.data.local.entity.WalletEntity(
                name = "Cash",
                type = "CASH",
                balance = 0,
                colorHex = "#4CAF50",
                accountNumber = "N/A"
            )
            financeDao.insertWallet(defaultWallet)
        }

        // BATCH FETCH: Get all SMS IDs that need checking
        val smsIdsToCheck = transactions.mapNotNull { it.smsId }

        // SINGLE QUERY: Get existing SMS IDs in one database call
        val existingSmsIds = if (smsIdsToCheck.isNotEmpty()) {
            financeDao.getExistingSmsIds(smsIdsToCheck).toSet()
        } else {
            emptySet()
        }

        // Filter to only new transactions (in-memory check)
        val newTransactions = transactions.filter { tx ->
            tx.smsId != null && tx.smsId !in existingSmsIds
        }

        var addedCount = 0
        val isTripMode = userPreferencesRepository.isTripMode.first()

        database.withTransaction {
            newTransactions.forEach { tx ->
                val amountCents = (Math.abs(tx.amount) * 100).toLong()
                val isIncome = tx.type == com.example.financetracker.model.TransactionType.CREDIT

                // Normalize merchant name and infer category
                val normalizedMerchant = MerchantNormalizer.normalize(tx.merchant)
                val inferredCategory = CategoryInferrer.inferCategory(
                    merchant = normalizedMerchant,
                    isIncome = isIncome,
                    isTripMode = isTripMode
                )

                val entity = com.example.financetracker.data.local.entity.TransactionEntity(
                    amount = amountCents,
                    categoryName = inferredCategory,
                    type = if (isIncome) "INCOME" else "EXPENSE",
                    timestamp = tx.date,
                    merchant = normalizedMerchant,
                    note = "Via SMS: ${tx.rawSender}",
                    walletId = defaultWalletId,
                    smsId = tx.smsId
                )
                addTransaction(entity)
                addedCount++
            }
        }

        // Update last sync timestamp after successful sync
        userPreferencesRepository.updateLastSyncTimestamp(System.currentTimeMillis())

        return addedCount
    }

    override suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.withTransaction {
            // 1. Delete
            financeDao.deleteTransaction(transaction)

            // 2. Revert Wallet Balance
            val amount = transaction.amount
            val type = transaction.type

            when (type) {
                "EXPENSE" -> {
                    // Revert Expense: Add back
                    financeDao.updateWalletBalance(transaction.walletId, amount)
                }
                "INCOME" -> {
                    // Revert Income: Subtract
                    financeDao.updateWalletBalance(transaction.walletId, -amount)
                }
                "TRANSFER" -> {
                    // Revert Transfer: Add back to source
                    financeDao.updateWalletBalance(transaction.walletId, amount)
                }
            }

            // 3. Revert Person Balance
            transaction.personId?.let { personId ->
                // Inverse of Add Logic
                val debtImpact = if (type == "EXPENSE") -amount else amount
                financeDao.updatePersonBalance(personId, debtImpact)
            }
        }
    }

    // Helper: Map Entity to UI Model with dynamic category lookup
    private fun mapToTransactionUiModel(
        entity: TransactionEntity,
        currencyCode: String,
        categoryMap: Map<String, com.example.financetracker.data.local.entity.CategoryEntity>,
        attachmentCounts: Map<Long, Int> = emptyMap()
    ): TransactionUiModel {
        val date = Instant.ofEpochMilli(entity.timestamp)
            .atZone(ZoneId.systemDefault())

        val dateStr = DateUtil.formatDateWithTime(entity.timestamp, context)

        val amountStr = formatCurrency(entity.amount, currencyCode)

        val color = if (entity.type == "INCOME") android.graphics.Color.parseColor("#4CAF50")
                    else android.graphics.Color.parseColor("#F44336")

        // Look up category from map to get actual color and iconId
        val category = categoryMap[entity.categoryName]
        val iconId = category?.iconId
            ?: com.example.financetracker.core.util.CategoryIconUtil.getDefaultIconIdForCategory(entity.categoryName)
        val categoryColor = category?.colorHex
            ?: if (entity.type == "INCOME") "#10B981" else "#EF4444"

        return TransactionUiModel(
            id = entity.id,
            title = entity.note,
            amount = entity.amount,
            amountFormatted = (if (entity.type == "INCOME") "+" else "-") + amountStr,
            amountColor = color,
            date = date.toLocalDate(),
            dateFormatted = dateStr,
            category = entity.categoryName,
            categoryIconId = iconId,
            categoryColorHex = categoryColor,
            type = entity.type,
            merchant = entity.merchant,
            timestamp = entity.timestamp,
            walletId = entity.walletId,
            smsId = entity.smsId,
            personId = entity.personId,
            groupId = entity.groupId,
            attachmentCount = attachmentCounts[entity.id] ?: 0
        )
    }

    private fun formatCurrency(amount: Long, currencyCode: String): String {
        return CurrencyFormatter.format(amount, currencyCode)
    }

    override fun getAllSubscriptions(): Flow<List<com.example.financetracker.data.local.entity.SubscriptionEntity>> {
        return financeDao.getAllSubscriptions()
    }

    override suspend fun addSubscription(merchant: String, amount: Long, frequency: String, nextDueDate: Long, color: String, isAuto: Boolean) {
        financeDao.insertSubscription(
            com.example.financetracker.data.local.entity.SubscriptionEntity(
                merchantName = merchant,
                amount = amount,
                frequency = frequency,
                nextDueDate = nextDueDate,
                colorHex = color,
                isAutoDetected = isAuto
            )
        )
    }

    override suspend fun deleteSubscription(id: Long) {
        financeDao.deleteSubscription(id)
    }

    override suspend fun updateSubscription(id: Long, merchant: String, amount: Long, frequency: String, nextDueDate: Long, color: String) {
         financeDao.updateSubscription(
            com.example.financetracker.data.local.entity.SubscriptionEntity(
                id = id,
                merchantName = merchant,
                amount = amount,
                frequency = frequency,
                nextDueDate = nextDueDate,
                colorHex = color,
                isAutoDetected = false // If manually updated, it's manually managed now
            )
        )
    }

    override fun getAllCategories(): Flow<List<com.example.financetracker.data.local.entity.CategoryEntity>> {
        return financeDao.getAllCategories()
    }

    override suspend fun addCategory(name: String, type: String, colorHex: String, iconId: Int): Boolean {
        // Enforce Unique Name Globally (Expense + Income combined)
        val exists = financeDao.checkCategoryNameExists(name) > 0
        if (exists) return false
        
        financeDao.insertCategory(com.example.financetracker.data.local.entity.CategoryEntity(name = name, type = type, colorHex = colorHex, iconId = iconId))
        return true
    }

    override suspend fun updateCategory(id: Long, name: String, type: String, colorHex: String, iconId: Int): Boolean {
        // Enforce Unique Name Globally (excluding self)
        val exists = financeDao.checkCategoryNameExistsExcluding(name, id) > 0
        if (exists) return false

        // Use direct field update to preserve isSystemDefault flag
        financeDao.updateCategoryFields(id, name, colorHex, iconId)
        return true
    }

    override suspend fun deleteCategory(id: Long) {
        // Create a dummy entity with ID to delete
        // Room only needs the @PrimaryKey for deletion if we pass the entity, 
        // but cleaner might be to add deleteById in DAO. 
        // Logic: Construct minimal entity.
        val entity = com.example.financetracker.data.local.entity.CategoryEntity(
             id = id, 
             name = "", 
             type = "", 
             colorHex = ""
        )
        financeDao.deleteCategory(entity)
    }

    override suspend fun getAllTransactionsForExport(): List<TransactionEntity> {
        return financeDao.getAllTransactionsList()
    }

    override suspend fun getTransactionsChunked(limit: Int, offset: Int): List<TransactionEntity> {
        return financeDao.getTransactionsChunked(limit, offset)
    }

    override suspend fun getTransactionCountForExport(): Int {
        return financeDao.getTransactionCountForExport()
    }

    override suspend fun seedDefaultCategories() {
        // First, clean up any duplicate categories from previous buggy seeding
        financeDao.deleteDuplicateCategories()

        // Expense categories with meaningful colors and iconIds
        val expenseCategories = listOf(
            Triple("Uncategorized", "#6B7280", 0),      // Gray - neutral/unknown
            Triple("Food & Dining", "#F97316", 1),      // Orange - warmth/food
            Triple("Transportation", "#3B82F6", 3),     // Blue - roads/sky/travel
            Triple("Shopping", "#EC4899", 2),           // Pink - retail/fashion
            Triple("Bills & Utilities", "#EAB308", 4),  // Yellow - electricity/attention
            Triple("Entertainment", "#8B5CF6", 5),      // Purple - fun/creativity
            Triple("Health & Wellness", "#10B981", 6),  // Emerald - health/life
            Triple("Money Transfer", "#64748B", 8)      // Slate - neutral transaction
        )

        // Income categories with iconIds
        val incomeCategories = listOf(
            Triple("Salary", "#22C55E", 9),             // Green - money/positive
            Triple("Money Received", "#14B8A6", 10)     // Teal - incoming funds
        )

        // Insert each category only if it doesn't already exist
        expenseCategories.forEach { (name, color, iconId) ->
            try {
                val exists = financeDao.checkCategoryNameExists(name) > 0
                if (!exists) {
                    financeDao.insertCategory(com.example.financetracker.data.local.entity.CategoryEntity(
                        name = name, type = "EXPENSE", colorHex = color, iconId = iconId, isSystemDefault = true
                    ))
                }
            } catch (e: Exception) {
                // Continue with other categories if one fails
                android.util.Log.e("FinanceRepository", "Failed to seed category: $name", e)
            }
        }
        incomeCategories.forEach { (name, color, iconId) ->
            try {
                val exists = financeDao.checkCategoryNameExists(name) > 0
                if (!exists) {
                    financeDao.insertCategory(com.example.financetracker.data.local.entity.CategoryEntity(
                        name = name, type = "INCOME", colorHex = color, iconId = iconId, isSystemDefault = true
                    ))
                }
            } catch (e: Exception) {
                // Continue with other categories if one fails
                android.util.Log.e("FinanceRepository", "Failed to seed category: $name", e)
            }
        }
    }

    override suspend fun ensureTripCategoryExists() {
        // First, clean up any duplicate Trip categories
        val tripCategories = financeDao.getCategoriesByName("Trip")
        if (tripCategories.size > 1) {
            // Keep only the first one, delete the rest
            tripCategories.drop(1).forEach { duplicate ->
                financeDao.deleteCategory(duplicate)
            }
        }

        // Now ensure Trip category exists
        val exists = financeDao.checkCategoryNameExists("Trip") > 0
        if (!exists) {
            financeDao.insertCategory(
                com.example.financetracker.data.local.entity.CategoryEntity(
                    name = "Trip",
                    type = "EXPENSE",
                    colorHex = "#06B6D4",  // Cyan - travel/adventure/sky
                    iconId = 7,
                    isSystemDefault = true
                )
            )
        }
    }

    /**
     * Update existing categories to have proper iconIds (for migration)
     */
    suspend fun updateCategoryIconIds() {
        val categories = financeDao.getAllCategoriesList()
        categories.forEach { category ->
            if (category.iconId == 0 && category.isSystemDefault) {
                val newIconId = com.example.financetracker.core.util.CategoryIconUtil.getDefaultIconIdForCategory(category.name)
                if (newIconId > 0) {
                    financeDao.updateCategoryIconId(category.id, newIconId)
                }
            }
        }
    }

    override suspend fun clearAllData() {
        database.withTransaction {
            // Clear notifications first (no foreign key dependencies)
            notificationDao.deleteAllNotifications()

            // Clear group-related data (respecting foreign key order)
            groupDao.deleteAllSettlements()
            groupDao.deleteAllGroupContributions()
            groupDao.deleteAllGroupSplits()
            groupDao.deleteAllGroupTransactions()
            groupDao.deleteAllGroupMembers()
            groupDao.deleteAllGroups()

            // Clear main finance data
            financeDao.deleteAllTransactions()
            financeDao.deleteAllSubscriptions()
            financeDao.deleteAllBudgets()
            financeDao.deleteAllPeople()
            financeDao.deleteAllWallets()

            // Keep categories - they will be re-seeded on app restart
        }
    }
}
