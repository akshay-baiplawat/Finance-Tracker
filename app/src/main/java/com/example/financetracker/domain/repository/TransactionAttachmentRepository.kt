package com.example.financetracker.domain.repository

import android.net.Uri
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import kotlinx.coroutines.flow.Flow

interface TransactionAttachmentRepository {

    fun getAttachmentsForTransaction(transactionId: Long): Flow<List<TransactionAttachmentEntity>>

    suspend fun getAttachmentsForTransactionSync(transactionId: Long): List<TransactionAttachmentEntity>

    fun getAttachmentCountsMap(): Flow<Map<Long, Int>>

    suspend fun addAttachment(transactionId: Long, uri: Uri): Long?

    suspend fun addAttachments(transactionId: Long, uris: List<Uri>): List<Long>

    suspend fun deleteAttachment(attachment: TransactionAttachmentEntity)

    suspend fun deleteAttachmentById(attachmentId: Long)

    suspend fun deleteAllAttachmentsForTransaction(transactionId: Long)

    suspend fun getAttachmentCount(transactionId: Long): Int

    suspend fun canAddMoreAttachments(transactionId: Long): Boolean

    suspend fun getAttachmentById(attachmentId: Long): TransactionAttachmentEntity?
}
