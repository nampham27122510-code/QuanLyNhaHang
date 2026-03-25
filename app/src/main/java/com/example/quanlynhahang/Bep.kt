package com.example.quanlynhahang

import android.os.Bundle
import android.os.Handler
import android.os.Looper
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
    val snapshots: List<DataSnapshot>,
    val isPaid: Boolean
)

class Bep : AppCompatActivity() {
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var database: DatabaseReference
    private val displayOrders = mutableListOf<GroupedItem>()
    private lateinit var bepAdapter: BepAdapter

    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (::bepAdapter.isInitialized && displayOrders.isNotEmpty()) {
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
        rv.itemAnimator = DefaultItemAnimator().apply { removeDuration = 500; moveDuration = 500 }

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

                    // Gộp theo bàn và tên món
                    val groups = rawList.groupBy { "${it.child("soBan").value}_${it.child("tenMon").value}" }
                    val newList = mutableListOf<GroupedItem>()

                    for ((_, items) in groups) {
                        val first = items[0]
                        val ban = first.child("soBan").value?.toString() ?: "0"
                        val paid = items.any { it.child("isPaid").value == true }

                        newList.add(GroupedItem(
                            ban,
                            first.child("tenMon").value.toString(),
                            items.size,
                            first.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L,
                            items,
                            paid
                        ))
                    }
                    newList.sortBy { it.timestamp }

                    displayOrders.clear()
                    displayOrders.addAll(newList)
                    bepAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun xửLyXongMonTaiBep(item: GroupedItem) {
        for (ds in item.snapshots) {
            ds.ref.child("status").setValue("cooked")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }
}