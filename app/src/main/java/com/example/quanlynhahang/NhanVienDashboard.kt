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

    private fun listenTableStatus(card: CardView, tableId: Int) {
        val imgTable = card.findViewWithTag<ImageView>("img_$tableId")
        val dotPay = card.findViewWithTag<View>("dotPay_$tableId")   // Chấm đỏ (Thanh toán)
        val dotDone = card.findViewWithTag<View>("dotDone_$tableId") // Chấm xanh (Giao món)

        // 1. Kiểm tra bàn có khách (Chưa thanh toán) -> Đổi màu nền bàn
        database.getReference("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var isOccupied = false
                var hasReadyToDeliver = false

                for (ds in snapshot.children) {
                    val b = ds.child("soBan").value?.toString() ?: ""
                    val status = ds.child("status").value?.toString() ?: ""

                    if (b == tableId.toString()) {
                        if (status != "paid") isOccupied = true
                        if (status == "done") hasReadyToDeliver = true
                    }
                }

                if (isOccupied) {
                    card.setCardBackgroundColor(Color.parseColor("#FFEBEE")) // Màu hồng nhạt (Có khách)
                    imgTable.setColorFilter(Color.parseColor("#E53935"))    // Icon đỏ
                } else {
                    card.setCardBackgroundColor(Color.WHITE)               // Trống
                    imgTable.setColorFilter(Color.parseColor("#757575"))    // Icon xám
                }

                // Hiển thị chấm xanh nếu có món xong từ bếp
                dotDone.visibility = if (hasReadyToDeliver) View.VISIBLE else View.INVISIBLE
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // 2. Lắng nghe yêu cầu thanh toán -> Hiện chấm đỏ
        database.getReference("Notifications_Pay").child("ban_$tableId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        dotPay.visibility = View.VISIBLE
                        // Có thể thêm hiệu ứng rung hoặc thông báo ở đây
                    } else {
                        dotPay.visibility = View.INVISIBLE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun createTableCard(id: Int): CardView {
        val card = CardView(this)
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = GridLayout.LayoutParams.WRAP_CONTENT
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(15, 15, 15, 15)
        card.layoutParams = params
        card.radius = 25f
        card.cardElevation = 8f
        card.setPadding(10, 20, 10, 20)

        val layout = RelativeLayout(this)
        layout.setPadding(10, 30, 10, 30)

        // Icon bàn
        val imgTable = ImageView(this)
        imgTable.setImageResource(R.drawable.ic_table)
        imgTable.tag = "img_$id"
        val imgParams = RelativeLayout.LayoutParams(120, 120)
        imgParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        imgTable.layoutParams = imgParams
        layout.addView(imgTable)

        // Tên bàn
        val tv = TextView(this)
        tv.text = "Bàn $id"
        tv.textSize = 16f
        tv.typeface = Typeface.DEFAULT_BOLD
        val tvParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT)
        tvParams.addRule(RelativeLayout.BELOW, imgTable.id)
        tvParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        tvParams.topMargin = 10
        tv.layoutParams = tvParams
        layout.addView(tv)

        // CHẤM ĐỎ: Yêu cầu thanh toán (Góc trên bên phải)
        val dotPay = View(this)
        dotPay.tag = "dotPay_$id"
        dotPay.visibility = View.INVISIBLE
        val dotPayParams = RelativeLayout.LayoutParams(35, 35)
        dotPayParams.addRule(RelativeLayout.ALIGN_TOP, imgTable.id)
        dotPayParams.addRule(RelativeLayout.ALIGN_RIGHT, imgTable.id)
        dotPay.layoutParams = dotPayParams
        dotPay.background = getDrawable(R.drawable.circle_red)
        layout.addView(dotPay)

        // CHẤM XANH LÁ: Có món đã làm xong (Góc trên bên trái)
        val dotDone = View(this)
        dotDone.tag = "dotDone_$id"
        dotDone.visibility = View.INVISIBLE
        val dotDoneParams = RelativeLayout.LayoutParams(35, 35)
        dotDoneParams.addRule(RelativeLayout.ALIGN_TOP, imgTable.id)
        dotDoneParams.addRule(RelativeLayout.ALIGN_LEFT, imgTable.id)
        dotDone.layoutParams = dotDoneParams
        dotDone.background = getDrawable(R.drawable.circle_green)
        layout.addView(dotDone)

        card.addView(layout)
        card.setOnClickListener {
            val intent = Intent(this, nhanvien::class.java)
            intent.putExtra("TABLE_ID", id.toString())
            startActivity(intent)
        }
        return card
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
                                .setMessage("Khách cần hỗ trợ ngay!")
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