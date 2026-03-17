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
import java.util.*

class Bep : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private var currentOrderCount = -1
    private lateinit var database: DatabaseReference

    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val updateHandler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            taiDuLieuHoaDon()
            updateHandler.postDelayed(this, 60000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bep)

        val edtBanHoanThanh = findViewById<EditText>(R.id.edtBanHoanThanh)
        val btnHoanThanhBan = findViewById<Button>(R.id.btnHoanThanhBan)

        // Kết nối Firebase vào nhánh Orders
        database = FirebaseDatabase.getInstance(DB_URL).getReference("Orders")

        taiDuLieuHoaDon()
        updateHandler.post(updateRunnable)

        // --- BẾP ẤN HOÀN THÀNH THEO SỐ BÀN ---
        btnHoanThanhBan.setOnClickListener {
            val banNhap = edtBanHoanThanh.text.toString().trim()
            if (banNhap.isNotEmpty()) {
                xửLyXongMonTaiBep(banNhap)
                edtBanHoanThanh.text.clear()
            } else {
                Toast.makeText(this, "Vui lòng nhập số bàn!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun taiDuLieuHoaDon() {
        val txtDanhSach = findViewById<TextView>(R.id.txtDanhSachOrder)

        // Chỉ hiển thị các món đang chờ nấu (status = waiting)
        database.orderByChild("status").equalTo("waiting")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var orderMoi = ""
                    var count = 0
                    val currentTime = System.currentTimeMillis()

                    for (ds in snapshot.children) {
                        count++
                        val ban = ds.child("soBan").value.toString()
                        val mon = ds.child("tenMon").value.toString()
                        val ts = ds.child("timestamp").value.toString().toLongOrNull() ?: currentTime

                        val phutTroiQua = (currentTime - ts) / 60000
                        val canhBaoTre = if (phutTroiQua >= 10) "⚠️ [TRỄ ${phutTroiQua}P] " else ""

                        orderMoi += "$count. $canhBaoTre[BÀN: $ban] -> $mon\n-----------------\n"
                    }

                    // Phát tiếng chuông khi có món mới
                    if (count > currentOrderCount && currentOrderCount != -1) {
                        phatTiengTing()
                    }
                    currentOrderCount = count
                    txtDanhSach.text = if (orderMoi.isEmpty()) "Hiện tại chưa có món nào cần nấu." else orderMoi
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun xửLyXongMonTaiBep(soBan: String) {
        // Tìm các món của bàn này đang 'waiting' để chuyển sang 'cooked'
        database.orderByChild("soBan").equalTo(soBan)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        var check = false
                        for (ds in snapshot.children) {
                            if (ds.child("status").value == "waiting") {
                                // CHỈ ĐỔI STATUS, KHÔNG XÓA ĐƠN
                                ds.ref.child("status").setValue("cooked")
                                check = true
                            }
                        }
                        if (check) {
                            Toast.makeText(this@Bep, "Đã chuyển đơn bàn $soBan sang chờ giao!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@Bep, "Bàn $soBan không có món nào đang chờ nấu", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@Bep, "Không thấy dữ liệu bàn $soBan", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
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