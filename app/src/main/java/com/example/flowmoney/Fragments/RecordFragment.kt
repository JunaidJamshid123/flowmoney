package com.example.flowmoney.Fragments

import android.app.Activity
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
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.AddTransaction
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
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

    // Data
    private val transactions = mutableListOf<Transaction>()
    private val categories = mutableListOf<Category>()
    private var transactionAdapter: TransactionAdapter? = null
    private var selectedCategoryPosition = 0
    private var selectedSortPosition = 0

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var transactionsListener: ListenerRegistration? = null

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

        // Initialize views
        initViews(view)

        // Setup RecyclerView
        setupRecyclerView()

        // Setup spinners
        setupSpinners()

        // Setup click listeners
        setupClickListeners()

        return view
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
    }
    
    private fun loadCategories() {
        val userId = auth.currentUser?.uid ?: return
        
        firestore.collection("categories")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { documents ->
                categories.clear()
                
                // Add "All" category at the beginning
                val allCategory = Category()
                allCategory.categoryId = "all"
                allCategory.name = "All Categories"
                categories.add(allCategory)
                
                // Add categories from Firestore
                for (document in documents) {
                    try {
                        val category = Category()
                        category.categoryId = document.getString("category_id") ?: ""
                        category.userId = document.getString("user_id") ?: ""
                        category.name = document.getString("name") ?: ""
                        category.iconBase64 = document.getString("icon_base64") ?: ""
                        category.isIncome = document.getBoolean("is_income") ?: false
                        
                        categories.add(category)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing category", e)
                    }
                }
                
                // Update category spinner
                updateCategorySpinner()
                
                // Load transactions after categories are loaded
                loadTransactions()
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading categories", e)
                Toast.makeText(context, "Failed to load categories: ${e.message}", Toast.LENGTH_SHORT).show()
                // Load transactions anyway
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
        
        // Remove any existing listener
        transactionsListener?.remove()
        
        // Create query for transactions
        val query = firestore.collection("transactions")
            .whereEqualTo("user_id", userId)
            .whereEqualTo("is_deleted", false)
            .orderBy("date", Query.Direction.DESCENDING)
        
        // Add real-time listener
        transactionsListener = query.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e(TAG, "Error listening for transactions", e)
                showEmptyState("Error loading transactions")
                return@addSnapshotListener
            }
            
            if (snapshot == null || snapshot.isEmpty) {
                transactions.clear()
                transactionAdapter?.notifyDataSetChanged()
                showEmptyState("No transactions yet")
                updateSummaryValues(0.0, 0.0, 0.0)
                return@addSnapshotListener
            }
            
            // Parse transactions
            val fetchedTransactions = snapshot.documents.mapNotNull { doc ->
                try {
                    val data = doc.data ?: return@mapNotNull null
                    Transaction.fromMap(data, doc.id)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing transaction", e)
                    null
                }
            }
            
            // Update transactions list
            transactions.clear()
            transactions.addAll(fetchedTransactions)
            
            // Filter and sort based on current selection
            filterTransactions()
            
            // Update summary values
            updateSummaryValues(
                income = transactions.filter { it.type == "income" }.sumOf { it.amount },
                expense = transactions.filter { it.type == "expense" }.sumOf { it.amount },
                savings = transactions.filter { it.type == "saving" }.sumOf { it.amount }
            )
            
            // Show/hide empty state
            if (transactions.isEmpty()) {
                showEmptyState("No transactions yet")
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
        val balance = income - expense - savings
        
        textTotalBalance.text = String.format("$%.2f", balance)
        textIncome.text = String.format("$%.2f", income)
        textExpenses.text = String.format("$%.2f", expense)
        textSavings.text = String.format("$%.2f", savings)
    }

    private fun showEmptyState(message: String) {
        // Set empty state message here if you have a text view for it
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
        
        override fun getItemCount(): Int = transactionList.size
        
        inner class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val categoryIcon: ImageView = itemView.findViewById(R.id.transaction_icon)
            private val categoryName: TextView = itemView.findViewById(R.id.category_name)
            private val transactionDate: TextView = itemView.findViewById(R.id.transaction_date)
            private val transactionAmount: TextView = itemView.findViewById(R.id.transaction_amount)
            
            fun bind(transaction: Transaction) {
                // Find category
                val category = categoryList.find { it.categoryId == transaction.categoryId }
                
                // Set category name
                categoryName.text = category?.name ?: "Unknown"
                
                // Set date
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                transactionDate.text = dateFormat.format(transaction.getDateAsDate())
                
                // Set amount with appropriate color and sign
                val amount = transaction.getSignedAmount()
                transactionAmount.text = String.format("$%,.2f", amount)
                
                // Set color based on transaction type
                val textColor = when (transaction.type) {
                    "income" -> getColorFromResource(R.color.income_green) 
                    "expense" -> getColorFromResource(R.color.expense_red)
                    "saving" -> getColorFromResource(R.color.saving_blue)
                    else -> getColorFromResource(R.color.black)
                }
                transactionAmount.setTextColor(textColor)
                
                // Set icon from base64 if available
                if (!category?.iconBase64.isNullOrEmpty()) {
                    val iconBitmap = base64ToBitmap(category?.iconBase64)
                    if (iconBitmap != null) {
                        categoryIcon.setImageBitmap(iconBitmap)
                    } else {
                        setDefaultIcon(transaction.type)
                    }
                } else {
                    setDefaultIcon(transaction.type)
                }
                
                // Set click listener
                itemView.setOnClickListener { onItemClick(transaction) }
            }
            
            private fun setDefaultIcon(transactionType: String) {
                val iconResource = when (transactionType) {
                    "income" -> R.drawable.income
                    "expense" -> R.drawable.shoppingg
                    "saving" -> R.drawable.saving
                    else -> R.drawable.cash
                }
                categoryIcon.setImageResource(iconResource)
            }
            
            private fun getColorFromResource(colorResId: Int): Int {
                return requireContext().getColor(colorResId)
            }
        }
    }
}