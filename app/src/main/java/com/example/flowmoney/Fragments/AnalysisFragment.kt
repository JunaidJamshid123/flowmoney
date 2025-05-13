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
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Activities.TransactionDetails
import com.example.flowmoney.Adapters.AnalysisTransactionAdapter
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.example.flowmoney.utlities.ExpenseChartView
import com.example.flowmoney.utlities.OfflineDisplayHelper
import com.example.flowmoney.utlities.OfflineStatusHelper
import com.example.flowmoney.viewmodels.CategoryViewModel
import com.example.flowmoney.viewmodels.NetworkStatusViewModel
import com.example.flowmoney.viewmodels.TransactionViewModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    private lateinit var emptyStateView: View
    private lateinit var emptyStateMessage: TextView
    
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
    
    // ViewModels for offline support
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var networkStatusViewModel: NetworkStatusViewModel
    private lateinit var offlineStatusHelper: OfflineStatusHelper
    private lateinit var offlineDisplayHelper: OfflineDisplayHelper
    
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_analysis, container, false)
        
        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Initialize ViewModels
        initViewModels()
        
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
        
        // Setup offline support
        setupOfflineSupport(view)
        
        // Load data
        loadData()
        
        return view
    }
    
    private fun initViewModels() {
        transactionViewModel = ViewModelProvider(requireActivity())[TransactionViewModel::class.java]
        categoryViewModel = ViewModelProvider(requireActivity())[CategoryViewModel::class.java]
        networkStatusViewModel = ViewModelProvider(requireActivity())[NetworkStatusViewModel::class.java]
        
        // Initialize offline display helper
        offlineDisplayHelper = OfflineDisplayHelper(requireContext(), networkStatusViewModel)
    }
    
    private fun setupOfflineSupport(view: View) {
        offlineStatusHelper = OfflineStatusHelper(
            requireContext(),
            networkStatusViewModel,
            viewLifecycleOwner
        )
        
        // Find empty state elements
        emptyStateView = view.findViewById(R.id.empty_state_container) ?: return
        emptyStateMessage = view.findViewById(R.id.empty_state_message) ?: return
        
        // Observe network changes
        networkStatusViewModel.getNetworkStatus().observe(viewLifecycleOwner) { isOnline ->
            if (filteredTransactions.isEmpty()) {
                updateEmptyState(if (isOnline) "No transactions found" else "You're offline. Data will sync when connection is restored.")
            }
        }
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
        
        // Initialize empty state
        try {
            emptyStateView = view.findViewById(R.id.empty_state_container)
            emptyStateMessage = view.findViewById(R.id.empty_state_message)
        } catch (e: Exception) {
            Log.e(TAG, "Empty state views not found", e)
        }
        
        // Setup button listeners
        btnSort.setOnClickListener {
            toggleSortOrder()
        }
        
        // Initially hide data point card
        dataPointCard.visibility = View.GONE
    }
    
    private fun setupTimePeriodButtons() {
        val buttons = listOf(btnDay, btnWeek, btnMonth, btnYear)
        
        // Set default selected button
        selectTimePeriodButton(btnMonth)
        
        buttons.forEach { button ->
            button.setOnClickListener {
                // Update UI for selected button
                selectTimePeriodButton(button)
                
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
                
                // Filter transactions
                filterTransactions()
            }
        }
    }
    
    private fun selectTimePeriodButton(selectedButton: Button) {
        val buttons = listOf(btnDay, btnWeek, btnMonth, btnYear)
        
        // Reset all buttons
        buttons.forEach { button ->
            button.setBackgroundResource(R.drawable.bg_button_unselected)
            button.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }
        
        // Set selected button
        selectedButton.setBackgroundResource(R.drawable.bg_button_selected)
        selectedButton.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
    }
    
    private fun setupFilterSpinner() {
        val filterOptions = arrayOf("Expense", "Income", "Saving")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFilter.adapter = adapter
        
        // Set default selection to "Expense"
        spinnerFilter.setSelection(0)
        
        // Set listener
        spinnerFilter.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                currentTransactionType = filterOptions[position].lowercase()
                updateChart()
                filterTransactions()
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
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
            setHasFixedSize(true)
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
        
        // Initially hide the data point card until a point is selected
        dataPointCard.visibility = View.GONE
    }
    
    private fun loadData() {
        // Show loading indicator
        showLoading(true)
        
        val userId = auth.currentUser?.uid
        if (userId == null) {
            // User not logged in
            showLoading(false)
            updateEmptyState("Please log in to view analysis")
            return
        }
        
        // Load categories from view model
        categoryViewModel.getAllCategories(userId).observe(viewLifecycleOwner) { categoryList ->
            categories.clear()
            // Convert list to map for easy lookup
            for (category in categoryList) {
                categories[category.categoryId] = category
            }
            
            // Load transactions now that we have categories
            loadTransactions(userId)
        }
    }
    
    private fun loadTransactions(userId: String) {
        // Use ViewModel to get transactions from local database
        transactionViewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { transactionList ->
            allTransactions.clear()
            allTransactions.addAll(transactionList)
            
            // Filter and update UI
            filterTransactions()
            updateChart()
            
            // Hide loading indicator
            showLoading(false)
            
            // Update empty state if necessary
            if (allTransactions.isEmpty()) {
                val isOnline = networkStatusViewModel.getNetworkStatus().value ?: false
                updateEmptyState(if (isOnline) "No transactions found" else "You're offline. Data will sync when connection is restored.")
            } else {
                hideEmptyState()
            }
        }
    }
    
    private fun filterTransactions() {
        // Filter by current transaction type
        filteredTransactions.clear()
        
        // First filter by transaction type
        val typeFiltered = allTransactions.filter { 
            it.type.equals(currentTransactionType, ignoreCase = true)
        }
        
        // Then filter by time period
        val calendar = Calendar.getInstance()
        val today = Calendar.getInstance()
        
        val timeFiltered = when (currentTimePeriod) {
            ExpenseChartView.TimePeriod.DAY -> {
                // Only today's transactions
                typeFiltered.filter {
                    val transactionCal = Calendar.getInstance()
                    transactionCal.time = it.getDateAsDate()
                    
                    transactionCal.get(Calendar.YEAR) == today.get(Calendar.YEAR) &&
                    transactionCal.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
                }
            }
            ExpenseChartView.TimePeriod.WEEK -> {
                // Last 7 days
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val weekAgo = calendar.timeInMillis
                
                typeFiltered.filter {
                    it.getDateAsDate().time >= weekAgo
                }
            }
            ExpenseChartView.TimePeriod.MONTH -> {
                // Last 30 days
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val monthAgo = calendar.timeInMillis
                
                typeFiltered.filter {
                    it.getDateAsDate().time >= monthAgo
                }
            }
            ExpenseChartView.TimePeriod.YEAR -> {
                // Last 365 days
                calendar.add(Calendar.DAY_OF_YEAR, -365)
                val yearAgo = calendar.timeInMillis
                
                typeFiltered.filter {
                    it.getDateAsDate().time >= yearAgo
                }
            }
        }
        
        filteredTransactions.addAll(timeFiltered)
        
        // Sort transactions
        sortTransactions()
        
        // Update UI
        updateTransactionAdapter()
        
        // Update empty state if needed
        if (filteredTransactions.isEmpty()) {
            updateEmptyState("No ${currentTransactionType.capitalize(Locale.ROOT)} transactions found for this time period")
        } else {
            hideEmptyState()
        }
        
        // Update title in the top spending section
        updateTopSpendingTitle()
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
        
        // Update sort button icon
        btnSort.setImageResource(
            if (isSortedByAmount) R.drawable.ic_sort else R.drawable.sort_descending
        )
        
        // Show toast message
        Toast.makeText(
            context,
            if (isSortedByAmount) "Sorted by amount" else "Sorted by date",
            Toast.LENGTH_SHORT
        ).show()
    }
    
    private fun updateTransactionAdapter() {
        transactionAdapter.updateData(filteredTransactions, categories)
        
        // Check if we need to show empty state
        if (filteredTransactions.isEmpty()) {
            recyclerViewSpending.visibility = View.GONE
            if (::emptyStateView.isInitialized) {
                emptyStateView.visibility = View.VISIBLE
            }
        } else {
            recyclerViewSpending.visibility = View.VISIBLE
            if (::emptyStateView.isInitialized) {
                emptyStateView.visibility = View.GONE
            }
        }
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
    
    private fun updateTopSpendingTitle() {
        val topSpendingText = view?.findViewById<TextView>(R.id.tv_top_spending)
        topSpendingText?.text = when (currentTimePeriod) {
            ExpenseChartView.TimePeriod.DAY -> "Today's ${currentTransactionType.capitalize(Locale.ROOT)}"
            ExpenseChartView.TimePeriod.WEEK -> "This Week's ${currentTransactionType.capitalize(Locale.ROOT)}"
            ExpenseChartView.TimePeriod.YEAR -> "This Year's ${currentTransactionType.capitalize(Locale.ROOT)}"
            else -> "This Month's ${currentTransactionType.capitalize(Locale.ROOT)}"
        }
    }
    
    private fun updateEmptyState(message: String) {
        if (::emptyStateView.isInitialized && ::emptyStateMessage.isInitialized) {
            emptyStateView.visibility = View.VISIBLE
            emptyStateMessage.text = message
            recyclerViewSpending.visibility = View.GONE
        }
    }
    
    private fun hideEmptyState() {
        if (::emptyStateView.isInitialized) {
            emptyStateView.visibility = View.GONE
            recyclerViewSpending.visibility = View.VISIBLE
        }
    }
    
    private fun showLoading(show: Boolean) {
        if (::progressBar.isInitialized) {
            progressBar.visibility = if (show) View.VISIBLE else View.GONE
        }
    }
    
    override fun onResume() {
        super.onResume()
        
        // Refresh data when returning to the fragment
        if (auth.currentUser != null) {
            loadData()
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
