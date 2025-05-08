package com.example.flowmoney.Activities

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Activities.TransactionDetails
import com.example.flowmoney.Adapters.AnalysisTransactionAdapter
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class HistoryActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "HistoryActivity"
    }

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    // UI Components
    private lateinit var backButton: CardView
    private lateinit var progressBar: ProgressBar
    private lateinit var emptyState: LinearLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var transactionCount: TextView
    private lateinit var monthText: TextView
    private lateinit var emptyText: TextView
    private lateinit var dateFilterButton: View

    // Summary cards components
    private lateinit var incomeAmount: TextView
    private lateinit var expenseAmount: TextView
    private lateinit var savingAmount: TextView

    // Filter buttons
    private lateinit var btnAll: Button
    private lateinit var btnIncome: Button
    private lateinit var btnExpense: Button
    private lateinit var btnSaving: Button

    // Data
    private val allTransactions = mutableListOf<Transaction>()
    private val filteredTransactions = mutableListOf<Transaction>()
    private val categories = mutableMapOf<String, Category>()
    
    // Filter state
    private var currentTransactionType: String? = null // null means "All"
    private var currentDateFilter: Date? = null // null means "All Time"
    private var isCalendarFiltered = false

    // Adapter
    private lateinit var transactionAdapter: AnalysisTransactionAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_history)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Initialize views
        initializeViews()
        setupListeners()
        setupRecyclerView()

        // Load transactions
        loadTransactions()
    }

    private fun initializeViews() {
        // Basic views
        backButton = findViewById(R.id.back_button)
        progressBar = findViewById(R.id.progress_bar)
        emptyState = findViewById(R.id.empty_state)
        recyclerView = findViewById(R.id.history_recycler_view)
        transactionCount = findViewById(R.id.transaction_count)
        monthText = findViewById(R.id.month_text)
        emptyText = findViewById(R.id.empty_text)
        dateFilterButton = findViewById(R.id.btn_date_filter)

        // Summary cards
        incomeAmount = findViewById(R.id.income_amount)
        expenseAmount = findViewById(R.id.expense_amount)
        savingAmount = findViewById(R.id.saving_amount)

        // Filter buttons
        btnAll = findViewById(R.id.btn_all)
        btnIncome = findViewById(R.id.btn_income)
        btnExpense = findViewById(R.id.btn_expense)
        btnSaving = findViewById(R.id.btn_saving)
    }

    private fun setupListeners() {
        // Back button
        backButton.setOnClickListener {
            finish()
        }

        // Filter buttons
        btnAll.setOnClickListener { 
            setActiveFilterButton(null)
            applyFilters()
        }
        
        btnIncome.setOnClickListener { 
            setActiveFilterButton("income") 
            applyFilters()
        }
        
        btnExpense.setOnClickListener { 
            setActiveFilterButton("expense") 
            applyFilters()
        }
        
        btnSaving.setOnClickListener { 
            setActiveFilterButton("saving") 
            applyFilters()
        }

        // Date filter
        dateFilterButton.setOnClickListener {
            showDateFilterDialog()
        }

        // Reset date filter by clicking on month text
        monthText.setOnClickListener {
            if (isCalendarFiltered) {
                isCalendarFiltered = false
                currentDateFilter = null
                monthText.text = "All Time"
                applyFilters()
            }
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = AnalysisTransactionAdapter(
            emptyList(),
            emptyMap()
        ) { transaction ->
            // Navigate to transaction details
            val intent = Intent(this, TransactionDetails::class.java)
            intent.putExtra(TransactionDetails.EXTRA_TRANSACTION_ID, transaction.transactionId)
            startActivity(intent)
        }

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@HistoryActivity)
            adapter = transactionAdapter
        }
    }

    private fun loadTransactions() {
        showLoading(true)

        val userId = auth.currentUser?.uid
        if (userId == null) {
            showLoading(false)
            showEmptyState("You need to be logged in to view your history")
            return
        }

        // Clear existing data
        allTransactions.clear()
        categories.clear()

        // Query transactions
        firestore.collection("transactions")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    try {
                        val data = document.data
                        val transaction = Transaction.fromMap(data, document.id)
                        
                        // Only add non-deleted transactions
                        if (!transaction.isDeleted) {
                            allTransactions.add(transaction)
                            
                            // Add category ID to load if not already loaded
                            if (transaction.categoryId.isNotEmpty() && !categories.containsKey(transaction.categoryId)) {
                                loadCategory(transaction.categoryId)
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction", e)
                    }
                }
                
                // Log transaction count for debugging
                Log.d(TAG, "Loaded ${allTransactions.size} transactions")
                
                // Apply initial filtering
                applyFilters()
                
                // Update summary amounts
                updateSummaryAmounts()
                
                // If we have no categories to load, we're done
                if (categories.isEmpty()) {
                    showLoading(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading transactions", e)
                showLoading(false)
                showEmptyState("Error loading transactions: ${e.message}")
            }
    }
    
    private fun loadCategory(categoryId: String) {
        firestore.collection("categories")
            .document(categoryId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    try {
                        val category = Category()
                        category.categoryId = document.getString("category_id") ?: ""
                        category.userId = document.getString("user_id") ?: ""
                        category.name = document.getString("name") ?: ""
                        category.iconBase64 = document.getString("icon_base64") ?: ""
                        category.isIncome = document.getBoolean("is_income") ?: false
                        
                        // Add to categories map
                        categories[categoryId] = category
                        
                        // Update adapter after loading all categories
                        transactionAdapter.updateData(filteredTransactions, categories)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing category", e)
                    }
                }
                
                // If this was the last category to load, hide loading indicator
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading category", e)
                showLoading(false)
            }
    }

    private fun applyFilters() {
        // Clear filtered transactions
        filteredTransactions.clear()
        
        // Apply transaction type filter
        val typeFiltered = if (currentTransactionType == null) {
            // "All" selected - no type filtering
            allTransactions
        } else {
            // Filter by selected type
            allTransactions.filter { it.type == currentTransactionType }
        }
        
        // Apply date filter if set
        val dateFiltered = if (currentDateFilter == null) {
            // "All Time" selected - no date filtering
            typeFiltered
        } else {
            // Filter by selected date (only show transactions from that day)
            typeFiltered.filter { isSameDay(it.getDateAsDate(), currentDateFilter!!) }
        }
        
        // Update filtered list
        filteredTransactions.addAll(dateFiltered)
        
        // Update UI
        updateTransactionAdapter()
        updateTransactionCount()

        // Show or hide empty state
        if (filteredTransactions.isEmpty()) {
            showEmptyState("No transactions found")
        } else {
            hideEmptyState()
        }
    }
    
    private fun isSameDay(date1: Date, date2: Date): Boolean {
        val cal1 = Calendar.getInstance().apply { time = date1 }
        val cal2 = Calendar.getInstance().apply { time = date2 }
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.MONTH) == cal2.get(Calendar.MONTH) &&
                cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
    }
    
    private fun updateTransactionAdapter() {
        transactionAdapter.updateData(filteredTransactions, categories)
    }
    
    private fun updateTransactionCount() {
        val count = filteredTransactions.size
        transactionCount.text = "$count ${if (count == 1) "transaction" else "transactions"}"
    }
    
    private fun updateSummaryAmounts() {
        // Calculate total income
        val totalIncome = allTransactions
            .filter { it.type == "income" }
            .sumOf { it.amount }
            
        // Calculate total expense
        val totalExpense = allTransactions
            .filter { it.type == "expense" }
            .sumOf { it.amount }
            
        // Calculate total savings
        val totalSavings = allTransactions
            .filter { it.type == "saving" }
            .sumOf { it.amount }
            
        // Format and update UI
        val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.US)
        incomeAmount.text = currencyFormatter.format(totalIncome)
        expenseAmount.text = currencyFormatter.format(totalExpense)
        savingAmount.text = currencyFormatter.format(totalSavings)
    }
    
    private fun setActiveFilterButton(type: String?) {
        // Update current filter
        currentTransactionType = type
        
        // Reset all buttons
        btnAll.setBackgroundResource(R.drawable.bg_button_unselected)
        btnAll.setTextColor(getColor(android.R.color.black))
        
        btnIncome.setBackgroundResource(R.drawable.bg_button_unselected)
        btnIncome.setTextColor(getColor(android.R.color.black))
        
        btnExpense.setBackgroundResource(R.drawable.bg_button_unselected)
        btnExpense.setTextColor(getColor(android.R.color.black))
        
        btnSaving.setBackgroundResource(R.drawable.bg_button_unselected)
        btnSaving.setTextColor(getColor(android.R.color.black))
        
        // Set the active button
        when (type) {
            "income" -> {
                btnIncome.setBackgroundResource(R.drawable.bg_button_selected)
                btnIncome.setTextColor(getColor(android.R.color.white))
            }
            "expense" -> {
                btnExpense.setBackgroundResource(R.drawable.bg_button_selected)
                btnExpense.setTextColor(getColor(android.R.color.white))
            }
            "saving" -> {
                btnSaving.setBackgroundResource(R.drawable.bg_button_selected)
                btnSaving.setTextColor(getColor(android.R.color.white))
            }
            else -> {
                btnAll.setBackgroundResource(R.drawable.bg_button_selected)
                btnAll.setTextColor(getColor(android.R.color.white))
            }
        }
    }
    
    private fun showDateFilterDialog() {
        val calendar = Calendar.getInstance()
        
        // Use current date if no filter is set
        if (currentDateFilter != null) {
            calendar.time = currentDateFilter!!
        }
        
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(Calendar.YEAR, year)
                calendar.set(Calendar.MONTH, month)
                calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
                
                // Update the filter
                currentDateFilter = calendar.time
                isCalendarFiltered = true
                
                // Update UI
                val dateFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                monthText.text = dateFormat.format(currentDateFilter!!)
                
                // Apply filters
                applyFilters()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        datePickerDialog.show()
    }
    
    private fun showLoading(show: Boolean) {
        progressBar.visibility = if (show) View.VISIBLE else View.GONE
    }
    
    private fun showEmptyState(message: String) {
        emptyText.text = message
        emptyState.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
    }
    
    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        recyclerView.visibility = View.VISIBLE
    }
}