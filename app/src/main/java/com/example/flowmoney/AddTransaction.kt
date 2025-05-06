package com.example.flowmoney

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.flowmoney.Models.Account
import com.example.flowmoney.Models.Category
import com.google.android.material.textfield.TextInputEditText
import java.text.SimpleDateFormat
import java.util.*

class AddTransaction : AppCompatActivity() {

    // UI Components
    private lateinit var btnClose: ImageButton
    private lateinit var categorySpinner: Spinner
    private lateinit var accountSpinner: Spinner
    private lateinit var radioExpense: RadioButton
    private lateinit var radioIncome: RadioButton
    private lateinit var radioSaving: RadioButton
    private lateinit var amountEditText: TextInputEditText
    private lateinit var clearAmountBtn: TextView
    private lateinit var dateTextView: TextView
    private lateinit var addInvoiceBtn: TextView
    private lateinit var notesEditText: TextInputEditText
    private lateinit var addTransactionBtn: TextView

    // Data
    private var selectedDate: Calendar = Calendar.getInstance()
    private var invoiceUri: Uri? = null
    private val dateFormatter = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault())

    // Sample data - Replace with real data source
    private val accounts = mutableListOf<Account>()
    private val categories = mutableListOf<Category>()

    // Invoice launcher
    private val getInvoice = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            invoiceUri = it
            addInvoiceBtn.text = "Invoice Added"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_transaction)

        initializeViews()
        loadSampleData()
        setupSpinners()
        setupDatePicker()
        setupListeners()
    }

    private fun initializeViews() {
        btnClose = findViewById(R.id.btn_close)
        categorySpinner = findViewById(R.id.category_spinner)
        accountSpinner = findViewById(R.id.account_spinner)
        radioExpense = findViewById(R.id.radio_expense)
        radioIncome = findViewById(R.id.radio_income)
        radioSaving = findViewById(R.id.radio_saving)
        amountEditText = findViewById(R.id.amount_edit_text)
        clearAmountBtn = findViewById(R.id.clear_amount_btn)
        dateTextView = findViewById(R.id.date_text_view)
        addInvoiceBtn = findViewById(R.id.add_invoice_btn)
        notesEditText = findViewById(R.id.notes_edit_text)
        addTransactionBtn = findViewById(R.id.add_transaction_btn)

        dateTextView.text = dateFormatter.format(selectedDate.time)
    }

    private fun loadSampleData() {
        accounts.apply {
            add(Account("1", "user1", "Cash", 1000.0, "Cash"))
            add(Account("2", "user1", "Bank", 5000.0, "Bank"))
            add(Account("3", "user1", "E-Wallet", 500.0, "E-Wallet"))
        }

        categories.apply {
            add(Category("1", "Shopping", "", R.drawable.sort_descending, false))
            add(Category("2", "Food", "", R.drawable.sort_descending, false))
            add(Category("3", "Transport", "", R.drawable.sort_descending, false))
            add(Category("4", "Bills", "", R.drawable.sort_descending, false))
            add(Category("5", "Salary", "", R.drawable.sort_descending, true))
        }
    }

    private fun setupSpinners() {
        val accountAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, accounts.map { it.accountName }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        accountSpinner.adapter = accountAdapter

        val categoryAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories.map { it.name }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = categoryAdapter
    }

    private fun setupDatePicker() {
        dateTextView.setOnClickListener {
            DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    selectedDate.set(year, month, dayOfMonth)
                    dateTextView.text = dateFormatter.format(selectedDate.time)
                },
                selectedDate.get(Calendar.YEAR),
                selectedDate.get(Calendar.MONTH),
                selectedDate.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
    }

    private fun setupListeners() {
        btnClose.setOnClickListener { finish() }
        clearAmountBtn.setOnClickListener { amountEditText.setText("") }
        addInvoiceBtn.setOnClickListener { getInvoice.launch("image/*") }
        addTransactionBtn.setOnClickListener { saveTransaction() }

        radioExpense.setOnClickListener { updateCategories(false) }
        radioIncome.setOnClickListener { updateCategories(true) }
        radioSaving.setOnClickListener { /* Handle savings logic if needed */ }
    }

    private fun updateCategories(isIncome: Boolean) {
        val filtered = categories.filter { it.isIncome == isIncome }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filtered.map { it.name }).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
        categorySpinner.adapter = adapter
    }

    // Removed @RequiresApi annotation to make the method compatible with API level 24
    private fun saveTransaction() {
        val amountStr = amountEditText.text.toString()
        val amount = amountStr.toDoubleOrNull()

        if (amountStr.isEmpty() || amount == null || amount <= 0) {
            Toast.makeText(this, "Please enter a valid amount", Toast.LENGTH_SHORT).show()
            return
        }

        val accountIndex = accountSpinner.selectedItemPosition
        val categoryIndex = categorySpinner.selectedItemPosition

        if (accountIndex < 0 || categoryIndex < 0) {
            Toast.makeText(this, "Please select account and category", Toast.LENGTH_SHORT).show()
            return
        }

        val transactionType = when {
            radioExpense.isChecked -> "expense"
            radioIncome.isChecked -> "income"
            radioSaving.isChecked -> "saving"
            else -> "expense"
        }

        // Fix the categoryId resolution issue by finding the proper category based on selection
        val selectedCategory = categorySpinner.adapter.getItem(categoryIndex) as String
        val categoryId = categories.find { it.name == selectedCategory }?.id ?: ""

        val resultIntent = Intent().apply {
            putExtra("amount", amount)
            putExtra("accountId", accounts[accountIndex].accountId)
            putExtra("categoryId", categoryId) // Fixed categoryId resolution
            putExtra("transactionType", transactionType)
            putExtra("date", selectedDate.timeInMillis)
            putExtra("notes", notesEditText.text.toString())
            invoiceUri?.let {
                putExtra("hasInvoice", true)
                putExtra("invoiceUri", it.toString())
            }
        }

        setResult(RESULT_OK, resultIntent)
        finish()
    }
}