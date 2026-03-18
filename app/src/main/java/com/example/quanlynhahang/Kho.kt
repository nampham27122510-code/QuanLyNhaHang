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
            // SỬA: Ép kiểu sang Double để Firebase hiểu đây là con số (Number)
            val slStr = edtSoLuong.text.toString().trim()
            val slValue = slStr.toDoubleOrNull() ?: 0.0

            if (ten.isNotEmpty() && slStr.isNotEmpty()) {
                // SỬA: Gửi slValue (Double) thay vì sl (String)
                database.child(ten).setValue(slValue).addOnSuccessListener {
                    Toast.makeText(this, "Đã cập nhật $ten: $slValue", Toast.LENGTH_SHORT).show()
                    edtTen.text.clear()
                    edtSoLuong.text.clear()
                }
            } else {
                Toast.makeText(this, "Vui lòng nhập đủ tên và số lượng!", Toast.LENGTH_SHORT).show()
            }
        }

        // Lắng nghe dữ liệu thay đổi để hiển thị danh sách
        database.addValueEventListener(object : com.google.firebase.database.ValueEventListener {
            override fun onDataChange(snapshot: com.google.firebase.database.DataSnapshot) {
                var hienThi = ""
                for (item in snapshot.children) {
                    // Hiển thị kèm đơn vị hoặc định dạng số cho đẹp
                    val value = item.value
                    hienThi += "• ${item.key}: $value\n"
                }
                txtDanhSach.text = if (hienThi.isEmpty()) "Kho trống" else hienThi
            }
            override fun onCancelled(error: com.google.firebase.database.DatabaseError) {}
        })
    }
}