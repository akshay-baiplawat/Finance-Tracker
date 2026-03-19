package com.example.financetracker.data.repository

import android.content.Context
import android.net.Uri
import com.example.financetracker.core.util.AppLogger
import com.example.financetracker.core.util.TransactionAttachmentUtil
import com.example.financetracker.data.local.dao.TransactionAttachmentDao
import com.example.financetracker.data.local.entity.TransactionAttachmentEntity
import com.example.financetracker.domain.repository.TransactionAttachmentRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionAttachmentRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val transactionAttachmentDao: TransactionAttachmentDao
) : TransactionAttachmentRepository {

    override fun getAttachmentsForTransaction(transactionId: Long): Flow<List<TransactionAttachmentEntity>> {
        return transactionAttachmentDao.getAttachmentsForTransaction(transactionId)
    }

    override suspend fun getAttachmentsForTransactionSync(transactionId: Long): List<TransactionAttachmentEntity> {
        return transactionAttachmentDao.getAttachmentsForTransactionSync(transactionId)
    }

    override fun getAttachmentCountsMap(): Flow<Map<Long, Int>> {
        return transactionAttachmentDao.getAttachmentCounts().map { counts ->
            counts.associate { it.transactionId to it.count }
        }
    }

    override suspend fun addAttachment(transactionId: Long, uri: Uri): Long? {
        val currentCount = transactionAttachmentDao.getAttachmentCount(transactionId)
        if (!TransactionAttachmentUtil.canAddMoreAttachments(currentCount)) {
            AppLogger.w("TransactionAttachmentRepository", "Cannot add more attachments. Max limit reached.")
            return null
        }

        val fileInfo = TransactionAttachmentUtil.copyFileToInternalStorage(
            context = context,
            uri = uri,
            transactionId = transactionId,
            index = currentCount
        ) ?: return null

        val attachment = TransactionAttachmentEntity(
            transactionId = transactionId,
            filePath = fileInfo.filePath,
            fileName = fileInfo.fileName,
            mimeType = fileInfo.mimeType,
            createdTimestamp = System.currentTimeMillis(),
            orderIndex = currentCount
        )

        return transactionAttachmentDao.insertAttachment(attachment)
    }

    override suspend fun addAttachments(transactionId: Long, uris: List<Uri>): List<Long> {
        val currentCount = transactionAttachmentDao.getAttachmentCount(transactionId)
        val maxToAdd = TransactionAttachmentUtil.getMaxAttachments() - currentCount

        if (maxToAdd <= 0) {
            AppLogger.w("TransactionAttachmentRepository", "Cannot add more attachments. Max limit reached.")
            return emptyList()
        }

        val urisToAdd = uris.take(maxToAdd)
        val insertedIds = mutableListOf<Long>()

        urisToAdd.forEachIndexed { index, uri ->
            val fileInfo = TransactionAttachmentUtil.copyFileToInternalStorage(
                context = context,
                uri = uri,
                transactionId = transactionId,
                index = currentCount + index
            )

            if (fileInfo != null) {
                val attachment = TransactionAttachmentEntity(
                    transactionId = transactionId,
                    filePath = fileInfo.filePath,
                    fileName = fileInfo.fileName,
                    mimeType = fileInfo.mimeType,
                    createdTimestamp = System.currentTimeMillis(),
                    orderIndex = currentCount + index
                )
                val id = transactionAttachmentDao.insertAttachment(attachment)
                insertedIds.add(id)
            }
        }

        return insertedIds
    }

    override suspend fun deleteAttachment(attachment: TransactionAttachmentEntity) {
        TransactionAttachmentUtil.deleteFile(attachment.filePath)
        transactionAttachmentDao.deleteAttachment(attachment)
    }

    override suspend fun deleteAttachmentById(attachmentId: Long) {
        val attachment = transactionAttachmentDao.getAttachmentById(attachmentId)
        if (attachment != null) {
            TransactionAttachmentUtil.deleteFile(attachment.filePath)
            transactionAttachmentDao.deleteAttachmentById(attachmentId)
        }
    }

    override suspend fun deleteAllAttachmentsForTransaction(transactionId: Long) {
        val attachments = transactionAttachmentDao.getAttachmentsForTransactionSync(transactionId)
        attachments.forEach { attachment ->
            TransactionAttachmentUtil.deleteFile(attachment.filePath)
        }
        transactionAttachmentDao.deleteAllAttachmentsForTransaction(transactionId)
    }

    override suspend fun getAttachmentCount(transactionId: Long): Int {
        return transactionAttachmentDao.getAttachmentCount(transactionId)
    }

    override suspend fun canAddMoreAttachments(transactionId: Long): Boolean {
        val currentCount = transactionAttachmentDao.getAttachmentCount(transactionId)
        return TransactionAttachmentUtil.canAddMoreAttachments(currentCount)
    }

    override suspend fun getAttachmentById(attachmentId: Long): TransactionAttachmentEntity? {
        return transactionAttachmentDao.getAttachmentById(attachmentId)
    }
}
