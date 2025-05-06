package com.example.flowmoney.Models

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import kotlinx.parcelize.Parcelize
import java.util.Date

/**
 * Transaction model for Firebase Firestore
 *
 * This model is designed to be easily stored and retrieved from Firebase Firestore
 * while maintaining all necessary transaction data.
 */
@Parcelize
data class Transaction(
    // Primary identifier - will be automatically set by Firestore
    @DocumentId
    val transactionId: String = "",

    // Foreign keys
    @PropertyName("user_id")
    val userId: String = "", // User who owns this transaction

    @PropertyName("account_id")
    val accountId: String = "", // Account used for the transaction

    @PropertyName("category_id")
    val categoryId: String = "", // Category of the transaction

    // Transaction metadata
    @PropertyName("type")
    val type: String = "expense", // Type of transaction (income, expense, saving, transfer)

    @PropertyName("amount")
    val amount: Double = 0.0, // Transaction amount

    @PropertyName("date")
    val date: Timestamp = Timestamp.now(), // Date of the transaction as Firestore Timestamp

    @PropertyName("created_at")
    val createdAt: Timestamp = Timestamp.now(), // Creation date

    @PropertyName("updated_at")
    val updatedAt: Timestamp? = null, // Last update date

    // Additional details
    @PropertyName("notes")
    val notes: String = "", // Additional notes/description

    @PropertyName("invoice_url")
    val invoiceUrl: String? = null, // URL to the stored invoice image in Firebase Storage

    @PropertyName("tags")
    val tags: List<String> = listOf(), // Optional tags for the transaction

    @PropertyName("location")
    val location: String? = null, // Optional location information

    // States
    @PropertyName("is_recurring")
    val isRecurring: Boolean = false, // Whether this is a recurring transaction

    @PropertyName("recurring_id")
    val recurringId: String? = null, // ID of the recurring transaction group if applicable

    @PropertyName("is_deleted")
    val isDeleted: Boolean = false // Soft deletion flag
): Parcelable {

    /**
     * Helper enum for transaction types
     * Note: This enum is excluded from Firestore and used only in app logic
     */

    enum class TransactionType {
        EXPENSE, INCOME, SAVING, TRANSFER;

        companion object {
            fun fromString(type: String): TransactionType {
                return when (type.lowercase()) {
                    "expense" -> EXPENSE
                    "income" -> INCOME
                    "saving" -> SAVING
                    "transfer" -> TRANSFER
                    else -> EXPENSE // Default to expense for unknown types
                }
            }
        }

        override fun toString(): String {
            return when (this) {
                EXPENSE -> "expense"
                INCOME -> "income"
                SAVING -> "saving"
                TRANSFER -> "transfer"
            }
        }
    }

    /**
     * Get the transaction type as enum
     */
    @Exclude
    fun getTransactionType(): TransactionType {
        return TransactionType.fromString(type)
    }

    /**
     * Check if this transaction increases the account balance
     */
    @Exclude
    fun increasesBalance(): Boolean {
        return type == "income"
    }

    /**
     * Check if this transaction decreases the account balance
     */
    @Exclude
    fun decreasesBalance(): Boolean {
        return type == "expense" || type == "saving"
    }

    /**
     * Get amount with sign (positive or negative)
     */
    @Exclude
    fun getSignedAmount(): Double {
        return when (type) {
            "expense" -> -amount
            "saving" -> -amount
            "transfer" -> -amount
            "income" -> amount
            else -> amount
        }
    }

    /**
     * Convert Firestore Timestamp to Date
     */
    @Exclude
    fun getDateAsDate(): Date {
        return date.toDate()
    }

    /**
     * Convert to a Map for Firestore
     */
    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "user_id" to userId,
            "account_id" to accountId,
            "category_id" to categoryId,
            "type" to type,
            "amount" to amount,
            "date" to date,
            "created_at" to createdAt,
            "updated_at" to updatedAt,
            "notes" to notes,
            "invoice_url" to invoiceUrl,
            "tags" to tags,
            "location" to location,
            "is_recurring" to isRecurring,
            "recurring_id" to recurringId,
            "is_deleted" to isDeleted
        )
    }

    companion object {
        /**
         * Create transaction from Firestore data
         */
        fun fromMap(data: Map<String, Any>, id: String): Transaction {
            return Transaction(
                transactionId = id,
                userId = data["user_id"] as? String ?: "",
                accountId = data["account_id"] as? String ?: "",
                categoryId = data["category_id"] as? String ?: "",
                type = data["type"] as? String ?: "expense",
                amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                date = data["date"] as? Timestamp ?: Timestamp.now(),
                createdAt = data["created_at"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updated_at"] as? Timestamp,
                notes = data["notes"] as? String ?: "",
                invoiceUrl = data["invoice_url"] as? String,
                tags = (data["tags"] as? List<*>)?.filterIsInstance<String>() ?: listOf(),
                location = data["location"] as? String,
                isRecurring = data["is_recurring"] as? Boolean ?: false,
                recurringId = data["recurring_id"] as? String,
                isDeleted = data["is_deleted"] as? Boolean ?: false
            )
        }

        /**
         * Create a new expense transaction
         */
        fun createExpense(
            userId: String,
            accountId: String,
            categoryId: String,
            amount: Double,
            date: Date = Date(),
            notes: String = "",
            invoiceUrl: String? = null
        ): Transaction {
            return Transaction(
                userId = userId,
                accountId = accountId,
                categoryId = categoryId,
                type = "expense",
                amount = amount,
                date = Timestamp(date),
                notes = notes,
                invoiceUrl = invoiceUrl
            )
        }

        /**
         * Create a new income transaction
         */
        fun createIncome(
            userId: String,
            accountId: String,
            categoryId: String,
            amount: Double,
            date: Date = Date(),
            notes: String = "",
            invoiceUrl: String? = null
        ): Transaction {
            return Transaction(
                userId = userId,
                accountId = accountId,
                categoryId = categoryId,
                type = "income",
                amount = amount,
                date = Timestamp(date),
                notes = notes,
                invoiceUrl = invoiceUrl
            )
        }

        /**
         * Create a new saving transaction
         */
        fun createSaving(
            userId: String,
            accountId: String,
            categoryId: String,
            amount: Double,
            date: Date = Date(),
            notes: String = "",
            invoiceUrl: String? = null
        ): Transaction {
            return Transaction(
                userId = userId,
                accountId = accountId,
                categoryId = categoryId,
                type = "saving",
                amount = amount,
                date = Timestamp(date),
                notes = notes,
                invoiceUrl = invoiceUrl
            )
        }

        /**
         * Create a new transfer transaction
         */
        fun createTransfer(
            userId: String,
            sourceAccountId: String,
            destinationCategoryId: String,
            amount: Double,
            date: Date = Date(),
            notes: String = ""
        ): Transaction {
            return Transaction(
                userId = userId,
                accountId = sourceAccountId,
                categoryId = destinationCategoryId,
                type = "transfer",
                amount = amount,
                date = Timestamp(date),
                notes = notes
            )
        }
    }
}