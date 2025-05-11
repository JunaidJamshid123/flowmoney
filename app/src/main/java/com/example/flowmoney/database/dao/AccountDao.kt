package com.example.flowmoney.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.flowmoney.database.entities.AccountEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Account entities
 */
@Dao
interface AccountDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(account: AccountEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(accounts: List<AccountEntity>)

    @Update
    suspend fun update(account: AccountEntity)

    @Delete
    suspend fun delete(account: AccountEntity)

    @Query("SELECT * FROM accounts WHERE accountId = :accountId")
    suspend fun getAccountById(accountId: String): AccountEntity?

    @Query("SELECT * FROM accounts WHERE userId = :userId ORDER BY accountName ASC")
    fun getAllAccounts(userId: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE userId = :userId AND accountType = :accountType ORDER BY accountName ASC")
    fun getAccountsByType(userId: String, accountType: String): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE needsSync = 1")
    suspend fun getUnSyncedAccounts(): List<AccountEntity>

    @Query("UPDATE accounts SET isSynced = 1, needsSync = 0 WHERE accountId = :accountId")
    suspend fun markAsSynced(accountId: String)

    @Query("UPDATE accounts SET balance = balance + :amount, updatedAt = :timestamp, needsSync = 1 WHERE accountId = :accountId")
    suspend fun updateBalance(accountId: String, amount: Double, timestamp: Long)
} 