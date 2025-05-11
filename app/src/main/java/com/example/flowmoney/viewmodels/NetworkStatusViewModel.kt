package com.example.flowmoney.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.example.flowmoney.FlowMoneyApplication
import com.example.flowmoney.database.sync.DataSyncManager

/**
 * ViewModel to handle network status updates
 * This can be used to show online/offline indicator in the UI
 */
class NetworkStatusViewModel(application: Application) : AndroidViewModel(application) {
    
    private val dataSyncManager: DataSyncManager
    
    init {
        dataSyncManager = (application as FlowMoneyApplication).dataSyncManager
    }
    
    /**
     * Returns a LiveData that indicates whether the device is online or offline
     */
    fun getNetworkStatus(): LiveData<Boolean> {
        return dataSyncManager.isOnline
    }
    
    /**
     * Trigger a manual sync with the server
     */
    suspend fun syncWithServer() {
        dataSyncManager.syncDataToServer()
    }
    
    /**
     * Pull fresh data from the server
     */
    suspend fun refreshData(userId: String) {
        dataSyncManager.fetchAllData(userId)
    }
    
    override fun onCleared() {
        super.onCleared()
    }
} 