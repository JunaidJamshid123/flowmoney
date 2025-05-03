package com.example.flowmoney.Fragments

import android.app.Activity
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class AccountsFragment : Fragment() {
    private val TAG = "AccountsFragment"

    // Views
    private lateinit var recyclerView: RecyclerView
    private lateinit var addAccountButton: Button
    private lateinit var backButton: ImageButton
    private lateinit var tabLayout: TabLayout
    private lateinit var emptyStateLayout: View
    private lateinit var emptyStateMessage: TextView
    private lateinit var progressBar: View

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Adapter
    private lateinit var accountsAdapter: AccountsAdapter

    // Current filter
    private var currentFilter: String? = null

    // Firestore listener
    private var accountsListener: ListenerRegistration? = null

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

        // Find the empty state message TextView if it exists
        emptyStateMessage = emptyStateLayout.findViewById(R.id.nav_trash) ?: TextView(requireContext())

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
            showEmptyState("Please log in to view accounts")
            return
        }

        // Show loading indicator
        progressBar.visibility = View.VISIBLE
        emptyStateLayout.visibility = View.GONE

        try {
            // Remove any existing listener
            accountsListener?.remove()

            val accountsCollection = firestore.collection("accounts")

            // Create base query - IMPORTANT: Apply all filters before orderBy
            var query = accountsCollection.whereEqualTo("user_id", currentUser.uid)

            // Apply filter if specified
            if (!filter.isNullOrEmpty()) {
                query = query.whereEqualTo("account_type", filter)
            }

            // Add ordering last
            query = query.orderBy("created_at", Query.Direction.DESCENDING)

            // Use a listener for real-time updates instead of one-time get()
            accountsListener = query.addSnapshotListener { snapshot, e ->
                // Hide loading indicator
                progressBar.visibility = View.GONE

                if (e != null) {
                    Log.e(TAG, "Error listening for accounts", e)

                    // Show a more specific error message for index errors
                    val errorMsg = when {
                        e.message?.contains("FAILED_PRECONDITION") == true &&
                                e.message?.contains("requires an index") == true -> {
                            "Database index needed. Please visit Firebase console or wait a few minutes and try again."
                        }
                        else -> "Failed to load accounts: ${e.message}"
                    }

                    Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                    showEmptyState("Couldn't load accounts")
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    // Show empty state with appropriate message
                    val filterName = when (filter) {
                        "bank" -> "bank "
                        "cash" -> "cash "
                        "e-wallet" -> "e-wallet "
                        "credit" -> "credit "
                        else -> ""
                    }
                    showEmptyState("No ${filterName}accounts found")
                    return@addSnapshotListener
                }

                val accounts = snapshot.mapNotNull { doc ->
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

                // Show/hide empty state based on results
                if (accounts.isEmpty()) {
                    showEmptyState("No accounts found")
                } else {
                    emptyStateLayout.visibility = View.GONE
                    Log.d(TAG, "Loaded ${accounts.size} accounts")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in loadAccounts", e)
            Toast.makeText(context, "An unexpected error occurred", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            showEmptyState("Something went wrong")
        }
    }

    private fun showEmptyState(message: String) {
        emptyStateLayout.visibility = View.VISIBLE

        // If TextView exists in your empty state layout, update its text
        try {
            emptyStateMessage.text = message
        } catch (e: Exception) {
            Log.e(TAG, "Could not update empty state message", e)
        }
    }

    private fun launchAddAccountActivity() {
        val intent = Intent(requireContext(), AddNewAccount::class.java)
        addAccountLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh accounts data when fragment becomes visible
        if (accountsListener == null) {
            loadAccounts(currentFilter)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the listener to prevent memory leaks
        accountsListener?.remove()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AccountsFragment()
    }
}