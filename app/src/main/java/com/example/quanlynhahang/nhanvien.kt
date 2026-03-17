package com.example.quanlynhahang

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class nhanvien : AppCompatActivity() {

    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var database: FirebaseDatabase
    private lateinit var tvPendingOrders: TextView
    private lateinit var rvMonChoGiao: RecyclerView

    // Adapter cho danh sách món cần đi giao
    private lateinit var giaoMonAdapter: GiaoMonAdapter
    private val cookedList = mutableListOf<DataSnapshot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nhanvien)

        // 1. Ánh xạ View
        tvPendingOrders = findViewById(R.id.tvPendingOrders)
        rvMonChoGiao = findViewById(R.id.rvMonChoGiao)
        database = FirebaseDatabase.getInstance(DB_URL)

        // 2. Cấu hình RecyclerView cho danh sách món chờ giao
        rvMonChoGiao.layoutManager = LinearLayoutManager(this)
        giaoMonAdapter = GiaoMonAdapter(cookedList) { ds ->
            // Khi nhấn nút "Đã giao" trong Adapter
            ds.ref.child("status").setValue("done")
            Toast.makeText(this, "Đã bưng món ra bàn!", Toast.LENGTH_SHORT).show()
        }
        rvMonChoGiao.adapter = giaoMonAdapter

        // 3. Các hàm lắng nghe dữ liệu
        countWaitingOrders()      // Đếm món đang nấu trong bếp
        listenForCookedOrders()   // Hiện món bếp đã nấu xong (chờ bưng)
        listenForPaymentRequests()// Nhận yêu cầu thanh toán (Popup)
    }

    // Đếm số lượng món Bếp đang làm (status = waiting)
    private fun countWaitingOrders() {
        val orderRef = database.getReference("Orders")
        orderRef.orderByChild("status").equalTo("waiting").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.childrenCount
                tvPendingOrders.text = "Đơn đang nấu: $count món"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // Lắng nghe danh sách món Bếp đã nấu xong (status = cooked)
    private fun listenForCookedOrders() {
        val orderRef = database.getReference("Orders")
        orderRef.orderByChild("status").equalTo("cooked").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                cookedList.clear()
                for (ds in snapshot.children) {
                    cookedList.add(ds)
                }
                giaoMonAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenForPaymentRequests() {
        val notiRef = database.getReference("Notifications")
        notiRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val message = snapshot.child("message").value.toString()
                    val table = snapshot.child("table").value.toString()
                    showPaymentAlert(message, table, notiRef)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showPaymentAlert(message: String, table: String, notiRef: DatabaseReference) {
        AlertDialog.Builder(this)
            .setTitle("💰 YÊU CẦU THANH TOÁN")
            .setMessage(message)
            .setCancelable(false)
            .setPositiveButton("ĐÃ KIỂM TRA & XÁC NHẬN") { _, _ ->
                confirmPaymentAndClearKitchen(table)
                notiRef.removeValue()
            }
            .setNegativeButton("HỦY", null)
            .show()
    }

    private fun confirmPaymentAndClearKitchen(table: String) {
        val orderRef = database.getReference("Orders")
        val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val revenueRef = database.getReference("Revenue").child(currentDate).child("total")

        orderRef.orderByChild("soBan").equalTo(table).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalToAdmin = 0L
                for (ds in snapshot.children) {
                    val status = ds.child("status").value.toString()
                    // Tính tiền cho tất cả món chưa được thanh toán (chờ nấu, chờ giao, hoặc đã giao xong)
                    if (status == "waiting" || status == "cooked" || status == "done") {
                        val giaMon = ds.child("gia").value.toString().toLongOrNull() ?: 0L
                        totalToAdmin += giaMon

                        // Sau khi thu tiền, đổi hết về status 'paid' hoặc xóa đi để dọn bàn
                        ds.ref.child("status").setValue("paid")
                    }
                }

                if (totalToAdmin > 0) {
                    revenueRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(mutableData: MutableData): Transaction.Result {
                            val current = mutableData.getValue(Long::class.java) ?: 0L
                            mutableData.value = current + totalToAdmin
                            return Transaction.success(mutableData)
                        }
                        override fun onComplete(a: DatabaseError?, b: Boolean, c: DataSnapshot?) {
                            if (b) Toast.makeText(this@nhanvien, "Doanh thu +${totalToAdmin}đ", Toast.LENGTH_SHORT).show()
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}