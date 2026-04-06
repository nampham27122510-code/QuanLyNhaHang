package com.example.quanlynhahang

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
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
        params.height = 300
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(15, 15, 15, 15)
        card.layoutParams = params
        card.radius = 45f
        card.cardElevation = 12f
        card.setCardBackgroundColor(Color.WHITE)

        val layout = RelativeLayout(this)
        layout.setPadding(10, 10, 10, 10)

        // 1. ICON BÀN
        val imgTable = ImageView(this)
        imgTable.id = View.generateViewId()
        imgTable.setImageResource(R.drawable.ic_table)
        imgTable.setColorFilter(Color.parseColor("#757575"))

        val imgParams = RelativeLayout.LayoutParams(150, 150)
        imgParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        imgTable.layoutParams = imgParams

        // 2. CHẤM ĐÈN BÁO
        val dot = View(this)
        dot.id = View.generateViewId()
        val dotParams = RelativeLayout.LayoutParams(40, 40) // Tăng kích thước chấm cho dễ nhìn
        dotParams.addRule(RelativeLayout.ALIGN_TOP, imgTable.id)
        dotParams.addRule(RelativeLayout.ALIGN_END, imgTable.id)
        dot.layoutParams = dotParams
        dot.setBackgroundResource(R.drawable.dot_shape)
        dot.visibility = View.INVISIBLE

        // 3. SỐ BÀN
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

        // Lắng nghe trạng thái Thanh toán (Ưu tiên Đỏ)
        database.getReference("Notifications_Pay").child(tableKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                if (s.exists()) {
                    dot.visibility = View.VISIBLE
                    dot.background.setTint(Color.RED)
                    imgTable.setColorFilter(Color.RED)
                    card.setCardBackgroundColor(Color.parseColor("#FFF5F5"))
                } else {
                    // Nếu không thanh toán thì mới check GỌI (Xanh)
                    checkCallStatus(tableKey, dot, imgTable, card)
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun checkCallStatus(tableKey: String, dot: View, imgTable: ImageView, card: CardView) {
        database.getReference("Notifications_Call").child(tableKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s2: DataSnapshot) {
                if (s2.exists()) {
                    dot.visibility = View.VISIBLE
                    dot.background.setTint(Color.BLUE)
                    imgTable.setColorFilter(Color.BLUE)
                    card.setCardBackgroundColor(Color.parseColor("#E3F2FD"))
                } else {
                    dot.visibility = View.INVISIBLE
                    imgTable.setColorFilter(Color.parseColor("#757575"))
                    card.setCardBackgroundColor(Color.WHITE)
                }
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun setupGlobalCallListener() {
        // Chỉ hiện thông báo cho các yêu cầu mới chưa xử lý
        database.getReference("Notifications_Call").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val tableId = snapshot.child("table").value?.toString() ?: ""
                if (tableId.isNotEmpty()) {
                    AlertDialog.Builder(this@NhanVienDashboard)
                        .setTitle("📢 BÀN $tableId GỌI!")
                        .setMessage("Khách cần hỗ trợ ngay!")
                        .setPositiveButton("ĐẾN NGAY") { _, _ -> snapshot.ref.removeValue() }
                        .setCancelable(false).show()
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}