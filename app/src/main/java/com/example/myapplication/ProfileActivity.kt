package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import android.widget.PopupMenu   // ✅ Thêm dòng này
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ProfileActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val tvUsername = findViewById<TextView>(R.id.tvUsername)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnProfile = findViewById<Button>(R.id.btnProfile)

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            tvUsername.text = user.email ?: "Người dùng"
            btnLogout.visibility = View.VISIBLE
            btnProfile.visibility = View.VISIBLE
        } else {
            tvUsername.text = "Khách"
            btnLogin.visibility = View.VISIBLE
        }

        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            recreate() // refresh lại trang để hiển thị đúng các nút
        }

        btnProfile.setOnClickListener {
            Toast.makeText(this, "Đang ở trang hồ sơ", Toast.LENGTH_SHORT).show()
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        val bottomNavigation =
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottomNavigation
            )

        bottomNavigation.selectedItemId = R.id.nav_account

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)
                    finish()
                    true
                }
                R.id.nav_account -> true
                else -> false
            }
        }
    }
}
