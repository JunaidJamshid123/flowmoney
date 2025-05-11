package com.example.flowmoney.database.sync

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.flowmoney.database.repository.AccountRepository
import com.example.flowmoney.database.repository.CategoryRepository
import com.example.flowmoney.database.repository.TransactionRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Manages data synchronization between local SQLite database and Firebase
 */
class DataSyncManager(
    private val context: Context,
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
    private val accountRepository: AccountRepository
) {
    private val _isOnline = MutableLiveData<Boolean>()
    val isOnline: LiveData<Boolean> = _isOnline
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _isOnline.postValue(true)
            
            // When network becomes available, trigger a sync
            CoroutineScope(Dispatchers.IO).launch {
                syncDataToServer()
            }
        }
        
        override fun onLost(network: Network) {
            _isOnline.postValue(false)
        }
    }
    
    init {
        // Initialize connectivity status
        updateNetworkStatus()
        
        // Register network callback
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Schedule periodic sync work
        schedulePeriodicSync()
    }
    
    /**
     * Update the current network status
     */
    private fun updateNetworkStatus() {
        val activeNetwork = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        val isConnected = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _isOnline.postValue(isConnected)
    }
    
    /**
     * Schedule periodic background sync using WorkManager
     */
    private fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES, // Sync every 15 minutes when connected
            5, TimeUnit.MINUTES // Flex period
        )
            .setConstraints(constraints)
            .build()
        
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "data_sync_work",
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            syncWorkRequest
        )
    }
    
    /**
     * Fetch all data from Firestore and store locally
     */
    suspend fun fetchAllData(userId: String) {
        withContext(Dispatchers.IO) {
            // Fetch in parallel
            launch { transactionRepository.fetchAndStoreTransactions(userId) }
            launch { categoryRepository.fetchAndStoreCategories(userId) }
            launch { accountRepository.fetchAndStoreAccounts(userId) }
        }
    }
    
    /**
     * Sync local changes to Firebase when online
     */
    suspend fun syncDataToServer() {
        if (_isOnline.value == true) {
            withContext(Dispatchers.IO) {
                try {
                    // Sync in order (accounts first, then categories, then transactions)
                    accountRepository.syncUnSyncedAccounts()
                    categoryRepository.syncUnSyncedCategories()
                    transactionRepository.syncUnSyncedTransactions()
                } catch (e: Exception) {
                    // Handle sync errors gracefully
                }
            }
        }
    }
    
    /**
     * Call this when the app is no longer using the sync manager
     */
    fun cleanup() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
    }
} 