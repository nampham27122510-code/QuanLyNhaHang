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

        // Ánh xạ các View từ XML
        val edtTaiKhoan = findViewById<EditText>(R.id.edtTaiKhoan)
        val edtMatKhau = findViewById<EditText>(R.id.edtMatKhau)
        val btnDangNhap = findViewById<Button>(R.id.btnDangNhap)

        btnDangNhap.setOnClickListener {
            val user = edtTaiKhoan.text.toString().trim()
            val pass = edtMatKhau.text.toString().trim()

            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Kiểm tra phân quyền tài khoản
            when {
                // 1. Tài khoản ADMIN
                user == "admin" && pass == "123" -> {
                    Toast.makeText(this, "Chào mừng Admin!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Admin::class.java))
                    finish() // Đóng màn hình đăng nhập
                }

                // 2. Tài khoản BẾP
                user == "bep1" && pass == "123" -> {
                    Toast.makeText(this, "Đã đăng nhập vào hệ thống Bếp!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Bep::class.java))
                    finish()
                }

                // 3. Tài khoản KHÁCH HÀNG (Dùng để gọi món tại bàn)
                user == "khach" && pass == "123" -> {
                    Toast.makeText(this, "Chào mừng quý khách!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, Order::class.java))
                    finish()
                }

                // 4. CẬP NHẬT: Tài khoản NHÂN VIÊN TIẾP TÂN (Xử lý thanh toán)
                user == "phucvu" && pass == "123" -> {
                    Toast.makeText(this, "Đã đăng nhập hệ thống Phục vụ!", Toast.LENGTH_SHORT).show()
                    // Chuyển tới màn hình Trạm tiếp tân vừa mới tạo
                    startActivity(Intent(this, nhanvien::class.java))
                    finish()
                }

                else -> {
                    Toast.makeText(this, "Tài khoản hoặc mật khẩu không chính xác!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}