package com.example.flowmoney.Activities.Frams

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.flowmoney.Activities.Authentication.Login
import com.example.flowmoney.R

class Fram1 : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_fram1)

        val btnNext = findViewById<Button>(R.id.btnNext)
        val btnSkip = findViewById<Button>(R.id.btnSkip)

        btnNext.setOnClickListener {
            // Navigate to Fram2 activity
            val intent = Intent(this, Fram2::class.java)
            startActivity(intent)
        }

        btnSkip.setOnClickListener {
            // Navigate to Login activity
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish() // Optional: prevents returning to onboarding screen with back button
        }
    }
}
