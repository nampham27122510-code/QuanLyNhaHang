package com.example.quanlynhahang

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    private lateinit var database: DatabaseReference
    private lateinit var revenueRef: DatabaseReference

    // Handler để cập nhật thời gian trễ mỗi phút một lần trên giao diện
    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            taiDuLieuHoaDon() // Cập nhật lại danh sách để làm mới số phút trễ
            updateHandler.postDelayed(this, 60000) // 1 phút chạy lại 1 lần
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bep)

        val btnXoaHet = findViewById<Button>(R.id.btnXoaHet)
        val edtBanHoanThanh = findViewById<EditText>(R.id.edtBanHoanThanh)
        val btnHoanThanhBan = findViewById<Button>(R.id.btnHoanThanhBan)

        // Kết nối Firebase
        database = FirebaseDatabase
            .getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Orders")

        revenueRef = FirebaseDatabase
            .getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("Revenue")

        // Bắt đầu lắng nghe và cập nhật đơn hàng
        taiDuLieuHoaDon()
        updateHandler.post(updateRunnable)

        // --- XỬ LÝ HOÀN THÀNH THEO SỐ BÀN ---
        btnHoanThanhBan.setOnClickListener {
            val banNhap = edtBanHoanThanh.text.toString().trim()
            if (banNhap.isNotEmpty()) {
                xửLyXongBan(banNhap)
                edtBanHoanThanh.text.clear()
            }
        }

        // --- XỬ LÝ XÓA HẾT (CHỐT DOANH THU TỔNG) ---
        btnXoaHet.setOnClickListener {
            database.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (ds in snapshot.children) {
                            luuDoanhThu(ds)
                        }
                        database.removeValue().addOnSuccessListener {
                            Toast.makeText(this@Bep, "Đã hoàn tất tất cả đơn!", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    private fun taiDuLieuHoaDon() {
        val txtDanhSach = findViewById<TextView>(R.id.txtDanhSachOrder)

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var orderMoi = ""
                var count = 0
                val currentTime = System.currentTimeMillis()

                for (ds in snapshot.children) {
                    count++
                    val ban = ds.child("soBan").value.toString()
                    val mon = ds.child("tenMon").value.toString()
                    val sl = ds.child("soLuong").value.toString()
                    val gio = ds.child("thoiGian").value.toString()

                    // Logic tính đơn trễ 10 phút
                    val ts = ds.child("timestamp").value.toString().toLongOrNull() ?: currentTime
                    val phutTroiQua = (currentTime - ts) / 60000

                    val canhBaoTre = if (phutTroiQua >= 10) "⚠️ [TRỄ ${phutTroiQua}P] " else ""

                    orderMoi += "$count. $canhBaoTre[BÀN: $ban] -> $sl x $mon ($gio)\n-----------------\n"
                }

                if (count > currentOrderCount && currentOrderCount != -1) {
                    phatTiengTing()
                }
                currentOrderCount = count
                txtDanhSach.text = if (orderMoi.isEmpty()) "Hiện tại chưa có đơn hàng nào." else orderMoi
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun xửLyXongBan(soBan: String) {
        database.orderByChild("soBan").equalTo(soBan)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        for (ds in snapshot.children) {
                            truKho(ds)
                            luuDoanhThu(ds)
                            ds.ref.removeValue()
                        }
                        Toast.makeText(this@Bep, "Bàn $soBan hoàn thành!", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@Bep, "Không thấy bàn $soBan", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun truKho(ds: DataSnapshot) {
        val idKho = ds.child("idKho").value.toString()
        val dinhMuc = ds.child("dinhMuc").value.toString().toDoubleOrNull() ?: 0.0
        val soLuongMon = ds.child("soLuong").value.toString().toDoubleOrNull() ?: 0.0
        val tongTru = dinhMuc * soLuongMon

        val khoRef = FirebaseDatabase.getInstance().getReference("Warehouse").child(idKho)
        khoRef.runTransaction(object : Transaction.Handler {
            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                val currentVal = mutableData.value
                val tonKhoHienTai = when (currentVal) {
                    is String -> currentVal.toDoubleOrNull() ?: 0.0
                    is Number -> currentVal.toDouble()
                    else -> 0.0
                }
                mutableData.value = (tonKhoHienTai - tongTru).toString()
                return Transaction.success(mutableData)
            }
            override fun onComplete(e: DatabaseError?, c: Boolean, s: DataSnapshot?) {}
        })
    }

    private fun luuDoanhThu(ds: DataSnapshot) {
        val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val monthKey = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val giaMon = ds.child("gia").value.toString().toLongOrNull() ?: 0L
        val soLuongMon = ds.child("soLuong").value.toString().toLongOrNull() ?: 0L
        val tenMon = ds.child("tenMon").value.toString()

        val revId = revenueRef.child("Daily").child(dateKey).push().key
        if (revId != null) {
            val revData = HashMap<String, Any>()
            revData["tenMon"] = tenMon
            revData["tien"] = giaMon * soLuongMon
            revData["thang"] = monthKey
            revenueRef.child("Daily").child(dateKey).child(revId).setValue(revData)
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
        updateHandler.removeCallbacks(updateRunnable)
        mediaPlayer?.release()
    }
}