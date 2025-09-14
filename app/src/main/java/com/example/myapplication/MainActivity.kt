package com.example.myapplication

import android.app.DatePickerDialog
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.google.firebase.auth.FirebaseAuth
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu
import android.content.Intent




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

    private val db = FirebaseFirestore.getInstance()
    private val expensesCollection = db.collection("expenses")

    private val currentUser = FirebaseAuth.getInstance().currentUser
    private val userId = currentUser?.uid

    private val categories = arrayOf(
        "Thuê nhà", "Hóa đơn điện", "Hóa đơn nước", "Hóa đơn internet",
        "Ăn uống", "Mua sắm", "Di chuyển", "Giải trí", "Y tế", "Giáo dục", "Khác"
    )

    private var isFormVisible = false
    private var totalAmount = 0.0
    private val currentMonth = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
    private lateinit var expenseAdapter: ExpenseAdapter
    private val expenseList = mutableListOf<Expense>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Thiết lập Toolbar làm ActionBar
//        setSupportActionBar(findViewById(R.id.toolbar))

        initializeViews()
        setupRecyclerView()
        setupCategorySpinner()
        setupDatePicker()
        setupFabButton()
        setupButtons()
        loadExpenses()
        setupBottomNavigation()
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
    }

    private fun setupRecyclerView() {
        expenseAdapter = ExpenseAdapter(expenseList)


        rvExpenses.layoutManager = LinearLayoutManager(this)
        rvExpenses.adapter = expenseAdapter

        // Thêm divider giữa các item
        val dividerItemDecoration = DividerItemDecoration(this, LinearLayoutManager.VERTICAL)
        rvExpenses.addItemDecoration(dividerItemDecoration)
    }

    private fun showExpenseDetail(expense: Expense) {
        // Hiển thị dialog hoặc activity chi tiết
        Toast.makeText(this, "Chi tiết: ${expense.description}", Toast.LENGTH_SHORT).show()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
    }

    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()

        btnDate.setOnClickListener {
            val datePicker = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth)
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    btnDate.text = dateFormat.format(calendar.time)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePicker.show()
        }
    }

    private fun setupFabButton() {
        fabAdd.setOnClickListener {
            if (isFormVisible) {
                hideForm()
            } else {
                showForm()
            }
        }
    }

    private fun setupButtons() {
        btnCancel.setOnClickListener {
            hideForm()
            clearForm()
        }

        btnSave.setOnClickListener {
            saveExpense()
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
        btnDate.text = "Chọn ngày"
    }

    private fun saveExpense() {
        val amountText = etAmount.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val category = spinnerCategory.selectedItem.toString()
        val dateText = btnDate.text.toString()

        if (amountText.isEmpty()) {
            etAmount.error = "Vui lòng nhập số tiền"
            return
        }

        val amount = try {
            amountText.toDouble()
        } catch (e: NumberFormatException) {
            etAmount.error = "Số tiền không hợp lệ"
            return
        }

        if (dateText == "Chọn ngày") {
            Toast.makeText(this, "Vui lòng chọn ngày", Toast.LENGTH_SHORT).show()
            return
        }

        // Tạo đối tượng expense
        val expense = hashMapOf(
            "userId" to userId,
            "amount" to amount,
            "description" to description,
            "category" to category,
            "date" to dateText,
            "monthYear" to currentMonth,
            "timestamp" to System.currentTimeMillis(),
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        // Lưu vào Firestore
        expensesCollection.add(expense)
            .addOnSuccessListener { documentReference ->
                Log.d("Firestore", "Document added with ID: ${documentReference.id}")

                // Tạo expense object từ dữ liệu vừa lưu
                val newExpense = Expense(
                    id = documentReference.id,
                    amount = amount,
                    description = description,
                    category = category,
                    date = dateText,
                    timestamp = System.currentTimeMillis()
                )

                // Thêm vào đầu danh sách và cập nhật adapter
                expenseList.add(0, newExpense)
                expenseAdapter.updateExpenses(expenseList)

                // Cập nhật tổng tiền
                totalAmount += amount
                updateTotalAmount()

                Toast.makeText(
                    this,
                    "Đã lưu chi tiêu: ${formatCurrency(amount)} VNĐ",
                    Toast.LENGTH_SHORT
                ).show()
                hideForm()
                clearForm()
            }
            .addOnFailureListener { e ->
                Log.w("Firestore", "Error adding document", e)
                Toast.makeText(this, "Lỗi khi lưu: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadExpenses() {
        expensesCollection
            .whereEqualTo("userId", userId)
            .whereEqualTo("monthYear", currentMonth)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                totalAmount = 0.0
                expenseList.clear()

                for (document in documents) {
                    val data = document.data
                    val expense = Expense(
                        id = document.id,
                        amount = data["amount"] as? Double ?: 0.0,
                        description = data["description"] as? String ?: "",
                        category = data["category"] as? String ?: "",
                        date = data["date"] as? String ?: "",
                        timestamp = data["timestamp"] as? Long ?: 0
                    )

                    expenseList.add(expense)
                    totalAmount += expense.amount
                }

                updateTotalAmount()
                expenseAdapter.updateExpenses(expenseList)

                if (expenseList.isNotEmpty()) {
                    Toast.makeText(this, "${expenseList.size} chi tiêu", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Log.e("Firestore", "Error getting documents: ${exception.message}", exception)
                Toast.makeText(this, "Firestore lỗi: ${exception.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun updateTotalAmount() {
        tvTotalAmount.text = "${formatCurrency(totalAmount)} VNĐ"
    }

    private fun formatCurrency(amount: Double): String {
        return "%,.0f".format(amount).replace(",", ".")
    }


    //kiểm tra phiene đăng nhập
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_account -> {
                // Lấy view làm anchor
                val anchorView = findViewById<View>(R.id.menu_account)
                showAccountPopupMenu(anchorView)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNavigation =
            findViewById<com.google.android.material.bottomnavigation.BottomNavigationView>(
                R.id.bottomNavigation
            )

        bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    hideForm()
                    true
                }

                R.id.nav_account -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }

                else -> false
            }
        }


    }
    private fun showAccountPopupMenu(anchorView: View) {
        val popup = PopupMenu(this, anchorView)
        popup.menuInflater.inflate(R.menu.account_popup_menu, popup.menu)

        val isLoggedIn = FirebaseAuth.getInstance().currentUser != null
        popup.menu.findItem(R.id.menu_login).isVisible = !isLoggedIn
        popup.menu.findItem(R.id.menu_logout).isVisible = isLoggedIn
        popup.menu.findItem(R.id.menu_profile).isVisible = isLoggedIn

        popup.setOnMenuItemClickListener { clickedItem ->
            when (clickedItem.itemId) {
                R.id.menu_login -> {
                    startActivity(Intent(this, LoginActivity::class.java))
                    true
                }

                R.id.menu_logout -> {
                    FirebaseAuth.getInstance().signOut()
                    recreate()
                    true
                }

                R.id.menu_profile -> {
                    Toast.makeText(this, "Xem hồ sơ", Toast.LENGTH_SHORT).show()
                    true
                }

                else -> false
            }
        }

        popup.show()
    }
}
