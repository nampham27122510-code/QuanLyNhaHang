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
        // Lắng nghe Orders để cập nhật màu sắc bàn
        database.child("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gridSoDoBan.removeAllViews()
                val banDangCoKhach = mutableSetOf<String>()

                for (ds in snapshot.children) {
                    val status = ds.child("status").value?.toString() ?: ""
                    val soBan = ds.child("soBan").value?.toString() ?: ""
                    // Bàn được coi là có khách nếu có món chưa ở trạng thái 'paid'
                    if (status != "paid" && soBan.isNotEmpty()) {
                        banDangCoKhach.add(soBan)
                    }
                }

                for (i in 1..100) {
                    val btnBan = Button(this@SoDoBan)
                    val params = GridLayout.LayoutParams()
                    params.width = 0
                    params.height = 150
                    params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
                    params.setMargins(8, 8, 8, 8)
                    btnBan.layoutParams = params
                    btnBan.text = "Bàn $i"

                    if (banDangCoKhach.contains(i.toString())) {
                        btnBan.setBackgroundColor(Color.parseColor("#E57373")) // Đỏ
                    } else {
                        btnBan.setBackgroundColor(Color.parseColor("#81C784")) // Xanh
                    }

                    btnBan.setOnClickListener {
                        val txt = if (banDangCoKhach.contains(i.toString())) "đang có khách" else "trống"
                        Toast.makeText(this@SoDoBan, "Bàn $i $txt", Toast.LENGTH_SHORT).show()
                    }
                    gridSoDoBan.addView(btnBan)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}