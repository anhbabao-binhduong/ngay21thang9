package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore

class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var edtUsername: EditText
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtConfirmPassword: EditText
    private lateinit var btnRegister: Button
    private lateinit var txtGoLogin: TextView

    private fun isPasswordValid(password: String): Boolean {
        val passwordRegex =
            Regex("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@\$!%*?&])[A-Za-z\\d@\$!%*?&]{6,}$")
        return passwordRegex.matches(password)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()

        edtUsername = findViewById(R.id.edtUsername)
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        edtConfirmPassword = findViewById(R.id.edtConfirmPassword)
        btnRegister = findViewById(R.id.btnRegister)
        txtGoLogin = findViewById(R.id.txtGoLogin)

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
                    .setPositiveButton("OK") { dialog, _ ->
                        edtPassword.text.clear()
                        edtConfirmPassword.text.clear()
                        edtPassword.requestFocus()
                        dialog.dismiss()
                    }
                    .setCancelable(false)
                    .show()
                return@setOnClickListener
            }

            // Đăng ký tài khoản
            mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (!task.isSuccessful) {
                        Toast.makeText(
                            this,
                            "Đăng ký thất bại: ${task.exception?.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                        return@addOnCompleteListener
                    }

                    // Cập nhật displayName để ProfileActivity hiển thị đúng tên
                    val user = mAuth.currentUser
                    if (user == null) {
                        Toast.makeText(this, "Không lấy được thông tin người dùng", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }

                    val profileUpdates = userProfileChangeRequest {
                        displayName = username
                        // photoUri = ... // nếu bạn có avatar mặc định
                    }

                    user.updateProfile(profileUpdates)
                        .addOnSuccessListener {
                            // (Tuỳ chọn) lưu thông tin vào Firestore
                            val db = FirebaseFirestore.getInstance()
                            val userMap = hashMapOf(
                                "uid" to user.uid,
                                "username" to username,
                                "email" to email,
                                "createdAt" to Timestamp.now()
                            )
                            db.collection("users").document(user.uid).set(userMap)

                            // Reload để đảm bảo displayName có ngay
                            user.reload().addOnCompleteListener {
                                Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Lỗi cập nhật tên: ${e.message}", Toast.LENGTH_SHORT).show()
                            // Dù lỗi updateProfile, vẫn điều hướng nhưng Profile sẽ fallback từ email
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        }
                }
        }

        txtGoLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}
