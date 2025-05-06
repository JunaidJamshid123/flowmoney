package com.example.flowmoney

import android.os.Bundle
import android.util.Log
import android.widget.Toast
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

    private var selectedIconResId: Int = R.drawable.cash // Default icon

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_new_category)

        try {
            // Initialize views
            etName = findViewById(R.id.etName)
            rvCategoryIcons = findViewById(R.id.rvCategoryIcons)
            btnCancel = findViewById(R.id.btnCancel)
            btnSave = findViewById(R.id.btnSave)

            // Important: Setup RecyclerView first with layout manager
            val layoutManager = GridLayoutManager(this, 4) // 4 columns
            rvCategoryIcons.layoutManager = layoutManager

            // Set fixed size to improve performance
            rvCategoryIcons.setHasFixedSize(true)

            // Then setup adapter
            setupIconsRecyclerView()
            setupButtons()

        } catch (e: Exception) {
            val errorMsg = "Error initializing AddNewCategory: ${e.message}"
            Log.e("AddNewCategory", errorMsg, e)
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun setupIconsRecyclerView() {
        try {
            // List of all category icons - Add all your drawable resources here
            val categoryIcons = listOf(
                R.drawable.cash,
                R.drawable.onlineshopping,
                R.drawable.restaurantt,
                R.drawable.income,
                R.drawable.shoppingg,
                R.drawable.wallet,
                R.drawable.saving,
                R.drawable.savinggg,
                R.drawable.gas,
                R.drawable.wine,
                R.drawable.won
            )

            Log.d("AddNewCategory", "Setting up RecyclerView with ${categoryIcons.size} icons")

            // Create adapter with proper error handling
            val adapter = CategoryIconAdapter(categoryIcons) { iconResId ->
                selectedIconResId = iconResId
                Log.d("AddNewCategory", "Icon selected: $iconResId")
            }

            // Set adapter
            rvCategoryIcons.adapter = adapter

            // Log success
            Log.d("AddNewCategory", "RecyclerView adapter successfully set")

        } catch (e: Exception) {
            val errorMsg = "Error setting up RecyclerView: ${e.message}"
            Log.e("AddNewCategory", errorMsg, e)
            Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show()
        }
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

            // Show success toast
            Toast.makeText(this, "Category saved successfully!", Toast.LENGTH_SHORT).show()

            // Close the dialog or activity
            finish()
        }
    }

    private fun saveCategory(name: String, iconResId: Int) {
        // TODO: Implement your logic to save the new category
        // For example:
        // val newCategory = Category(name = name, iconResId = iconResId)
        // viewModel.saveCategory(newCategory)
        Log.d("AddNewCategory", "Saving category: $name with icon: $iconResId")
    }
}