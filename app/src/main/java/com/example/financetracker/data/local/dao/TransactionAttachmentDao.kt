package com.example.financetracker.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionAttachmentDao {

    @Query("SELECT * FROM transaction_attachments WHERE transactionId = :transactionId ORDER BY orderIndex ASC")
    fun getAttachmentsForTransaction(transactionId: Long): Flow<List<TransactionAttachmentEntity>>

    @Query("SELECT * FROM transaction_attachments WHERE transactionId = :transactionId ORDER BY orderIndex ASC")
    suspend fun getAttachmentsForTransactionSync(transactionId: Long): List<TransactionAttachmentEntity>

    @Query("SELECT transactionId, COUNT(*) as count FROM transaction_attachments GROUP BY transactionId")
    fun getAttachmentCounts(): Flow<List<AttachmentCountTuple>>

    @Query("SELECT COUNT(*) FROM transaction_attachments WHERE transactionId = :transactionId")
    suspend fun getAttachmentCount(transactionId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachment(attachment: TransactionAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttachments(attachments: List<TransactionAttachmentEntity>)

    @Delete
    suspend fun deleteAttachment(attachment: TransactionAttachmentEntity)

    @Query("DELETE FROM transaction_attachments WHERE id = :attachmentId")
    suspend fun deleteAttachmentById(attachmentId: Long)

    @Query("DELETE FROM transaction_attachments WHERE transactionId = :transactionId")
    suspend fun deleteAllAttachmentsForTransaction(transactionId: Long)

    @Query("SELECT * FROM transaction_attachments WHERE id = :attachmentId")
    suspend fun getAttachmentById(attachmentId: Long): TransactionAttachmentEntity?
}

data class AttachmentCountTuple(
    val transactionId: Long,
    val count: Int
)
