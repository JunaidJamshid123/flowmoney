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

    // Store total balance
    private var totalBalance: Double = 0.0

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
        emptyStateMessage = view.findViewById(R.id.tv_empty_state_message)
        progressBar = view.findViewById(R.id.progress_bar)

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

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Load accounts when view is created
        loadAccounts()
    }

    private fun setupRecyclerView() {
        // Initialize adapter with empty list
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
        recyclerView.visibility = View.GONE

        try {
            // Remove any existing listener
            accountsListener?.remove()

            val accountsCollection = firestore.collection("accounts")

            // Using just a single field query to avoid index issues
            val query = accountsCollection
                .whereEqualTo("user_id", currentUser.uid)
                
            Log.d(TAG, "Querying accounts for user ID: ${currentUser.uid}")

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
                    showEmptyState("No accounts found. Add your first account!")
                    updateTotalBalance(0.0)
                    recyclerView.visibility = View.GONE
                    return@addSnapshotListener
                }

                val accounts = snapshot.mapNotNull { doc ->
                    try {
                        val account = Account()
                        account.accountId = doc.getString("account_id") ?: doc.id
                        account.userId = doc.getString("user_id") ?: ""
                        account.accountName = doc.getString("account_name") ?: ""
                        account.balance = doc.getDouble("balance") ?: 0.0
                        account.accountType = doc.getString("account_type") ?: ""
                        account.accountImageUrl = doc.getString("account_image_url")
                        account.note = doc.getString("note")
                        account.createdAt = doc.getLong("created_at") ?: 0L
                        account.updatedAt = doc.getLong("updated_at") ?: 0L
                        account
                    } catch (e: Exception) {
                        Log.e(TAG, "Error converting document to Account", e)
                        null
                    }
                }

                // Debug log for accounts retrieved
                Log.d(TAG, "Retrieved ${accounts.size} accounts for user ${currentUser.uid}")
                accounts.forEach { account ->
                    Log.d(TAG, "Account: ${account.accountId}, Name: ${account.accountName}, Type: ${account.accountType}, Balance: ${account.balance}")
                }

                // Sort accounts by name in memory (instead of in the query)
                val sortedAccounts = accounts.sortedBy { it.accountName }

                // Update the adapter with the new data
                accountsAdapter.submitList(sortedAccounts)

                // Calculate and display total balance
                totalBalance = sortedAccounts.sumOf { it.balance }
                updateTotalBalance(totalBalance)

                // Show/hide empty state based on results
                if (sortedAccounts.isEmpty()) {
                    showEmptyState("No accounts found. Add your first account!")
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                    Log.d(TAG, "Loaded ${sortedAccounts.size} accounts")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error in loadAccounts", e)
            Toast.makeText(context, "An unexpected error occurred", Toast.LENGTH_SHORT).show()
            progressBar.visibility = View.GONE
            showEmptyState("Something went wrong")
            recyclerView.visibility = View.GONE
        }
    }

    private fun updateTotalBalance(balance: Double) {
        // Store balance for other fragments to access
        totalBalance = balance

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
        loadAccounts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Clean up the listener to prevent memory leaks
        accountsListener?.remove()
    }

    companion object {
        @JvmStatic
        fun newInstance() = AccountsFragment()

        // Public method to get total accounts balance from any fragment
        var accountsTotalBalance = 0.0
            private set
            
        // Method to update account balance when a transaction is made
        fun updateAccountBalance(accountId: String, amount: Double, isExpense: Boolean) {
            val firestore = FirebaseFirestore.getInstance()
            val accountRef = firestore.collection("accounts").document(accountId)
            
            // Use a transaction to safely update the balance
            firestore.runTransaction { transaction ->
                val snapshot = transaction.get(accountRef)
                val currentBalance = snapshot.getDouble("balance") ?: 0.0
                
                // Calculate new balance - add for income, subtract for expense
                val newBalance = if (isExpense) {
                    currentBalance - amount
                } else {
                    currentBalance + amount
                }
                
                // Update the balance field
                transaction.update(accountRef, "balance", newBalance)
                
                // Also update last modified timestamp
                transaction.update(accountRef, "updated_at", System.currentTimeMillis())
                
                // Return result value (not used in this case)
                null
            }.addOnSuccessListener {
                Log.d("AccountsFragment", "Successfully updated account balance for $accountId")
            }.addOnFailureListener { e ->
                Log.e("AccountsFragment", "Error updating account balance", e)
            }
        }
    }
}