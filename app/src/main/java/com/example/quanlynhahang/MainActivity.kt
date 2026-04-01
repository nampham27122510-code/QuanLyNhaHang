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
        // Kết nối với file XML giao diện tím xịn xò của Nam
        setContentView(R.layout.activity_main)

        // Ánh xạ đúng các ID từ file activity_main.xml
        val edtTaiKhoan = findViewById<EditText>(R.id.edtTaiKhoan)
        val edtMatKhau = findViewById<EditText>(R.id.edtMatKhau)
        val btnDangNhap = findViewById<Button>(R.id.btnDangNhap)

        btnDangNhap.setOnClickListener {
            val user = edtTaiKhoan.text.toString().trim()
            val pass = edtMatKhau.text.toString().trim()

            // Kiểm tra nhập liệu trống
            if (user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Xử lý đăng nhập cho từng bộ phận
            when {
                user == "admin" && pass == "123" -> {
                    startActivity(Intent(this, Admin::class.java))
                    finish() // Đóng màn hình đăng nhập sau khi vào thành công
                }
                user == "bep1" && pass == "123" -> {
                    startActivity(Intent(this, Bep::class.java))
                    finish()
                }
                user == "phucvu" && pass == "123" -> {
                    // Chuyển sang Sơ đồ 30 bàn (NhanVienDashboard) thay vì vào thẳng nhanvien
                    val intent = Intent(this, NhanVienDashboard::class.java)
                    startActivity(intent)
                    finish()
                }
                else -> {
                    Toast.makeText(this, "Sai tài khoản hoặc mật khẩu!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}