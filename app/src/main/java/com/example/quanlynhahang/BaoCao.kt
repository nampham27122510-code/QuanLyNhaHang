package com.example.quanlynhahang

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class BaoCao : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bao_cao)

        val txtKetQua = findViewById<TextView>(R.id.txtKetQuaBaoCao)
        val btnNgay = findViewById<Button>(R.id.btnDoanhThuNgay)
        val btnThang = findViewById<Button>(R.id.btnDoanhThuThang)

        // SỬA: Trỏ thẳng vào "Revenue" vì nhanvien.kt lưu vào Revenue/yyyy-MM-dd/total
        val database = FirebaseDatabase.getInstance().getReference("Revenue")

        // XỬ LÝ XEM THEO NGÀY
        btnNgay.setOnClickListener {
            val homNay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

            database.child(homNay).child("total").addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Lấy giá trị trực tiếp từ node 'total'
                    val tong = snapshot.getValue(Long::class.java) ?: 0L

                    val formattedPrice = String.format("%,d", tong)
                    txtKetQua.text = "📅 Doanh thu ngày $homNay:\n\n💰 $formattedPrice VNĐ"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // XỬ LÝ XEM THEO THÁNG
        btnThang.setOnClickListener {
            val thangNay = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var tongThang = 0L

                    // Duyệt qua tất cả các ngày trong node Revenue
                    for (ngaySnapshot in snapshot.children) {
                        val keyNgay = ngaySnapshot.key ?: ""
                        // Kiểm tra nếu ngày đó thuộc tháng hiện tại
                        if (keyNgay.startsWith(thangNay)) {
                            val doanhThuNgay = ngaySnapshot.child("total").getValue(Long::class.java) ?: 0L
                            tongThang += doanhThuNgay
                        }
                    }

                    val formattedPrice = String.format("%,d", tongThang)
                    txtKetQua.text = "📊 Tổng doanh thu tháng $thangNay:\n\n💰 $formattedPrice VNĐ"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}