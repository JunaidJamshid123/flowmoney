package com.example.flowmoney.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.flowmoney.FlowMoneyApplication
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.database.repository.TransactionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

/**
 * ViewModel for handling transaction data, both online and offline
 */
class TransactionViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: TransactionRepository
    
    init {
        repository = (application as FlowMoneyApplication).transactionRepository
    }
    
    /**
     * Get all transactions for the current user
     */
    fun getAllTransactions(userId: String): LiveData<List<Transaction>> {
        return repository.getAllTransactions(userId).asLiveData(Dispatchers.IO)
    }
    
    /**
     * Get transactions for a specific account
     */
    fun getTransactionsByAccount(userId: String, accountId: String): LiveData<List<Transaction>> {
        return repository.getTransactionsByAccount(userId, accountId).asLiveData(Dispatchers.IO)
    }
    
    /**
     * Get transactions for a specific category
     */
    fun getTransactionsByCategory(userId: String, categoryId: String): LiveData<List<Transaction>> {
        return repository.getTransactionsByCategory(userId, categoryId).asLiveData(Dispatchers.IO)
    }
    
    /**
     * Get a single transaction by ID
     */
    suspend fun getTransactionById(transactionId: String): Transaction? {
        return repository.getTransactionById(transactionId)
    }
    
    /**
     * Create a new transaction
     */
    fun createTransaction(
        userId: String,
        accountId: String,
        categoryId: String,
        type: String,
        amount: Double,
        date: Date,
        notes: String = "",
        invoiceBase64: String? = null,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val transactionId = repository.createTransaction(
                userId, accountId, categoryId, type, amount, date, notes, invoiceBase64
            )
            onComplete(transactionId)
        }
    }
    
    /**
     * Update an existing transaction
     */
    fun updateTransaction(transaction: Transaction, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateTransaction(transaction)
            onComplete()
        }
    }
    
    /**
     * Delete a transaction
     */
    fun deleteTransaction(transactionId: String, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteTransaction(transactionId)
            onComplete()
        }
    }
} 