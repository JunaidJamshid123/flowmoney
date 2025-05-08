package com.example.flowmoney.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Adapters.BudgetAdapter
import com.example.flowmoney.Adapters.BudgetCategoryAdapter
import com.example.flowmoney.Models.Budget
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class BudgetFragment : Fragment() {
    private lateinit var rvExistingBudgets: RecyclerView
    private lateinit var rvBudgetCategories: RecyclerView
    private lateinit var emptyBudgetsText: TextView
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private val budgets = mutableListOf<Budget>()
    private val categories = mutableMapOf<String, Category>()
    private val expenseCategories = mutableListOf<Category>()
    
    private lateinit var budgetAdapter: BudgetAdapter
    private lateinit var categoryAdapter: BudgetCategoryAdapter
    
    // Current month and year
    private val calendar = Calendar.getInstance()
    private val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
    private val currentYear = calendar.get(Calendar.YEAR)
    
    companion object {
        private const val TAG = "BudgetFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_budget, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        // Initialize views
        initViews(view)
        
        // Setup adapters
        setupAdapters()
        
        // Load data
        loadData()
    }
    
    override fun onResume() {
        super.onResume()
        // Reload budgets when returning to this fragment
        loadBudgets()
    }
    
    private fun initViews(view: View) {
        rvExistingBudgets = view.findViewById(R.id.rv_existing_budgets)
        rvBudgetCategories = view.findViewById(R.id.rv_budget_categories)
        
        // Create TextView for empty budgets message
        emptyBudgetsText = TextView(requireContext()).apply {
            text = "No budgets set yet"
            textSize = 16f
            setPadding(32, 16, 32, 16)
            visibility = View.GONE
        }
        val parent = rvExistingBudgets.parent as ViewGroup
        parent.addView(emptyBudgetsText, parent.indexOfChild(rvExistingBudgets) + 1)
    }
    
    private fun setupAdapters() {
        // Setup budget adapter
        budgetAdapter = BudgetAdapter(requireContext(), budgets, categories)
        rvExistingBudgets.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = budgetAdapter
        }
        
        // Setup category adapter
        categoryAdapter = BudgetCategoryAdapter(requireContext(), expenseCategories)
        rvBudgetCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }
    
    private fun loadData() {
        loadCategories()
        loadBudgets()
        loadTransactions()
    }
    
    private fun loadCategories() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("categories")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                categories.clear()
                expenseCategories.clear()
                
                for (document in documents) {
                    try {
                        val category = Category()
                        category.categoryId = document.getString("category_id") ?: document.id
                        category.userId = document.getString("user_id") ?: ""
                        category.name = document.getString("name") ?: "Unknown"
                        category.iconBase64 = document.getString("icon_base64") ?: ""
                        category.isIncome = document.getBoolean("is_income") ?: false
                        
                        // Add to all categories map
                        categories[category.categoryId] = category
                        
                        // Add to expense categories list if not income
                        if (!category.isIncome) {
                            expenseCategories.add(category)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing category: ${e.message}")
                    }
                }
                
                // Update category adapter
                categoryAdapter.updateCategories(expenseCategories)
                
                // Also update budget adapter since it needs categories
                budgetAdapter.updateData(budgets, categories)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading categories: ${e.message}")
                Toast.makeText(requireContext(), "Failed to load categories", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadBudgets() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection(Budget.COLLECTION_NAME)
            .whereEqualTo("user_id", userId)
            .whereEqualTo("month", currentMonth)
            .whereEqualTo("year", currentYear)
            .get()
            .addOnSuccessListener { documents ->
                budgets.clear()
                
                for (document in documents) {
                    try {
                        val data = document.data
                        val budget = Budget.fromMap(data, document.id)
                        budgets.add(budget)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing budget: ${e.message}")
                    }
                }
                
                // Update adapter
                budgetAdapter.updateData(budgets, categories)
                
                // Show/hide empty message
                emptyBudgetsText.visibility = if (budgets.isEmpty()) View.VISIBLE else View.GONE
                rvExistingBudgets.visibility = if (budgets.isEmpty()) View.GONE else View.VISIBLE
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading budgets: ${e.message}")
                Toast.makeText(requireContext(), "Failed to load budgets", Toast.LENGTH_SHORT).show()
            }
    }
    
    private fun loadTransactions() {
        val userId = auth.currentUser?.uid ?: return
        
        // Get the start and end dates for the current month
        val startCalendar = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, 1, 0, 0, 0) // Month is 0-based in Calendar
            set(Calendar.MILLISECOND, 0)
        }
        val endCalendar = Calendar.getInstance().apply {
            set(currentYear, currentMonth - 1, getActualMaximum(Calendar.DAY_OF_MONTH), 23, 59, 59)
            set(Calendar.MILLISECOND, 999)
        }
        
        // Query for transactions in this month
        firestore.collection("transactions")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("is_deleted", false)
            .whereEqualTo("type", "expense") // Only expense transactions count towards budget
            .whereGreaterThanOrEqualTo("date", com.google.firebase.Timestamp(startCalendar.time))
            .whereLessThanOrEqualTo("date", com.google.firebase.Timestamp(endCalendar.time))
            .get()
            .addOnSuccessListener { documents ->
                // Create a map to track spending by category
                val categorySpending = mutableMapOf<String, Double>()
                
                // Calculate spending for each category
                for (document in documents) {
                    try {
                        val data = document.data
                        val transaction = Transaction.fromMap(data, document.id)
                        val categoryId = transaction.categoryId
                        val amount = transaction.amount
                        
                        // Add transaction amount to category spending
                        val currentSpending = categorySpending.getOrDefault(categoryId, 0.0)
                        categorySpending[categoryId] = currentSpending + amount
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction: ${e.message}")
                    }
                }
                
                // Update budgets with spending data
                updateBudgetSpending(categorySpending)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading transactions: ${e.message}")
            }
    }
    
    private fun updateBudgetSpending(categorySpending: Map<String, Double>) {
        // Update each budget with the spent amount
        var updatedBudgets = false
        
        for (budget in budgets) {
            val categoryId = budget.categoryId
            val spent = categorySpending[categoryId] ?: 0.0
            
            if (budget.spent != spent) {
                budget.spent = spent
                updatedBudgets = true
                
                // Update Firestore with new spent amount
                firestore.collection(Budget.COLLECTION_NAME)
                    .document(budget.budgetId)
                    .update("spent", spent)
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating budget spending: ${e.message}")
                    }
            }
        }
        
        // Update adapter if any budgets were updated
        if (updatedBudgets) {
            budgetAdapter.updateData(budgets, categories)
        }
    }
}