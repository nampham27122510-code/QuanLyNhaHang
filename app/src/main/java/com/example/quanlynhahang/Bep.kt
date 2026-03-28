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

    // Sử dụng Handler để tạo vòng lặp cập nhật thời gian mỗi giây
    private val timerHandler = Handler(Looper.getMainLooper())
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (::bepAdapter.isInitialized && displayOrders.isNotEmpty()) {
                // SỬA: Thông báo cho Adapter cập nhật lại vùng hiển thị thời gian mà không vẽ lại cả item
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

        // Hiệu ứng thêm/xóa mượt mà cho danh sách bếp
        rv.itemAnimator = DefaultItemAnimator().apply {
            removeDuration = 500
            moveDuration = 500
        }

        bepAdapter = BepAdapter(displayOrders) { item -> xửLyXongMonTaiBep(item) }
        rv.adapter = bepAdapter

        database = FirebaseDatabase.getInstance(DB_URL).getReference("Orders")
        taiDuLieuHoaDon()

        // Bắt đầu chạy đồng hồ đếm giây
        timerHandler.post(timerRunnable)
    }

    private fun taiDuLieuHoaDon() {
        // Chỉ lấy món "waiting" và gộp nhóm theo bàn + tên món
        database.orderByChild("status").equalTo("waiting")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val rawList = mutableListOf<DataSnapshot>()
                    for (ds in snapshot.children) rawList.add(ds)

                    // Đồng nhất Key 'soBan' (B hoa)
                    val groups = rawList.groupBy {
                        "${it.child("soBan").value?.toString() ?: "0"}_${it.child("tenMon").value?.toString() ?: "NoName"}"
                    }

                    val newList = mutableListOf<GroupedItem>()

                    for ((_, items) in groups) {
                        val first = items[0]
                        val ban = first.child("soBan").value?.toString() ?: "0"

                        // Kiểm tra trạng thái thanh toán đồng bộ
                        val paid = items.any { it.child("status").value == "paid" }

                        newList.add(GroupedItem(
                            ban,
                            first.child("tenMon").value?.toString() ?: "Món không tên",
                            items.size,
                            // Đảm bảo lấy đúng Long để tính toán thời gian
                            first.child("timestamp").value?.toString()?.toLongOrNull() ?: System.currentTimeMillis(),
                            items,
                            paid
                        ))
                    }

                    // Sắp xếp món cũ lên đầu để ưu tiên nấu trước
                    newList.sortBy { it.timestamp }

                    displayOrders.clear()
                    displayOrders.addAll(newList)
                    bepAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun xửLyXongMonTaiBep(item: GroupedItem) {
        // Chuyển trạng thái sang 'cooked' để biến mất khỏi màn hình bếp và hiện bên phục vụ
        for (ds in item.snapshots) {
            ds.ref.child("status").setValue("cooked")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Quan trọng: Phải dừng Handler khi thoát Activity để tránh tốn pin và lỗi bộ nhớ
        timerHandler.removeCallbacks(timerRunnable)
    }
}