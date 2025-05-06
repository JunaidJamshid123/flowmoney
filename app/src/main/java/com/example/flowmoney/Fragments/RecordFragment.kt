package com.example.flowmoney.Fragments

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.AddTransaction
import com.example.flowmoney.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordFragment : Fragment() {

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

    // Activity result launcher for AddTransaction
    private val addTransactionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data = result.data
            if (data != null) {
                // Process the returned transaction data
                val amount = data.getDoubleExtra("amount", 0.0)
                val accountId = data.getStringExtra("accountId") ?: ""
                val categoryId = data.getStringExtra("categoryId") ?: ""
                val transactionType = data.getStringExtra("transactionType") ?: "expense"
                val date = data.getLongExtra("date", System.currentTimeMillis())
                val notes = data.getStringExtra("notes") ?: ""
                val hasInvoice = data.getBooleanExtra("hasInvoice", false)
                val invoiceUri = data.getStringExtra("invoiceUri")

                // Here you would save the transaction to your database
                saveTransactionToDatabase(amount, accountId, categoryId, transactionType, date, notes, hasInvoice, invoiceUri)

                // Show a success message
                Toast.makeText(requireContext(), "Transaction added successfully", Toast.LENGTH_SHORT).show()

                // Update UI (refresh transaction list)
                updateTransactionList()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_record, container, false)

        // Initialize views
        initViews(view)

        // Setup spinners
        setupSpinners()

        // Setup RecyclerView
        setupRecyclerView()

        // Setup click listeners
        setupClickListeners()

        return view
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

    private fun setupSpinners() {
        // Category spinner setup
        val categoryOptions = arrayOf("All", "Food", "Transport", "Shopping", "Bills", "Entertainment", "Other")
        val categoryAdapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categoryOptions
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = categoryAdapter

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

    private fun setupRecyclerView() {
        recyclerRecords.layoutManager = LinearLayoutManager(requireContext())
        // Set your adapter here
        // recyclerRecords.adapter = yourAdapter

        // Show empty state if no transactions
        updateEmptyState()
    }

    private fun updateEmptyState() {
        // This is just an example, you should check if your data is empty
        val hasTransactions = false // Replace with your logic
        emptyState.visibility = if (hasTransactions) View.GONE else View.VISIBLE
        recyclerRecords.visibility = if (hasTransactions) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        fabAdd.setOnClickListener {
            launchAddTransaction()
        }

        // Add listeners for your spinners/filters if needed
        categorySpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Filter transactions based on selected category
                updateTransactionList()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        })

        sortSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                // Sort transactions based on selected option
                updateTransactionList()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                // Do nothing
            }
        })
    }

    private fun launchAddTransaction() {
        val intent = Intent(requireContext(), AddTransaction::class.java)
        addTransactionLauncher.launch(intent)
    }

    private fun saveTransactionToDatabase(
        amount: Double,
        accountId: String,
        categoryId: String,
        transactionType: String,
        date: Long,
        notes: String,
        hasInvoice: Boolean,
        invoiceUri: String?
    ) {
        // Here you would implement the logic to save the transaction to Firebase
        // This is just a placeholder for your implementation

        // Example code:
        // val transaction = Transaction(
        //     id = UUID.randomUUID().toString(),
        //     userId = getCurrentUserId(),
        //     amount = amount,
        //     accountId = accountId,
        //     categoryId = categoryId,
        //     type = transactionType,
        //     date = date,
        //     notes = notes,
        //     hasInvoice = hasInvoice,
        //     invoiceUri = invoiceUri
        // )
        //
        // FirebaseFirestore.getInstance()
        //     .collection("transactions")
        //     .document(transaction.id)
        //     .set(transaction)
        //     .addOnSuccessListener {
        //         // Transaction saved successfully
        //     }
        //     .addOnFailureListener { e ->
        //         // Handle error
        //     }
    }

    private fun updateTransactionList() {
        // Implement logic to refresh transaction list based on filters
        // This would typically involve querying your database with the filters
        // and updating your RecyclerView adapter

        // After updating the list, update the empty state
        updateEmptyState()

        // Also update summary values
        updateSummaryValues()
    }

    private fun updateSummaryValues() {
        // Example implementation - replace with your actual data
        val totalIncome = 1000.0
        val totalExpenses = 750.0
        val totalSavings = 250.0
        val balance = totalIncome - totalExpenses

        textTotalBalance.text = String.format("$%.2f", balance)
        textIncome.text = String.format("$%.2f", totalIncome)
        textExpenses.text = String.format("$%.2f", totalExpenses)
        textSavings.text = String.format("$%.2f", totalSavings)
    }

    companion object {
        @JvmStatic
        fun newInstance() = RecordFragment()
    }
}