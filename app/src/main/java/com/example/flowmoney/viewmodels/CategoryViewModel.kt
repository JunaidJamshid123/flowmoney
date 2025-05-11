package com.example.flowmoney.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.flowmoney.FlowMoneyApplication
import com.example.flowmoney.Models.Category
import com.example.flowmoney.database.repository.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * ViewModel for handling category data, both online and offline
 */
class CategoryViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository: CategoryRepository
    
    init {
        repository = (application as FlowMoneyApplication).categoryRepository
    }
    
    /**
     * Get all categories for the current user
     */
    fun getAllCategories(userId: String): LiveData<List<Category>> {
        return repository.getAllCategories(userId).asLiveData(Dispatchers.IO)
    }
    
    /**
     * Get categories of a specific type (income/expense)
     */
    fun getCategoriesByType(userId: String, isIncome: Boolean): LiveData<List<Category>> {
        return repository.getCategoriesByType(userId, isIncome).asLiveData(Dispatchers.IO)
    }
    
    /**
     * Get a single category by ID
     */
    suspend fun getCategoryById(categoryId: String): Category? {
        return repository.getCategoryById(categoryId)
    }
    
    /**
     * Create a new category
     */
    fun createCategory(
        userId: String,
        name: String,
        iconBase64: String,
        isIncome: Boolean,
        onComplete: (String) -> Unit
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val categoryId = repository.createCategory(userId, name, iconBase64, isIncome)
            onComplete(categoryId)
        }
    }
    
    /**
     * Update an existing category
     */
    fun updateCategory(category: Category, onComplete: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCategory(category)
            onComplete()
        }
    }
} 