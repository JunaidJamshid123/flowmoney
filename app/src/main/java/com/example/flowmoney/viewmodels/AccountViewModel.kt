package com.example.flowmoney.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.flowmoney.FlowMoneyApplication
import com.example.flowmoney.Models.Account
import com.example.flowmoney.database.repository.AccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for handling account data, both online and offline
 */
class AccountViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: AccountRepository
    
    init {
        repository = (application as FlowMoneyApplication).accountRepository
    }
    
    /**
     * Get all accounts for the current user
     */
    fun getAllAccounts(userId: String): LiveData<List<Account>> {
        return repository.getAllAccounts(userId).asLiveData(Dispatchers.IO)
    }
    
    /**
     * Get accounts of a specific type
     */
    fun getAccountsByType(userId: String, accountType: String): LiveData<List<Account>> {
        return repository.getAccountsByType(userId, accountType).asLiveData(Dispatchers.IO)
    }
    
    /**
     * Get a single account by ID
     */
    suspend fun getAccountById(accountId: String): Account? {
        return repository.getAccountById(accountId)
    }
    
    /**
     * Create a new account
     */
    fun createAccount(
        userId: String,
        accountName: String,
        balance: Double,
        accountType: String,
        accountImageUrl: String? = null,
        note: String? = null,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val accountId = repository.createAccount(
                userId, accountName, balance, accountType, accountImageUrl, note
            )
            onComplete(accountId)
        }
    }
    
    /**
     * Update an existing account
     */
    fun updateAccount(account: Account, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAccount(account)
            onComplete()
        }
    }
    
    /**
     * Update account balance
     */
    fun updateAccountBalance(accountId: String, amount: Double, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateAccountBalance(accountId, amount)
            onComplete()
        }
    }
} 