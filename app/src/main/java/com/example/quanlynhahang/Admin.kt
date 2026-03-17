package com.example.quanlynhahang

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Admin : AppCompatActivity() {

    // URL Firebase chuẩn của bạn
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val btnDoanhThuThang = findViewById<Button>(R.id.btnDoanhThuThang)
        val btnDonHangNgay = findViewById<Button>(R.id.btnDonHangNgay)
        val btnQuanLyKho = findViewById<Button>(R.id.btnQuanLyKho)
        val btnQuanLyMenu = findViewById<Button>(R.id.btnQuanLyMenu)

        // Kết nối đến nhánh Revenue (Nơi nhân viên đẩy tiền về)
        val database = FirebaseDatabase.getInstance(DB_URL).getReference("Revenue")

        // 1. XỬ LÝ DOANH THU THÁNG (Cộng dồn tất cả 'total' của các ngày trong tháng)
        btnDoanhThuThang.setOnClickListener {
            val thangNay = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var tongThang = 0L
                    for (ngay in snapshot.children) {
                        // Nếu key ngày (yyyy-MM-dd) bắt đầu bằng tháng này (yyyy-MM)
                        if (ngay.key?.startsWith(thangNay) == true) {
                            // Lấy giá trị từ nhánh 'total' mà nhân viên đã cộng dồn
                            val tienNgay = ngay.child("total").value.toString().toLongOrNull() ?: 0L
                            tongThang += tienNgay
                        }
                    }
                    hienThiDialog("Báo cáo tháng $thangNay",
                        "Tổng doanh thu tháng: ${String.format("%,d", tongThang)} VNĐ")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // 2. XỬ LÝ DOANH THU NGÀY
        btnDonHangNgay.setOnClickListener {
            val homNay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            database.child(homNay).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Lấy trực tiếp số tổng mà nhân viên đã xác nhận thu tiền
                    val tongNgay = snapshot.child("total").value.toString().toLongOrNull() ?: 0L

                    hienThiDialog("Báo cáo ngày $homNay",
                        "Tổng doanh thu thực thu: ${String.format("%,d", tongNgay)} VNĐ\n\n(Lưu ý: Chỉ tính các đơn nhân viên đã nhấn 'Xác nhận thu tiền')")
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // 3. QUẢN LÝ KHO
        btnQuanLyKho.setOnClickListener {
            startActivity(Intent(this, Kho::class.java))
        }

        // 4. QUẢN LÝ MENU
        btnQuanLyMenu.setOnClickListener {
            startActivity(Intent(this, QuanLyMenu::class.java))
        }
    }

    private fun hienThiDialog(tieuDe: String, noiDung: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(tieuDe)
        builder.setMessage(noiDung)
        builder.setPositiveButton("Đã hiểu", null)
        builder.show()
    }
}