package com.example.quanlynhahang

import android.os.Bundle
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

        // Lấy số bàn được truyền từ NhanVienDashboard
        selectedTable = intent.getStringExtra("TABLE_ID") ?: ""
        title = "Chi tiết Bàn $selectedTable"
        database = FirebaseDatabase.getInstance(DB_URL)

        // 1. Cấu hình RecyclerView Thanh Toán
        rvThanhToan = findViewById(R.id.rvThanhToan)
        rvThanhToan.layoutManager = LinearLayoutManager(this)
        thanhToanAdapter = ThanhToanAdapter(paymentList) { table, total, key ->
            // Gọi hàm xử lý khi nhấn nút XÁC NHẬN trên Adapter
            xuLyThanhToanDutDiem(table)
        }
        rvThanhToan.adapter = thanhToanAdapter

        // 2. Cấu hình RecyclerView Bê Đồ (Món đã nấu xong)
        rvMonChoGiao = findViewById(R.id.rvMonChoGiao)
        rvMonChoGiao.layoutManager = LinearLayoutManager(this)
        giaoMonAdapter = GiaoMonAdapter(deliveryList) { snapshot ->
            // Chuyển trạng thái món thành 'delivered' khi nhân viên bưng ra
            snapshot.ref.child("status").setValue("delivered")
        }
        rvMonChoGiao.adapter = giaoMonAdapter

        listenData()
    }

    private fun listenData() {
        if (selectedTable.isEmpty()) return

        val tableKey = "ban_$selectedTable"

        // Lắng nghe yêu cầu thanh toán từ nhánh Notifications_Pay
        database.getReference("Notifications_Pay").child(tableKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                paymentList.clear()
                if (s.exists()) {
                    paymentList.add(s)
                }
                thanhToanAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Lắng nghe các món có trạng thái 'cooked' để đi giao
        database.getReference("Orders").orderByChild("soBan").equalTo(selectedTable)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    deliveryList.clear()
                    for (ds in s.children) {
                        val status = ds.child("status").value?.toString() ?: ""
                        if (status == "cooked") {
                            deliveryList.add(ds)
                        }
                    }
                    giaoMonAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    // --- HÀM QUAN TRỌNG: XỬ LÝ CHỐT TIỀN & DỌN SẠCH THÔNG BÁO ---
    private fun xuLyThanhToanDutDiem(table: String) {
        val orderRef = database.getReference("Orders")
        val revenueRef = database.getReference("Revenue")
        val notifyRef = database.getReference("Notifications_Pay")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Chuẩn hóa Key thông báo để tránh lỗi không xóa được bàn (như Bàn 2)
        val notifyKey = if (table.startsWith("ban_")) table else "ban_$table"

        // 1. Tìm và cập nhật tất cả món ăn của bàn này thành 'paid'
        orderRef.orderByChild("soBan").equalTo(table).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) return

                val updates = hashMapOf<String, Any?>()
                var totalTablePrice = 0L

                for (ds in snapshot.children) {
                    val status = ds.child("status").value?.toString() ?: ""
                    if (status != "paid") {
                        val gia = ds.child("gia").getValue(Long::class.java) ?: 0L
                        totalTablePrice += gia
                        // Gom các món cần update vào một Map
                        updates["${ds.key}/status"] = "paid"
                    }
                }

                if (updates.isEmpty()) return

                // Thực hiện cập nhật trạng thái món ăn hàng loạt
                orderRef.updateChildren(updates).addOnSuccessListener {

                    // 2. Cộng dồn tiền vào doanh thu ngày bằng Transaction
                    revenueRef.child(today).child("total").runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val currentTotal = currentData.getValue(Long::class.java) ?: 0L
                            currentData.value = currentTotal + totalTablePrice
                            return Transaction.success(currentData)
                        }

                        override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                            // 3. Xóa thông báo yêu cầu thanh toán (Tắt đèn đỏ trên Dashboard)
                            notifyRef.child(notifyKey).removeValue().addOnSuccessListener {
                                Toast.makeText(this@nhanvien, "Đã thanh toán Bàn $table: ${String.format("%,d", totalTablePrice)} VNĐ", Toast.LENGTH_LONG).show()
                                finish() // Thoát ra màn hình Sơ đồ bàn
                            }.addOnFailureListener {
                                Toast.makeText(this@nhanvien, "Lỗi xóa thông báo thanh toán!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    })
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}