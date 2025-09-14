package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import androidx.appcompat.app.AlertDialog

class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var edtUsername: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var txtGoLogin: TextView

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex = Regex(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{6,}$"
        )
        return passwordRegex.matches(password)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        // FirebaseAuth instance
        mAuth = FirebaseAuth.getInstance()

        // Ánh xạ view từ XML
        edtUsername = findViewById(R.id.edtUsername)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        txtGoLogin = findViewById(R.id.txtGoLogin)

        // Xử lý sự kiện nút Đăng ký
        btnRegister.setOnClickListener {
            val username = edtUsername.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()
            val confirmPassword = edtConfirmPassword.text.toString().trim()

            if (username.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isPasswordValid(password)) {
                AlertDialog.Builder(this)
                    .setTitle("Mật khẩu chưa hợp lệ")
                    .setMessage(
                        """
            Mật khẩu phải thỏa mãn:
            • Ít nhất 6 ký tự
            • Có chữ thường
            • Có chữ in hoa
            • Có số
            • Có ký tự đặc biệt
            """.trimIndent()
                    )
                    .setPositiveButton("OK") { dialogInterface, _ ->
                        // Khi nhấn OK, xóa mật khẩu để user nhập lại
                        edtPassword.text.clear()
                        edtConfirmPassword.text.clear()
                        edtPassword.requestFocus()
                        dialogInterface.dismiss()
                    }
                    .setCancelable(false) // bắt buộc phải nhấn OK
                    .show()
                return@setOnClickListener
            }




            // ✅ Nếu hợp lệ thì tiến hành đăng ký Firebase
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val user = mAuth.currentUser
                        val userId = user?.uid ?: return@addOnCompleteListener

                        val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        val userMap = hashMapOf(
                            "uid" to userId,
                            "username" to username,
                            "email" to email,
                            "createdAt" to com.google.firebase.Timestamp.now()
                        )

                        db.collection("users").document(userId).set(userMap)

                        Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    } else {
                        Toast.makeText(
                            this,
                            "Đăng ký thất bại: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
        }


        // Chuyển sang LoginActivity khi bấm TextView
        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
