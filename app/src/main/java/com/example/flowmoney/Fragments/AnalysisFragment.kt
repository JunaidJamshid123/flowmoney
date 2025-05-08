package com.example.flowmoney.Fragments

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Activities.TransactionDetails
import com.example.flowmoney.Adapters.AnalysisTransactionAdapter
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.example.flowmoney.utlities.ExpenseChartView
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Calendar
import java.util.Locale

class AnalysisFragment : Fragment() {
    companion object {
        private const val TAG = "AnalysisFragment"
        
        @JvmStatic
        fun newInstance() = AnalysisFragment()
    }
    
    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    
    // UI Components
    private lateinit var spinnerFilter: Spinner
    private lateinit var recyclerViewSpending: RecyclerView
    private lateinit var expenseChart: ExpenseChartView
    private lateinit var dataPointValue: TextView
    private lateinit var dataPointCard: CardView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnSort: ImageButton
    
    // Time period buttons
    private lateinit var btnDay: Button
    private lateinit var btnWeek: Button
    private lateinit var btnMonth: Button
    private lateinit var btnYear: Button
    
    // Month labels
    private lateinit var monthLabels: ViewGroup
    
    // Adapters
    private lateinit var transactionAdapter: AnalysisTransactionAdapter
    
    // Data
    private val allTransactions = mutableListOf<Transaction>()
    private val filteredTransactions = mutableListOf<Transaction>()
    private val categories = mutableMapOf<String, Category>()
    
    // Current state
    private var currentTimePeriod = ExpenseChartView.TimePeriod.MONTH
    private var currentTransactionType = "expense"
    private var isSortedByAmount = true
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_analysis, container, false)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Initialize views
        initializeViews(view)
        
        // Setup time period buttons
        setupTimePeriodButtons()
        
        // Setup filter spinner
        setupFilterSpinner()
        
        // Setup recycler view
        setupRecyclerView()
        
        // Setup chart
        setupChart()
        
        // Load data
        loadData()
        
        return view
    }
    
    private fun initializeViews(view: View) {
        spinnerFilter = view.findViewById(R.id.spinner_filter)
        recyclerViewSpending = view.findViewById(R.id.recycler_view_spending)
        expenseChart = view.findViewById(R.id.expense_chart)
        dataPointValue = view.findViewById(R.id.tv_data_point_value)
        dataPointCard = view.findViewById(R.id.data_point_card)
        progressBar = view.findViewById(R.id.progress_bar)
        btnSort = view.findViewById(R.id.btn_sort)
        
        btnDay = view.findViewById(R.id.btn_day)
        btnWeek = view.findViewById(R.id.btn_week)
        btnMonth = view.findViewById(R.id.btn_month)
        btnYear = view.findViewById(R.id.btn_year)
        
        monthLabels = view.findViewById(R.id.month_labels)
        
        // Setup button listeners
        btnSort.setOnClickListener {
            toggleSortOrder()
        }
    }
    
    private fun setupTimePeriodButtons() {
        val buttons = listOf(btnDay, btnWeek, btnMonth, btnYear)
        
        // Set default selected button
        btnMonth.setBackgroundResource(R.drawable.bg_button_selected)
        btnMonth.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        
        buttons.forEach { button ->
            button.setOnClickListener {
                // Reset all buttons
                buttons.forEach { btn ->
                    btn.setBackgroundResource(R.drawable.bg_button_unselected)
                    btn.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.black))
                }
                
                // Set selected button
                button.setBackgroundResource(R.drawable.bg_button_selected)
                button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                
                // Update time period
                currentTimePeriod = when (button) {
                    btnDay -> ExpenseChartView.TimePeriod.DAY
                    btnWeek -> ExpenseChartView.TimePeriod.WEEK
                    btnYear -> ExpenseChartView.TimePeriod.YEAR
                    else -> ExpenseChartView.TimePeriod.MONTH
                }
                
                // Update month labels visibility
                updateMonthLabelsVisibility()
                
                // Update chart
                updateChart()
            }
        }
    }
    
    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("Expense", "Income", "Saving")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter
        
        // Set default selection to "Expense"
        spinnerFilter.setSelection(0)
        
        // Set listener
        spinnerFilter.setOnItemSelectedListener { _, _, position, _ ->
            currentTransactionType = filterOptions[position].lowercase()
            updateChart()
            filterTransactions()
        }
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = AnalysisTransactionAdapter(
            emptyList(),
            emptyMap()
        ) { transaction ->
            // Navigate to transaction details
            val intent = Intent(requireContext(), TransactionDetails::class.java)
            intent.putExtra(TransactionDetails.EXTRA_TRANSACTION_ID, transaction.transactionId)
            startActivity(intent)
        }
        
        recyclerViewSpending.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
        }
    }
    
    private fun setupChart() {
        // Setup data point selection callback
        expenseChart.onDataPointSelected = { index, value, label ->
            // Update data point indicator
            val format = NumberFormat.getCurrencyInstance(Locale.US)
            dataPointValue.text = format.format(value)
            
            // Make sure data point card is visible
            dataPointCard.visibility = View.VISIBLE
        }
    }
    
    private fun loadData() {
        // Show loading indicator
        showLoading(true)
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // User not logged in
            showLoading(false)
            return
        }
        
        // Clear existing data
        allTransactions.clear()
        categories.clear()
        
        // Load transactions
        loadTransactions(userId)
    }
    
    private fun loadTransactions(userId: String) {
        // Get transactions for the last year
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.YEAR, -1)
        val oneYearAgo = Timestamp(calendar.time)
        
        // Query transactions
        firestore.collection("transactions")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                allTransactions.clear()
                
                for (document in documents) {
                    try {
                        val data = document.data
                        val transaction = Transaction.fromMap(data, document.id)
                        allTransactions.add(transaction)
                        
                        // Add category ID to load if not already loaded
                        if (transaction.categoryId.isNotEmpty() && !categories.containsKey(transaction.categoryId)) {
                            loadCategory(transaction.categoryId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction", e)
                    }
                }
                
                // Initial filter and update
                filterTransactions()
                
                // If we have no categories to load, we're done
                if (categories.isEmpty()) {
                    showLoading(false)
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading transactions", e)
                showLoading(false)
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
                        
                        // Update adapter
                        updateTransactionAdapter()
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
    
    private fun filterTransactions() {
        // Filter by current transaction type
        filteredTransactions.clear()
        filteredTransactions.addAll(allTransactions.filter { 
            it.type.equals(currentTransactionType, ignoreCase = true) 
        })
        
        // Sort transactions
        sortTransactions()
        
        // Update UI
        updateTransactionAdapter()
        updateChart()
    }
    
    private fun sortTransactions() {
        filteredTransactions.sortWith(
            if (isSortedByAmount) {
                compareByDescending { it.amount }
            } else {
                compareByDescending { it.date.seconds }
            }
        )
    }
    
    private fun toggleSortOrder() {
        isSortedByAmount = !isSortedByAmount
        sortTransactions()
        updateTransactionAdapter()
    }
    
    private fun updateTransactionAdapter() {
        transactionAdapter.updateData(filteredTransactions, categories)
    }
    
    private fun updateChart() {
        // Update chart with current data
        expenseChart.updateWithTransactions(
            allTransactions,
            currentTimePeriod,
            currentTransactionType
        )
        
        // Update month labels visibility based on period
        updateMonthLabelsVisibility()
    }
    
    private fun updateMonthLabelsVisibility() {
        // Only show month labels for year view
        monthLabels.visibility = if (currentTimePeriod == ExpenseChartView.TimePeriod.YEAR) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (::progressBar.isInitialized) {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }
}

// Extension function to simplify setting OnItemSelectedListener
private fun Spinner.setOnItemSelectedListener(
    onNothingSelected: (parent: android.widget.AdapterView<*>?) -> Unit = { _ -> },
    onItemSelected: (parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) -> Unit
) {
    this.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
            onItemSelected(parent, view, position, id)
        }
        
        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
            onNothingSelected(parent)
        }
    }
}