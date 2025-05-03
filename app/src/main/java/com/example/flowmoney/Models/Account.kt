package com.example.flowmoney.Models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

@IgnoreExtraProperties
class Account : Serializable {

    @PropertyName("account_id")
    var accountId: String = ""  // Unique ID for the account (can be Firestore doc ID)

    @PropertyName("user_id")
    var userId: String = ""  // The user to whom this account belongs

    @PropertyName("account_name")
    var accountName: String = ""  // e.g., "My Bank", "Cash", "Crypto Wallet"

    @PropertyName("balance")
    var balance: Double = 0.0  // Initial balance

    @PropertyName("account_type")
    var accountType: String = ""  // e.g., "Bank", "Cash", "E-Wallet", "Crypto"

    @PropertyName("account_image_url")
    var accountImageUrl: String? = null  // Optional image/icon URL

    @PropertyName("note")
    var note: String? = null  // Optional description or notes

    @PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis()

    @PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis()

    // Empty constructor (required by Firestore)
    constructor()

    // Minimal constructor
    constructor(accountId: String, userId: String, accountName: String, balance: Double) {
        this.accountId = accountId
        this.userId = userId
        this.accountName = accountName
        this.balance = balance
    }

    // Full constructor
    constructor(
        accountId: String,
        userId: String,
        accountName: String,
        balance: Double,
        accountType: String,
        accountImageUrl: String? = null,
        note: String? = null
    ) {
        this.accountId = accountId
        this.userId = userId
        this.accountName = accountName
        this.balance = balance
        this.accountType = accountType
        this.accountImageUrl = accountImageUrl
        this.note = note
    }

    @Exclude
    fun updateBalance(amount: Double) {
        this.balance += amount
        this.updatedAt = System.currentTimeMillis()
    }

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "account_id" to accountId,
            "user_id" to userId,
            "account_name" to accountName,
            "balance" to balance,
            "account_type" to accountType,
            "account_image_url" to accountImageUrl,
            "note" to note,
            "created_at" to createdAt,
            "updated_at" to updatedAt
        )
    }

    override fun toString(): String {
        return "Account(accountId='$accountId', userId='$userId', name='$accountName', balance=$balance, type='$accountType')"
    }
}
