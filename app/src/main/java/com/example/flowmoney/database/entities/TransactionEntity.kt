package com.example.flowmoney.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.database.converters.DateConverters
import com.google.firebase.Timestamp
import java.util.Date

/**
 * Room entity representing a transaction in the local SQLite database
 */
@Entity(tableName = "transactions")
@TypeConverters(DateConverters::class)
data class TransactionEntity(
    @PrimaryKey
    val transactionId: String,
    
    // Foreign keys
    val userId: String,
    val accountId: String,
    val categoryId: String,
    
    // Transaction metadata
    val type: String,
    val amount: Double,
    val date: Date,
    val createdAt: Date,
    val updatedAt: Date?,
    
    // Additional details
    val notes: String,
    val invoiceUrl: String?,
    val invoiceBase64: String?,
    val location: String?,
    
    // States
    val isRecurring: Boolean,
    val recurringId: String?,
    val isDeleted: Boolean,
    
    // Sync status
    val isSynced: Boolean,
    val needsSync: Boolean
) {
    /**
     * Convert local entity to Firestore model
     */
    fun toFirestoreModel(): Transaction {
        return Transaction(
            transactionId = transactionId,
            userId = userId,
            accountId = accountId,
            categoryId = categoryId,
            type = type,
            amount = amount,
            date = Timestamp(date),
            createdAt = Timestamp(createdAt),
            updatedAt = updatedAt?.let { Timestamp(it) },
            notes = notes,
            invoiceUrl = invoiceUrl,
            invoiceBase64 = invoiceBase64,
            tags = listOf(), // Tags not implemented in local storage yet
            location = location,
            isRecurring = isRecurring,
            recurringId = recurringId,
            isDeleted = isDeleted
        )
    }
    
    companion object {
        /**
         * Convert Firestore model to local entity
         */
        fun fromFirestoreModel(transaction: Transaction, isSynced: Boolean = true): TransactionEntity {
            return TransactionEntity(
                transactionId = transaction.transactionId,
                userId = transaction.userId,
                accountId = transaction.accountId,
                categoryId = transaction.categoryId,
                type = transaction.type,
                amount = transaction.amount,
                date = transaction.date.toDate(),
                createdAt = transaction.createdAt.toDate(),
                updatedAt = transaction.updatedAt?.toDate(),
                notes = transaction.notes,
                invoiceUrl = transaction.invoiceUrl,
                invoiceBase64 = transaction.invoiceBase64,
                location = transaction.location,
                isRecurring = transaction.isRecurring,
                recurringId = transaction.recurringId,
                isDeleted = transaction.isDeleted,
                isSynced = isSynced,
                needsSync = false
            )
        }
        
        /**
         * Create a new local transaction (not yet synced)
         */
        fun createLocalTransaction(
            transactionId: String,
            userId: String,
            accountId: String,
            categoryId: String,
            type: String,
            amount: Double,
            date: Date = Date(),
            notes: String = "",
            invoiceBase64: String? = null
        ): TransactionEntity {
            val now = Date()
            return TransactionEntity(
                transactionId = transactionId,
                userId = userId,
                accountId = accountId,
                categoryId = categoryId,
                type = type,
                amount = amount,
                date = date,
                createdAt = now,
                updatedAt = null,
                notes = notes,
                invoiceUrl = null,
                invoiceBase64 = invoiceBase64,
                location = null,
                isRecurring = false,
                recurringId = null,
                isDeleted = false,
                isSynced = false,
                needsSync = true
            )
        }
    }
} 