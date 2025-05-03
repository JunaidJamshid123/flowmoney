package com.example.flowmoney.Adapters

import android.content.Context
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
import com.bumptech.glide.request.RequestOptions

class AccountsAdapter(
    private val context: Context,
    private val onItemClicked: (Account) -> Unit
) : ListAdapter<Account, AccountsAdapter.AccountViewHolder>(AccountDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.account_item, parent, false)
        return AccountViewHolder(view, onItemClicked)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)
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

        init {
            itemView.setOnClickListener {
                currentAccount?.let { account ->
                    onItemClicked(account)
                }
            }
        }

        fun bind(account: Account, context: Context) {
            currentAccount = account

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

            // Set account icon based on type
            val iconRes = when (account.accountType.lowercase()) {
                "bank" -> R.drawable.bank
                "cash" -> R.drawable.cash
                "e-wallet" -> R.drawable.wallet
                "credit" -> R.drawable.creditcard
                else -> R.drawable.creditcard
            }

            // If account has a custom image URL, load it using Glide
            if (!account.accountImageUrl.isNullOrEmpty()) {
                try {
                    Glide.with(context)
                        .load(account.accountImageUrl)
                        .apply(RequestOptions()
                            .placeholder(iconRes)
                            .error(iconRes)
                            .circleCrop())
                        .into(accountIcon)
                } catch (e: Exception) {
                    // Fallback to default icon if Glide fails
                    accountIcon.setImageResource(iconRes)
                }
            } else {
                // Use default icon
                accountIcon.setImageResource(iconRes)
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