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
        rv.itemAnimator = DefaultItemAnimator()

        bepAdapter = BepAdapter(displayOrders) { item -> xửLyXongMonTaiBep(item) }
        rv.adapter = bepAdapter

        database = FirebaseDatabase.getInstance(DB_URL).getReference("Orders")
        taiDuLieuChoBep()

        timerHandler.post(timerRunnable)
    }

    private fun taiDuLieuChoBep() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val rawList = mutableListOf<DataSnapshot>()
                for (ds in snapshot.children) {
                    val status = ds.child("status").value?.toString() ?: ""
                    // Bếp chỉ hiện món chờ nấu, không quan tâm việc thanh toán
                    if (status == "waiting") {
                        rawList.add(ds)
                    }
                }

                val groups = rawList.groupBy {
                    val ban = it.child("soBan").value?.toString() ?: "0"
                    val mon = it.child("tenMon").value?.toString() ?: "NoName"
                    "${ban}_${mon}"
                }

                val newList = mutableListOf<GroupedItem>()
                for ((_, items) in groups) {
                    val first = items[0]
                    val ban = first.child("soBan").value?.toString() ?: "0"
                    val paidStatus = items.any { it.child("isPaid").value == true }

                    newList.add(GroupedItem(
                        ban,
                        first.child("tenMon").value?.toString() ?: "Món không tên",
                        items.size,
                        first.child("timestamp").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
                        items,
                        paidStatus
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