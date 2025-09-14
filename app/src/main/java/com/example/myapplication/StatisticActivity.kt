package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class StatisticActivity : AppCompatActivity() {

    private lateinit var tvStats: TextView
    private lateinit var btnSelectMonth: Button

    private val db = FirebaseFirestore.getInstance()
    private val currentUser get() = FirebaseAuth.getInstance().currentUser

    private var selectedMonthYear: String =
        SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistic)

        tvStats = findViewById(R.id.tvStats)
        btnSelectMonth = findViewById(R.id.btnSelectMonth)

        btnSelectMonth.text = "Tháng: $selectedMonthYear"
        btnSelectMonth.setOnClickListener { showMonthYearDialog() }

        loadStatistics(selectedMonthYear)
        setupBottomNavigation()
    }

    /** Dialog 2 Spinner: Chọn tháng & năm */
    private fun showMonthYearDialog() {
        val months = (1..12).map { String.format("%02d", it) }.toTypedArray()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val years = (currentYear - 5..currentYear + 5).map { it.toString() }.toTypedArray()

        val view = layoutInflater.inflate(R.layout.dialog_month_year, null)
        val spMonth = view.findViewById<Spinner>(R.id.spMonth)
        val spYear = view.findViewById<Spinner>(R.id.spYear)

        spMonth.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, months)
        spYear.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, years)

        // Preselect theo tháng/năm hiện tại
        spMonth.setSelection(months.indexOf(selectedMonthYear.substring(0, 2)))
        spYear.setSelection(years.indexOf(selectedMonthYear.substring(3)))

        AlertDialog.Builder(this)
            .setTitle("Chọn tháng/năm")
            .setView(view)
            .setPositiveButton("OK") { _, _ ->
                val month = spMonth.selectedItem.toString()
                val year = spYear.selectedItem.toString()
                selectedMonthYear = "$month/$year"
                btnSelectMonth.text = "Tháng: $selectedMonthYear"
                loadStatistics(selectedMonthYear)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun loadStatistics(monthYear: String) {
        val uid = currentUser?.uid ?: run { tvStats.text = "Bạn chưa đăng nhập."; return }

        db.collection("expenses")
            .whereEqualTo("userId", uid)
            .whereEqualTo("monthYear", monthYear)
            .get()
            .addOnSuccessListener { documents ->
                var total = 0.0
                val categoryMap = mutableMapOf<String, Double>()
                for (doc in documents) {
                    val amount = (doc["amount"] as? Number)?.toDouble() ?: 0.0
                    val category = doc["category"] as? String ?: "Khác"
                    total += amount
                    categoryMap[category] = (categoryMap[category] ?: 0.0) + amount
                }

                val b = StringBuilder()
                b.append("Chi tiêu tháng $monthYear\n")
                b.append("Tổng cộng: ${"%,.0f".format(total).replace(",", ".")} VNĐ\n\n")
                b.append("Theo danh mục:\n")
                for ((cat, sum) in categoryMap) {
                    b.append("- $cat: ${"%,.0f".format(sum).replace(",", ".")} VNĐ\n")
                }
                if (categoryMap.isEmpty()) b.append("\nChưa có chi tiêu trong tháng này.")
                tvStats.text = b.toString()
            }
            .addOnFailureListener { e -> tvStats.text = "Lỗi tải thống kê: ${e.message}" }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNavigation.selectedItemId = R.id.nav_statistics
        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    startActivity(Intent(this, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP))
                    true
                }
                R.id.nav_statistics -> true
                R.id.nav_account -> {
                    val user = FirebaseAuth.getInstance().currentUser
                    if (user == null) startActivity(Intent(this, LoginActivity::class.java))
                    else startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }
}
