package com.example.flowmoney.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.flowmoney.Models.Account
import com.example.flowmoney.database.converters.DateConverters
import java.util.Date

/**
 * Room entity representing an account in the local SQLite database
 */
@Entity(tableName = "accounts")
@TypeConverters(DateConverters::class)
data class AccountEntity(
    @PrimaryKey
    val accountId: String,
    
    val userId: String,
    val accountName: String,
    val balance: Double,
    val accountType: String,
    val accountImageUrl: String?,
    val note: String?,
    val createdAt: Date,
    val updatedAt: Date,
    
    // Sync status
    val isSynced: Boolean,
    val needsSync: Boolean
) {
    /**
     * Convert local entity to Firestore model
     */
    fun toFirestoreModel(): Account {
        return Account().apply {
            this.accountId = this@AccountEntity.accountId
            this.userId = this@AccountEntity.userId
            this.accountName = this@AccountEntity.accountName
            this.balance = this@AccountEntity.balance
            this.accountType = this@AccountEntity.accountType
            this.accountImageUrl = this@AccountEntity.accountImageUrl
            this.note = this@AccountEntity.note
            this.createdAt = this@AccountEntity.createdAt.time
            this.updatedAt = this@AccountEntity.updatedAt.time
        }
    }
    
    companion object {
        /**
         * Convert Firestore model to local entity
         */
        fun fromFirestoreModel(account: Account, isSynced: Boolean = true): AccountEntity {
            return AccountEntity(
                accountId = account.accountId,
                userId = account.userId,
                accountName = account.accountName,
                balance = account.balance,
                accountType = account.accountType,
                accountImageUrl = account.accountImageUrl,
                note = account.note,
                createdAt = Date(account.createdAt),
                updatedAt = Date(account.updatedAt),
                isSynced = isSynced,
                needsSync = false
            )
        }
        
        /**
         * Create a new local account (not yet synced)
         */
        fun createLocalAccount(
            accountId: String,
            userId: String,
            accountName: String,
            balance: Double,
            accountType: String,
            accountImageUrl: String? = null,
            note: String? = null
        ): AccountEntity {
            val now = Date()
            return AccountEntity(
                accountId = accountId,
                userId = userId,
                accountName = accountName,
                balance = balance,
                accountType = accountType,
                accountImageUrl = accountImageUrl,
                note = note,
                createdAt = now,
                updatedAt = now,
                isSynced = false,
                needsSync = true
            )
        }
    }
} 