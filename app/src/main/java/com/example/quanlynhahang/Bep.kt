package com.example.quanlynhahang

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class Bep : AppCompatActivity() {
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var database: DatabaseReference
    private val displayOrders = mutableListOf<TableGroup>()
    private lateinit var bepAdapter: BepAdapter
    private lateinit var tvEmptyKitchen: TextView

    // GIỮ NGUYÊN: Bộ Timer đếm giây chính xác của Nam
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (::bepAdapter.isInitialized && displayOrders.isNotEmpty()) {
                bepAdapter.notifyDataSetChanged()
            }
            timerHandler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bep)

        tvEmptyKitchen = findViewById(R.id.tvEmptyKitchen)
        val rv = findViewById<RecyclerView>(R.id.rvDanhSachMon)
        rv.layoutManager = LinearLayoutManager(this)
        rv.itemAnimator = DefaultItemAnimator()

        bepAdapter = BepAdapter(displayOrders) { dishItem -> xửLyXongMonTaiBep(dishItem) }
        rv.adapter = bepAdapter

        database = FirebaseDatabase.getInstance(DB_URL).getReference("Orders")
        taiDuLieuChoBep()

        timerHandler.post(timerRunnable)
    }

    private fun taiDuLieuChoBep() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tableMap = mutableMapOf<String, MutableList<DataSnapshot>>()

                for (ds in snapshot.children) {
                    val status = ds.child("status").value?.toString() ?: ""
                    if (status == "waiting") {
                        val soBan = ds.child("soBan").value?.toString() ?: "0"
                        if (!tableMap.containsKey(soBan)) tableMap[soBan] = mutableListOf()
                        tableMap[soBan]?.add(ds)
                    }
                }

                val newList = mutableListOf<TableGroup>()
                for ((soBan, snapshots) in tableMap) {
                    // Gom tất cả ghi chú của khách tại bàn này
                    val allNotes = snapshots.mapNotNull { it.child("ghiChu").value?.toString() }
                        .filter { it.isNotEmpty() && it != "Không có" }
                        .distinct().joinToString(", ")

                    val minTime = snapshots.minOf {
                        it.child("timestamp").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                    }

                    // FIX LỖI: Truyền đúng 3 tham số DishItem(id, tenMon, snapshot)
                    val dishItems = snapshots.map {
                        DishItem(it.key ?: "", it.child("tenMon").value?.toString() ?: "Món không tên", it)
                    }.toMutableList()

                    newList.add(TableGroup(soBan, if (allNotes.isEmpty()) "Không có" else allNotes, minTime, dishItems))
                }

                newList.sortBy { it.timestamp }
                tvEmptyKitchen.visibility = if (newList.isEmpty()) View.VISIBLE else View.GONE

                displayOrders.clear()
                displayOrders.addAll(newList)
                bepAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // GIỮ NGUYÊN: Logic trừ kho bằng Transaction của Nam
    private fun xửLyXongMonTaiBep(dish: DishItem) {
        val rootRef = FirebaseDatabase.getInstance(DB_URL).reference

        rootRef.child("Menu").child(dish.tenMon).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val idKho = snapshot.child("idKho").value?.toString()
                val dinhMuc = snapshot.child("dinhMuc").value?.toString()?.toDoubleOrNull() ?: 0.0

                if (!idKho.isNullOrEmpty() && dinhMuc > 0) {
                    rootRef.child("Warehouse").child(idKho).runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val currentStock = currentData.getValue(Double::class.java) ?: 0.0
                            currentData.value = currentStock - dinhMuc
                            return Transaction.success(currentData)
                        }
                        override fun onComplete(error: DatabaseError?, b: Boolean, ds: DataSnapshot?) {
                            if (b) {
                                runOnUiThread { Toast.makeText(this@Bep, "Đã trừ kho món: ${dish.tenMon}", Toast.LENGTH_SHORT).show() }
                            }
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child(dish.id).child("status").setValue("cooked")
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }
}