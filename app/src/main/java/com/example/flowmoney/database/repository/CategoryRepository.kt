package com.example.flowmoney.database.repository

import com.example.flowmoney.Models.Category
import com.example.flowmoney.database.dao.CategoryDao
import com.example.flowmoney.database.entities.CategoryEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Repository for handling category data from both local database and Firestore
 */
class CategoryRepository(private val categoryDao: CategoryDao) {
    
    private val firestore = FirebaseFirestore.getInstance()
    private val categoriesCollection = firestore.collection(Category.COLLECTION_NAME)
    
    // Local operations
    
    /**
     * Get all categories for a user from local database
     */
    fun getAllCategories(userId: String): Flow<List<Category>> {
        return categoryDao.getAllCategories(userId).map { entities ->
            entities.map { it.toFirestoreModel() }
        }
    }
    
    /**
     * Get categories of a specific type (income/expense) from local database
     */
    fun getCategoriesByType(userId: String, isIncome: Boolean): Flow<List<Category>> {
        return categoryDao.getCategoriesByType(userId, isIncome).map { entities ->
            entities.map { it.toFirestoreModel() }
        }
    }
    
    /**
     * Get a single category by ID from local database
     */
    suspend fun getCategoryById(categoryId: String): Category? {
        return categoryDao.getCategoryById(categoryId)?.toFirestoreModel()
    }
    
    /**
     * Create a new category locally
     */
    suspend fun createCategory(
        userId: String,
        name: String,
        iconBase64: String,
        isIncome: Boolean
    ): String {
        val categoryId = UUID.randomUUID().toString()
        val entity = CategoryEntity.createLocalCategory(
            categoryId = categoryId,
            userId = userId,
            name = name,
            iconBase64 = iconBase64,
            isIncome = isIncome
        )
        categoryDao.insert(entity)
        return categoryId
    }
    
    /**
     * Save a category to local database
     */
    suspend fun saveCategory(category: Category) {
        val entity = CategoryEntity.fromFirestoreModel(category, false)
        categoryDao.insert(entity)
    }
    
    /**
     * Update an existing category in local database
     */
    suspend fun updateCategory(category: Category) {
        val existingEntity = categoryDao.getCategoryById(category.categoryId)
        if (existingEntity != null) {
            val updatedEntity = CategoryEntity.fromFirestoreModel(category, false)
            categoryDao.update(updatedEntity)
        }
    }
    
    // Firebase operations
    
    /**
     * Fetch categories from Firestore and store locally
     */
    suspend fun fetchAndStoreCategories(userId: String) {
        try {
            val querySnapshot = categoriesCollection
                .whereEqualTo("user_id", userId)
                .get()
                .await()
            
            val categories = querySnapshot.documents.mapNotNull { document ->
                val category = Category()
                document.data?.let { data ->
                    category.categoryId = document.id
                    category.userId = data["user_id"] as? String ?: ""
                    category.name = data["name"] as? String ?: ""
                    category.iconBase64 = data["icon_base64"] as? String ?: ""
                    category.isIncome = data["is_income"] as? Boolean ?: false
                    category.createdAt = (data["created_at"] as? Long) ?: System.currentTimeMillis()
                    category.updatedAt = (data["updated_at"] as? Long) ?: System.currentTimeMillis()
                    category
                }
            }
            
            val entities = categories.map { CategoryEntity.fromFirestoreModel(it, true) }
            categoryDao.insertAll(entities)
        } catch (e: Exception) {
            // Handle failure gracefully, log error, etc.
        }
    }
    
    /**
     * Sync local changes to Firestore
     */
    suspend fun syncUnSyncedCategories() {
        val unSyncedCategories = categoryDao.getUnSyncedCategories()
        
        for (category in unSyncedCategories) {
            try {
                // Create or update in Firestore
                val firestoreModel = category.toFirestoreModel()
                categoriesCollection.document(category.categoryId)
                    .set(firestoreModel.toMap()).await()
                
                // Mark as synced locally
                categoryDao.markAsSynced(category.categoryId)
            } catch (e: Exception) {
                // Handle failure gracefully, log error, retry later, etc.
            }
        }
    }
} 