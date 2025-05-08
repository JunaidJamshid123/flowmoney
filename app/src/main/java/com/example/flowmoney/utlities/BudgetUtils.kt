package com.example.flowmoney.utlities

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

/**
 * Utility class to handle budget updates when transactions are created, deleted, or modified
 */
object BudgetUtils {
    private const val TAG = "BudgetUtils"
    private val firestore = FirebaseFirestore.getInstance()
    
    /**
     * Updates the budget spent amount when a transaction is created or modified
     * 
     * @param userId The user ID
     * @param categoryId The category ID
     * @param amount The transaction amount
     * @param isDeleted Whether the transaction is being deleted (to subtract from budget)
     */
    fun updateBudgetSpending(userId: String, categoryId: String, amount: Double, isDeleted: Boolean = false) {
        // Only continue if this is for an expense category
        if (categoryId.isEmpty()) return
        
        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Check if there's a budget for this category in the current month
        firestore.collection("budgets")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("category_id", categoryId)
            .whereEqualTo("month", currentMonth)
            .whereEqualTo("year", currentYear)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    // Found a budget, update it
                    val budget = documents.documents[0]
                    val budgetId = budget.id
                    val currentSpent = budget.getDouble("spent") ?: 0.0
                    
                    // Calculate new spent amount 
                    val newSpent = if (isDeleted) {
                        // If transaction is deleted, subtract amount
                        currentSpent - amount
                    } else {
                        // If new transaction, add amount
                        currentSpent + amount
                    }
                    
                    // Make sure we don't go below zero
                    val finalSpent = newSpent.coerceAtLeast(0.0)
                    
                    // Update Firestore
                    firestore.collection("budgets")
                        .document(budgetId)
                        .update(
                            mapOf(
                                "spent" to finalSpent,
                                "updated_at" to Timestamp.now()
                            )
                        )
                        .addOnSuccessListener {
                            Log.d(TAG, "Budget spending updated successfully")
                        }
                        .addOnFailureListener { e ->
                            Log.e(TAG, "Error updating budget spending", e)
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking budget", e)
            }
    }
    
    /**
     * Recalculates all budget spending for the current month based on transactions.
     * This is useful after bulk operations or to ensure data consistency.
     * 
     * @param userId The user ID
     */
    fun recalculateAllBudgets(userId: String) {
        // Get current month and year
        val calendar = Calendar.getInstance()
        val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
        val currentYear = calendar.get(Calendar.YEAR)
        
        // Get the start and end dates for the current month
        val startCalendar = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, 1, 0, 0, 0) // Month is 0-based in Calendar
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        // Query for all expense transactions in this month
        firestore.collection("transactions")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("is_deleted", false)
            .whereEqualTo("type", "expense")
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(startCalendar.time))
            .whereLessThanOrEqualTo("date", com.google.firebase.Timestamp(endCalendar.time))
            .get()
            .addOnSuccessListener { transactions ->
                // Get all budgets for this month
                firestore.collection("budgets")
                    .whereEqualTo("user_id", userId)
                    .whereEqualTo("month", currentMonth)
                    .whereEqualTo("year", currentYear)
                    .get()
                    .addOnSuccessListener { budgets ->
                        // Create a map of category ID to budget document ID
                        val budgetMap = mutableMapOf<String, String>()
                        for (budget in budgets) {
                            val categoryId = budget.getString("category_id") ?: continue
                            budgetMap[categoryId] = budget.id
                        }
                        
                        // Create a map to track spending by category
                        val categorySpending = mutableMapOf<String, Double>()
                        
                        // Calculate spending for each category
                        for (transaction in transactions) {
                            try {
                                val categoryId = transaction.getString("category_id") ?: continue
                                val amount = transaction.getDouble("amount") ?: 0.0
                                
                                // Add transaction amount to category spending
                                val currentSpending = categorySpending.getOrDefault(categoryId, 0.0)
                                categorySpending[categoryId] = currentSpending + amount
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing transaction: ${e.message}")
                            }
                        }
                        
                        // Update each budget with its calculated spending
                        for ((categoryId, budgetId) in budgetMap) {
                            val spent = categorySpending[categoryId] ?: 0.0
                            firestore.collection("budgets")
                                .document(budgetId)
                                .update(
                                    mapOf(
                                        "spent" to spent,
                                        "updated_at" to Timestamp.now()
                                    )
                                )
                                .addOnSuccessListener {
                                    Log.d(TAG, "Budget $budgetId spending recalculated: $spent")
                                }
                                .addOnFailureListener { e ->
                                    Log.e(TAG, "Error updating budget $budgetId spending", e)
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error fetching budgets: ${e.message}")
                    }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error fetching transactions: ${e.message}")
            }
    }
} 