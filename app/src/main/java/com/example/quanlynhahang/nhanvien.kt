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
            // Khi giao xong, chuyển status thành delivered để bàn có thể về màu XÁM
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
                        // Đồng bộ status "cooked" từ bếp
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

        orderRef.orderByChild("soBan").equalTo(table).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val updates = hashMapOf<String, Any?>()
                for (ds in snapshot.children) {
                    // Đánh dấu đã trả tiền nhưng giữ nguyên status để nhân viên bưng nốt đồ
                    updates["${ds.key}/isPaid"] = true
                }

                if (updates.isEmpty()) return

                orderRef.updateChildren(updates).addOnSuccessListener {
                    revenueRef.runTransaction(object : Transaction.Handler {
                        override fun doTransaction(currentData: MutableData): Transaction.Result {
                            val current = currentData.getValue(Long::class.java) ?: 0L
                            currentData.value = current + totalAmount
                            return Transaction.success(currentData)
                        }
                        override fun onComplete(e: DatabaseError?, b: Boolean, s: DataSnapshot?) {
                            notifyRef.child("ban_$table").removeValue()
                            Toast.makeText(this@nhanvien, "Đã thu tiền Bàn $table!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                    })
                }
            }
            override fun onCancelled(p0: DatabaseError) {}
        })
    }
}