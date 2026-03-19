package com.example.financetracker.di

import com.example.financetracker.data.repository.FinanceRepositoryImpl
import com.example.financetracker.data.repository.GroupRepositoryImpl
import com.example.financetracker.data.repository.TransactionAttachmentRepositoryImpl
import com.example.financetracker.domain.repository.FinanceRepository
import com.example.financetracker.domain.repository.GroupRepository
import com.example.financetracker.domain.repository.TransactionAttachmentRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    abstract fun bindFinanceRepository(
        financeRepositoryImpl: FinanceRepositoryImpl
    ): FinanceRepository

    @Binds
    abstract fun bindTransactionRepository(
        transactionRepositoryImpl: com.example.financetracker.data.repository.TransactionRepositoryImpl
    ): com.example.financetracker.domain.repository.TransactionRepository

    @Binds
    abstract fun bindUserPreferencesRepository(
        userPreferencesRepositoryImpl: com.example.financetracker.data.repository.UserPreferencesRepositoryImpl
    ): com.example.financetracker.domain.repository.UserPreferencesRepository

    @Binds
    abstract fun bindNotificationRepository(
        notificationRepositoryImpl: com.example.financetracker.data.repository.NotificationRepositoryImpl
    ): com.example.financetracker.domain.repository.NotificationRepository

    @Binds
    abstract fun bindGroupRepository(
        groupRepositoryImpl: GroupRepositoryImpl
    ): GroupRepository

    @Binds
    abstract fun bindTransactionAttachmentRepository(
        transactionAttachmentRepositoryImpl: TransactionAttachmentRepositoryImpl
    ): TransactionAttachmentRepository
}