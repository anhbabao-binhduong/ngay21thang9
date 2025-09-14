package com.example.myapplication

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.PopupMenu
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var formLayout: View
    private lateinit var etAmount: TextInputEditText
    private lateinit var etDescription: TextInputEditText
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnDate: Button
    private lateinit var btnCancel: Button
    private lateinit var btnSave: Button
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var tvTotalAmount: TextView
    private lateinit var rvExpenses: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvSeeAll: TextView

    private val db = FirebaseFirestore.getInstance()
    private val expensesCollection = db.collection("expenses")

    // Auth động
    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()
    private val currentUser get() = auth.currentUser
    private val userId get() = currentUser?.uid

    private val categories = arrayOf(
        "Thuê nhà", "Hóa đơn điện", "Hóa đơn nước", "Hóa đơn internet",
        "Ăn uống", "Mua sắm", "Di chuyển", "Giải trí", "Y tế", "Giáo dục", "Khác"
    )

    private var isFormVisible = false
    private var totalAmount = 0.0
    private lateinit var expenseAdapter: ExpenseAdapter
    private val expenseList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dùng đúng layout bạn đã khai báo
        setContentView(R.layout.activity_main)

        initializeViews()
        setupRecyclerView()
        setupCategorySpinner()
        setupDatePicker()
        setupFabButton()
        setupButtons()
        setupSeeAll()

        // Set mặc định hôm nay cho nút ngày
        btnDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        // 🔹 Trang chủ: load TẤT CẢ chi tiêu (mọi tháng)
        loadAllExpenses()
        setupBottomNavigation()
    }

    override fun onResume() {
        super.onResume()
        hideForm()
        loadAllExpenses()
    }

    private fun initializeViews() {
        formLayout = findViewById(R.id.formLayout)
        etAmount = findViewById(R.id.etAmount)
        etDescription = findViewById(R.id.etDescription)
        spinnerCategory = findViewById(R.id.spinnerCategory)
        btnDate = findViewById(R.id.btnDate)
        btnCancel = findViewById(R.id.btnCancel)
        btnSave = findViewById(R.id.btnSave)
        fabAdd = findViewById(R.id.fabAdd)
        tvTotalAmount = findViewById(R.id.tvTotalAmount)
        rvExpenses = findViewById(R.id.rvExpenses)
        tvSeeAll = findViewById(R.id.tvSeeAll)
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(expenseList)
        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = expenseAdapter
        rvExpenses.addItemDecoration(DividerItemDecoration(this, LinearLayoutManager.VERTICAL))
        // RecyclerView cao 180dp theo layout -> hiển thị ~2 item và có thể scroll
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        btnDate.setOnClickListener {
            val dialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    btnDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                        .format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            dialog.show()
        }
    }

    // Yêu cầu login trước khi làm action
    private fun requireLoginThen(action: () -> Unit) {
        if (currentUser == null) {
            Toast.makeText(this, "Bạn cần đăng nhập để sử dụng chức năng này", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
        } else action()
    }

    private fun setupFabButton() {
        fabAdd.setOnClickListener {
            requireLoginThen { if (isFormVisible) hideForm() else showForm() }
        }
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener { hideForm(); clearForm() }
        btnSave.setOnClickListener { requireLoginThen { saveExpense() } }
    }

    private fun setupSeeAll() {
        tvSeeAll.setOnClickListener {
            requireLoginThen { startActivity(Intent(this, StatisticActivity::class.java)) }
        }
    }

    private fun showForm() {
        formLayout.visibility = View.VISIBLE
        isFormVisible = true
        updateFabIcon()
    }

    private fun hideForm() {
        formLayout.visibility = View.GONE
        isFormVisible = false
        updateFabIcon()
    }

    private fun updateFabIcon() {
        if (isFormVisible) {
            fabAdd.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_close))
            fabAdd.contentDescription = "Đóng form thêm chi tiêu"
        } else {
            fabAdd.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.ic_add))
            fabAdd.contentDescription = "Mở form thêm chi tiêu"
        }
    }

    private fun clearForm() {
        etAmount.text?.clear()
        etDescription.text?.clear()
        spinnerCategory.setSelection(0)
        btnDate.text = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())
    }

    // Parse ngày an toàn (chấp nhận vài format thường gặp)
    private fun safeParseDateOrNull(dateStr: String): Date? {
        val patterns = listOf("dd/MM/yyyy", "d/M/yyyy", "dd-M-yyyy", "yyyy-MM-dd")
        for (p in patterns) {
            try {
                val fmt = SimpleDateFormat(p, Locale.getDefault()).apply { isLenient = false }
                return fmt.parse(dateStr)
            } catch (_: Exception) { }
        }
        Log.w("ExpenseDate", "Không parse được ngày: $dateStr")
        return null
    }

    private fun monthYearFrom(date: Date): String =
        SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(date)

    private fun saveExpense() {
        val amountText = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val dateText = btnDate.text.toString().trim()

        if (amountText.isEmpty()) { etAmount.error = "Vui lòng nhập số tiền"; return }
        val amount = try { amountText.toDouble() } catch (_: NumberFormatException) {
            etAmount.error = "Số tiền không hợp lệ"; return
        }
        if (dateText.isEmpty() || dateText.equals("Chọn ngày", ignoreCase = true)) {
            Toast.makeText(this, "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show(); return
        }
        val uid = userId ?: run { startActivity(Intent(this, LoginActivity::class.java)); return }

        val pickedDate = safeParseDateOrNull(dateText) ?: Date()
        val monthYear = monthYearFrom(pickedDate)
        val pickedTime = pickedDate.time

        val expense = hashMapOf(
            "userId" to uid,
            "amount" to amount,
            "description" to description,
            "category" to category,
            "date" to SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(pickedDate),
            "monthYear" to monthYear,      // để trang Thống kê lọc theo tháng
            "timestamp" to pickedTime,     // dùng sắp xếp
            "createdAt" to Timestamp.now()
        )

        expensesCollection.add(expense)
            .addOnSuccessListener { doc ->
                val newExpense = Expense(
                    id = doc.id,
                    amount = amount,
                    description = description,
                    category = category,
                    date = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(pickedDate),
                    timestamp = pickedTime
                )

                // Trang chủ hiển thị TẤT CẢ, nên thêm trực tiếp vào đầu danh sách
                expenseList.add(0, newExpense)
                expenseAdapter.updateExpenses(expenseList)

                totalAmount += amount
                updateTotalAmount()

                hideForm(); clearForm()
                Toast.makeText(this, "Đã lưu: ${formatCurrency(amount)} VNĐ", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Lỗi khi lưu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /** 🔹 Trang chủ: tải TẤT CẢ chi tiêu của user (mọi tháng) */
    private fun loadAllExpenses() {
        val uid = userId ?: run {
            totalAmount = 0.0
            expenseList.clear()
            updateTotalAmount()
            expenseAdapter.updateExpenses(expenseList)
            return
        }

        // Không dùng orderBy để tránh yêu cầu composite index; sort ở client
        expensesCollection
            .whereEqualTo("userId", uid)
            .get()
            .addOnSuccessListener { docs ->
                totalAmount = 0.0
                val tmp = mutableListOf<Expense>()
                for (document in docs) {
                    val data = document.data
                    val expense = Expense(
                        id = document.id,
                        amount = (data["amount"] as? Number)?.toDouble() ?: 0.0,
                        description = data["description"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        date = data["date"] as? String ?: "",
                        timestamp = (data["timestamp"] as? Number)?.toLong() ?: 0L
                    )
                    tmp.add(expense)
                    totalAmount += expense.amount
                }

                // Sắp xếp mới nhất lên trên
                tmp.sortByDescending { it.timestamp }

                expenseList.clear()
                expenseList.addAll(tmp)

                updateTotalAmount()
                expenseAdapter.updateExpenses(expenseList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Firestore lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateTotalAmount() {
        tvTotalAmount.text = "${formatCurrency(totalAmount)} VNĐ"
    }

    private fun formatCurrency(amount: Double): String =
        "%,.0f".format(amount).replace(",", ".")

    // (nếu có menu_account)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_account -> {
                val anchorView = findViewById<View>(R.id.menu_account) ?: fabAdd
                showAccountPopupMenu(anchorView); true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation =
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(R.id.bottomNavigation)

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { hideForm(); loadAllExpenses(); true }
                R.id.nav_statistics -> {
                    requireLoginThen {
                        startActivity(Intent(this, StatisticActivity::class.java))
                    }
                    true
                }
                R.id.nav_account -> { startActivity(Intent(this, ProfileActivity::class.java)); true }
                else -> false
            }
        }
    }

    private fun showAccountPopupMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.account_popup_menu, popup.menu)
        val isLoggedIn = currentUser != null
        popup.menu.findItem(R.id.menu_login).isVisible = !isLoggedIn
        popup.menu.findItem(R.id.menu_logout).isVisible = isLoggedIn
        popup.menu.findItem(R.id.menu_profile).isVisible = isLoggedIn

        popup.setOnMenuItemClickListener { clickedItem ->
            when (clickedItem.itemId) {
                R.id.menu_login -> { startActivity(Intent(this, LoginActivity::class.java)); true }
                R.id.menu_logout -> { FirebaseAuth.getInstance().signOut(); recreate(); true }
                R.id.menu_profile -> { Toast.makeText(this, "Xem hồ sơ", Toast.LENGTH_SHORT).show(); true }
                else -> false
            }
        }
        popup.show()
    }
}
