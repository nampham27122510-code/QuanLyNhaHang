package com.example.quanlynhahang

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class Admin : AppCompatActivity() {

    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        database = FirebaseDatabase.getInstance(DB_URL)

        // GIẢ SỬ: Khi bạn nhấn vào nút thanh toán của bàn nào đó trong danh sách
        // Bạn hãy gọi hàm showThanhToanDialog(soBan)
    }

    // HÀM FIX LỖI NULL: Tính tiền và hiển thị Dialog
    fun showThanhToanDialog(soBan: String) {
        val orderRef = database.getReference("Orders")

        orderRef.orderByChild("soBan").equalTo(soBan)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@Admin, "Bàn $soBan trống!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    var tongTien = 0.0
                    val details = StringBuilder("HÓA ĐƠN BÀN $soBan\n\n")

                    for (ds in snapshot.children) {
                        // Chỉ tính tiền món chưa nấu xong hoặc đã nấu xong nhưng chưa trả tiền
                        val isPaid = ds.child("isPaid").value == true
                        if (!isPaid) {
                            val ten = ds.child("tenMon").value?.toString() ?: "Món không tên"
                            val gia = ds.child("gia").value?.toString()?.toDoubleOrNull() ?: 0.0
                            tongTien += gia
                            details.append("- $ten: $gia VNĐ\n")
                        }
                    }

                    // HIỂN THỊ DIALOG - Đảm bảo Message không bị null
                    AlertDialog.Builder(this@Admin)
                        .setTitle("💰 THANH TOÁN")
                        .setMessage(if (tongTien > 0) "${details}\nTổng: $tongTien VNĐ" else "Bàn này đã thanh toán xong!")
                        .setPositiveButton("XÁC NHẬN") { _, _ ->
                            if (tongTien > 0) xuLyMarkPaid(soBan)
                        }
                        .setNegativeButton("ĐÓNG", null)
                        .show()
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun xuLyMarkPaid(soBan: String) {
        database.getReference("Orders").orderByChild("soBan").equalTo(soBan)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { order ->
                        // Đánh dấu isPaid = true để Bếp thấy thẻ vàng
                        order.ref.child("isPaid").setValue(true)
                    }
                    Toast.makeText(this@Admin, "Đã thanh toán bàn $soBan thành công!", Toast.LENGTH_SHORT).show()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}