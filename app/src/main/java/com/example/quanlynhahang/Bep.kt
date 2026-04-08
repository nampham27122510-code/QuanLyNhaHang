package com.example.quanlynhahang

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
    val snapshots: List<DataSnapshot>,
    val isPaid: Boolean,
    val ghiChu: String
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
                    if (ds.child("status").value == "waiting") {
                        rawList.add(ds)
                    }
                }

                val groups = rawList.groupBy {
                    "${it.child("soBan").value}_${it.child("tenMon").value}"
                }

                val newList = mutableListOf<GroupedItem>()
                for ((_, items) in groups) {
                    val first = items[0]

                    // Gộp ghi chú từ Firebase
                    val allNotes = items.mapNotNull { it.child("ghiChu").value?.toString() }
                        .filter { it.isNotEmpty() && it != "Không có" }
                        .distinct().joinToString(", ")

                    newList.add(GroupedItem(
                        first.child("soBan").value.toString(),
                        first.child("tenMon").value.toString(),
                        items.size,
                        first.child("timestamp").value.toString().toLongOrNull() ?: System.currentTimeMillis(),
                        items,
                        items.any { it.child("isPaid").value == true },
                        if (allNotes.isEmpty()) "Không có" else allNotes
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
        val rootRef = FirebaseDatabase.getInstance(DB_URL).reference

        // 1. Logic Trừ Kho
        rootRef.child("Menu").child(item.tenMon).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val idKho = snapshot.child("idKho").value?.toString()
                val dinhMuc = snapshot.child("dinhMuc").value?.toString()?.toDoubleOrNull() ?: 0.0

                if (!idKho.isNullOrEmpty() && dinhMuc > 0) {
                    val totalSub = item.soLuong * dinhMuc
                    rootRef.child("Warehouse").child(idKho).runTransaction(object : Transaction.Handler {
                        override fun doTransaction(data: MutableData): Transaction.Result {
                            val current = data.getValue(Double::class.java) ?: 0.0
                            data.value = current - totalSub
                            return Transaction.success(data)
                        }
                        override fun onComplete(e: DatabaseError?, b: Boolean, d: DataSnapshot?) {}
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Chuyển trạng thái cooked
        for (ds in item.snapshots) {
            ds.ref.child("status").setValue("cooked")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }
}