package com.example.quanlynhahang

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.*

class ThucTrangNhaHang : AppCompatActivity() {

    private lateinit var gridThucTrang: GridLayout
    private val database = FirebaseDatabase.getInstance().reference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Dùng chung layout với sơ đồ bàn để tiết kiệm thời gian, hoặc bạn tự tạo activity_thuc_trang
        setContentView(R.layout.activity_so_do_ban)

        val tvTitle = findViewById<TextView>(R.id.tvTitleSoDo) // Giả sử bạn có ID này ở đầu XML
        tvTitle?.text = "THỰC TRẠNG NHÀ HÀNG"

        gridThucTrang = findViewById(R.id.gridSoDoBan)
        loadThucTrangHienTai()
    }

    private fun loadThucTrangHienTai() {
        database.child("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gridThucTrang.removeAllViews()

                // Tập hợp các bàn đang có món chưa giao xong (đang có khách)
                val activeTables = mutableSetOf<String>()
                for (ds in snapshot.children) {
                    val status = ds.child("status").value?.toString() ?: ""
                    if (status != "delivered") {
                        activeTables.add(ds.child("soBan").value?.toString() ?: "")
                    }
                }

                for (i in 1..30) {
                    val tableId = i.toString()
                    val isBusy = activeTables.contains(tableId)

                    val card = createSimpleTableCard(tableId, isBusy)
                    gridThucTrang.addView(card)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createSimpleTableCard(id: String, isBusy: Boolean): CardView {
        val card = CardView(this).apply {
            val params = GridLayout.LayoutParams()
            params.width = 0; params.height = 220
            params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
            params.setMargins(10, 10, 10, 10)
            layoutParams = params
            radius = 20f
            cardElevation = 5f
            setCardBackgroundColor(if (isBusy) Color.parseColor("#FFEBEE") else Color.WHITE)
        }

        val layout = RelativeLayout(this)
        val img = ImageView(this).apply {
            setImageResource(R.drawable.ic_table)
            // Màu Đỏ nếu có khách, màu Xám nếu trống
            setColorFilter(if (isBusy) Color.RED else Color.LTGRAY)
            layoutParams = RelativeLayout.LayoutParams(100, 100).apply {
                addRule(RelativeLayout.CENTER_IN_PARENT)
            }
        }

        val tv = TextView(this).apply {
            text = "Bàn $id"
            textSize = 14f
                    setTextColor(if (isBusy) Color.RED else Color.BLACK)
            layoutParams = RelativeLayout.LayoutParams(-2, -2).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                bottomMargin = 10
            }
        }

        layout.addView(img)
        layout.addView(tv)
        card.addView(layout)
        return card
    }
}