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

        // 1. Setup RecyclerView Thanh Toán
        rvThanhToan = findViewById(R.id.rvThanhToan)
        rvThanhToan.layoutManager = LinearLayoutManager(this)
        thanhToanAdapter = ThanhToanAdapter(paymentList) { table, total, key ->
            // Khi nhấn nút XÁC NHẬN CK
            xuLyThanhToanDutDiem(table)
        }
        rvThanhToan.adapter = thanhToanAdapter

        // 2. Setup RecyclerView Bê Đồ
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
        // Lắng nghe yêu cầu thanh toán
        database.getReference("Notifications_Pay").child(tableKey).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(s: DataSnapshot) {
                paymentList.clear()
                if (s.exists()) paymentList.add(s)
                thanhToanAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(e: DatabaseError) {}
        })

        // Lắng nghe món chờ bưng
        database.getReference("Orders").orderByChild("soBan").equalTo(selectedTable)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(s: DataSnapshot) {
                    deliveryList.clear()
                    for (ds in s.children) {
                        if (ds.child("status").value == "cooked") deliveryList.add(ds)
                    }
                    giaoMonAdapter.notifyDataSetChanged()
                }
                override fun onCancelled(e: DatabaseError) {}
            })
    }

    // --- LOGIC QUAN TRỌNG: DỌN BÀN & CHỐT TIỀN ---
    private fun xuLyThanhToanDutDiem(table: String) {
        val orderRef = database.getReference("Orders")
        val revenueRef = database.getReference("Revenue")
        val notifyRef = database.getReference("Notifications_Pay")
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // Bước 1: Tìm tất cả món của bàn này chưa thanh toán
        orderRef.orderByChild("soBan").equalTo(table).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalTablePrice = 0L

                for (ds in snapshot.children) {
                    if (ds.child("status").value != "paid") {
                        val gia = ds.child("gia").getValue(Long::class.java) ?: 0L
                        totalTablePrice += gia
                        // Chuyển trạng thái món sang 'paid' (để không hiện lại nữa)
                        ds.ref.child("status").setValue("paid")
                    }
                }

                // Bước 2: Cộng dồn tiền vào doanh thu ngày hôm nay
                revenueRef.child(today).child("total").runTransaction(object : Transaction.Handler {
                    override fun doTransaction(currentData: MutableData): Transaction.Result {
                        val currentTotal = currentData.getValue(Long::class.java) ?: 0L
                        currentData.value = currentTotal + totalTablePrice
                        return Transaction.success(currentData)
                    }
                    override fun onComplete(error: DatabaseError?, committed: Boolean, currentData: DataSnapshot?) {
                        // Bước 3: Xóa thông báo đèn đỏ (Notifications_Pay)
                        notifyRef.child("ban_$table").removeValue().addOnSuccessListener {
                            Toast.makeText(this@nhanvien, "Đã chốt Bàn $table, Tổng: $totalTablePrice VNĐ", Toast.LENGTH_LONG).show()
                            finish() // Xong việc thì quay về sơ đồ 30 bàn
                        }
                    }
                })
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}