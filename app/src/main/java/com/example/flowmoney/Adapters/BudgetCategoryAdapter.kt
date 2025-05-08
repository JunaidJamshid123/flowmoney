package com.example.flowmoney.Adapters

import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Models.Category
import com.example.flowmoney.R
import com.example.flowmoney.SetBudget
import de.hdodenhof.circleimageview.CircleImageView

class BudgetCategoryAdapter(
    private val context: Context,
    private var categories: List<Category> = listOf()
) : RecyclerView.Adapter<BudgetCategoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryIcon: CircleImageView = itemView.findViewById(R.id.fl_category_background)
        val categoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        val setBudgetButton: Button = itemView.findViewById(R.id.btn_set_category_budget)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_budget_category, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = categories.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val category = categories[position]
        
        // Set category name
        holder.categoryName.text = category.name
        
        // Set category icon
        if (category.iconBase64.isNotEmpty()) {
            try {
                val decodedBytes = Base64.decode(category.iconBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.categoryIcon.setImageBitmap(bitmap)
                
                // Set border color based on income/expense
                val borderColor = if (category.isIncome) {
                    context.getColor(R.color.income_green)
                } else {
                    context.getColor(R.color.expense_red)
                }
                holder.categoryIcon.setBorderColor(borderColor)
            } catch (e: Exception) {
                holder.categoryIcon.setImageResource(R.drawable.shoppingg)
            }
        } else {
            holder.categoryIcon.setImageResource(R.drawable.shoppingg)
        }
        
        // Set button style based on income/expense type
        val buttonColor = if (category.isIncome) {
            context.getColor(R.color.income_green)
        } else {
            context.getColor(R.color.expense_red)
        }
        holder.setBudgetButton.setBackgroundColor(buttonColor)
        
        // Set button click listener
        holder.setBudgetButton.setOnClickListener {
            val intent = Intent(context, SetBudget::class.java)
            intent.putExtra("category_id", category.categoryId)
            intent.putExtra("category_name", category.name)
            intent.putExtra("category_icon", category.iconBase64)
            intent.putExtra("is_income", category.isIncome)
            context.startActivity(intent)
        }
    }
    
    fun updateCategories(newCategories: List<Category>) {
        categories = newCategories
        notifyDataSetChanged()
    }
} 