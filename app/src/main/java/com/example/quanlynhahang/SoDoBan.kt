package com.example.quanlynhahang

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.firebase.database.*

class SoDoBan : AppCompatActivity() {

    private lateinit var gridSoDoBan: GridLayout
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private val database by lazy { FirebaseDatabase.getInstance(DB_URL).reference }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_so_do_ban)
        gridSoDoBan = findViewById(R.id.gridSoDoBan)
        loadTrangThaiBan()
    }

    private fun loadTrangThaiBan() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                gridSoDoBan.removeAllViews()
                val ordersSnap = snapshot.child("Orders")
                val paySnap = snapshot.child("Notifications_Pay")
                val tableStatusSnap = snapshot.child("TableStatus")

                for (i in 1..30) {
                    val tableId = i.toString()
                    val card = createTableCard(tableId)

                    // 1. Kiểm tra cờ xác nhận thanh toán từ phía nhân viên
                    val isConfirmedPaid = tableStatusSnap
                        .child("ban_$tableId").value?.toString() == "confirmed_paid"

                    var hasActiveOrder = false // Bàn có khách (bất kể trạng thái món)
                    var hasCookedDish = false  // Có món đã nấu xong chờ giao (chấm xanh)
                    val isRequestingPay = paySnap.child("ban_$tableId").exists() // Khách gọi thanh toán (chấm đỏ)

                    // 2. Duyệt qua toàn bộ node Orders để xác định trạng thái bàn
                    for (ds in ordersSnap.children) {
                        if (ds.child("soBan").value?.toString() == tableId) {
                            // CỨ CÒN ORDER LÀ CÒN KHÁCH -> BÀN ĐỎ
                            hasActiveOrder = true

                            // Nếu có món ở trạng thái "cooked", bật chấm xanh thông báo
                            if (ds.child("status").value?.toString() == "cooked") {
                                hasCookedDish = true
                            }
                        }
                    }

                    val tableColor: Int
                    val backgroundColor: Int

                    when {
                        // ƯU TIÊN 1: Nhân viên vừa xác nhận thanh toán thành công -> Xám ngay
                        isConfirmedPaid -> {
                            tableColor = Color.parseColor("#757575")
                            backgroundColor = Color.WHITE
                            // Reset flag TableStatus để bàn sẵn sàng cho lượt khách tiếp theo
                            database.child("TableStatus").child("ban_$tableId").removeValue()
                        }

                        // ƯU TIÊN 2: Bàn có món (dù là đang đợi, đã nấu, hay đã giao) -> GIỮ MÀU ĐỎ
                        hasActiveOrder -> {
                            tableColor = Color.RED
                            backgroundColor = Color.parseColor("#FFF0F0")
                        }

                        // ƯU TIÊN 3: Bàn trống hoàn toàn -> MÀU XÁM
                        else -> {
                            tableColor = Color.parseColor("#757575")
                            backgroundColor = Color.WHITE
                        }
                    }

                    // Cập nhật giao diện Icon và các chấm thông báo
                    updateTableUI(card, tableId, tableColor, hasCookedDish, isRequestingPay)
                    card.setCardBackgroundColor(backgroundColor)

                    card.setOnClickListener {
                        startActivity(
                            Intent(this@SoDoBan, nhanvien::class.java)
                                .putExtra("TABLE_ID", tableId)
                        )
                    }
                    gridSoDoBan.addView(card)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createTableCard(idTable: String): CardView {
        val card = CardView(this)
        val params = GridLayout.LayoutParams()
        params.width = 0; params.height = 250
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(12, 12, 12, 12)
        card.layoutParams = params
        card.radius = 25f; card.cardElevation = 6f

        val layout = RelativeLayout(this)
        val imgTable = ImageView(this)
        val newId = View.generateViewId()
        imgTable.id = newId
        imgTable.setImageResource(R.drawable.ic_table)
        imgTable.layoutParams = RelativeLayout.LayoutParams(110, 110).apply {
            addRule(RelativeLayout.CENTER_IN_PARENT)
        }
        imgTable.tag = "img_$idTable"

        val tv = TextView(this).apply {
            text = "Bàn $idTable"; textSize = 14f; setTypeface(null, Typeface.BOLD)
            layoutParams = RelativeLayout.LayoutParams(-2, -2).apply {
                addRule(RelativeLayout.ALIGN_TOP, newId)
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                topMargin = -45
            }
        }

        val dotGreen = View(this).apply {
            tag = "dotGreen_$idTable"
            layoutParams = RelativeLayout.LayoutParams(35, 35).apply {
                addRule(RelativeLayout.ALIGN_TOP, newId)
                addRule(RelativeLayout.ALIGN_LEFT, newId)
            }
            setBackgroundResource(R.drawable.circle_green)
            visibility = View.INVISIBLE
        }

        val dotRed = View(this).apply {
            tag = "dotRed_$idTable"
            layoutParams = RelativeLayout.LayoutParams(35, 35).apply {
                addRule(RelativeLayout.ALIGN_TOP, newId)
                addRule(RelativeLayout.ALIGN_RIGHT, newId)
            }
            setBackgroundResource(R.drawable.circle_red)
            visibility = View.INVISIBLE
        }

        layout.addView(imgTable)
        layout.addView(tv)
        layout.addView(dotGreen)
        layout.addView(dotRed)
        card.addView(layout)
        return card
    }

    private fun updateTableUI(card: CardView, id: String, color: Int, cooked: Boolean, payReq: Boolean) {
        card.findViewWithTag<ImageView>("img_$id")?.setColorFilter(color)
        card.findViewWithTag<View>("dotGreen_$id")?.visibility = if (cooked) View.VISIBLE else View.INVISIBLE
        card.findViewWithTag<View>("dotRed_$id")?.visibility = if (payReq) View.VISIBLE else View.INVISIBLE
    }
}