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
        val menuRef = FirebaseDatabase.getInstance(DB_URL).getReference("Menu")
        val warehouseRef = FirebaseDatabase.getInstance(DB_URL).getReference("Warehouse")

        database.orderByChild("soBan").equalTo(soBan)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        var check = false
                        for (ds in snapshot.children) {
                            if (ds.child("status").value == "waiting") {
                                val tenMon = ds.child("tenMon").value.toString()

                                // 1. Lấy thông tin định mức và mã kho từ Menu
                                menuRef.child(tenMon).addListenerForSingleValueEvent(object : ValueEventListener {
                                    override fun onDataChange(menuSnap: DataSnapshot) {
                                        val idKho = menuSnap.child("idKho").value.toString()
                                        val dinhMuc = menuSnap.child("dinhMuc").value.toString().toDoubleOrNull() ?: 0.0

                                        // 2. Tiến hành trừ kho bằng Transaction để đảm bảo tính chính xác
                                        if (idKho.isNotEmpty()) {
                                            warehouseRef.child(idKho).runTransaction(object : Transaction.Handler {
                                                override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                                    // Đọc số lượng hiện tại (ép kiểu Double)
                                                    val currentSl = mutableData.getValue(Double::class.java) ?: 0.0
                                                    // Trừ kho
                                                    mutableData.value = currentSl - dinhMuc
                                                    return Transaction.success(mutableData)
                                                }
                                                override fun onComplete(a: DatabaseError?, b: Boolean, c: DataSnapshot?) {}
                                            })
                                        }
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })

                                // 3. Đổi trạng thái sang 'cooked'
                                ds.ref.child("status").setValue("cooked")
                                check = true
                            }
                        }
                        if (check) {
                            Toast.makeText(this@Bep, "Đã xong món & trừ kho bàn $soBan!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@Bep, "Bàn $soBan không có món đang chờ", Toast.LENGTH_SHORT).show()
                        }
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