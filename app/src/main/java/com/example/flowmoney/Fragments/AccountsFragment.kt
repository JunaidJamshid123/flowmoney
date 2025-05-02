package com.example.flowmoney.Fragments

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.R
import com.google.android.material.tabs.TabLayout
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.view.Window
import android.view.WindowManager
import com.example.flowmoney.AddNewAccount

class AccountsFragment : Fragment() {

    private lateinit var tabLayout: TabLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var addAccountButton: Button
    private lateinit var backButton: ImageButton

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_accounts, container, false)

        // Initialize views
        tabLayout = view.findViewById(R.id.tab_layout)
        recyclerView = view.findViewById(R.id.rv_accounts)
        addAccountButton = view.findViewById(R.id.btn_add_account)
        backButton = view.findViewById(R.id.btn_back)

        // Set up RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        // You would set your adapter here
        // recyclerView.adapter = yourAdapter

        // Tab layout listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab selection
                when (tab?.position) {
                    0 -> {
                        // Cards tab selected
                        // Update your RecyclerView with cards data
                    }
                    1 -> {
                        // Accounts tab selected
                        // Update your RecyclerView with accounts data
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not needed for now
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Not needed for now
            }
        })

        // Set click listener for add account button
        addAccountButton.setOnClickListener {
            showAddAccountDialog()
        }

        // Set click listener for back button
        backButton.setOnClickListener {
            // Go back or close fragment
            activity?.onBackPressed()
        }

        return view
    }

    private fun showAddAccountDialog() {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.activity_add_new_account)

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
            // Get input values and save account

            // Close dialog
            dialog.dismiss()

            // Refresh accounts list
            // refreshAccountsList()
        }

        dialog.show()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AccountsFragment()
    }
}