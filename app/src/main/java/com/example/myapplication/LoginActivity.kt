package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var edtEmail: EditText
    private lateinit var edtPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var txtGoRegister: TextView
    private lateinit var txtForgotPassword: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        // Ánh xạ view
        edtEmail = findViewById(R.id.edtEmail)
        edtPassword = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        txtGoRegister = findViewById(R.id.txtGoRegister)
        txtForgotPassword = findViewById(R.id.txtForgotPassword)

        // Xử lý login
        btnLogin.setOnClickListener {
            val email = edtEmail.text.toString().trim()
            val password = edtPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ email và mật khẩu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        Log.d("Login", "signInWithEmail:success")
                        Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                        // Chuyển sang MainActivity
                        val intent = Intent(this, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Log.w("Login", "signInWithEmail:failure", task.exception)
                        Toast.makeText(this, "Sai email hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        // Chuyển sang trang đăng ký
        txtGoRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Quên mật khẩu
        txtForgotPassword.setOnClickListener {
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

    }

}
