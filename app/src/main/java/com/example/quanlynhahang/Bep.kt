package com.example.quanlynhahang

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

// Data class dùng chung
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
                // Cập nhật thời gian mà không làm treo UI
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

        // Cấu hình Animator: Chìa khóa để các thẻ dưới trôi lên mượt
        rv.itemAnimator = DefaultItemAnimator().apply {
            removeDuration = 300
            moveDuration = 300
        }

        bepAdapter = BepAdapter(displayOrders)
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
                        val minTs = items.minOfOrNull { it.child("timestamp").value.toString().toLongOrNull() ?: System.currentTimeMillis() } ?: System.currentTimeMillis()
                        newList.add(GroupedItem(first.child("soBan").value.toString(), first.child("tenMon").value.toString(), items.size, minTs, items))
                    }
                    newList.sortBy { it.timestamp }

                    // CHỐNG KHỰNG: Chỉ cập nhật khi có món mới vào.
                    // Khi nấu xong, Adapter đã tự xóa rồi nên không cần nạp lại ở đây.
                    if (newList.size > displayOrders.size || displayOrders.isEmpty() || newList.isEmpty()) {
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

    private fun phatTiengTing() {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(this, R.raw.tingting)
            mediaPlayer?.start()
        } catch (e: Exception) {}
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
        mediaPlayer?.release()
    }
}