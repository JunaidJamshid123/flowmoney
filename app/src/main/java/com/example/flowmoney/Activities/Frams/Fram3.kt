package com.example.flowmoney.Activities.Frams

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.flowmoney.Activities.Authentication.Login

import com.example.flowmoney.R

class Fram3 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fram3)

        // Initialize Views
        val btnSkip = findViewById<TextView>(R.id.btnSkip)
        val btnGetStarted = findViewById<Button>(R.id.btnGetStarted)

        // Handle "Skip" click
        btnSkip.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }

        // Handle "Get Started" click
        btnGetStarted.setOnClickListener {
            val intent = Intent(this, Login::class.java)
            startActivity(intent)
            finish()
        }
    }
}
