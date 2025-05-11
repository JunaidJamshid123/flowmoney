package com.example.flowmoney.database.repository

import com.example.flowmoney.Models.Account
import com.example.flowmoney.database.dao.AccountDao
import com.example.flowmoney.database.entities.AccountEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

/**
 * Repository for handling account data from both local database and Firestore
 */
class AccountRepository(private val accountDao: AccountDao) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val accountsCollection = firestore.collection("accounts")
    
    // Local operations
    
    /**
     * Get all accounts for a user from local database
     */
    fun getAllAccounts(userId: String): Flow<List<Account>> {
        return accountDao.getAllAccounts(userId).map { entities ->
            entities.map { it.toFirestoreModel() }
        }
    }
    
    /**
     * Get accounts of a specific type from local database
     */
    fun getAccountsByType(userId: String, accountType: String): Flow<List<Account>> {
        return accountDao.getAccountsByType(userId, accountType).map { entities ->
            entities.map { it.toFirestoreModel() }
        }
    }
    
    /**
     * Get a single account by ID from local database
     */
    suspend fun getAccountById(accountId: String): Account? {
        return accountDao.getAccountById(accountId)?.toFirestoreModel()
    }
    
    /**
     * Create a new account locally
     */
    suspend fun createAccount(
        userId: String,
        accountName: String,
        balance: Double,
        accountType: String,
        accountImageUrl: String? = null,
        note: String? = null
    ): String {
        val accountId = UUID.randomUUID().toString()
        val entity = AccountEntity.createLocalAccount(
            accountId = accountId,
            userId = userId,
            accountName = accountName,
            balance = balance,
            accountType = accountType,
            accountImageUrl = accountImageUrl,
            note = note
        )
        accountDao.insert(entity)
        return accountId
    }
    
    /**
     * Save an account to local database
     */
    suspend fun saveAccount(account: Account) {
        val entity = AccountEntity.fromFirestoreModel(account, false)
        accountDao.insert(entity)
    }
    
    /**
     * Update an existing account in local database
     */
    suspend fun updateAccount(account: Account) {
        val existingEntity = accountDao.getAccountById(account.accountId)
        if (existingEntity != null) {
            val updatedEntity = AccountEntity.fromFirestoreModel(account, false)
            accountDao.update(updatedEntity)
        }
    }
    
    /**
     * Update account balance
     */
    suspend fun updateAccountBalance(accountId: String, amount: Double) {
        accountDao.updateBalance(accountId, amount, Date().time)
    }
    
    // Firebase operations
    
    /**
     * Fetch accounts from Firestore and store locally
     */
    suspend fun fetchAndStoreAccounts(userId: String) {
        try {
            val querySnapshot = accountsCollection
                .whereEqualTo("user_id", userId)
                .get()
                .await()
            
            val accounts = querySnapshot.documents.mapNotNull { document ->
                val account = Account()
                document.data?.let { data ->
                    account.accountId = document.id
                    account.userId = data["user_id"] as? String ?: ""
                    account.accountName = data["account_name"] as? String ?: ""
                    account.balance = (data["balance"] as? Number)?.toDouble() ?: 0.0
                    account.accountType = data["account_type"] as? String ?: ""
                    account.accountImageUrl = data["account_image_url"] as? String
                    account.note = data["note"] as? String
                    account.createdAt = (data["created_at"] as? Long) ?: System.currentTimeMillis()
                    account.updatedAt = (data["updated_at"] as? Long) ?: System.currentTimeMillis()
                    account
                }
            }
            
            val entities = accounts.map { AccountEntity.fromFirestoreModel(it, true) }
            accountDao.insertAll(entities)
        } catch (e: Exception) {
            // Handle failure gracefully, log error, etc.
        }
    }
    
    /**
     * Sync local changes to Firestore
     */
    suspend fun syncUnSyncedAccounts() {
        val unSyncedAccounts = accountDao.getUnSyncedAccounts()
        
        for (account in unSyncedAccounts) {
            try {
                // Create or update in Firestore
                val firestoreModel = account.toFirestoreModel()
                accountsCollection.document(account.accountId)
                    .set(firestoreModel.toMap()).await()
                
                // Mark as synced locally
                accountDao.markAsSynced(account.accountId)
            } catch (e: Exception) {
                // Handle failure gracefully, log error, retry later, etc.
            }
        }
    }
} 