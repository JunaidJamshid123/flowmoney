package com.example.flowmoney.Adapters

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.flowmoney.Models.Category
import com.example.flowmoney.R
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Adapter for displaying categories in a RecyclerView
 */
class CategoryAdapter(
    private val categories: List<Category>,
    private val onCategoryClicked: (Category) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val categoryName: TextView = itemView.findViewById(R.id.tv_category_name)
        val categoryIcon: ImageView = itemView.findViewById(R.id.iv_category_icon)
        val categoryDate: TextView = itemView.findViewById(R.id.tv_category_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.category_item, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = categories[position]

        // Set category name
        holder.categoryName.text = category.name

        // Set category icon
        if (category.iconBase64.isNotEmpty()) {
            try {
                // Convert Base64 string to bitmap
                val decodedBytes = Base64.decode(category.iconBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.categoryIcon.setImageBitmap(bitmap)
            } catch (e: Exception) {
                // If there's an error with the Base64 string, use the resource ID as fallback
                if (category.iconResourceId != 0) {
                    holder.categoryIcon.setImageResource(category.iconResourceId)
                } else {
                    // Use a default icon if all else fails
                    holder.categoryIcon.setImageResource(R.drawable.cash)
                }
            }
        } else if (category.iconResourceId != 0) {
            // Use resource ID if Base64 is empty
            holder.categoryIcon.setImageResource(category.iconResourceId)
        } else {
            // Use default icon if both are missing
            holder.categoryIcon.setImageResource(R.drawable.cash)
        }

        // Set creation date
        val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        holder.categoryDate.text = dateFormat.format(category.createdAt)

        // Set click listener
        holder.itemView.setOnClickListener {
            onCategoryClicked(category)
        }
    }

    override fun getItemCount(): Int = categories.size
}