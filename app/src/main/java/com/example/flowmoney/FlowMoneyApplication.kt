package com.example.flowmoney

import android.app.Application
import com.example.flowmoney.database.AppDatabase
import com.example.flowmoney.database.repository.AccountRepository
import com.example.flowmoney.database.repository.CategoryRepository
import com.example.flowmoney.database.repository.TransactionRepository
import com.example.flowmoney.database.sync.DataSyncManager
import com.example.flowmoney.notification.NotificationHelper
import com.google.firebase.auth.FirebaseAuth
import com.onesignal.OneSignal
import com.onesignal.debug.LogLevel
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
        const val ONESIGNAL_APP_ID = "3b8ae4d1-f9ed-42e2-b874-7be04269dda2"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize OneSignal
        initOneSignal()
        
        // Initialize notification helper
        notificationHelper = NotificationHelper(this)
        
        // Initialize data for currently logged in user
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            applicationScope.launch(Dispatchers.IO) {
                dataSyncManager.fetchAllData(currentUser.uid)
            }
        }
    }
    
    private fun initOneSignal() {
        // Enable verbose logging for debugging (remove in production)
        OneSignal.Debug.logLevel = LogLevel.VERBOSE
        
        // Initialize with OneSignal App ID
        OneSignal.initWithContext(this, ONESIGNAL_APP_ID)
        
        // Request notification permission
        applicationScope.launch(Dispatchers.IO) {
            OneSignal.Notifications.requestPermission(true)
        }
    }
} 