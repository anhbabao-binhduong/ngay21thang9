package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import java.util.Locale


class ProfileActivity : AppCompatActivity() {

    private lateinit var tvUsername: TextView
    private lateinit var tvEmail: TextView
    private lateinit var btnLogin: Button
    private lateinit var btnLogout: Button
    private lateinit var btnProfile: Button
    private lateinit var imgAvatar: ImageView
    private lateinit var bottomNavigation: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        bindViews()
        bindUserState()
        bindActions()
        setupBottomNavigation()
    }

    private fun bindViews() {
        tvUsername = findViewById(R.id.tvUsername)
        tvEmail = findViewById(R.id.tvEmail)
        btnLogin = findViewById(R.id.btnLogin)
        btnLogout = findViewById(R.id.btnLogout)
        btnProfile = findViewById(R.id.btnProfile)
        imgAvatar = findViewById(R.id.imgAvatar)
        bottomNavigation = findViewById(R.id.bottomNavigation)
    }

    private fun bindUserState() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val niceName = when {
                !user.displayName.isNullOrBlank() -> user.displayName!!
                // một số provider để tên trong providerData
                user.providerData.any { !it.displayName.isNullOrBlank() } ->
                    user.providerData.first { !it.displayName.isNullOrBlank() }.displayName!!
                else -> makeNameFromEmail(user.email)
            }

            tvUsername.text = niceName
            tvEmail.text = user.email ?: "—"

            btnLogin.visibility = View.GONE
            btnLogout.visibility = View.VISIBLE
            btnProfile.visibility = View.VISIBLE
            imgAvatar.contentDescription = niceName
        } else {
            tvUsername.text = "Khách"
            tvEmail.text = "—"
            btnLogin.visibility = View.VISIBLE
            btnLogout.visibility = View.GONE
            btnProfile.visibility = View.GONE
        }
    }

    /** Tạo tên đẹp từ email: "sidat123@gmail.com" -> "Sidat123" hoặc "si dat123" nếu có dấu . _ - */
    private fun makeNameFromEmail(email: String?): String {
        if (email.isNullOrBlank()) return "Người dùng"
        val local = email.substringBefore('@')
        // tách theo . _ - rồi viết hoa chữ cái đầu
        return local.split('.', '_', '-')
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase(Locale.getDefault()) else c.toString() }
            }
    }


    private fun bindActions() {
        btnLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
        btnLogout.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            recreate() // refresh để cập nhật nút & thông tin
        }
        btnProfile.setOnClickListener {
            Toast.makeText(this, "Đang ở trang hồ sơ", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupBottomNavigation() {
        // đồng bộ với nav: chọn tab Account
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
                R.id.nav_statistics -> {
                    val intent = Intent(this, StatisticActivity::class.java)
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
