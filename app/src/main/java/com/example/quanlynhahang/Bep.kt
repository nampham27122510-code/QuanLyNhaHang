package com.example.quanlynhahang

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

data class GroupedItem(
    val soBan: String,
    val tenMon: String,
    val soLuong: Int,
    val timestamp: Long,
    val snapshots: List<DataSnapshot>
)

class Bep : AppCompatActivity() {
    private var mediaPlayer: MediaPlayer? = null
    private var currentOrderCount = -1
    private lateinit var database: DatabaseReference
    private val displayOrders = mutableListOf<GroupedItem>()
    private lateinit var bepAdapter: BepAdapter
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (::bepAdapter.isInitialized && displayOrders.isNotEmpty()) {
                // Đếm giây bằng Payload để không làm giật Animation
                bepAdapter.notifyItemRangeChanged(0, displayOrders.size, "UPDATE_TIME")
            }
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bep)

        val rv = findViewById<RecyclerView>(R.id.rvDanhSachMon)
        rv.layoutManager = LinearLayoutManager(this)

        // KHÔI PHỤC ANIMATION TRÔI MƯỢT
        rv.itemAnimator = DefaultItemAnimator().apply {
            removeDuration = 400
            moveDuration = 400
        }

        bepAdapter = BepAdapter(displayOrders) { item -> xửLyXongMonTaiBep(item) }
        rv.adapter = bepAdapter

        database = FirebaseDatabase.getInstance(DB_URL).getReference("Orders")
        taiDuLieuHoaDon()
        timerHandler.post(timerRunnable)
    }

    private fun taiDuLieuHoaDon() {
        database.orderByChild("status").equalTo("waiting")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rawList = mutableListOf<DataSnapshot>()
                    for (ds in snapshot.children) rawList.add(ds)

                    val groups = rawList.groupBy { "${it.child("soBan").value}_${it.child("tenMon").value}" }
                    val newList = mutableListOf<GroupedItem>()
                    for ((_, items) in groups) {
                        val first = items[0]
                        val minTs = items.minOfOrNull { it.child("timestamp").value.toString().toLongOrNull() ?: 0L } ?: 0L
                        newList.add(GroupedItem(first.child("soBan").value.toString(), first.child("tenMon").value.toString(), items.size, minTs, items))
                    }
                    newList.sortBy { it.timestamp }

                    // CHỈ CẬP NHẬT KHI THỰC SỰ CÓ THAY ĐỔI ĐỂ GIỮ ANIMATION
                    if (newList.size != displayOrders.size) {
                        displayOrders.clear()
                        displayOrders.addAll(newList)
                        bepAdapter.notifyDataSetChanged()
                    }

                    if (rawList.size > currentOrderCount && currentOrderCount != -1) phatTiengTing()
                    currentOrderCount = rawList.size
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun xửLyXongMonTaiBep(item: GroupedItem) {
        val menuRef = FirebaseDatabase.getInstance(DB_URL).getReference("Menu")
        val warehouseRef = FirebaseDatabase.getInstance(DB_URL).getReference("Warehouse")

        for (ds in item.snapshots) {
            val tenMon = ds.child("tenMon").value.toString()
            menuRef.child(tenMon).addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(menuSnap: DataSnapshot) {
                    val idKho = menuSnap.child("idKho").value.toString()
                    val dinhMuc = menuSnap.child("dinhMuc").value.toString().toDoubleOrNull() ?: 0.0
                    if (idKho.isNotEmpty()) {
                        warehouseRef.child(idKho).runTransaction(object : Transaction.Handler {
                            override fun doTransaction(mutableData: MutableData): Transaction.Result {
                                val currentSl = mutableData.getValue(Double::class.java) ?: 0.0
                                mutableData.value = currentSl - dinhMuc
                                return Transaction.success(mutableData)
                            }
                            override fun onComplete(a: DatabaseError?, b: Boolean, c: DataSnapshot?) {}
                        })
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
            ds.ref.child("status").setValue("cooked")
        }
    }

    private fun phatTiengTing() { /* Logic phát âm thanh */ }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }
}