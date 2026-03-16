package com.example.quanlynhahang

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Admin : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val btnDoanhThuThang = findViewById<Button>(R.id.btnDoanhThuThang)
        val btnDonHangNgay = findViewById<Button>(R.id.btnDonHangNgay)
        val btnQuanLyKho = findViewById<Button>(R.id.btnQuanLyKho)
        val btnQuanLyMenu = findViewById<Button>(R.id.btnQuanLyMenu)

        // Kết nối đến nhánh doanh thu trên Firebase
        val database = FirebaseDatabase
            .getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Revenue").child("Daily")

        // 1. XỬ LÝ DOANH THU THÁNG
        btnDoanhThuThang.setOnClickListener {
            val thangNay = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var tongThang = 0L
                    for (ngay in snapshot.children) {
                        // Kiểm tra xem ngày đó có thuộc tháng hiện tại không
                        if (ngay.key?.startsWith(thangNay) == true) {
                            for (donHang in ngay.children) {
                                tongThang += donHang.child("tien").value.toString().toLongOrNull() ?: 0L
                            }
                        }
                    }
                    hienThiDialog("Báo cáo tháng $thangNay", "Tổng doanh thu: ${String.format("%,d", tongThang)} VNĐ")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // 2. XỬ LÝ ĐƠN HÀNG VÀ DOANH THU NGÀY
        btnDonHangNgay.setOnClickListener {
            val homNay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            database.child(homNay).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var tongNgay = 0L
                    var soDon = 0
                    for (ds in snapshot.children) {
                        tongNgay += ds.child("tien").value.toString().toLongOrNull() ?: 0L
                        soDon++
                    }
                    hienThiDialog("Báo cáo ngày $homNay", "Số đơn hoàn thành: $soDon\nTổng doanh thu: ${String.format("%,d", tongNgay)} VNĐ")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // 3. XỬ LÝ QUẢN LÝ KHO
        btnQuanLyKho.setOnClickListener {
            val intent = Intent(this, Kho::class.java)
            startActivity(intent)
        }

        // 4. XỬ LÝ QUẢN LÝ MENU
        btnQuanLyMenu.setOnClickListener {
            val intent = Intent(this, QuanLyMenu::class.java)
            startActivity(intent)
        }
    }

    // Hàm phụ để hiển thị hộp thoại thông báo cho đẹp
    private fun hienThiDialog(tieuDe: String, noiDung: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(tieuDe)
        builder.setMessage(noiDung)
        builder.setPositiveButton("Đã hiểu", null)
        builder.show()
    }
}