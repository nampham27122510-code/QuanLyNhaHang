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
    private val database = FirebaseDatabase.getInstance().reference

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

                for (i in 1..100) {
                    val tableId = i.toString()
                    val card = createTableCard(tableId)

                    var hasOrder = false
                    var allPaid = true
                    var hasProcessingItems = false
                    var hasCooked = false
                    val isWaitingPay = paySnap.child("ban_$tableId").exists()

                    for (ds in ordersSnap.children) {
                        val banDB = ds.child("soBan").value?.toString() ?: ""
                        if (banDB == tableId) {
                            hasOrder = true
                            val status = ds.child("status").value?.toString() ?: ""

                            // Kiểm tra thanh toán
                            if (status != "paid") {
                                allPaid = false
                            }

                            // Kiểm tra xem còn món nào chưa phục vụ xong (đang chờ hoặc đang nấu)
                            if (status == "waiting" || status == "cooked") {
                                hasProcessingItems = true
                            }

                            // Chấm xanh báo món đã nấu xong
                            if (status == "cooked") {
                                hasCooked = true
                            }
                        }
                    }

                    // --- LOGIC MÀU SẮC NAM YÊU CẦU ---
                    val tableColor: Int
                    if (!hasOrder || (allPaid && !hasProcessingItems)) {
                        // Bàn trống hoặc Đã thanh toán xong + Hết món chờ
                        tableColor = Color.parseColor("#757575") // XÁM
                        card.setCardBackgroundColor(Color.WHITE)
                    } else if (!allPaid) {
                        // CHƯA THANH TOÁN (Dù còn hay hết món chờ)
                        tableColor = Color.RED // ĐỎ
                        card.setCardBackgroundColor(Color.parseColor("#FFF0F0"))
                    } else {
                        // ĐÃ THANH TOÁN nhưng CÒN MÓN CHƯA XỬ LÝ XONG
                        tableColor = Color.parseColor("#4CAF50") // XANH LÁ
                        card.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                    }

                    updateTableUI(card, tableId, tableColor, hasCooked, isWaitingPay)

                    card.setOnClickListener {
                        val intent = Intent(this@SoDoBan, nhanvien::class.java)
                        intent.putExtra("TABLE_ID", tableId)
                        startActivity(intent)
                    }

                    gridSoDoBan.addView(card)
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun createTableCard(id: String): CardView {
        val card = CardView(this)
        val params = GridLayout.LayoutParams()
        params.width = 0
        params.height = 250
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f)
        params.setMargins(12, 12, 12, 12)
        card.layoutParams = params
        card.radius = 25f
        card.cardElevation = 6f

        val layout = RelativeLayout(this)

        val imgTable = ImageView(this)
        imgTable.id = View.generateViewId()
        imgTable.setImageResource(R.drawable.ic_table)
        val imgParams = RelativeLayout.LayoutParams(110, 110)
        imgParams.addRule(RelativeLayout.CENTER_IN_PARENT)
        imgTable.layoutParams = imgParams
        imgTable.tag = "img_$id"

        val tv = TextView(this)
        tv.text = "Bàn $id"
        tv.textSize = 14f
        tv.setTypeface(null, Typeface.BOLD)
        val tvParams = RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.WRAP_CONTENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
        )
        tvParams.addRule(RelativeLayout.ALIGN_TOP, imgTable.id)
        tvParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        tvParams.topMargin = -45 // Dịch chữ lên trên để không trùng viền bàn
        tv.layoutParams = tvParams

        val dotGreen = View(this)
        dotGreen.tag = "dotGreen_$id"
        val gParams = RelativeLayout.LayoutParams(35, 35)
        gParams.addRule(RelativeLayout.ALIGN_TOP, imgTable.id)
        gParams.addRule(RelativeLayout.ALIGN_LEFT, imgTable.id)
        dotGreen.layoutParams = gParams
        dotGreen.setBackgroundResource(R.drawable.circle_green)
        dotGreen.visibility = View.INVISIBLE

        val dotRed = View(this)
        dotRed.tag = "dotRed_$id"
        val rParams = RelativeLayout.LayoutParams(35, 35)
        rParams.addRule(RelativeLayout.ALIGN_TOP, imgTable.id)
        rParams.addRule(RelativeLayout.ALIGN_RIGHT, imgTable.id)
        dotRed.layoutParams = rParams
        dotRed.setBackgroundResource(R.drawable.circle_red)
        dotRed.visibility = View.INVISIBLE

        layout.addView(imgTable)
        layout.addView(tv)
        layout.addView(dotGreen)
        layout.addView(dotRed)
        card.addView(layout)

        return card
    }

    private fun updateTableUI(card: CardView, id: String, color: Int, cooked: Boolean, pay: Boolean) {
        val img = card.findViewWithTag<ImageView>("img_$id")
        val dotG = card.findViewWithTag<View>("dotGreen_$id")
        val dotR = card.findViewWithTag<View>("dotRed_$id")

        img?.setColorFilter(color)
        dotG?.visibility = if (cooked) View.VISIBLE else View.INVISIBLE
        dotR?.visibility = if (pay) View.VISIBLE else View.INVISIBLE
    }
}