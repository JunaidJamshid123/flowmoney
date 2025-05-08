package com.example.flowmoney.Activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Adapters.SearchAdapter
import com.example.flowmoney.Models.Category
import com.example.flowmoney.Models.Transaction
import com.example.flowmoney.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class SearchActivity : AppCompatActivity() {
    private lateinit var backButton: CardView
    private lateinit var searchEditText: EditText
    private lateinit var searchResultsRecyclerView: RecyclerView
    private lateinit var noResultsText: TextView
    private lateinit var progressBar: ProgressBar
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    
    private val transactions = mutableListOf<Transaction>()
    private val categories = mutableMapOf<String, Category>()
    private lateinit var searchAdapter: SearchAdapter
    
    private val TAG = "SearchActivity"
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_search)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        
        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        
        // Initialize views
        initViews()
        
        // Setup adapter
        setupAdapter()
        
        // Setup listeners
        setupListeners()
        
        // Load initial data
        loadTransactions()
    }
    
    private fun initViews() {
        backButton = findViewById(R.id.back_button)
        searchEditText = findViewById(R.id.search_edit_text)
        searchResultsRecyclerView = findViewById(R.id.search_results_recycler_view)
        
        // Create a TextView for showing "No Results" message
        noResultsText = TextView(this).apply {
            text = "No transactions found"
            textSize = 16f
            visibility = View.GONE
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, 100, 0, 0)
        }
        val parentView = searchResultsRecyclerView.parent as View
        if (parentView is androidx.constraintlayout.widget.ConstraintLayout) {
            noResultsText.id = View.generateViewId()
            val params = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.MATCH_PARENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            params.topToBottom = R.id.divider
            parentView.addView(noResultsText, params)
        }
        
        // Find or create progress bar
        progressBar = ProgressBar(this, null, android.R.attr.progressBarStyleLarge).apply {
            visibility = View.GONE
            id = View.generateViewId()
        }
        if (parentView is androidx.constraintlayout.widget.ConstraintLayout) {
            val params = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams(
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT,
                androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.WRAP_CONTENT
            )
            params.topToBottom = R.id.divider
            params.startToStart = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            params.endToEnd = androidx.constraintlayout.widget.ConstraintLayout.LayoutParams.PARENT_ID
            params.topMargin = 100
            parentView.addView(progressBar, params)
        }
    }
    
    private fun setupAdapter() {
        // Create search adapter
        searchAdapter = SearchAdapter(this, transactions, categories) { transaction ->
            // Handle transaction click by navigating to TransactionDetails
            val intent = Intent(this, TransactionDetails::class.java).apply {
                putExtra(TransactionDetails.EXTRA_TRANSACTION_ID, transaction.transactionId)
            }
            startActivity(intent)
        }
        
        // Set up the RecyclerView
        searchResultsRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SearchActivity)
            adapter = searchAdapter
        }
    }
    
    private fun setupListeners() {
        // Back button click
        backButton.setOnClickListener {
            finish()
        }
        
        // Search text change listener
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                filterTransactions(query)
            }
        })
    }
    
    private fun filterTransactions(query: String) {
        if (transactions.isEmpty()) return
        
        if (query.isEmpty()) {
            // Show all transactions
            searchAdapter.updateData(transactions, categories)
            showNoResultsMessage(false)
            return
        }
        
        val filteredList = transactions.filter { transaction ->
            val category = categories[transaction.categoryId]
            val categoryName = category?.name?.lowercase() ?: ""
            val notes = transaction.notes.lowercase()
            val amount = transaction.amount.toString()
            val type = transaction.type.lowercase()
            
            categoryName.contains(query.lowercase()) || 
            notes.contains(query.lowercase()) ||
            amount.contains(query) ||
            type.contains(query.lowercase())
        }
        
        searchAdapter.updateData(filteredList, categories)
        showNoResultsMessage(filteredList.isEmpty())
    }
    
    private fun showNoResultsMessage(show: Boolean) {
        if (show) {
            searchResultsRecyclerView.visibility = View.GONE
            noResultsText.visibility = View.VISIBLE
        } else {
            searchResultsRecyclerView.visibility = View.VISIBLE
            noResultsText.visibility = View.GONE
        }
    }
    
    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
    
    private fun loadTransactions() {
        val userId = auth.currentUser?.uid ?: return
        
        showLoading(true)
        transactions.clear()
        categories.clear()
        
        // Load all transactions first
        firestore.collection("transactions")
            .whereEqualTo("user_id", userId)
            .get()
            .addOnSuccessListener { transactionDocuments ->
                if (transactionDocuments.isEmpty) {
                    showLoading(false)
                    showNoResultsMessage(true)
                    return@addOnSuccessListener
                }
                
                for (document in transactionDocuments) {
                    try {
                        val data = document.data
                        val transaction = Transaction.fromMap(data, document.id)
                        transactions.add(transaction)
                        
                        // Add this category ID to those we need to load
                        if (transaction.categoryId.isNotEmpty() && !categories.containsKey(transaction.categoryId)) {
                            loadCategory(transaction.categoryId)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing transaction: ${e.message}")
                    }
                }
                
                // Update adapter with transactions
                searchAdapter.updateData(transactions, categories)
                
                // Hide loading
                showLoading(false)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading transactions: ${e.message}")
                Toast.makeText(this, "Failed to load transactions", Toast.LENGTH_SHORT).show()
                showLoading(false)
                showNoResultsMessage(true)
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
                        category.categoryId = document.getString("category_id") ?: categoryId
                        category.userId = document.getString("user_id") ?: ""
                        category.name = document.getString("name") ?: "Unknown Category"
                        category.iconBase64 = document.getString("icon_base64") ?: ""
                        category.isIncome = document.getBoolean("is_income") ?: false
                        
                        categories[categoryId] = category
                        
                        // Update adapter with new category data
                        searchAdapter.updateData(transactions, categories)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing category: ${e.message}")
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading category: ${e.message}")
            }
    }
}