package com.example.quanlynhahang

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Order : AppCompatActivity() {

    // Tạo một danh sách để lưu dữ liệu món ăn từ Menu
    private val menuList = mutableListOf<DataSnapshot>()
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        val edtSoBan = findViewById<EditText>(R.id.edtSoBan)
        val spnMenu = findViewById<Spinner>(R.id.spnMenu)
        val edtSoLuong = findViewById<EditText>(R.id.edtSoLuong)
        val btnGuiOrder = findViewById<Button>(R.id.btnGuiOrder)

        val database = FirebaseDatabase.getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
        val menuRef = database.getReference("Menu")
        val orderRef = database.getReference("Orders")

        // 1. Tải danh sách Menu từ Firebase về Spinner
        val menuNames = mutableListOf<String>()
        adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, menuNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnMenu.adapter = adapter

        menuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                menuNames.clear()
                menuList.clear()
                for (ds in snapshot.children) {
                    val ten = ds.key.toString() // Ví dụ: "Phở Bò"
                    menuNames.add(ten)
                    menuList.add(ds)
                }
                adapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Xử lý gửi đơn
        btnGuiOrder.setOnClickListener {
            val soBan = edtSoBan.text.toString().trim()
            val soLuong = edtSoLuong.text.toString().trim()
            val selectedPosition = spnMenu.selectedItemPosition

            if (soBan.isEmpty() || selectedPosition == -1) {
                Toast.makeText(this, "Vui lòng chọn món và nhập số bàn!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Lấy thông tin chi tiết của món đã chọn từ danh sách menuList
            val monSelected = menuList[selectedPosition]
            val tenMon = monSelected.key.toString()
            val gia = monSelected.child("gia").value
            val idKho = monSelected.child("idKho").value
            val dinhMuc = monSelected.child("dinhMuc").value
            val thoiGian = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

            val orderId = orderRef.push().key
            val dataMap = HashMap<String, Any?>()
            dataMap["soBan"] = soBan
            dataMap["tenMon"] = tenMon
            dataMap["soLuong"] = soLuong
            dataMap["gia"] = gia
            dataMap["idKho"] = idKho
            dataMap["dinhMuc"] = dinhMuc
            dataMap["thoiGian"] = thoiGian

            if (orderId != null) {
                orderRef.child(orderId).setValue(dataMap).addOnSuccessListener {
                    Toast.makeText(this, "Đã gửi $tenMon cho $soBan", Toast.LENGTH_SHORT).show()
                    edtSoLuong.setText("1")
                }
            }
        }
    }
}