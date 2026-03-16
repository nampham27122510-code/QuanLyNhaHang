package com.example.quanlynhahang

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class Kho : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_kho)

        val edtTen = findViewById<EditText>(R.id.edtTenVatLieu)
        val edtSoLuong = findViewById<EditText>(R.id.edtSoLuong)
        val btnCapNhat = findViewById<Button>(R.id.btnCapNhatKho)
        val txtDanhSach = findViewById<TextView>(R.id.txtDanhSachKho)

        // Kết nối tới nhánh "Warehouse" trên Firebase
        val database = FirebaseDatabase
            .getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Warehouse")

        btnCapNhat.setOnClickListener {
            val ten = edtTen.text.toString().trim()
            val sl = edtSoLuong.text.toString().trim()

            if (ten.isNotEmpty() && sl.isNotEmpty()) {
                // Lưu vào Firebase: Tên vật liệu làm Key, Số lượng làm Value
                database.child(ten).setValue(sl).addOnSuccessListener {
                    Toast.makeText(this, "Đã cập nhật $ten", Toast.LENGTH_SHORT).show()
                    edtTen.text.clear()
                    edtSoLuong.text.clear()
                }
            }
        }

        // Lắng nghe dữ liệu thay đổi để hiển thị danh sách
        database.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                var hienThi = ""
                for (item in snapshot.children) {
                    hienThi += "${item.key}: ${item.value}\n"
                }
                txtDanhSach.text = if (hienThi.isEmpty()) "Kho trống" else hienThi
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }
}