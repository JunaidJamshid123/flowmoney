package com.example.flowmoney.Models

import com.google.firebase.firestore.DocumentId
import java.util.Date

/**
 * Model class representing a category in the budget app
 */
data class Category(
    @DocumentId
    val id: String = "", // Firestore document ID
    val name: String = "",
    val iconBase64: String = "", // Icon stored as Base64 encoded string
    val iconResourceId: Int = 0, // For local reference only (not stored in Firestore)
    val isIncome: Boolean = false,
    val createdAt: Date = Date(),
    val updatedAt: Date = Date()
) {
    // Empty constructor required for Firestore
    constructor() : this("", "", "", 0, false, Date(), Date())

    companion object {
        // Constants for Firestore
        const val COLLECTION_NAME = "categories"
        const val FIELD_NAME = "name"
        const val FIELD_ICON_BASE64 = "iconBase64"
        const val FIELD_IS_INCOME = "isIncome"
        const val FIELD_CREATED_AT = "createdAt"
        const val FIELD_UPDATED_AT = "updatedAt"
    }

    // Convert to HashMap for Firestore
    fun toMap(): Map<String, Any> {
        return hashMapOf(
            FIELD_NAME to name,
            FIELD_ICON_BASE64 to iconBase64,
            FIELD_IS_INCOME to isIncome,
            FIELD_CREATED_AT to createdAt,
            FIELD_UPDATED_AT to updatedAt
        )
    }
}