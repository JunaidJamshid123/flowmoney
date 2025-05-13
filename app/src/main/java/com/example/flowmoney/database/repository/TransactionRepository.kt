package com.example.flowmoney.database.repository

import android.content.Context
import android.util.Log
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.database.LocalDataCache
import com.example.flowmoney.database.dao.TransactionDao
import com.example.flowmoney.database.entities.TransactionEntity
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

/**
 * Repository for handling transaction data from both local database and Firestore
 */
class TransactionRepository(
    private val transactionDao: TransactionDao,
    private val context: Context? = null
) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val transactionsCollection = firestore.collection("transactions")
    private val localDataCache = context?.let { LocalDataCache.getInstance(it) }
    private val TAG = "TransactionRepository"
    
    // Local operations
    
    /**
     * Get all transactions for a user from local database
     */
    fun getAllTransactions(userId: String): Flow<List<Transaction>> {
        return transactionDao.getAllTransactions(userId).map { entities ->
            entities.filter { !it.isDeleted }.map { it.toFirestoreModel() }
        }
    }
    
    /**
     * Get all transactions for a specific account from local database
     */
    fun getTransactionsByAccount(userId: String, accountId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByAccount(userId, accountId).map { entities ->
            entities.map { it.toFirestoreModel() }
        }
    }
    
    /**
     * Get all transactions for a specific category from local database
     */
    fun getTransactionsByCategory(userId: String, categoryId: String): Flow<List<Transaction>> {
        return transactionDao.getTransactionsByCategory(userId, categoryId).map { entities ->
            entities.map { it.toFirestoreModel() }
        }
    }
    
    /**
     * Get a single transaction by ID from local database
     */
    suspend fun getTransactionById(transactionId: String): Transaction? {
        return transactionDao.getTransactionById(transactionId)?.toFirestoreModel()
    }
    
    /**
     * Create a new transaction (works both online and offline)
     */
    suspend fun createTransaction(
        userId: String,
        accountId: String,
        categoryId: String,
        type: String,
        amount: Double,
        date: Date,
        notes: String = "",
        invoiceBase64: String? = null
    ): String {
        val transactionId = UUID.randomUUID().toString()
        
        // Create transaction entity
        val entity = TransactionEntity.createLocalTransaction(
            transactionId = transactionId,
            userId = userId,
            accountId = accountId,
            categoryId = categoryId,
            type = type,
            amount = amount,
            date = date,
            notes = notes,
            invoiceBase64 = invoiceBase64
        )

        try {
            // First save locally
            transactionDao.insert(entity)
            
            try {
                // Try to update Firestore if online
                val transactionData = mapOf(
                    "transaction_id" to transactionId,
                    "user_id" to userId,
                    "account_id" to accountId,
                    "category_id" to categoryId,
                    "type" to type,
                    "amount" to amount,
                    "date" to Timestamp(date),
                    "created_at" to Timestamp.now(),
                    "updated_at" to Timestamp.now(),
                    "notes" to notes,
                    "invoice_base64" to invoiceBase64,
                    "is_deleted" to false
                )

                // Update Firestore and account balance
                firestore.runTransaction { transaction ->
                    // Update account balance
                    val accountRef = firestore.collection("accounts").document(accountId)
                    val account = transaction.get(accountRef)
                    val currentBalance = account.getDouble("balance") ?: 0.0
                    val newBalance = when (type) {
                        "income" -> currentBalance + amount
                        "expense", "saving" -> currentBalance - amount
                        else -> currentBalance
                    }
                    
                    // Save transaction and update account
                    transaction.set(transactionsCollection.document(transactionId), transactionData)
                    transaction.update(accountRef, "balance", newBalance)
                }.await()

                // Mark as synced in local DB
                transactionDao.markAsSynced(transactionId)
                Log.d(TAG, "Transaction synced with Firestore: $transactionId")
            } catch (e: Exception) {
                // If Firestore update fails, keep it marked for sync later
                Log.e(TAG, "Failed to sync transaction with Firestore: $transactionId", e)
            }

            return transactionId
        } catch (e: Exception) {
            Log.e(TAG, "Error creating transaction", e)
            throw e
        }
    }
    
    /**
     * Save a transaction to local database
     */
    suspend fun saveTransaction(transaction: Transaction) {
        val entity = TransactionEntity.fromFirestoreModel(transaction, false)
        transactionDao.insert(entity)
    }
    
    /**
     * Update an existing transaction and adjust account balance
     */
    suspend fun updateTransaction(transaction: Transaction) {
        try {
            // Get the existing transaction
            val existingTransaction = transactionDao.getTransactionById(transaction.transactionId)
                ?: throw Exception("Transaction not found")
            
            // Update account balance if amount or type changed
            if (existingTransaction.amount != transaction.amount || 
                existingTransaction.type != transaction.type ||
                existingTransaction.accountId != transaction.accountId) {
                
                // Revert old transaction effect
                val oldAccountRef = firestore.collection("accounts").document(existingTransaction.accountId)
                firestore.runTransaction { t ->
                    val oldAccount = t.get(oldAccountRef)
                    val oldBalance = oldAccount.getDouble("balance") ?: 0.0
                    val revertedBalance = when (existingTransaction.type) {
                        "income" -> oldBalance - existingTransaction.amount
                        "expense", "saving" -> oldBalance + existingTransaction.amount
                        else -> oldBalance
                    }
                    t.update(oldAccountRef, "balance", revertedBalance)
                }.await()
                
                // Apply new transaction effect if account is different
                if (existingTransaction.accountId != transaction.accountId) {
                    val newAccountRef = firestore.collection("accounts").document(transaction.accountId)
                    firestore.runTransaction { t ->
                        val newAccount = t.get(newAccountRef)
                        val currentBalance = newAccount.getDouble("balance") ?: 0.0
                        val newBalance = when (transaction.type) {
                            "income" -> currentBalance + transaction.amount
                            "expense", "saving" -> currentBalance - transaction.amount
                            else -> currentBalance
                        }
                        t.update(newAccountRef, "balance", newBalance)
                    }.await()
                }
            }
            
            // Update transaction in local database
            val updatedEntity = TransactionEntity.fromFirestoreModel(transaction, false)
            transactionDao.update(updatedEntity)
            
            Log.d(TAG, "Updated transaction and adjusted account balance: ${transaction.transactionId}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error updating transaction or adjusting balance", e)
            throw e
        }
    }
    
    /**
     * Delete a transaction (mark as deleted) in local database
     */
    suspend fun deleteTransaction(transactionId: String) {
        transactionDao.markAsDeleted(transactionId)
    }
    
    // Firebase operations
    
    /**
     * Fetch transactions from Firestore and store locally
     */
    suspend fun fetchAndStoreTransactions(userId: String) {
        try {
            val querySnapshot = transactionsCollection
                .whereEqualTo("user_id", userId)
                .get()
                .await()
            
            val transactions = querySnapshot.documents.mapNotNull { document ->
                val data = document.data
                if (data != null) {
                    Transaction.fromMap(data, document.id)
                } else null
            }
            
            Log.d(TAG, "Fetched ${transactions.size} transactions from Firestore")
            
            val entities = transactions.map { TransactionEntity.fromFirestoreModel(it, true) }
            transactionDao.insertAll(entities)
            
            // Cache transactions locally
            localDataCache?.cacheTransactions(userId, transactions)
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching transactions", e)
            
            // If fetch fails, try to load from local cache as fallback
            val cachedTransactions = localDataCache?.getCachedTransactions(userId)
            if (!cachedTransactions.isNullOrEmpty()) {
                Log.d(TAG, "Using ${cachedTransactions.size} cached transactions as fallback")
                val entities = cachedTransactions.map { TransactionEntity.fromFirestoreModel(it, false) }
                transactionDao.insertAll(entities)
            }
        }
    }
    
    /**
     * Sync all unsynced transactions with Firestore
     */
    suspend fun syncUnSyncedTransactions() {
        try {
            val unSyncedTransactions = transactionDao.getUnSyncedTransactions()
            Log.d(TAG, "Found ${unSyncedTransactions.size} unsynced transactions")

            for (transaction in unSyncedTransactions) {
                try {
                    val transactionData = mapOf(
                        "transaction_id" to transaction.transactionId,
                        "user_id" to transaction.userId,
                        "account_id" to transaction.accountId,
                        "category_id" to transaction.categoryId,
                        "type" to transaction.type,
                        "amount" to transaction.amount,
                        "date" to Timestamp(transaction.date),
                        "created_at" to Timestamp(transaction.createdAt),
                        "updated_at" to Timestamp.now(),
                        "notes" to transaction.notes,
                        "invoice_base64" to transaction.invoiceBase64,
                        "is_deleted" to transaction.isDeleted
                    )

                    // Update Firestore and account balance
                    firestore.runTransaction { firestoreTransaction ->
                        // Update account balance
                        val accountRef = firestore.collection("accounts").document(transaction.accountId)
                        val account = firestoreTransaction.get(accountRef)
                        val currentBalance = account.getDouble("balance") ?: 0.0
                        val newBalance = when (transaction.type) {
                            "income" -> currentBalance + transaction.amount
                            "expense", "saving" -> currentBalance - transaction.amount
                            else -> currentBalance
                        }
                        
                        // Save transaction and update account
                        firestoreTransaction.set(transactionsCollection.document(transaction.transactionId), transactionData)
                        firestoreTransaction.update(accountRef, "balance", newBalance)
                    }.await()

                    // Mark as synced in local DB
                    transactionDao.markAsSynced(transaction.transactionId)
                    Log.d(TAG, "Successfully synced transaction: ${transaction.transactionId}")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to sync transaction ${transaction.transactionId}", e)
                    // Continue with next transaction
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing unsynced transactions", e)
            throw e
        }
    }
    
    /**
     * Sync transactions from Firestore to local database
     */
    suspend fun syncTransactionsFromFirestore(userId: String) {
        try {
            // Get transactions from Firestore
            val snapshot = transactionsCollection
                .whereEqualTo("user_id", userId)
                .whereEqualTo("is_deleted", false)
                .get()
                .await()
            
            // Convert to Transaction objects and batch insert
            val transactions = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Transaction.fromMap(data, doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing transaction ${doc.id}", e)
                    null
                }
            }
            
            // Batch insert all transactions
            val entities = transactions.map { transaction ->
                TransactionEntity.fromFirestoreModel(transaction, true)
            }
            
            // Use transaction to ensure atomic operation
            transactionDao.deleteAllTransactions(userId) // Clear old data
            transactionDao.insertAll(entities) // Insert new data
            
            // Cache transactions for offline access
            localDataCache?.cacheTransactions(userId, transactions)
            
            Log.d(TAG, "Successfully synced ${transactions.size} transactions from Firestore")
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing transactions from Firestore", e)
            // Don't throw the exception - let the app continue with local data
        }
    }
} 