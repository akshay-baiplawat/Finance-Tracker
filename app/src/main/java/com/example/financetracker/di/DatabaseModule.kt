package com.example.financetracker.di

import android.app.Application
import androidx.room.Room
import com.example.financetracker.data.local.AppDatabase
import com.example.financetracker.data.local.FinanceDao
import com.example.financetracker.data.local.dao.GroupDao
import com.example.financetracker.data.local.dao.MerchantCategoryPreferenceDao
import com.example.financetracker.data.local.dao.TransactionAttachmentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        application: Application,
        callback: AppDatabase.Callback
    ): AppDatabase {
        return Room.databaseBuilder(application, AppDatabase::class.java, "finance_tracker.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .addCallback(callback)
            .build()
    }

    @Provides
    fun provideFinanceDao(db: AppDatabase): FinanceDao {
        return db.financeDao()
    }

    @Provides
    fun provideNotificationDao(db: AppDatabase): com.example.financetracker.data.local.dao.NotificationDao {
        return db.notificationDao()
    }

    @Provides
    fun provideGroupDao(db: AppDatabase): GroupDao {
        return db.groupDao()
    }

    @Provides
    fun provideTransactionAttachmentDao(db: AppDatabase): TransactionAttachmentDao {
        return db.transactionAttachmentDao()
    }

    @Provides
    fun provideMerchantCategoryPreferenceDao(db: AppDatabase): MerchantCategoryPreferenceDao {
        return db.merchantCategoryPreferenceDao()
    }
}