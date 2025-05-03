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
import java.text.NumberFormat
import java.util.Locale

class AccountsAdapter(
    private val context: Context,
    private val onAccountClick: (Account) -> Unit
) : ListAdapter<Account, AccountsAdapter.AccountViewHolder>(AccountDiffCallback()) {

    // View holder for the account items
    class AccountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val iconImageView: ImageView = itemView.findViewById(R.id.iv_account_icon)
        val nameTextView: TextView = itemView.findViewById(R.id.tv_account_name)
        val typeTextView: TextView = itemView.findViewById(R.id.tv_account_type)
        val balanceTextView: TextView = itemView.findViewById(R.id.tv_account_balance)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AccountViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.account_item, parent, false)
        return AccountViewHolder(view)
    }

    override fun onBindViewHolder(holder: AccountViewHolder, position: Int) {
        val account = getItem(position)

        // Set account name
        holder.nameTextView.text = account.accountName

        // Set account type
        holder.typeTextView.text = account.accountType

        // Format and set balance
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())
        holder.balanceTextView.text = currencyFormat.format(account.balance)

        // Set appropriate icon based on account type
        when (account.accountType.lowercase()) {
            "bank", "checking", "savings" -> {
                holder.iconImageView.setImageResource(R.drawable.bank)
            }
            "cash" -> {
                holder.iconImageView.setImageResource(R.drawable.cash)
            }
            "credit" -> {
                holder.iconImageView.setImageResource(R.drawable.creditcard)
            }
            "wallet", "e-wallet" -> {
                holder.iconImageView.setImageResource(R.drawable.wallet)
            }
            "crypto" -> {
                holder.iconImageView.setImageResource(R.drawable.bitcoin)
            }
            else -> {
                holder.iconImageView.setImageResource(R.drawable.creditcard)
            }
        }

        // Set click listener
        holder.itemView.setOnClickListener {
            onAccountClick(account)
        }
    }
}

// DiffUtil callback for efficient list updates
class AccountDiffCallback : DiffUtil.ItemCallback<Account>() {
    override fun areItemsTheSame(oldItem: Account, newItem: Account): Boolean {
        return oldItem.accountId == newItem.accountId
    }

    override fun areContentsTheSame(oldItem: Account, newItem: Account): Boolean {
        return oldItem.accountName == newItem.accountName &&
                oldItem.accountType == newItem.accountType &&
                oldItem.balance == newItem.balance
    }
}