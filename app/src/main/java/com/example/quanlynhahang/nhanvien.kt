package com.example.quanlynhahang

import android.os.Bundle
import android.widget.TextView
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

    private lateinit var rvMonChoGiao: RecyclerView
    private lateinit var giaoMonAdapter: GiaoMonAdapter
    private val deliveryList = mutableListOf<DataSnapshot>()
    private lateinit var tvPendingOrders: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nhanvien)

        database = FirebaseDatabase.getInstance(DB_URL)
        tvPendingOrders = findViewById(R.id.tvPendingOrders)

        rvThanhToan = findViewById(R.id.rvThanhToan)
        rvThanhToan.layoutManager = LinearLayoutManager(this)

        // SỬA: Khi ấn xác nhận, gọi hàm xử lý thanh toán trực tiếp
        thanhToanAdapter = ThanhToanAdapter(paymentList) { table, _, key ->
            thanhToanHoaDon(table, key)
        }
        rvThanhToan.adapter = thanhToanAdapter

        rvMonChoGiao = findViewById(R.id.rvMonChoGiao)
        rvMonChoGiao.layoutManager = LinearLayoutManager(this)
        giaoMonAdapter = GiaoMonAdapter(deliveryList) { snapshot ->
            snapshot.ref.child("status").setValue("delivered")
            Toast.makeText(this, "Đã giao món thành công!", Toast.LENGTH_SHORT).show()
        }
        rvMonChoGiao.adapter = giaoMonAdapter

        listenNotifications()
        listenCookedOrders()
    }

    private fun listenNotifications() {
        database.getReference("Notifications").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                paymentList.clear()
                for (ds in s.children) {
                    if (ds.hasChild("table")) {
                        paymentList.add(ds)
                    }
                }
                paymentList.sortByDescending { it.child("timestamp").value?.toString()?.toLongOrNull() ?: 0L }
                thanhToanAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {}
        })
    }

    private fun listenCookedOrders() {
        database.getReference("Orders").orderByChild("status").equalTo("cooked")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    deliveryList.clear()
                    for (ds in snapshot.children) {
                        deliveryList.add(ds)
                    }
                    giaoMonAdapter.notifyDataSetChanged()
                    tvPendingOrders.text = "🚚 Đơn chờ bưng: ${deliveryList.size} món"
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // SỬA: Hàm này bây giờ tự tính tiền và cộng vào Revenue cho Admin thấy luôn
    private fun thanhToanHoaDon(table: String, key: String) {
        val orderRef = database.getReference("Orders")
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val revenueRef = database.getReference("Revenue").child(date).child("total")

        orderRef.orderByChild("soBan").equalTo(table)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalAmount = 0L

                    if (snapshot.exists()) {
                        // 1. Tính tổng tiền thực tế của các món chưa thanh toán
                        for (ds in snapshot.children) {
                            val status = ds.child("status").value?.toString() ?: ""
                            if (status != "paid") {
                                val gia = ds.child("gia").value?.toString()?.toLongOrNull() ?: 0L
                                totalAmount += gia
                                // Chuyển sang paid ngay
                                ds.ref.child("status").setValue("paid")
                            }
                        }

                        // 2. Cộng vào doanh thu Admin bằng Transaction
                        if (totalAmount > 0) {
                            revenueRef.runTransaction(object : Transaction.Handler {
                                override fun doTransaction(data: MutableData): Transaction.Result {
                                    val cur = data.getValue(Long::class.java) ?: 0L
                                    data.value = cur + totalAmount
                                    return Transaction.success(data)
                                }

                                override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {
                                    if (b) {
                                        // 3. Xóa thông báo yêu cầu ngay khi cộng tiền xong
                                        database.getReference("Notifications").child(key).removeValue()
                                        Toast.makeText(this@nhanvien, "💰 Đã thu ${String.format("%,d", totalAmount)} VNĐ bàn $table", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            })
                        } else {
                            // Nếu không có món nào để tính tiền, vẫn xóa thông báo cho sạch máy
                            database.getReference("Notifications").child(key).removeValue()
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}