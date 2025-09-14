package com.example.myapplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var edtForgotEmail: EditText
    private lateinit var btnResetPassword: Button
    private lateinit var btnBackToLogin: Button
    private lateinit var mAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        mAuth = FirebaseAuth.getInstance()
        edtForgotEmail = findViewById(R.id.edtForgotEmail)
        btnResetPassword = findViewById(R.id.btnResetPassword)
        btnBackToLogin = findViewById(R.id.btnBackToLogin)

        btnResetPassword.setOnClickListener {
            val email = edtForgotEmail.text.toString().trim()

            if (email.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập email", Toast.LENGTH_SHORT).show()
            } else {
                mAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            AlertDialog.Builder(this)
                                .setTitle("Reset mật khẩu")
                                .setMessage("Email đặt lại mật khẩu đã được gửi tới $email")
                                .setPositiveButton("OK") { _, _ -> finish() }
                                .show()
                        } else {
                            Toast.makeText(this, "Không gửi được email. Kiểm tra lại địa chỉ!", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }

        // Xử lý nút quay về
        btnBackToLogin.setOnClickListener {
            finish() // Đóng trang ForgotPasswordActivity và quay về LoginActivity
        }
    }
}
