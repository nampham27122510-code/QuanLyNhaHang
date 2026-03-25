package com.example.quanlynhahang

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class nhanvien : AppCompatActivity() {

    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var database: FirebaseDatabase
    private lateinit var rvThanhToan: RecyclerView
    private lateinit var thanhToanAdapter: ThanhToanAdapter
    private val paymentList = mutableListOf<DataSnapshot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nhanvien)

        database = FirebaseDatabase.getInstance(DB_URL)
        rvThanhToan = findViewById(R.id.rvThanhToan)

        rvThanhToan.layoutManager = LinearLayoutManager(this)
        thanhToanAdapter = ThanhToanAdapter(paymentList) { table, total, key ->
            // Khi nhấn nút Xác nhận, hàm này sẽ chạy
            thanhToanHoaDon(table, total, key)
        }
        rvThanhToan.adapter = thanhToanAdapter

        listenNotifications()
    }

    private fun listenNotifications() {
        database.getReference("Notifications").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                paymentList.clear()
                for (ds in s.children) if (ds.hasChild("table")) paymentList.add(ds)
                thanhToanAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun thanhToanHoaDon(table: String, total: Long, key: String) {
        val orderRef = database.getReference("Orders")
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds in snapshot.children) {
                    // Dùng 'soban' để lọc đơn hàng trong bảng Orders
                    val ban = ds.child("soban").value?.toString()?.trim() ?: ""
                    if (ban == table) {
                        ds.ref.child("status").setValue("paid")
                    }
                }

                // Lưu doanh thu và XÓA THẺ thông báo ngay lập tức
                database.getReference("Revenue").child(date).child("total").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(data: MutableData): Transaction.Result {
                        val cur = data.getValue(Long::class.java) ?: 0L
                        data.value = cur + total
                        return Transaction.success(data)
                    }
                    override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {
                        if (b) {
                            // Xóa node thông báo theo ID ngẫu nhiên (-OoX...)
                            database.getReference("Notifications").child(key).removeValue()
                            Toast.makeText(this@nhanvien, "Đã thu $total VNĐ bàn $table", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}