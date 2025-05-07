package com.example.flowmoney.Models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.io.Serializable

/**
 * Model class representing a category in the budget app
 */
@IgnoreExtraProperties
class Category : Serializable {

    @PropertyName("category_id")
    var categoryId: String = ""  // Unique ID for the category

    @PropertyName("user_id")
    var userId: String = ""  // The user to whom this category belongs

    @PropertyName("name")
    var name: String = ""  // Category name

    @PropertyName("icon_base64")
    var iconBase64: String = ""  // Icon stored as Base64 encoded string

    @PropertyName("is_income")
    var isIncome: Boolean = false  // Whether this is an income category

    @PropertyName("created_at")
    var createdAt: Long = System.currentTimeMillis()

    @PropertyName("updated_at")
    var updatedAt: Long = System.currentTimeMillis()

    @Exclude
    var iconResourceId: Int = 0  // For local reference only (not stored in Firestore)

    // Empty constructor (required by Firestore)
    constructor()

    // Minimal constructor
    constructor(categoryId: String, userId: String, name: String, isIncome: Boolean) {
        this.categoryId = categoryId
        this.userId = userId
        this.name = name
        this.isIncome = isIncome
    }

    // Full constructor
    constructor(
        categoryId: String,
        userId: String,
        name: String,
        iconBase64: String,
        isIncome: Boolean,
        iconResourceId: Int = 0
    ) {
        this.categoryId = categoryId
        this.userId = userId
        this.name = name
        this.iconBase64 = iconBase64
        this.isIncome = isIncome
        this.iconResourceId = iconResourceId
    }

    @Exclude
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "category_id" to categoryId,
            "user_id" to userId,
            "name" to name,
            "icon_base64" to iconBase64,
            "is_income" to isIncome,
            "created_at" to createdAt,
            "updated_at" to updatedAt
        )
    }

    companion object {
        // Collection name in Firestore
        const val COLLECTION_NAME = "categories"
    }

    override fun toString(): String {
        return "Category(categoryId='$categoryId', userId='$userId', name='$name', isIncome=$isIncome)"
    }
}