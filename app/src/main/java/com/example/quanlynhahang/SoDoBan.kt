package com.example.quanlynhahang

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.GridLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*

class SoDoBan : AppCompatActivity() {

    private lateinit var gridSoDoBan: GridLayout
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_so_do_ban)

        gridSoDoBan = findViewById(R.id.gridSoDoBan)

        loadTrangThaiBan()
    }

    private fun loadTrangThaiBan() {
        // Lấy toàn bộ Orders để kiểm tra bàn nào chưa thanh toán
        database.child("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gridSoDoBan.removeAllViews()

                // Tập hợp các số bàn đang có khách (chưa thanh toán)
                val banDangCoKhach = mutableSetOf<String>()
                for (ds in snapshot.children) {
                    val status = ds.child("status").value?.toString() ?: ""
                    val soBan = ds.child("soBan").value?.toString() ?: ""
                    if (status != "paid" && soBan.isNotEmpty()) {
                        banDangCoKhach.add(soBan)
                    }
                }

                // Vẽ 100 bàn
                for (i in 1..100) {
                    val btnBan = Button(this@SoDoBan)
                    val params = GridLayout.LayoutParams()
                    params.width = 0
                    params.height = 150
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    params.setMargins(8, 8, 8, 8)
                    btnBan.layoutParams = params

                    btnBan.text = "Bàn $i"
                    btnBan.textSize = 12f

                    // Nếu số bàn i nằm trong danh sách đang có khách -> Màu Đỏ, ngược lại Màu Xanh
                    if (banDangCoKhach.contains(i.toString())) {
                        btnBan.setBackgroundColor(Color.parseColor("#E57373")) // Đỏ nhạt
                        btnBan.setTextColor(Color.WHITE)
                    } else {
                        btnBan.setBackgroundColor(Color.parseColor("#81C784")) // Xanh nhạt
                        btnBan.setTextColor(Color.WHITE)
                    }

                    btnBan.setOnClickListener {
                        if (banDangCoKhach.contains(i.toString())) {
                            Toast.makeText(this@SoDoBan, "Bàn $i đang có khách", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@SoDoBan, "Bàn $i trống", Toast.LENGTH_SHORT).show()
                        }
                    }

                    gridSoDoBan.addView(btnBan)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }
}