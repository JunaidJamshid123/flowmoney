package com.example.flowmoney.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

/**
 * Budget model for Firebase Firestore
 *
 * This model represents a budget set for a specific category
 */
class Budget : Serializable {
    @DocumentId
    var budgetId: String = ""
    
    @PropertyName("user_id")
    var userId: String = ""
    
    @PropertyName("category_id")
    var categoryId: String = ""
    
    @PropertyName("month")
    var month: Int = 0  // 1-12
    
    @PropertyName("year")
    var year: Int = 0
    
    @PropertyName("limit")
    var limit: Double = 0.0
    
    @PropertyName("spent")
    var spent: Double = 0.0
    
    @PropertyName("created_at")
    var createdAt: Timestamp = Timestamp.now()
    
    @PropertyName("updated_at")
    var updatedAt: Timestamp = Timestamp.now()
    
    // Default constructor (required by Firestore)
    constructor()
    
    // Minimal constructor
    constructor(
        userId: String,
        categoryId: String,
        month: Int,
        year: Int,
        limit: Double
    ) {
        this.userId = userId
        this.categoryId = categoryId
        this.month = month
        this.year = year
        this.limit = limit
        this.spent = 0.0
    }
    
    // Full constructor
    constructor(
        budgetId: String,
        userId: String,
        categoryId: String,
        month: Int,
        year: Int,
        limit: Double,
        spent: Double,
        createdAt: Timestamp,
        updatedAt: Timestamp
    ) {
        this.budgetId = budgetId
        this.userId = userId
        this.categoryId = categoryId
        this.month = month
        this.year = year
        this.limit = limit
        this.spent = spent
        this.createdAt = createdAt
        this.updatedAt = updatedAt
    }
    
    /**
     * Get remaining budget amount
     */
    @Exclude
    fun getRemaining(): Double {
        return limit - spent
    }
    
    /**
     * Get budget progress percentage
     */
    @Exclude
    fun getProgress(): Int {
        if (limit <= 0) return 0
        val progress = (spent / limit) * 100
        return progress.toInt().coerceIn(0, 100)
    }
    
    /**
     * Check if budget is exceeded
     */
    @Exclude
    fun isExceeded(): Boolean {
        return spent > limit
    }
    
    /**
     * Convert to Map for Firestore
     */
    @Exclude
    fun toMap(): Map<String, Any> {
        return mapOf(
            "user_id" to userId,
            "category_id" to categoryId,
            "month" to month,
            "year" to year,
            "limit" to limit,
            "spent" to spent,
            "created_at" to createdAt,
            "updated_at" to updatedAt
        )
    }
    
    companion object {
        /**
         * Create from Firestore data
         */
        fun fromMap(data: Map<String, Any>, id: String): Budget {
            return Budget(
                budgetId = id,
                userId = data["user_id"] as? String ?: "",
                categoryId = data["category_id"] as? String ?: "",
                month = (data["month"] as? Number)?.toInt() ?: 0,
                year = (data["year"] as? Number)?.toInt() ?: 0,
                limit = (data["limit"] as? Number)?.toDouble() ?: 0.0,
                spent = (data["spent"] as? Number)?.toDouble() ?: 0.0,
                createdAt = data["created_at"] as? Timestamp ?: Timestamp.now(),
                updatedAt = data["updated_at"] as? Timestamp ?: Timestamp.now()
            )
        }
        
        /**
         * Firestore collection name
         */
        const val COLLECTION_NAME = "budgets"
    }
} 