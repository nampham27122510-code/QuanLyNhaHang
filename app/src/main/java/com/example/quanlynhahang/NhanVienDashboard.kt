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

    private fun listenTableStatus(card: CardView, tableId: Int) {
        val imgTable = card.findViewWithTag<ImageView>("img_$tableId")
        val dotPay = card.findViewWithTag<View>("dotPay_$tableId")
        val dotDone = card.findViewWithTag<View>("dotDone_$tableId")

        database.getReference("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasUnpaid = false
                var hasWaitingFood = false
                var hasCookedItem = false

                for (ds in snapshot.children) {
                    val b = ds.child("soBan").value?.toString() ?: ""
                    if (b == tableId.toString()) {
                        val status = ds.child("status").value?.toString() ?: ""
                        val isPaid = ds.child("isPaid").value == true

                        if (status != "delivered") {
                            if (!isPaid) hasUnpaid = true
                            else hasWaitingFood = true

                            if (status == "cooked") hasCookedItem = true
                        }
                    }
                }

                when {
                    hasUnpaid -> {
                        card.setCardBackgroundColor(Color.parseColor("#FFEBEE"))
                        imgTable.setColorFilter(Color.RED)
                    }
                    hasWaitingFood -> {
                        card.setCardBackgroundColor(Color.parseColor("#E8F5E9"))
                        imgTable.setColorFilter(Color.parseColor("#4CAF50"))
                    }
                    else -> {
                        card.setCardBackgroundColor(Color.WHITE)
                        imgTable.setColorFilter(Color.parseColor("#757575"))
                    }
                }
                dotDone.visibility = if (hasCookedItem) View.VISIBLE else View.INVISIBLE
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        database.getReference("Notifications_Pay").child("ban_$tableId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dotPay.visibility = if (snapshot.exists()) View.VISIBLE else View.INVISIBLE
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

        val layout = RelativeLayout(this)
        layout.setPadding(10, 30, 10, 30)

        // FIX LỖI Val cannot be reassigned TẠI ĐÂY
        val imgTable = ImageView(this)
        val viewId = View.generateViewId()
        imgTable.id = viewId // Gán ID đúng cách
        imgTable.setImageResource(R.drawable.ic_table)
        imgTable.tag = "img_$id"
        val imgParams = RelativeLayout.LayoutParams(120, 120)
        imgParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        imgTable.layoutParams = imgParams
        layout.addView(imgTable)

        val tv = TextView(this)
        tv.text = "Bàn $id"
        tv.textSize = 16f
        tv.typeface = Typeface.DEFAULT_BOLD
        val tvParams = RelativeLayout.LayoutParams(-2, -2)
        tvParams.addRule(RelativeLayout.BELOW, viewId)
        tvParams.addRule(RelativeLayout.CENTER_HORIZONTAL)
        tvParams.topMargin = 10
        tv.layoutParams = tvParams
        layout.addView(tv)

        val dotPay = View(this).apply {
            tag = "dotPay_$id"
            visibility = View.INVISIBLE
            val p = RelativeLayout.LayoutParams(35, 35)
            p.addRule(RelativeLayout.ALIGN_TOP, viewId)
            p.addRule(RelativeLayout.ALIGN_RIGHT, viewId)
            layoutParams = p
            background = getDrawable(R.drawable.circle_red)
        }
        layout.addView(dotPay)

        val dotDone = View(this).apply {
            tag = "dotDone_$id"
            visibility = View.INVISIBLE
            val p = RelativeLayout.LayoutParams(35, 35)
            p.addRule(RelativeLayout.ALIGN_TOP, viewId)
            p.addRule(RelativeLayout.ALIGN_LEFT, viewId)
            layoutParams = p
            background = getDrawable(R.drawable.circle_green)
        }
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
                for (ds in snapshot.children) {
                    val tableId = ds.child("table").value?.toString() ?: ""
                    if (tableId.isNotEmpty()) {
                        AlertDialog.Builder(this@NhanVienDashboard)
                            .setTitle("📢 BÀN $tableId GỌI!")
                            .setPositiveButton("ĐẾN NGAY") { _, _ -> ds.ref.removeValue() }
                            .setCancelable(false).show()
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}