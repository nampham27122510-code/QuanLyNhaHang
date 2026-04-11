package com.example.quanlynhahang

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlynhahang.com.example.quanlynhahang.ThanhToanAdapter
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class nhanvien : AppCompatActivity() {
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var database: FirebaseDatabase
    private var selectedTable = ""

    private lateinit var rvThanhToan: RecyclerView
    private lateinit var thanhToanAdapter: ThanhToanAdapter
    private val paymentList = mutableListOf<DataSnapshot>()

    private lateinit var rvMonChoGiao: RecyclerView
    private lateinit var giaoMonAdapter: GiaoMonAdapter
    private val deliveryList = mutableListOf<DataSnapshot>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nhanvien)

        selectedTable = intent.getStringExtra("TABLE_ID") ?: ""
        title = "Chi tiết Bàn $selectedTable"
        database = FirebaseDatabase.getInstance(DB_URL)

        rvThanhToan = findViewById(R.id.rvThanhToan)
        rvThanhToan.layoutManager = LinearLayoutManager(this)
        thanhToanAdapter = ThanhToanAdapter(paymentList) { table, total, key ->
            thucHienThanhToan(table, total)
        }
        rvThanhToan.adapter = thanhToanAdapter

        rvMonChoGiao = findViewById(R.id.rvMonChoGiao)
        rvMonChoGiao.layoutManager = LinearLayoutManager(this)
        giaoMonAdapter = GiaoMonAdapter(deliveryList) { snapshot ->
            snapshot.ref.child("status").setValue("delivered")
        }
        rvMonChoGiao.adapter = giaoMonAdapter

        listenData()
    }

    private fun listenData() {
        val tableKey = "ban_$selectedTable"
        database.getReference("Notifications_Pay").child(tableKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                paymentList.clear()
                if (s.exists()) paymentList.add(s)
                thanhToanAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        database.getReference("Orders").orderByChild("soBan").equalTo(selectedTable)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    deliveryList.clear()
                    for (ds in s.children) {
                        if (ds.child("status").value == "cooked") deliveryList.add(ds)
                    }
                    giaoMonAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun thucHienThanhToan(table: String, totalAmount: Long) {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val orderRef = database.getReference("Orders")
        val revenueRef = database.getReference("Revenue").child(today).child("total")
        val notifyRef = database.getReference("Notifications_Pay")
        val tableStatusRef = database.getReference("TableStatus").child("ban_$table")

        // --- LOGIC MỚI: Sao lưu vào OrderHistory để giữ dữ liệu cho biểu đồ tròn ---
        val historyRef = database.getReference("OrderHistory").child(today)

        orderRef.orderByChild("soBan").equalTo(table)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        Toast.makeText(this@nhanvien, "Không có đơn hàng để thanh toán!", Toast.LENGTH_SHORT).show()
                        return
                    }

                    val deleteUpdates = hashMapOf<String, Any?>()

                    for (ds in snapshot.children) {
                        // 1. Sao lưu từng món vào OrderHistory trước khi xóa
                        val orderData = ds.value
                        historyRef.push().setValue(orderData)

                        // 2. Gom ID để xóa khỏi node Orders
                        deleteUpdates[ds.key!!] = null
                    }

                    // Bước 1: Ghi confirmed_paid để SoDoBan chuyển xám và tự xóa node
                    tableStatusRef.setValue("confirmed_paid").addOnSuccessListener {

                        // Bước 2: Xóa toàn bộ Orders của bàn này (vì đã được lưu vào history ở trên)
                        orderRef.updateChildren(deleteUpdates).addOnSuccessListener {

                            // Bước 3: Cộng doanh thu
                            revenueRef.runTransaction(object : Transaction.Handler {
                                override fun doTransaction(currentData: MutableData): Transaction.Result {
                                    val current = currentData.getValue(Long::class.java) ?: 0L
                                    currentData.value = current + totalAmount
                                    return Transaction.success(currentData)
                                }
                                override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {
                                    // Bước 4: Xóa thông báo yêu cầu thanh toán (chấm đỏ)
                                    notifyRef.child("ban_$table").removeValue()
                                    Toast.makeText(
                                        this@nhanvien,
                                        "Thanh toán thành công! Bàn $table đã trống.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    finish()
                                }
                            })
                        }
                    }
                }
                override fun onCancelled(p0: DatabaseError) {
                    Log.e("Firebase_Error", "Thanh toán thất bại: ${p0.message}")
                }
            })
    }
}