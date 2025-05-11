package com.example.flowmoney.database

import android.content.Context
import android.util.Log
import com.example.flowmoney.Models.Transaction
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File
import java.lang.reflect.Type
import java.util.concurrent.atomic.AtomicBoolean

/**
 * A utility class for caching data locally when offline
 */
class LocalDataCache(private val context: Context) {
    companion object {
        private const val TAG = "LocalDataCache"
        
        // Cache file names
        private const val TRANSACTIONS_CACHE = "transactions_cache.json"
        private const val CATEGORIES_CACHE = "categories_cache.json"
        private const val ACCOUNTS_CACHE = "accounts_cache.json"
        
        // Singleton instance
        @Volatile
        private var INSTANCE: LocalDataCache? = null
        private val isInitializing = AtomicBoolean(false)
        
        fun getInstance(context: Context): LocalDataCache {
            return INSTANCE ?: synchronized(this) {
                if (isInitializing.get()) {
                    // Wait for initialization to complete
                    while (isInitializing.get()) {
                        Thread.sleep(10)
                    }
                    INSTANCE!!
                } else {
                    isInitializing.set(true)
                    try {
                        val instance = LocalDataCache(context.applicationContext)
                        INSTANCE = instance
                        instance
                    } finally {
                        isInitializing.set(false)
                    }
                }
            }
        }
    }
    
    private val gson = Gson()
    
    /**
     * Cache transactions locally
     */
    fun cacheTransactions(userId: String, transactions: List<Transaction>): Boolean {
        val userTransactionsFile = getUserSpecificFile(userId, TRANSACTIONS_CACHE)
        return try {
            val json = gson.toJson(transactions)
            userTransactionsFile.writeText(json)
            Log.d(TAG, "Cached ${transactions.size} transactions for user $userId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cache transactions", e)
            false
        }
    }
    
    /**
     * Get cached transactions
     */
    fun getCachedTransactions(userId: String): List<Transaction> {
        val userTransactionsFile = getUserSpecificFile(userId, TRANSACTIONS_CACHE)
        return try {
            if (!userTransactionsFile.exists()) {
                Log.d(TAG, "No cached transactions for user $userId")
                return emptyList()
            }
            
            val json = userTransactionsFile.readText()
            val type: Type = object : TypeToken<List<Transaction>>() {}.type
            val transactions: List<Transaction> = gson.fromJson(json, type)
            Log.d(TAG, "Retrieved ${transactions.size} cached transactions for user $userId")
            transactions
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve cached transactions", e)
            emptyList()
        }
    }
    
    /**
     * Get user-specific cache file
     */
    private fun getUserSpecificFile(userId: String, fileName: String): File {
        val cacheDir = File(context.cacheDir, "user_${userId}_data")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        return File(cacheDir, fileName)
    }
    
    /**
     * Clear all cached data for a user
     */
    fun clearUserCache(userId: String) {
        val cacheDir = File(context.cacheDir, "user_${userId}_data")
        if (cacheDir.exists()) {
            cacheDir.deleteRecursively()
            Log.d(TAG, "Cleared cache for user $userId")
        }
    }
} 