package com.example.flowmoney

import android.app.Application
import com.example.flowmoney.database.AppDatabase
import com.example.flowmoney.database.repository.AccountRepository
import com.example.flowmoney.database.repository.CategoryRepository
import com.example.flowmoney.database.repository.TransactionRepository
import com.example.flowmoney.database.sync.DataSyncManager
import com.example.flowmoney.notification.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Main application class that initializes the offline support
 */
class FlowMoneyApplication : Application() {
    // Database and repositories
    private val database by lazy { AppDatabase.getDatabase(this) }
    
    val transactionRepository by lazy { TransactionRepository(database.transactionDao(), this) }
    val categoryRepository by lazy { CategoryRepository(database.categoryDao()) }
    val accountRepository by lazy { AccountRepository(database.accountDao()) }
    
    // Notification helper
    lateinit var notificationHelper: NotificationHelper
    
    // Sync manager
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    val dataSyncManager by lazy {
        DataSyncManager(
            this,
            transactionRepository,
            categoryRepository,
            accountRepository
        )
    }
    
    companion object {
        lateinit var instance: FlowMoneyApplication
            private set
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize notification helper
        notificationHelper = NotificationHelper(this)
        
        // Initialize data sync manager
        applicationScope.launch {
            // Initial sync when app starts
            FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
                dataSyncManager.fetchAllData(userId)
            }
        }
    }
} 