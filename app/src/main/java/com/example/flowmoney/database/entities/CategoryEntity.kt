package com.example.flowmoney.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.flowmoney.Models.Category
import com.example.flowmoney.database.converters.DateConverters
import java.util.Date

/**
 * Room entity representing a category in the local SQLite database
 */
@Entity(tableName = "categories")
@TypeConverters(DateConverters::class)
data class CategoryEntity(
    @PrimaryKey
    val categoryId: String,
    
    val userId: String,
    val name: String,
    val iconBase64: String,
    val isIncome: Boolean,
    val createdAt: Date,
    val updatedAt: Date,
    
    // Sync status
    val isSynced: Boolean,
    val needsSync: Boolean
) {
    /**
     * Convert local entity to Firestore model
     */
    fun toFirestoreModel(): Category {
        return Category().apply {
            this.categoryId = this@CategoryEntity.categoryId
            this.userId = this@CategoryEntity.userId
            this.name = this@CategoryEntity.name
            this.iconBase64 = this@CategoryEntity.iconBase64
            this.isIncome = this@CategoryEntity.isIncome
            this.createdAt = this@CategoryEntity.createdAt.time
            this.updatedAt = this@CategoryEntity.updatedAt.time
        }
    }
    
    companion object {
        /**
         * Convert Firestore model to local entity
         */
        fun fromFirestoreModel(category: Category, isSynced: Boolean = true): CategoryEntity {
            return CategoryEntity(
                categoryId = category.categoryId,
                userId = category.userId,
                name = category.name,
                iconBase64 = category.iconBase64,
                isIncome = category.isIncome,
                createdAt = Date(category.createdAt),
                updatedAt = Date(category.updatedAt),
                isSynced = isSynced,
                needsSync = false
            )
        }
        
        /**
         * Create a new local category (not yet synced)
         */
        fun createLocalCategory(
            categoryId: String,
            userId: String,
            name: String,
            iconBase64: String,
            isIncome: Boolean
        ): CategoryEntity {
            val now = Date()
            return CategoryEntity(
                categoryId = categoryId,
                userId = userId,
                name = name,
                iconBase64 = iconBase64,
                isIncome = isIncome,
                createdAt = now,
                updatedAt = now,
                isSynced = false,
                needsSync = true
            )
        }
    }
} 