package com.example.flowmoney.Fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.R
import com.google.android.material.floatingactionbutton.FloatingActionButton

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
        // This is just an example, you should check if your data is empty
        val hasTransactions = true // Replace with your logic
        emptyState.visibility = if (hasTransactions) View.GONE else View.VISIBLE
        recyclerRecords.visibility = if (hasTransactions) View.VISIBLE else View.GONE
    }

    private fun setupClickListeners() {
        fabAdd.setOnClickListener {
            showAddTransactionDialog()
        }
    }

    private fun showAddTransactionDialog() {
        // Create dialog
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        dialog.setContentView(R.layout.activity_add_transaction)

        // Make dialog background transparent to show rounded corners properly
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog width to match parent with margins
        val layoutParams = dialog.window?.attributes
        layoutParams?.width = ViewGroup.LayoutParams.MATCH_PARENT
        dialog.window?.attributes = layoutParams

        // Set close button click listener
        val btnClose = dialog.findViewById<ImageButton>(R.id.btn_close)
        btnClose.setOnClickListener {
            dialog.dismiss()
        }

        // Show dialog
        dialog.show()
    }

    // You can keep the companion object if needed for creating instances
    companion object {
        @JvmStatic
        fun newInstance() = RecordFragment()
    }
}