package com.example.quanlynhahang

import android.media.MediaPlayer
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Bep : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentOrderCount = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bep)

        val txtDanhSach = findViewById<TextView>(R.id.txtDanhSachOrder)
        val btnXoaHet = findViewById<Button>(R.id.btnXoaHet)
        val edtBanHoanThanh = findViewById<EditText>(R.id.edtBanHoanThanh)
        val btnHoanThanhBan = findViewById<Button>(R.id.btnHoanThanhBan)

        val database = FirebaseDatabase
            .getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Orders")

        val revenueRef = FirebaseDatabase
            .getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Revenue")

        // --- 1. LẮNG NGHE DỮ LIỆU ---
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var orderMoi = ""
                var count = 0
                for (ds in snapshot.children) {
                    count++
                    val ban = ds.child("soBan").value.toString()
                    val mon = ds.child("tenMon").value.toString()
                    val sl = ds.child("soLuong").value.toString()
                    val gio = ds.child("thoiGian").value.toString()
                    orderMoi += "$count. [BÀN: $ban] -> $sl x $mon ($gio)\n-----------------\n"
                }

                if (count > currentOrderCount && currentOrderCount != -1) {
                    phatTiengTing()
                }
                currentOrderCount = count

                txtDanhSach.text = if (orderMoi.isEmpty()) "Hiện tại chưa có đơn hàng nào." else orderMoi
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // --- 2. XỬ LÝ HOÀN THÀNH THEO SỐ BÀN ---
        btnHoanThanhBan.setOnClickListener {
            val banNhap = edtBanHoanThanh.text.toString().trim()
            if (banNhap.isNotEmpty()) {
                database.orderByChild("soBan").equalTo(banNhap)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.exists()) {
                                val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                                val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                                for (ds in snapshot.children) {
                                    val idKho = ds.child("idKho").value.toString()
                                    val dinhMuc = ds.child("dinhMuc").value.toString().toDoubleOrNull() ?: 0.0
                                    val soLuongMon = ds.child("soLuong").value.toString().toDoubleOrNull() ?: 0.0
                                    val giaMon = ds.child("gia").value.toString().toLongOrNull() ?: 0L
                                    val tenMon = ds.child("tenMon").value.toString()

                                    // A. TRỪ KHO (Xử lý an toàn cho dữ liệu String)
                                    val tongTru = dinhMuc * soLuongMon
                                    val khoRef = FirebaseDatabase.getInstance().getReference("Warehouse").child(idKho)

                                    khoRef.runTransaction(object : Transaction.Handler {
                                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                            val currentVal = mutableData.value
                                            if (currentVal == null) return Transaction.success(mutableData)

                                            // Ép kiểu an toàn từ String hoặc Number sang Double
                                            val tonKhoHienTai = when (currentVal) {
                                                is String -> currentVal.toDoubleOrNull() ?: 0.0
                                                is Number -> currentVal.toDouble()
                                                else -> currentVal.toString().toDoubleOrNull() ?: 0.0
                                            }

                                            // Thực hiện phép trừ và lưu lại dạng String cho đồng bộ
                                            mutableData.value = (tonKhoHienTai - tongTru).toString()
                                            return Transaction.success(mutableData)
                                        }
                                        override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
                                    })

                                    // B. LƯU DOANH THU
                                    val tongTienDon = giaMon * soLuongMon.toLong()
                                    val revId = revenueRef.child("Daily").child(dateKey).push().key
                                    if (revId != null) {
                                        val revData = HashMap<String, Any>()
                                        revData["tenMon"] = tenMon
                                        revData["tien"] = tongTienDon
                                        revData["thang"] = monthKey
                                        revenueRef.child("Daily").child(dateKey).child(revId).setValue(revData)
                                    }
                                    ds.ref.removeValue()
                                }
                                Toast.makeText(this@Bep, "Bàn $banNhap xong! Đã trừ kho & lưu doanh thu.", Toast.LENGTH_SHORT).show()
                                edtBanHoanThanh.text.clear()
                            } else {
                                Toast.makeText(this@Bep, "Không thấy bàn này!", Toast.LENGTH_SHORT).show()
                            }
                        }
                        override fun onCancelled(error: DatabaseError) {}
                    })
            }
        }

        // --- 3. DỌN SẠCH TẤT CẢ (VÀ CỘNG DOANH THU) ---
        btnXoaHet.setOnClickListener {
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                        for (ds in snapshot.children) {
                            val giaMon = ds.child("gia").value.toString().toLongOrNull() ?: 0L
                            val soLuongMon = ds.child("soLuong").value.toString().toLongOrNull() ?: 0L
                            val tenMon = ds.child("tenMon").value.toString()

                            val tongTienDon = giaMon * soLuongMon
                            val revId = revenueRef.child("Daily").child(dateKey).push().key
                            if (revId != null) {
                                val revData = HashMap<String, Any>()
                                revData["tenMon"] = tenMon
                                revData["tien"] = tongTienDon
                                revData["thang"] = monthKey
                                revenueRef.child("Daily").child(dateKey).child(revId).setValue(revData)
                            }
                        }

                        database.removeValue().addOnSuccessListener {
                            currentOrderCount = 0
                            Toast.makeText(this@Bep, "Đã hoàn tất tất cả đơn & lưu doanh thu!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@Bep, "Không có đơn hàng nào!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun phatTiengTing() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.tingting)
            mediaPlayer?.start()
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
    }
}