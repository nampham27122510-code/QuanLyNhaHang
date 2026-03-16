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

        val database = FirebaseDatabase.getInstance().getReference("Revenue").child("Daily")

        // XỬ LÝ XEM THEO NGÀY
        btnNgay.setOnClickListener {
            val homNay = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            database.child(homNay).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var tong = 0L
                    for (ds in snapshot.children) {
                        tong += ds.child("tien").value.toString().toLongOrNull() ?: 0L
                    }
                    txtKetQua.text = "Doanh thu ngày $homNay:\n\n${tong} VNĐ"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }

        // XỬ LÝ XEM THEO THÁNG (Duyệt tất cả các ngày trong tháng)
        btnThang.setOnClickListener {
            val thangNay = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var tongThang = 0L
                    for (ngay in snapshot.children) {
                        if (ngay.key?.startsWith(thangNay) == true) {
                            for (donHang in ngay.children) {
                                tongThang += donHang.child("tien").value.toString().toLongOrNull() ?: 0L
                            }
                        }
                    }
                    txtKetQua.text = "Tổng doanh thu tháng $thangNay:\n\n${tongThang} VNĐ"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }
}