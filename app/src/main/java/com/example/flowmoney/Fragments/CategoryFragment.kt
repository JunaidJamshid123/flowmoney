package com.example.flowmoney.Fragments

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.R
import android.widget.EditText
import android.widget.RadioGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton

class CategoryFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var balanceTextView: TextView
    private lateinit var addCategoryButton: FloatingActionButton
    private lateinit var backButton: CardView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_category, container, false)

        // Initialize views
        recyclerView = view.findViewById(R.id.rv_categories)
        balanceTextView = view.findViewById(R.id.tv_balance)
        addCategoryButton = view.findViewById(R.id.fab_add_category)
        backButton = view.findViewById(R.id.cv_back_button)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        // You would set your adapter here
        // recyclerView.adapter = yourAdapter

        // Set click listener for add category button
        addCategoryButton.setOnClickListener {
            showAddCategoryDialog()
        }

        // Set click listener for back button
        backButton.setOnClickListener {
            // Go back or close fragment
            activity?.onBackPressed()
        }

        return view
    }

    private fun showAddCategoryDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.activity_add_new_category)

        // Make dialog background transparent
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        // Set dialog width to match parent
        dialog.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )

        // Set gravity to center
        dialog.window?.setGravity(Gravity.CENTER)

        // Set animation
        dialog.window?.attributes?.windowAnimations = R.style.DialogAnimation

        // Find buttons in the dialog
        val cancelButton = dialog.findViewById<Button>(R.id.btnCancel)
        val saveButton = dialog.findViewById<Button>(R.id.btnSave)

        // Set click listeners for buttons
        cancelButton.setOnClickListener {
            dialog.dismiss()
        }

        saveButton.setOnClickListener {
            // Handle save action here
            // Get input values and save category
            val categoryName = dialog.findViewById<EditText>(R.id.etName).text.toString()
            val isIncome = dialog.findViewById<RadioGroup>(R.id.rgType).checkedRadioButtonId == R.id.rbIncome
            val selectedIconId = dialog.findViewById<RadioGroup>(R.id.rgIcons).checkedRadioButtonId

            // Process and save the category
            saveCategory(categoryName, isIncome, selectedIconId)

            // Close dialog
            dialog.dismiss()

            // Refresh categories list
            refreshCategoriesList()
        }

        dialog.show()
    }

    private fun saveCategory(name: String, isIncome: Boolean, iconId: Int) {
        // Implement saving the category to your data source
        // This could be a database, shared preferences, etc.
        // ...
    }

    private fun refreshCategoriesList() {
        // Refresh the RecyclerView with updated data
        // ...
    }

    companion object {
        @JvmStatic
        fun newInstance() = CategoryFragment()
    }
}