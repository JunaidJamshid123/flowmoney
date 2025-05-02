package com.example.flowmoney

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.view.MenuItem
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.flowmoney.Fragments.*
import com.example.flowmoney.Models.User
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationView
import androidx.appcompat.widget.Toolbar
import android.widget.Toast
import com.bumptech.glide.Glide
import com.example.flowmoney.Activities.HistoryActivity
import com.example.flowmoney.Activities.ProfileActivity
import com.example.flowmoney.Activities.SearchActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Firebase components
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Set up the toolbar
        val toolbar = findViewById<Toolbar>(R.id.app_bar_main)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // Set up the drawer layout
        drawerLayout = findViewById(R.id.drawer_layout)
        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Set up the navigation view
        navigationView = findViewById(R.id.nav_view)
        navigationView.setNavigationItemSelectedListener(this)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupBottomNavigation()

        // Set initial fragment
        if (savedInstanceState == null) {
            replaceFragment(RecordFragment())
            navigationView.setCheckedItem(R.id.nav_home)
        }

        // Load user data for the navigation header
        loadUserData()
    }

    private fun loadUserData() {
        val currentUser = auth.currentUser

        currentUser?.let { user ->
            // Get the user data from Firestore using the current user's UID
            firestore.collection("users")
                .document(user.uid)
                .get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists()) {
                        // Create a User object from the document data
                        val userData = User()
                        userData.userId = documentSnapshot.id
                        userData.fullName = documentSnapshot.getString("full_name") ?: ""
                        userData.username = documentSnapshot.getString("username") ?: ""
                        userData.email = documentSnapshot.getString("email") ?: ""
                        userData.phoneNumber = documentSnapshot.getString("phone_number")
                        userData.profileImageUrl = documentSnapshot.getString("profile_image_url")
                        userData.address = documentSnapshot.getString("address")
                        userData.isSocialLogin = documentSnapshot.getBoolean("is_social_login") ?: false
                        userData.socialLoginType = documentSnapshot.getString("social_login_type")

                        // Update the navigation header with user data
                        updateNavigationHeader(userData)
                    } else {
                        // User document doesn't exist in Firestore
                        Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                        // Show default values in the navigation header
                        showDefaultNavigationHeader()
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Error loading user data: ${e.message}", Toast.LENGTH_SHORT).show()
                    // Show default values in the navigation header
                    showDefaultNavigationHeader()
                }
        } ?: run {
            // User is not logged in
            showDefaultNavigationHeader()
        }
    }

    private fun showDefaultNavigationHeader() {
        val headerView = navigationView.getHeaderView(0)
        val profileName = headerView.findViewById<TextView>(R.id.profile_name)
        val profileEmail = headerView.findViewById<TextView>(R.id.profile_email)
        val profileImageView = headerView.findViewById<CircleImageView>(R.id.profile_image)

        profileName.text = "Guest User"
        profileEmail.text = "Please sign in"
        profileImageView.setImageResource(R.drawable.default_profile)
    }

    private fun updateNavigationHeader(user: User) {
        // Get the header view from navigation view
        val headerView = navigationView.getHeaderView(0)

        // Find views in the header layout
        val profileImageView = headerView.findViewById<CircleImageView>(R.id.profile_image)
        val profileNameTextView = headerView.findViewById<TextView>(R.id.profile_name)
        val profileEmailTextView = headerView.findViewById<TextView>(R.id.profile_email)

        // Set user data to views
        profileNameTextView.text = user.fullName
        profileEmailTextView.text = user.email

        // Load profile image if available
        if (!user.profileImageUrl.isNullOrEmpty()) {
            try {
                // Check if the image is a Base64 string
                if (user.profileImageUrl!!.length > 100) { // Likely a Base64 string if very long
                    try {
                        val imageBytes = Base64.decode(user.profileImageUrl, Base64.DEFAULT)
                        val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

                        if (decodedBitmap != null) {
                            profileImageView.setImageBitmap(decodedBitmap)
                        } else {
                            // If bitmap decoding fails, use default image
                            profileImageView.setImageResource(R.drawable.default_profile)
                        }
                    } catch (e: Exception) {
                        // If Base64 decoding fails, try loading as URL
                        Glide.with(this)
                            .load(user.profileImageUrl)
                            .placeholder(R.drawable.default_profile)
                            .error(R.drawable.default_profile)
                            .into(profileImageView)
                    }
                } else {
                    // Treat as URL if not Base64
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.default_profile)
                        .error(R.drawable.default_profile)
                        .into(profileImageView)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // If all attempts fail, use default image
                profileImageView.setImageResource(R.drawable.default_profile)
            }
        } else {
            // No profile image URL, use default
            profileImageView.setImageResource(R.drawable.default_profile)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_records -> {
                    replaceFragment(RecordFragment())
                    true
                }
                R.id.nav_analysis -> {
                    replaceFragment(AnalysisFragment())
                    true
                }
                R.id.nav_budgets -> {
                    replaceFragment(BudgetFragment())
                    true
                }
                R.id.nav_accounts -> {
                    replaceFragment(AccountsFragment())
                    true
                }
                R.id.nav_history -> {
                    replaceFragment(CategoryFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation drawer item clicks
        when (item.itemId) {
            R.id.nav_home -> {
                replaceFragment(RecordFragment())
                Toast.makeText(this, "Home", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_message -> {
                Toast.makeText(this, "Search", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, SearchActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_sync -> {
                Toast.makeText(this, "Sync", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_trash -> {
                Toast.makeText(this, "Trash", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_settings -> {
                Toast.makeText(this, "History", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, HistoryActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_profile -> {
                Toast.makeText(this, "Profile", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
            }
            R.id.nav_login -> {
                Toast.makeText(this, "Login", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_share -> {
                Toast.makeText(this, "Share", Toast.LENGTH_SHORT).show()
            }
            R.id.nav_rate -> {
                Toast.makeText(this, "Rate us", Toast.LENGTH_SHORT).show()
            }
        }

        // Close the drawer
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    override fun onBackPressed() {
        // Close drawer first if it's open, otherwise handle back normally
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    // Call this method when user logs in/out to refresh the navigation header
    fun refreshUserData() {
        loadUserData()
    }

    // This method should be called from ProfileActivity after profile update
    override fun onResume() {
        super.onResume()
        // Reload user data when returning to MainActivity
        loadUserData()
    }
}