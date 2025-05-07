package com.example.flowmoney.utlities

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import com.example.flowmoney.Models.Category
import com.google.firebase.firestore.FirebaseFirestore
import java.io.ByteArrayOutputStream
import java.util.Date
import java.util.UUID

/**
 * Utility class for Firestore operations
 */
object FirestoreUtils {
    private const val TAG = "FirestoreUtils"

    // Reference to Firestore
    private val db = FirebaseFirestore.getInstance()

    /**
     * Save a category to Firestore
     *
     * @param context The application context
     * @param name The category name
     * @param iconResourceId The drawable resource ID of the icon
     * @param isIncome Whether the category is for income
     * @param categoryType The type of category (expense, income, or saving)
     * @param userId The ID of the user
     * @param onSuccess Callback when save is successful, returns the created Category
     * @param onFailure Callback when save fails, returns the error message
     */
    fun saveCategory(
        context: Context,
        name: String,
        iconResourceId: Int,
        isIncome: Boolean,
        categoryType: String,
        userId: String,
        onSuccess: (Category) -> Unit,
        onFailure: (String) -> Unit
    ) {
        try {
            // Convert drawable to base64 string
            val iconBase64 = convertDrawableToBase64(context, iconResourceId)

            // Generate a new category ID
            val categoryId = UUID.randomUUID().toString()
            
            // Create a new category object using the new constructor
            val newCategory = Category(
                categoryId = categoryId,
                userId = userId,
                name = name,
                iconBase64 = iconBase64,
                isIncome = isIncome,
                iconResourceId = iconResourceId
            )
            
            // Set timestamps
            val currentTime = System.currentTimeMillis()
            newCategory.createdAt = currentTime
            newCategory.updatedAt = currentTime
            
            // Convert to map and add category_type
            val categoryMap = newCategory.toMap().toMutableMap()
            categoryMap["category_type"] = categoryType

            // Add to Firestore
            db.collection(Category.COLLECTION_NAME)
                .document(categoryId)
                .set(categoryMap)
                .addOnSuccessListener {
                    Log.d(TAG, "Category saved with ID: $categoryId")
                    // Return the category
                    onSuccess(newCategory)
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error adding category", e)
                    onFailure("Failed to save category: ${e.message}")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error in saveCategory", e)
            onFailure("Error processing category: ${e.message}")
        }
    }

    /**
     * Get all categories from Firestore
     *
     * @param userId The ID of the user whose categories to fetch
     * @param onSuccess Callback when fetch is successful, returns list of categories
     * @param onFailure Callback when fetch fails, returns the error message
     */
    fun getAllCategories(
        userId: String,
        onSuccess: (List<Category>) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Category.COLLECTION_NAME)
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { result ->
                val categories = result.documents.mapNotNull { document ->
                    try {
                        // Create a new Category object and populate it manually
                        val category = Category()
                        category.categoryId = document.getString("category_id") ?: ""
                        category.userId = document.getString("user_id") ?: ""
                        category.name = document.getString("name") ?: ""
                        category.iconBase64 = document.getString("icon_base64") ?: ""
                        category.isIncome = document.getBoolean("is_income") ?: false
                        category.createdAt = document.getLong("created_at") ?: 0L
                        category.updatedAt = document.getLong("updated_at") ?: 0L
                        
                        category
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Category", e)
                        null
                    }
                }
                onSuccess(categories)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error getting categories", e)
                onFailure("Failed to load categories: ${e.message}")
            }
    }

    /**
     * Convert a drawable resource to Base64 string
     *
     * @param context The application context
     * @param resourceId The drawable resource ID
     * @return The Base64 encoded string of the drawable
     */
    private fun convertDrawableToBase64(context: Context, resourceId: Int): String {
        try {
            // Get the drawable from resource ID
            val drawable = ContextCompat.getDrawable(context, resourceId)
                ?: throw IllegalArgumentException("Resource not found: $resourceId")

            // Create a bitmap with the drawable dimensions
            val bitmap = Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )

            // Draw the drawable onto the bitmap
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            // Convert bitmap to byte array
            val byteArrayOutputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream)
            val byteArray = byteArrayOutputStream.toByteArray()

            // Convert byte array to Base64 string
            return Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting drawable to Base64", e)
            throw e
        }
    }

    /**
     * Delete a category from Firestore
     *
     * @param categoryId The ID of the category to delete
     * @param onSuccess Callback when delete is successful
     * @param onFailure Callback when delete fails, returns the error message
     */
    fun deleteCategory(
        categoryId: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection(Category.COLLECTION_NAME)
            .document(categoryId)
            .delete()
            .addOnSuccessListener {
                Log.d(TAG, "Category successfully deleted")
                onSuccess()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error deleting category", e)
                onFailure("Failed to delete category: ${e.message}")
            }
    }
}