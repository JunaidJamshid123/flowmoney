package com.example.flowmoney.Fragments

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.DatePicker
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.flowmoney.AddTransaction
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.example.flowmoney.utlities.OfflineDisplayHelper
import com.example.flowmoney.utlities.OfflineStatusHelper
import com.example.flowmoney.viewmodels.CategoryViewModel
import com.example.flowmoney.viewmodels.NetworkStatusViewModel
import com.example.flowmoney.viewmodels.TransactionViewModel
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class RecordFragment : Fragment() {
    private val TAG = "RecordFragment"

    // View properties
    private lateinit var categorySpinner: Spinner
    private lateinit var sortSpinner: Spinner
    private lateinit var recyclerRecords: RecyclerView
    private lateinit var textTotalBalance: TextView
    private lateinit var textIncome: TextView
    private lateinit var textExpenses: TextView
    private lateinit var textSavings: TextView
    private lateinit var emptyState: View
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var dateFilterButton: View
    private lateinit var dateFilterText: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var emptyStateMessage: TextView

    // Data
    private val transactions = mutableListOf<Transaction>()
    private val categories = mutableListOf<Category>()
    private var transactionAdapter: TransactionAdapter? = null
    private var selectedCategoryPosition = 0
    private var selectedSortPosition = 0
    private var selectedDate: Calendar? = null

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var transactionsListener: ListenerRegistration? = null
    
    // ViewModels for offline support
    private lateinit var transactionViewModel: TransactionViewModel
    private lateinit var categoryViewModel: CategoryViewModel
    private lateinit var networkStatusViewModel: NetworkStatusViewModel
    private lateinit var offlineStatusHelper: OfflineStatusHelper
    private lateinit var offlineDisplayHelper: OfflineDisplayHelper

    // Activity result launcher for AddTransaction
    private val addTransactionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            // Refresh transactions when returning from AddTransaction
            loadTransactions()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_record, container, false)

        // Initialize Firebase
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        
        // Initialize ViewModels
        initViewModels()

        // Initialize views
        initViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup spinners
        setupSpinners()

        // Setup click listeners
        setupClickListeners()
        
        // Setup offline status helper
        setupOfflineSupport(view)

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
        
        // Initialize the offline indicator in the empty view
        val emptyStateContainer = view.findViewById<ViewGroup>(R.id.empty_state_container)
        if (emptyStateContainer != null) {
            emptyStateContainer.removeAllViews()
            emptyStateContainer.addView(
                offlineStatusHelper.createOfflineEmptyView(emptyStateContainer)
            )
        }
        
        // Find empty state message text view
        emptyStateMessage = view.findViewById(R.id.empty_state_message)
        
        // Observe network changes to update UI
        networkStatusViewModel.getNetworkStatus().observe(viewLifecycleOwner) { isOnline ->
            swipeRefreshLayout.isEnabled = isOnline
            if (!isOnline) {
                swipeRefreshLayout.isRefreshing = false
            }
            
            // Update empty state message if needed
            if (transactions.isEmpty()) {
                showEmptyState(if (isOnline) "No transactions yet" else "You're offline. Your transactions will appear here.")
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Check if user is logged in
        if (auth.currentUser == null) {
            showEmptyState("Please log in to view transactions")
            return
        }

        // Load categories first, then transactions
        loadCategories()
    }

    private fun initViews(view: View) {
        categorySpinner = view.findViewById(R.id.category_spinner)
        sortSpinner = view.findViewById(R.id.sort_spinner)
        recyclerRecords = view.findViewById(R.id.recycler_records)
        textTotalBalance = view.findViewById(R.id.text_total_balance)
        textIncome = view.findViewById(R.id.text_income)
        textExpenses = view.findViewById(R.id.text_expenses)
        textSavings = view.findViewById(R.id.text_savings)
        emptyState = view.findViewById(R.id.empty_state)
        fabAdd = view.findViewById(R.id.fab_add)
        dateFilterButton = view.findViewById(R.id.date_filter_button)
        dateFilterText = view.findViewById(R.id.date_filter_text)
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout)
        
        // Initialize swipe refresh
        swipeRefreshLayout.setOnRefreshListener {
            refreshData()
        }
    }
    
    private fun refreshData() {
        val userId = auth.currentUser?.uid ?: return
        
        CoroutineScope(Dispatchers.Main).launch {
            try {
                withContext(Dispatchers.IO) {
                    networkStatusViewModel.refreshData(userId)
                }
                // Data will be automatically refreshed by LiveData observers
            } catch (e: Exception) {
                Toast.makeText(context, "Error refreshing data: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter(transactions, categories) { transaction ->
            // Launch transaction details activity
            val intent = Intent(requireContext(), com.example.flowmoney.Activities.TransactionDetails::class.java)
            intent.putExtra(com.example.flowmoney.Activities.TransactionDetails.EXTRA_TRANSACTION_ID, transaction.transactionId)
            startActivity(intent)
        }

        recyclerRecords.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = transactionAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupSpinners() {
        // Category spinner will be populated after loading categories from Firestore

        // Sort spinner setup
        val sortOptions = arrayOf("Newest", "Oldest", "Highest", "Lowest")
        val sortAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            sortOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        sortSpinner.adapter = sortAdapter
    }

    private fun setupClickListeners() {
        fabAdd.setOnClickListener {
            launchAddTransaction()
        }

        // Add listeners for your spinners/filters
        categorySpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategoryPosition = position
                filterTransactions()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }

        sortSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedSortPosition = position
                filterTransactions()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                // Do nothing
            }
        }
        
        // Setup date filter button
        dateFilterButton.setOnClickListener {
            showDatePicker()
        }
    }
    
    private fun showDatePicker() {
        val calendar = selectedDate ?: Calendar.getInstance()
        
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                // Update the selected date
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                selectedDate = newCalendar
                
                // Update the filter text
                updateDateFilterText()
                
                // Apply the date filter
                filterTransactions()
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        
        // Add a clear button to remove date filter
        datePickerDialog.setButton(
            DatePickerDialog.BUTTON_NEUTRAL, 
            "Clear Filter"
        ) { _, _ ->
            selectedDate = null
            updateDateFilterText()
            filterTransactions()
        }
        
        datePickerDialog.show()
    }
    
    private fun updateDateFilterText() {
        if (selectedDate != null) {
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            dateFilterText.text = dateFormat.format(selectedDate!!.time)
            dateFilterText.visibility = View.VISIBLE
        } else {
            dateFilterText.visibility = View.GONE
        }
    }
    
    private fun loadCategories() {
        val userId = auth.currentUser?.uid ?: return
        
        // Use ViewModel to get categories from local database
        categoryViewModel.getAllCategories(userId).observe(viewLifecycleOwner) { categoryList ->
            categories.clear()
            
            // Add "All" category at the beginning
            val allCategory = Category()
            allCategory.categoryId = "all"
            allCategory.name = "All Categories"
            categories.add(allCategory)
            
            // Add categories from database
            categories.addAll(categoryList)
            
            // Update category spinner
            updateCategorySpinner()
            
            // Load transactions after categories are loaded
            loadTransactions()
        }
    }

    private fun updateCategorySpinner() {
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories.map { it.name }
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = categoryAdapter
    }

    private fun launchAddTransaction() {
        val intent = Intent(requireContext(), AddTransaction::class.java)
        addTransactionLauncher.launch(intent)
    }

    private fun loadTransactions() {
        val userId = auth.currentUser?.uid ?: return
        
        // Use ViewModel to get transactions from local database
        transactionViewModel.getAllTransactions(userId).observe(viewLifecycleOwner) { transactionList ->
            transactions.clear()
            transactions.addAll(transactionList)
            
            // Filter and sort based on current selection
            filterTransactions()
            
            // Calculate summary values using the helper
            val summary = offlineDisplayHelper.calculateSummary(transactions)
            updateSummaryValues(summary.first, summary.second, summary.third)
            
            // Show/hide empty state
            if (transactions.isEmpty()) {
                val isOnline = networkStatusViewModel.getNetworkStatus().value ?: false
                showEmptyState(if (isOnline) "No transactions yet" else "You're offline. Your transactions will appear here.")
            } else {
                hideEmptyState()
            }
        }
    }

    private fun filterTransactions() {
        // Skip if adapter isn't initialized yet
        if (transactionAdapter == null) return
        
        // Make a copy of all transactions
        val filteredList = transactions.toMutableList()
        
        // Apply category filter if not "All"
        if (selectedCategoryPosition > 0 && categories.size > selectedCategoryPosition) {
            val selectedCategory = categories[selectedCategoryPosition]
            filteredList.retainAll { it.categoryId == selectedCategory.categoryId }
        }
        
        // Apply date filter if selected
        selectedDate?.let { calendar ->
            // Set time to beginning of day
            val startOfDay = Calendar.getInstance()
            startOfDay.timeInMillis = calendar.timeInMillis
            startOfDay.set(Calendar.HOUR_OF_DAY, 0)
            startOfDay.set(Calendar.MINUTE, 0)
            startOfDay.set(Calendar.SECOND, 0)
            
            // Set time to end of day
            val endOfDay = Calendar.getInstance()
            endOfDay.timeInMillis = calendar.timeInMillis
            endOfDay.set(Calendar.HOUR_OF_DAY, 23)
            endOfDay.set(Calendar.MINUTE, 59)
            endOfDay.set(Calendar.SECOND, 59)
            
            // Create Timestamps for comparison
            val startTimestamp = Timestamp(startOfDay.time)
            val endTimestamp = Timestamp(endOfDay.time)
            
            // Filter transactions within the day
            filteredList.retainAll { 
                it.date.compareTo(startTimestamp) >= 0 && it.date.compareTo(endTimestamp) <= 0 
            }
        }
        
        // Apply sorting
        when (selectedSortPosition) {
            0 -> filteredList.sortByDescending { it.date.seconds } // Newest
            1 -> filteredList.sortBy { it.date.seconds } // Oldest
            2 -> filteredList.sortByDescending { it.amount } // Highest
            3 -> filteredList.sortBy { it.amount } // Lowest
        }
        
        // Update adapter with filtered list
        transactionAdapter?.updateTransactions(filteredList)
        
        // Show/hide empty state
        if (filteredList.isEmpty()) {
            showEmptyState("No transactions match your filters")
        } else {
            hideEmptyState()
        }
    }

    private fun updateSummaryValues(income: Double, expense: Double, savings: Double) {
        // Calculate total balance as income - (expense + savings)
        val calculatedBalance = income - (expense + savings)
        
        // Display calculated balance (not from AccountsFragment)
        textTotalBalance.text = String.format("$%,.2f", calculatedBalance)
        
        // Display income, expenses and savings
        textIncome.text = String.format("$%.2f", income)
        textExpenses.text = String.format("$%.2f", expense)
        textSavings.text = String.format("$%.2f", savings)
    }

    private fun showEmptyState(message: String) {
        // Set empty state message
        if (::emptyStateMessage.isInitialized) {
            emptyStateMessage.text = message
        }
        emptyState.visibility = View.VISIBLE
        recyclerRecords.visibility = View.GONE
    }
    
    private fun hideEmptyState() {
        emptyState.visibility = View.GONE
        recyclerRecords.visibility = View.VISIBLE
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up listener to prevent memory leaks
        transactionsListener?.remove()
    }

    override fun onResume() {
        super.onResume()
        // Refresh data when returning to the fragment
        if (auth.currentUser != null) {
            loadTransactions()
        }
    }

    /**
     * Helper method to convert base64 string to bitmap
     */
    private fun base64ToBitmap(base64String: String?): Bitmap? {
        if (base64String.isNullOrEmpty()) return null
        
        return try {
            val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e(TAG, "Error converting base64 to bitmap", e)
            null
        }
    }

    companion object {
        @JvmStatic
        fun newInstance() = RecordFragment()
    }
    
    /**
     * Adapter for transactions
     */
    inner class TransactionAdapter(
        private var transactionList: List<Transaction>,
        private val categoryList: List<Category>,
        private val onItemClick: (Transaction) -> Unit
    ) : RecyclerView.Adapter<TransactionAdapter.TransactionViewHolder>() {
        
        fun updateTransactions(newTransactions: List<Transaction>) {
            transactionList = newTransactions
            notifyDataSetChanged()
        }
        
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TransactionViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_transaction, parent, false)
            return TransactionViewHolder(view)
        }
        
        override fun onBindViewHolder(holder: TransactionViewHolder, position: Int) {
            val transaction = transactionList[position]
            holder.bind(transaction)
        }
        
        override fun getItemCount() = transactionList.size
        
        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val textName: TextView = itemView.findViewById(R.id.text_name)
            private val textAmount: TextView = itemView.findViewById(R.id.text_amount)
            private val textDate: TextView = itemView.findViewById(R.id.text_date)
            private val textCategory: TextView = itemView.findViewById(R.id.text_category)
            private val imageCategory: ImageView = itemView.findViewById(R.id.image_category)
            
            fun bind(transaction: Transaction) {
                // Set transaction notes or description
                textName.text = transaction.notes.ifEmpty { "Transaction" }
                
                // Set amount with appropriate formatting based on transaction type
                val amount = transaction.amount
                val formattedAmount = String.format("$%.2f", amount)
                textAmount.text = when (transaction.type) {
                    "income" -> "+$formattedAmount"
                    "expense" -> "-$formattedAmount"
                    "saving" -> "-$formattedAmount (Saving)"
                    else -> formattedAmount
                }
                
                // Set text color based on transaction type
                val colorRes = when (transaction.type) {
                    "income" -> android.R.color.holo_green_dark
                    "expense", "saving" -> android.R.color.holo_red_dark
                    else -> android.R.color.black
                }
                textAmount.setTextColor(resources.getColor(colorRes, null))
                
                // Format date
                val date = transaction.date.toDate()
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                textDate.text = dateFormat.format(date)
                
                // Find category
                val category = categoryList.find { it.categoryId == transaction.categoryId }
                textCategory.text = category?.name ?: "Uncategorized"
                
                // Set category icon if available
                if (category != null && category.iconBase64.isNotEmpty()) {
                    val bitmap = base64ToBitmap(category.iconBase64)
                    if (bitmap != null) {
                        imageCategory.setImageBitmap(bitmap)
                    } else {
                        // Default icon if bitmap creation fails
                        imageCategory.setImageResource(R.drawable.ic_menu_camera)
                    }
                } else {
                    // Default icon if no category or icon
                    imageCategory.setImageResource(R.drawable.ic_menu_camera)
                }
                
                // Set click listener
                itemView.setOnClickListener {
                    onItemClick(transaction)
                }
            }
        }
    }
}