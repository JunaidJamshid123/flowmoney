package com.example.flowmoney.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flowmoney.database.entities.TransactionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Transaction entities
 */
@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(transactions: List<TransactionEntity>)

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE transactionId = :transactionId")
    suspend fun getTransactionById(transactionId: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE userId = :userId AND isDeleted = 0 ORDER BY date DESC")
    fun getAllTransactions(userId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND accountId = :accountId AND isDeleted = 0 ORDER BY date DESC")
    fun getTransactionsByAccount(userId: String, accountId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE userId = :userId AND categoryId = :categoryId AND isDeleted = 0 ORDER BY date DESC")
    fun getTransactionsByCategory(userId: String, categoryId: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE needsSync = 1")
    suspend fun getUnSyncedTransactions(): List<TransactionEntity>

    @Query("UPDATE transactions SET isSynced = 1, needsSync = 0 WHERE transactionId = :transactionId")
    suspend fun markAsSynced(transactionId: String)

    @Query("UPDATE transactions SET isDeleted = 1, needsSync = 1 WHERE transactionId = :transactionId")
    suspend fun markAsDeleted(transactionId: String)
} 