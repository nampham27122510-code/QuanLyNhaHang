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
                val allTableIds = mutableSetOf<String>()

                // Lấy danh sách tất cả các bàn đang có trong Orders để kiểm tra trạng thái phục vụ
                for (ds in snapshot.children) {
                    val soBan = ds.child("soBan").value?.toString() ?: continue
                    allTableIds.add(soBan)

                    val status = ds.child("status").value?.toString() ?: ""
                    if (status == "waiting") {
                        if (!tableMap.containsKey(soBan)) tableMap[soBan] = mutableListOf()
                        tableMap[soBan]?.add(ds)
                    }
                }

                // LOGIC QUAN TRỌNG: Cập nhật allServed dựa trên việc còn món waiting hay không
                val rootRef = FirebaseDatabase.getInstance(DB_URL).reference
                allTableIds.forEach { idBan ->
                    val isStillWaiting = tableMap.containsKey(idBan)
                    // Nếu không còn món waiting thì allServed = true, ngược lại là false
                    rootRef.child("Tables").child(idBan).child("allServed").setValue(!isStillWaiting)
                }

                val newList = mutableListOf<TableGroup>()
                for ((soBan, snapshots) in tableMap) {
                    val dishGroupMap = mutableMapOf<String, DishItem>()

                    for (ds in snapshots) {
                        val tenMon = ds.child("tenMon").value?.toString() ?: "Món không tên"
                        val id = ds.key ?: ""

                        if (dishGroupMap.containsKey(tenMon)) {
                            dishGroupMap[tenMon]?.apply {
                                this.soLuong += 1
                                this.idList.add(id)
                            }
                        } else {
                            dishGroupMap[tenMon] = DishItem(
                                idList = mutableListOf(id),
                                tenMon = tenMon,
                                soLuong = 1,
                                snapshot = ds
                            )
                        }
                    }

                    val allNotes = snapshots.mapNotNull { it.child("ghiChu").value?.toString() }
                        .filter { it.isNotEmpty() && it != "Không có" }
                        .distinct().joinToString(", ")

                    val minTime = snapshots.minOfOrNull {
                        it.child("timestamp").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis()
                    } ?: System.currentTimeMillis()

                    newList.add(TableGroup(soBan, if (allNotes.isEmpty()) "Không có" else allNotes, minTime, dishGroupMap.values.toMutableList()))
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

    private fun xửLyXongMonTaiBep(dish: DishItem) {
        val rootRef = FirebaseDatabase.getInstance(DB_URL).reference

        // 1. Cập nhật trạng thái "cooked" cho tất cả các ID của món trùng tên này
        dish.idList.forEach { id ->
            database.child(id).child("status").setValue("cooked")
        }

        // 2. Logic trừ kho
        rootRef.child("Menu").child(dish.tenMon).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val idKho = snapshot.child("idKho").value?.toString()
                val dinhMucDonVi = snapshot.child("dinhMuc").value?.toString()?.toDoubleOrNull() ?: 0.0
                val tongDinhMuc = dinhMucDonVi * dish.soLuong

                if (!idKho.isNullOrEmpty() && tongDinhMuc > 0) {
                    rootRef.child("Warehouse").child(idKho).runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val currentStock = currentData.getValue(Double::class.java) ?: 0.0
                            currentData.value = currentStock - tongDinhMuc
                            return Transaction.success(currentData)
                        }
                        override fun onComplete(error: DatabaseError?, b: Boolean, ds: DataSnapshot?) {
                            if (b) {
                                runOnUiThread {
                                    Toast.makeText(this@Bep, "Đã giao: ${dish.tenMon} x${dish.soLuong}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        timerHandler.removeCallbacks(timerRunnable)
    }
}