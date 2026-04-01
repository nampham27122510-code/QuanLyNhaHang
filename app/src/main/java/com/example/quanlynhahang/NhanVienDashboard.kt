package com.example.quanlynhahang

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.*

class NhanVienDashboard : AppCompatActivity() {

    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var gridTables: GridLayout
    private val database = FirebaseDatabase.getInstance(DB_URL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Đảm bảo file XML của bạn có columnCount="3"
        setContentView(R.layout.activity_nhanvien_dashboard)

        gridTables = findViewById(R.id.gridTables)
        setupGlobalCallListener()
        setupTableGrid()
    }

    private fun setupTableGrid() {
        gridTables.removeAllViews()
        for (i in 1..30) {
            val card = createTableCard(i)
            gridTables.addView(card)
            listenTableStatus(card, i)
        }
    }

    private fun createTableCard(id: Int): CardView {
        val card = CardView(this)
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = 300 // CHỈNH CHIỀU CAO THẤP LẠI CHO VUÔNG VẮN
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(15, 15, 15, 15)
        card.layoutParams = params
        card.radius = 45f // Bo góc tròn hiện đại
        card.cardElevation = 12f
        card.setCardBackgroundColor(Color.WHITE)

        val layout = RelativeLayout(this)
        layout.setPadding(10, 10, 10, 10)

        // 1. ICON BÀN (Dùng icon SVG Nam vừa copy)
        val imgTable = ImageView(this)
        imgTable.id = View.generateViewId()
        // Đảm bảo Nam đã tạo file ic_table.xml trong drawable
        imgTable.setImageResource(R.drawable.ic_table)
        imgTable.setColorFilter(Color.parseColor("#757575")) // Màu xám đậm cho rõ

        val imgParams = RelativeLayout.LayoutParams(150, 150) // TĂNG KÍCH THƯỚC ICON
        imgParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        imgTable.layoutParams = imgParams

        // 2. CHẤM ĐÈN BÁO (Nằm ở góc icon)
        val dot = View(this)
        dot.id = View.generateViewId()
        val dotParams = RelativeLayout.LayoutParams(35, 35)
        dotParams.addRule(RelativeLayout.ALIGN_TOP, imgTable.id)
        dotParams.addRule(RelativeLayout.ALIGN_END, imgTable.id)
        dot.layoutParams = dotParams
        dot.setBackgroundResource(R.drawable.dot_shape)
        dot.visibility = View.INVISIBLE

        // 3. SỐ BÀN (Nằm dưới cùng)
        val text = TextView(this)
        text.text = "Bàn $id"
        text.textSize = 16f
        text.setTextColor(Color.BLACK)
        text.setTypeface(null, Typeface.BOLD)
        val textParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        textParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
        textParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        textParams.bottomMargin = 15
        text.layoutParams = textParams

        layout.addView(imgTable)
        layout.addView(dot)
        layout.addView(text)
        card.addView(layout)

        card.setOnClickListener {
            val intent = Intent(this, nhanvien::class.java)
            intent.putExtra("TABLE_ID", id.toString())
            startActivity(intent)
        }
        return card
    }

    private fun listenTableStatus(card: CardView, id: Int) {
        val layout = card.getChildAt(0) as RelativeLayout
        val dot = layout.getChildAt(1)
        val imgTable = layout.getChildAt(0) as ImageView
        val tableKey = "ban_$id"

        database.getReference("Notifications_Pay").child(tableKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                if (s.exists()) {
                    dot.visibility = View.VISIBLE
                    dot.background.setTint(Color.RED)
                    imgTable.setColorFilter(Color.RED) // Đổi icon sang đỏ
                    card.setCardBackgroundColor(Color.parseColor("#FFF5F5"))
                } else {
                    database.getReference("Notifications_Call").child(tableKey).addValueEventListener(object : ValueEventListener {
                        override fun onDataChange(s2: DataSnapshot) {
                            if (s2.exists()) {
                                dot.visibility = View.VISIBLE
                                dot.background.setTint(Color.BLUE)
                                imgTable.setColorFilter(Color.BLUE) // Đổi icon sang xanh
                                card.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                            } else {
                                dot.visibility = View.INVISIBLE
                                imgTable.setColorFilter(Color.parseColor("#757575")) // Quay về xám
                                card.setCardBackgroundColor(Color.WHITE)
                            }
                        }
                        override fun onCancelled(e: DatabaseError) {}
                    })
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun setupGlobalCallListener() {
        database.getReference("Notifications_Call").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChildren()) {
                    for (ds in snapshot.children) {
                        val tableId = ds.child("table").value?.toString() ?: ""
                        if (tableId.isNotEmpty()) {
                            AlertDialog.Builder(this@NhanVienDashboard)
                                .setTitle("📢 BÀN $tableId GỌI!")
                                .setMessage("Khách cần hỗ trợ!")
                                .setPositiveButton("ĐẾN NGAY") { _, _ -> ds.ref.removeValue() }
                                .setCancelable(false).show()
                        }
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}