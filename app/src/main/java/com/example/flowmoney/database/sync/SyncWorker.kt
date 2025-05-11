package com.example.flowmoney.database.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.flowmoney.database.AppDatabase
import com.example.flowmoney.database.repository.AccountRepository
import com.example.flowmoney.database.repository.CategoryRepository
import com.example.flowmoney.database.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker class for syncing data in the background using WorkManager
 */
class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            // Get database instance
            val database = AppDatabase.getDatabase(applicationContext)
            
            // Create repositories
            val transactionRepository = TransactionRepository(database.transactionDao())
            val categoryRepository = CategoryRepository(database.categoryDao())
            val accountRepository = AccountRepository(database.accountDao())
            
            // Sync data to server
            accountRepository.syncUnSyncedAccounts()
            categoryRepository.syncUnSyncedCategories()
            transactionRepository.syncUnSyncedTransactions()
            
            // Return success
            Result.success()
        } catch (e: Exception) {
            // Return retry if there was an error
            Result.retry()
        }
    }
} 