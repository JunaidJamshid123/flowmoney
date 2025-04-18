package com.example.flowmoney

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Set up the bottom navigation
        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_records -> {

                    true
                }
                R.id.nav_analysis -> {

                    true
                }
                R.id.nav_budgets -> {

                    true
                }
                R.id.nav_accounts -> {

                    true
                }
                R.id.nav_history -> {

                    true
                }
                else -> false
            }
        }

        // Set default selected item
        bottomNavigationView.selectedItemId = R.id.nav_records
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}