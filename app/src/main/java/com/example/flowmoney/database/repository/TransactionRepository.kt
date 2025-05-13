package com.example.flowmoney.database.repository

import android.content.Context
import android.util.Log
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.database.LocalDataCache
import com.example.flowmoney.database.dao.TransactionDao
import com.example.flowmoney.database.entities.TransactionEntity
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
     * Create a new transaction locally and update account balance
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
            // Update account balance in Firestore
            val accountRef = firestore.collection("accounts").document(accountId)
            firestore.runTransaction { transaction ->
                val account = transaction.get(accountRef)
                val currentBalance = account.getDouble("balance") ?: 0.0
                
                // Calculate new balance based on transaction type
                val newBalance = when (type) {
                    "income" -> currentBalance + amount
                    "expense", "saving" -> currentBalance - amount
                    else -> currentBalance
                }
                
                // Update account balance
                transaction.update(accountRef, "balance", newBalance)
            }.await()
            
            // Save transaction locally
            transactionDao.insert(entity)
            
            Log.d(TAG, "Created new transaction and updated account balance: $transactionId")
            return transactionId
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating transaction or updating balance", e)
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
     * Sync local changes to Firestore
     */
    suspend fun syncUnSyncedTransactions() {
        val unSyncedTransactions = transactionDao.getUnSyncedTransactions()
        
        Log.d(TAG, "Syncing ${unSyncedTransactions.size} unsynced transactions to Firestore")
        
        for (transaction in unSyncedTransactions) {
            try {
                if (transaction.isDeleted) {
                    // Delete from Firestore
                    transactionsCollection.document(transaction.transactionId).delete().await()
                } else {
                    // Create or update in Firestore
                    val firestoreModel = transaction.toFirestoreModel()
                    transactionsCollection.document(transaction.transactionId)
                        .set(firestoreModel.toMap()).await()
                }
                
                // Mark as synced locally
                transactionDao.markAsSynced(transaction.transactionId)
                Log.d(TAG, "Successfully synced transaction ${transaction.transactionId}")
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing transaction ${transaction.transactionId}", e)
                // Continue with next transaction
            }
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