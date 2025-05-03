package com.example.flowmoney

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Adapters.CategoryIconAdapter


class AddNewCategory : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var rvCategoryIcons: RecyclerView
    private lateinit var btnCancel: MaterialButton
    private lateinit var btnSave: MaterialButton

    private var selectedIconResId: Int = R.drawable.ic_cash // Default icon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_category)

        // Initialize views
        etName = findViewById(R.id.etName)
        rvCategoryIcons = findViewById(R.id.rvCategoryIcons)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)

        setupIconsRecyclerView()
        setupButtons()
    }

    private fun setupIconsRecyclerView() {
        // List of all category icons
        val categoryIcons = listOf(
            R.drawable.cash,         // Cash/Money
            R.drawable.onlineshopping,     // Clothing
            R.drawable.restaurantt,         // Food
            R.drawable.income,         // Home
            R.drawable.shoppingg,     // Shopping
            R.drawable.wallet,      // Medical
            R.drawable.saving,// Entertainment
            R.drawable.savinggg,       // Health
            R.drawable.gas,    // Checklist
            R.drawable.wine,
            R.drawable.won// Sports
        )

        // Create and set adapter
        val adapter = CategoryIconAdapter(categoryIcons) { iconResId ->
            selectedIconResId = iconResId
        }

        // Configure RecyclerView
        rvCategoryIcons.layoutManager = GridLayoutManager(this, 5)
        rvCategoryIcons.adapter = adapter
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener {
            // Dismiss the dialog or finish activity
            finish()
        }

        btnSave.setOnClickListener {
            val categoryName = etName.text.toString().trim()

            if (categoryName.isEmpty()) {
                etName.error = "Please enter a category name"
                return@setOnClickListener
            }

            // Create new category with the selected name and icon
            saveCategory(categoryName, selectedIconResId)

            // Close the dialog or activity
            finish()
        }
    }

    private fun saveCategory(name: String, iconResId: Int) {
        // TODO: Implement your logic to save the new category
        // For example:
        // val newCategory = Category(name = name, iconResId = iconResId)
        // viewModel.saveCategory(newCategory)
    }
}