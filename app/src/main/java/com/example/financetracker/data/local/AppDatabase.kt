package com.example.financetracker.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.financetracker.data.local.dao.GroupDao
import com.example.financetracker.data.local.dao.MerchantCategoryPreferenceDao
import com.example.financetracker.data.local.dao.TransactionAttachmentDao
import com.example.financetracker.data.local.entity.BudgetEntity
import com.example.financetracker.data.local.entity.GroupEntity
import com.example.financetracker.data.local.entity.GroupMemberEntity
import com.example.financetracker.data.local.entity.GroupTransactionEntity
import com.example.financetracker.data.local.entity.GroupSplitEntity
import com.example.financetracker.data.local.entity.GroupContributionEntity
import com.example.financetracker.data.local.entity.MerchantCategoryPreferenceEntity
import com.example.financetracker.data.local.entity.SettlementEntity
import com.example.financetracker.data.local.entity.PersonEntity
import com.example.financetracker.data.local.entity.TransactionEntity
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import com.example.financetracker.data.local.entity.WalletEntity
import com.example.financetracker.data.local.entity.SubscriptionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Provider

@Database(
    entities = [
        WalletEntity::class,
        PersonEntity::class,
        TransactionEntity::class,
        TransactionAttachmentEntity::class,
        BudgetEntity::class,
        GroupEntity::class,
        GroupMemberEntity::class,
        GroupTransactionEntity::class,
        GroupSplitEntity::class,
        GroupContributionEntity::class,
        SettlementEntity::class,
        SubscriptionEntity::class,
        MerchantCategoryPreferenceEntity::class,
        com.example.financetracker.data.local.entity.CategoryEntity::class,
        com.example.financetracker.data.local.entity.NotificationEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun financeDao(): FinanceDao
    abstract fun notificationDao(): com.example.financetracker.data.local.dao.NotificationDao
    abstract fun groupDao(): GroupDao
    abstract fun transactionAttachmentDao(): TransactionAttachmentDao
    abstract fun merchantCategoryPreferenceDao(): MerchantCategoryPreferenceDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `transaction_attachments` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `transactionId` INTEGER NOT NULL,
                        `filePath` TEXT NOT NULL,
                        `fileName` TEXT NOT NULL,
                        `mimeType` TEXT NOT NULL,
                        `createdTimestamp` INTEGER NOT NULL,
                        `orderIndex` INTEGER NOT NULL DEFAULT 0,
                        FOREIGN KEY(`transactionId`) REFERENCES `transactions`(`id`) ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_transaction_attachments_transactionId` ON `transaction_attachments` (`transactionId`)")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Create merchant_category_preferences table for user learning
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `merchant_category_preferences` (
                        `merchantNormalized` TEXT NOT NULL PRIMARY KEY,
                        `categoryName` TEXT NOT NULL,
                        `lastUpdated` INTEGER NOT NULL,
                        `correctionCount` INTEGER NOT NULL DEFAULT 1
                    )
                    """.trimIndent()
                )
            }
        }
    }

    class Callback @Inject constructor(
        private val database: Provider<AppDatabase>,
        @com.example.financetracker.di.ApplicationScope private val applicationScope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)

            val dao = database.get().financeDao()
            applicationScope.launch(Dispatchers.IO) {
                populateDatabase(dao)
            }
        }

        private suspend fun populateDatabase(dao: FinanceDao) {
            // Seeding disabled requested by user
        }
    }
}