package com.example.quanlynhahang

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class QuanLyMenu : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quan_ly_menu)

        val edtTen = findViewById<EditText>(R.id.edtTenMonMoi)
        val edtGia = findViewById<EditText>(R.id.edtGiaMon)
        val edtKho = findViewById<EditText>(R.id.edtIdKho)
        val edtDinh = findViewById<EditText>(R.id.edtDinhMuc)
        val btnLuu = findViewById<Button>(R.id.btnLuuMenu)

        val database = FirebaseDatabase.getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Menu")

        btnLuu.setOnClickListener {
            val ten = edtTen.text.toString().trim()
            val gia = edtGia.text.toString().trim().toIntOrNull() ?: 0
            val idKho = edtKho.text.toString().trim()
            val dinhMuc = edtDinh.text.toString().trim().toDoubleOrNull() ?: 0.0

            if (ten.isNotEmpty() && idKho.isNotEmpty()) {
                val monMoi = HashMap<String, Any>()
                monMoi["gia"] = gia
                monMoi["idKho"] = idKho
                monMoi["dinhMuc"] = dinhMuc
                // Chúng ta dùng tên món làm Key luôn để dễ quản lý
                database.child(ten).setValue(monMoi).addOnSuccessListener {
                    Toast.makeText(this, "Đã thêm $ten vào thực đơn!", Toast.LENGTH_SHORT).show()
                    finish() // Đóng màn hình sau khi thêm xong
                }
            } else {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}