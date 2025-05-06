package com.example.flowmoney.Adapters

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Models.Account
import com.example.flowmoney.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

class AccountsAdapter(
    private val context: Context,
    private val onItemClicked: (Account) -> Unit
) : ListAdapter<Account, AccountsAdapter.AccountViewHolder>(AccountDiffCallback()) {

    private val TAG = "AccountsAdapter"

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.account_item, parent, false)
        return AccountViewHolder(view, onItemClicked)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
        Log.d(TAG, "Binding account at position $position: ${account.accountName}")
        holder.bind(account, context)
    }

    class AccountViewHolder(
        itemView: View,
        private val onItemClicked: (Account) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val accountIcon: ImageView = itemView.findViewById(R.id.iv_account_icon)
        private val accountName: TextView = itemView.findViewById(R.id.tv_account_name)
        private val accountType: TextView = itemView.findViewById(R.id.tv_account_type)
        private val accountBalance: TextView = itemView.findViewById(R.id.tv_account_balance)
        private var currentAccount: Account? = null
        private val TAG = "AccountViewHolder"

        init {
            itemView.setOnClickListener {
                currentAccount?.let { account ->
                    onItemClicked(account)
                }
            }
        }

        fun bind(account: Account, context: Context) {
            currentAccount = account

            Log.d(TAG, "Binding account: ${account.accountId}, Name: ${account.accountName}")

            // Set account name
            accountName.text = account.accountName

            // Set account type with proper capitalization
            val formattedType = account.accountType.replaceFirstChar {
                if (it.isLowerCase()) it.titlecase() else it.toString()
            }
            accountType.text = formattedType

            // Format and set balance
            val formattedBalance = String.format("$%,.2f", account.balance)
            accountBalance.text = formattedBalance

            // Determine default icon resource based on account type
            val iconRes = getIconResourceForAccountType(account.accountType)

            // If account has a base64 image string, try to decode it
            if (!account.accountImageUrl.isNullOrEmpty()) {
                try {
                    // Check if it starts with http or https (remote URL)
                    if (account.accountImageUrl!!.startsWith("http")) {
                        // Load remote image with Glide
                        Glide.with(context)
                            .load(account.accountImageUrl)
                            .apply(RequestOptions()
                                .placeholder(iconRes)
                                .error(iconRes)
                                .diskCacheStrategy(DiskCacheStrategy.ALL)
                                .circleCrop())
                            .into(accountIcon)
                    } else {
                        // Try to decode Base64 string
                        try {
                            val imageBytes = Base64.decode(account.accountImageUrl, Base64.DEFAULT)
                            val decodedImage = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                            if (decodedImage != null) {
                                accountIcon.setImageBitmap(decodedImage)
                            } else {
                                // If decoding fails, use fallback
                                Log.e(TAG, "Failed to decode Base64 image for account: ${account.accountName}")
                                accountIcon.setImageResource(iconRes)
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Error decoding Base64 image: ${e.message}")
                            accountIcon.setImageResource(iconRes)
                        }
                    }
                } catch (e: Exception) {
                    // Fallback to default icon if image loading fails
                    Log.e(TAG, "Error loading account image: ${e.message}")
                    accountIcon.setImageResource(iconRes)
                }
            } else {
                // Use default icon
                accountIcon.setImageResource(iconRes)
            }
        }

        private fun getIconResourceForAccountType(accountType: String): Int {
            return when (accountType.lowercase()) {
                "bank account" -> R.drawable.bank
                "cash" -> R.drawable.cash
                "e-wallet" -> R.drawable.onlinewallet
                "credit card" -> R.drawable.creditcard
                "mobile banking" -> R.drawable.mobilebanking
                "mobile money" -> R.drawable.smartphone
                "cryptocurrency" -> R.drawable.bitcoin
                else -> R.drawable.creditcard // Default fallback
            }
        }
    }

    class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
        override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.accountId == newItem.accountId
        }

        override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
            return oldItem.accountName == newItem.accountName &&
                    oldItem.accountType == newItem.accountType &&
                    oldItem.balance == newItem.balance &&
                    oldItem.accountImageUrl == newItem.accountImageUrl
        }
    }
}