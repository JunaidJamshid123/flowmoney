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
    private lateinit var totalBalanceTextView: TextView
    private lateinit var emptyStateLayout: View
    private lateinit var emptyStateMessage: TextView
    private lateinit var progressBar: View

    // Firebase
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    // Adapter
    private lateinit var accountsAdapter: AccountsAdapter

    // Firestore listener
    private var accountsListener: ListenerRegistration? = null

    // Use ActivityResultLauncher for handling the result from AddNewAccount activity
    private val addAccountLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "Account created successfully, refreshing accounts list")
            loadAccounts()
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
        totalBalanceTextView = view.findViewById(R.id.tv_total_balance)
        emptyStateLayout = view.findViewById(R.id.empty_state_layout)
        progressBar = view.findViewById(R.id.progress_bar)

        // Find the empty state message TextView
        emptyStateMessage = view.findViewById(R.id.tv_empty_state_message)

        // Setup adapter
        setupRecyclerView()

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

    private fun loadAccounts() {
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

            // Create a simple query that only filters by user_id and sorts by creation date
            // This avoids the need for complex indexes
            val query = accountsCollection
                .whereEqualTo("user_id", currentUser.uid)
                .orderBy("created_at", Query.Direction.DESCENDING)

            // Use a listener for real-time updates
            accountsListener = query.addSnapshotListener { snapshot, e ->
                // Hide loading indicator
                progressBar.visibility = View.GONE

                if (e != null) {
                    Log.e(TAG, "Error listening for accounts", e)
                    Toast.makeText(context, "Failed to load accounts: ${e.message}", Toast.LENGTH_LONG).show()
                    showEmptyState("Couldn't load accounts")
                    return@addSnapshotListener
                }

                if (snapshot == null || snapshot.isEmpty) {
                    // Show empty state when no accounts found
                    showEmptyState("No accounts found")
                    updateTotalBalance(0.0)
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

                // Calculate and display total balance
                val totalBalance = accounts.sumOf { it.balance }
                updateTotalBalance(totalBalance)

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

    private fun updateTotalBalance(balance: Double) {
        // Format the balance with currency symbol and 2 decimal places
        val formattedBalance = String.format("$%,.2f", balance)
        totalBalanceTextView.text = formattedBalance
    }

    private fun showEmptyState(message: String) {
        emptyStateLayout.visibility = View.VISIBLE
        emptyStateMessage.text = message
    }

    private fun launchAddAccountActivity() {
        val intent = Intent(requireContext(), AddNewAccount::class.java)
        addAccountLauncher.launch(intent)
    }

    override fun onResume() {
        super.onResume()
        // Refresh accounts data when fragment becomes visible
        if (accountsListener == null) {
            loadAccounts()
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