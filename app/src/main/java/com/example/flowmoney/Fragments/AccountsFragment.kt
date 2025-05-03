package com.example.flowmoney.Fragments

import android.app.Activity
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
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.flowmoney.Adapters.AccountsAdapter
import com.example.flowmoney.AddNewAccount
import com.example.flowmoney.Models.Account
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AccountsFragment : Fragment() {
    private val TAG = "AccountsFragment"

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var addAccountButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var tabLayout: TabLayout
    private lateinit var emptyStateLayout: View
    private lateinit var progressBar: View

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Adapter
    private lateinit var accountsAdapter: AccountsAdapter

    // Current filter
    private var currentFilter: String? = null

    // Use ActivityResultLauncher for handling the result from AddNewAccount activity
    private val addAccountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Account created successfully, refreshing accounts list")
            loadAccounts(currentFilter)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_accounts, container, false)

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Initialize views
        recyclerView = view.findViewById(R.id.rv_accounts)
        addAccountButton = view.findViewById(R.id.btn_add_account)
        backButton = view.findViewById(R.id.btn_back)
        tabLayout = view.findViewById(R.id.tab_layout)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        progressBar = view.findViewById(R.id.progress_bar)

        // Setup adapter
        setupRecyclerView()

        // Setup tabs
        setupTabLayout()

        // Set click listener for add account button
        addAccountButton.setOnClickListener {
            launchAddAccountActivity()
        }

        // Set click listener for back button
        backButton.setOnClickListener {
            // Go back or close fragment
            activity?.onBackPressed()
        }

        // Load initial data
        loadAccounts()

        return view
    }

    private fun setupTabLayout() {
        // Tab layout listener
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                // Handle tab selection
                when (tab?.position) {
                    0 -> {
                        // All accounts
                        loadAccounts(null)
                    }
                    1 -> {
                        // Bank accounts
                        loadAccounts("bank")
                    }
                    2 -> {
                        // Cash accounts
                        loadAccounts("cash")
                    }
                    3 -> {
                        // E-Wallet accounts
                        loadAccounts("e-wallet")
                    }
                    4 -> {
                        // Credit accounts
                        loadAccounts("credit")
                    }
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                // Not needed for now
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Refresh data on tab reselection
                when (tab?.position) {
                    0 -> loadAccounts(null)
                    1 -> loadAccounts("bank")
                    2 -> loadAccounts("cash")
                    3 -> loadAccounts("e-wallet")
                    4 -> loadAccounts("credit")
                }
            }
        })
    }

    private fun setupRecyclerView() {
        // Initialize adapter
        accountsAdapter = AccountsAdapter(requireContext()) { account ->
            // Handle account item click
            onAccountClicked(account)
        }

        // Set up RecyclerView
        recyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = accountsAdapter
            setHasFixedSize(true)
        }
    }

    private fun onAccountClicked(account: Account) {
        // Handle account click - you can navigate to account details or other actions
        Toast.makeText(context, "Clicked: ${account.accountName}", Toast.LENGTH_SHORT).show()

        // You can implement navigation to account details here
        // For example:
        // val intent = Intent(requireContext(), AccountDetailsActivity::class.java)
        // intent.putExtra("ACCOUNT_ID", account.accountId)
        // startActivity(intent)
    }

    private fun loadAccounts(filter: String? = null) {
        currentFilter = filter

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Log.e(TAG, "No user is logged in")
            return
        }

        // Show loading indicator
        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE

        val accountsCollection = firestore.collection("accounts")

        // Create base query
        var query = accountsCollection
            .whereEqualTo("user_id", currentUser.uid)
            .orderBy("created_at", Query.Direction.DESCENDING)

        // Apply filter if specified
        if (!filter.isNullOrEmpty()) {
            query = query.whereEqualTo("account_type", filter)
        }

        query.get()
            .addOnSuccessListener { documents ->
                val accounts = documents.mapNotNull { doc ->
                    try {
                        val account = doc.toObject(Account::class.java)
                        // Ensure accountId is set if it's not coming from Firestore
                        if (account.accountId.isEmpty()) {
                            account.accountId = doc.id
                        }
                        account
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Account", e)
                        null
                    }
                }

                // Update the adapter with the new data
                accountsAdapter.submitList(accounts)

                // Hide loading indicator
                progressBar.visibility = View.GONE

                // Show empty state if no accounts
                if (accounts.isEmpty()) {
                    // Show empty state
                    emptyStateLayout.visibility = View.VISIBLE
                    Log.d(TAG, "No accounts found")
                } else {
                    // Hide empty state
                    emptyStateLayout.visibility = View.GONE
                    Log.d(TAG, "Loaded ${accounts.size} accounts")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error loading accounts", e)
                Toast.makeText(context, "Failed to load accounts: ${e.message}", Toast.LENGTH_SHORT).show()

                // Hide loading indicator
                progressBar.visibility = View.GONE
                // Show empty state on error
                emptyStateLayout.visibility = View.VISIBLE
            }
    }

    private fun launchAddAccountActivity() {
        val intent = Intent(requireContext(), AddNewAccount::class.java)
        addAccountLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh accounts data when fragment becomes visible
        loadAccounts(currentFilter)
    }

    companion object {
        @JvmStatic
        fun newInstance() = AccountsFragment()
    }
}