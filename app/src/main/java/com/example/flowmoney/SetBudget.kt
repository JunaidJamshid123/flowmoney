package com.example.flowmoney

import android.app.Activity
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.flowmoney.Models.Budget
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SetBudget : AppCompatActivity() {
    
    // UI components
    private lateinit var tvSetBudgetTitle: TextView
    private lateinit var ivCategoryIcon: CircleImageView
    private lateinit var tvCategoryName: TextView
    private lateinit var tvMonth: TextView
    private lateinit var etLimit: EditText
    private lateinit var btnCancel: Button
    private lateinit var btnSet: Button
    
    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    // Data
    private var categoryId: String = ""
    private var categoryName: String = ""
    private var categoryIcon: String = ""
    private var isIncome: Boolean = false
    private var budgetId: String = ""
    private var budgetLimit: Double = 0.0
    private var editMode: Boolean = false
    
    // Current month and year
    private val calendar = Calendar.getInstance()
    private val currentMonth = calendar.get(Calendar.MONTH) + 1 // Calendar months are 0-based
    private val currentYear = calendar.get(Calendar.YEAR)
    
    companion object {
        private const val TAG = "SetBudget"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_set_budget)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        // Get data from intent
        getIntentData()
        
        // Initialize views
        initViews()
        
        // Setup views
        setupViews()
        
        // Setup click listeners
        setupClickListeners()
    }
    
    private fun getIntentData() {
        intent.extras?.let { bundle ->
            categoryId = bundle.getString("category_id", "")
            categoryName = bundle.getString("category_name", "")
            categoryIcon = bundle.getString("category_icon", "")
            isIncome = bundle.getBoolean("is_income", false)
            budgetId = bundle.getString("budget_id", "")
            budgetLimit = bundle.getDouble("budget_limit", 0.0)
            editMode = bundle.getBoolean("edit_mode", false)
        }
    }
    
    private fun initViews() {
        tvSetBudgetTitle = findViewById(R.id.tvSetBudgetTitle)
        ivCategoryIcon = findViewById(R.id.ivCategoryIcon)
        tvCategoryName = findViewById(R.id.tvCategoryName)
        tvMonth = findViewById(R.id.tvMonth)
        etLimit = findViewById(R.id.etLimit)
        btnCancel = findViewById(R.id.btnCancel)
        btnSet = findViewById(R.id.btnSet)
    }
    
    private fun setupViews() {
        // Set title based on mode
        tvSetBudgetTitle.text = if (editMode) "Update Budget" else "Set Budget"
        
        // Set category name
        tvCategoryName.text = categoryName
        
        // Set category icon
        if (categoryIcon.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(categoryIcon, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                ivCategoryIcon.setImageBitmap(bitmap)
                
                // Set border color based on income/expense
                val borderColor = if (isIncome) {
                    ContextCompat.getColor(this, R.color.income_green)
                } else {
                    ContextCompat.getColor(this, R.color.expense_red)
                }
                ivCategoryIcon.setBorderColor(borderColor)
            } catch (e: Exception) {
                Log.e(TAG, "Error decoding icon: ${e.message}")
                ivCategoryIcon.setImageResource(R.drawable.shoppingg)
            }
        } else {
            ivCategoryIcon.setImageResource(R.drawable.shoppingg)
        }
        
        // Set month text
        val monthFormat = SimpleDateFormat("MMMM, yyyy", Locale.getDefault())
        calendar.set(Calendar.MONTH, currentMonth - 1) // Calendar months are 0-based
        calendar.set(Calendar.YEAR, currentYear)
        tvMonth.text = "Month: ${monthFormat.format(calendar.time)}"
        
        // Set existing limit if editing
        if (editMode && budgetLimit > 0) {
            etLimit.setText(budgetLimit.toString())
        }
        
        // Set button text based on mode
        btnSet.text = if (editMode) "UPDATE" else "SET"
    }
    
    private fun setupClickListeners() {
        btnCancel.setOnClickListener {
            finish()
        }
        
        btnSet.setOnClickListener {
            saveBudget()
        }
    }
    
    private fun saveBudget() {
        // Get user ID from Firebase Auth
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "You must be logged in to set a budget", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Get limit from edit text
        val limitStr = etLimit.text.toString().trim()
        if (limitStr.isEmpty()) {
            Toast.makeText(this, "Please enter a budget limit", Toast.LENGTH_SHORT).show()
            return
        }
        
        val limit = limitStr.toDoubleOrNull() ?: run {
            Toast.makeText(this, "Invalid budget limit", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (limit <= 0) {
            Toast.makeText(this, "Budget limit must be greater than zero", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Show loading state
        setLoadingState(true)
        
        if (editMode && budgetId.isNotEmpty()) {
            // Update existing budget
            updateBudget(userId, limit)
        } else {
            // Check if budget already exists for this category/month/year
            checkExistingBudget(userId, limit)
        }
    }
    
    private fun checkExistingBudget(userId: String, limit: Double) {
        firestore.collection(Budget.COLLECTION_NAME)
            .whereEqualTo("user_id", userId)
            .whereEqualTo("category_id", categoryId)
            .whereEqualTo("month", currentMonth)
            .whereEqualTo("year", currentYear)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // No existing budget, create a new one
                    createNewBudget(userId, limit)
                } else {
                    // Budget already exists, update it
                    val existingBudget = documents.documents.first()
                    budgetId = existingBudget.id
                    editMode = true
                    updateBudget(userId, limit)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking existing budget: ${e.message}")
                Toast.makeText(this, "Failed to check existing budget", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
    
    private fun createNewBudget(userId: String, limit: Double) {
        // Create a new budget object
        val budget = Budget(
            userId = userId,
            categoryId = categoryId,
            month = currentMonth,
            year = currentYear,
            limit = limit
        )
        
        // Save to Firestore
        firestore.collection(Budget.COLLECTION_NAME)
            .add(budget.toMap())
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "Budget added with ID: ${documentReference.id}")
                
                // Check if there are any existing transactions that would exceed this budget
                checkExistingTransactions(categoryId, limit)
                
                Toast.makeText(this, "Budget set successfully", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error adding budget: ${e.message}")
                Toast.makeText(this, "Failed to set budget", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
    
    private fun updateBudget(userId: String, limit: Double) {
        // Update the budget document
        val updates = mapOf(
            "limit" to limit,
            "updated_at" to Timestamp.now()
        )
        
        firestore.collection(Budget.COLLECTION_NAME)
            .document(budgetId)
            .update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "Budget updated successfully")
                
                // Check if the new limit is exceeded by current spending
                firestore.collection(Budget.COLLECTION_NAME)
                    .document(budgetId)
                    .get()
                    .addOnSuccessListener { budgetDoc ->
                        val spent = budgetDoc.getDouble("spent") ?: 0.0
                        if (spent > limit) {
                            (application as FlowMoneyApplication).notificationHelper
                                .notifyBudgetExceeded(categoryName, limit, spent)
                        }
                    }
                
                Toast.makeText(this, "Budget updated successfully", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error updating budget: ${e.message}")
                Toast.makeText(this, "Failed to update budget", Toast.LENGTH_SHORT).show()
                setLoadingState(false)
            }
    }
    
    private fun checkExistingTransactions(categoryId: String, limit: Double) {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.MONTH, currentMonth - 1) // Calendar months are 0-based
        calendar.set(Calendar.YEAR, currentYear)
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        val startOfMonth = calendar.time
        
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH))
        val endOfMonth = calendar.time
        
        // Query transactions for this category in the current month
        firestore.collection("transactions")
            .whereEqualTo("category_id", categoryId)
            .whereEqualTo("type", "expense")
            .whereGreaterThanOrEqualTo("date", Timestamp(startOfMonth))
            .whereLessThanOrEqualTo("date", Timestamp(endOfMonth))
            .get()
            .addOnSuccessListener { documents ->
                var totalSpent = 0.0
                for (doc in documents) {
                    totalSpent += doc.getDouble("amount") ?: 0.0
                }
                
                // If total spent exceeds the new budget, send notification
                if (totalSpent > limit) {
                    (application as FlowMoneyApplication).notificationHelper
                        .notifyBudgetExceeded(categoryName, limit, totalSpent)
                }
            }
    }
    
    private fun setLoadingState(loading: Boolean) {
        if (loading) {
            btnSet.isEnabled = false
            btnCancel.isEnabled = false
            btnSet.text = "SAVING..."
        } else {
            btnSet.isEnabled = true
            btnCancel.isEnabled = true
            btnSet.text = if (editMode) "UPDATE" else "SET"
        }
    }
    
    private fun checkBudgetLimitsAndNotify() {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val userId = currentUser.uid
        
        firestore.collection("budgets")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val categoryId = document.getString("category_id") ?: continue
                    val budgetLimit = document.getDouble("limit") ?: continue
                    val currentAmount = document.getDouble("spent") ?: 0.0
                    
                    // Check if budget is exceeded
                    if (currentAmount > budgetLimit) {
                        // Get category name
                        firestore.collection("categories")
                            .document(categoryId)
                            .get()
                            .addOnSuccessListener { categoryDoc ->
                                val categoryName = categoryDoc.getString("name") ?: "Category"
                                
                                // Send notification for exceeded budget
                                (application as FlowMoneyApplication).notificationHelper.notifyBudgetExceeded(
                                    categoryName, 
                                    budgetLimit, 
                                    currentAmount
                                )
                            }
                    }
                }
            }
    }
    
    private fun saveBudgetToFirestore(budget: Budget) {
        if (budget.categoryId.isBlank() || budget.limit <= 0) {
            Toast.makeText(this, "Invalid budget data", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading indicator
        findViewById<View>(R.id.progressBar).visibility = View.VISIBLE
        
        // Reference to the budget document in Firestore
        val budgetRef = firestore.collection("budgets").document(budget.budgetId)
        
        // Convert Budget object to Map
        val budgetData = budget.toMap()
        
        // Add the budget to Firestore
        budgetRef.set(budgetData)
            .addOnSuccessListener {
                Log.d(TAG, "Budget saved successfully!")
                
                // Check if the budget is already exceeded and notify if so
                if (budget.spent > budget.limit) {
                    // Get category name
                    firestore.collection("categories")
                        .document(budget.categoryId)
                        .get()
                        .addOnSuccessListener { categoryDoc ->
                            val categoryName = categoryDoc.getString("name") ?: "Category"
                            
                            // Send notification for exceeded budget
                            (application as FlowMoneyApplication).notificationHelper.notifyBudgetExceeded(
                                categoryName, 
                                budget.limit, 
                                budget.spent
                            )
                        }
                }
                
                // Hide loading indicator
                findViewById<View>(R.id.progressBar).visibility = View.GONE
                
                // Set result and finish
                setResult(Activity.RESULT_OK)
                finish()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error saving budget", e)
                Toast.makeText(this, "Failed to save budget: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Hide loading indicator
                findViewById<View>(R.id.progressBar).visibility = View.GONE
            }
    }
}