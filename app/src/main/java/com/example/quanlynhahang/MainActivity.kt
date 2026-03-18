package com.example.quanlynhahang

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val edtTaiKhoan = findViewById<EditText>(R.id.edtTaiKhoan)
        val edtMatKhau = findViewById<EditText>(R.id.edtMatKhau)
        val btnDangNhap = findViewById<Button>(R.id.btnDangNhap)

        btnDangNhap.setOnClickListener {
            val user = edtTaiKhoan.text.toString().trim()
            val pass = edtMatKhau.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            when {
                user == "admin" && pass == "123" -> {
                    startActivity(Intent(this, Admin::class.java))
                    finish() // Vào Admin xong thì đóng Login
                }
                user == "bep1" && pass == "123" -> {
                    startActivity(Intent(this, Bep::class.java))
                    finish()
                }
                user == "phucvu" && pass == "123" -> {
                    startActivity(Intent(this, nhanvien::class.java))
                    finish()
                }
                else -> Toast.makeText(this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
            }
        }

        // MẶC ĐỊNH: Nhấn Back tại đây sẽ tự động quay về màn hình Order
        // vì Order đang ở dưới Backstack.
    }
}