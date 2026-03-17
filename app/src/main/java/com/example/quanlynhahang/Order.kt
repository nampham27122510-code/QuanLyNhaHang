package com.example.quanlynhahang

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.navigation.NavigationView
import com.google.firebase.database.*
import java.util.*

class Order : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var btnViewCart: ExtendedFloatingActionButton
    private val foodList = mutableListOf<DataSnapshot>()
    private val cartList = mutableListOf<DataSnapshot>()

    private var customerName = ""
    private var customerPhone = ""
    private var tableNumber = ""
    private var customerNote = ""

    private var lastTotalAmount = 0L
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        drawerLayout = findViewById(R.id.drawer_layout)
        btnViewCart = findViewById(R.id.btnViewCart)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)
        val rvFood = findViewById<RecyclerView>(R.id.rvFood)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        rvFood.layoutManager = GridLayoutManager(this, 2)
        foodAdapter = FoodAdapter(foodList) {
            cartList.add(it)
            btnViewCart.text = "Giỏ hàng (${cartList.size})"
        }
        rvFood.adapter = foodAdapter

        val database = FirebaseDatabase.getInstance(DB_URL)
        val menuRef = database.getReference("Menu")

        menuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()
                if (snapshot.exists()) {
                    for (ds in snapshot.children) {
                        foodList.add(ds)
                    }
                }
                foodAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Order, "Lỗi kết nối: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        btnOpenMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_payment -> showPaymentDialog()
                R.id.nav_service -> Toast.makeText(this@Order, "Đã gọi phục vụ!", Toast.LENGTH_SHORT).show()
                R.id.nav_login -> {
                    startActivity(Intent(this@Order, MainActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        btnViewCart.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this@Order, "Chưa chọn món nào!", Toast.LENGTH_SHORT).show()
            } else if (tableNumber.isEmpty()) {
                showUserInfoDialog()
            } else {
                confirmAndOrder()
            }
        }
    }

    private fun showUserInfoDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_dialog_user_info, null)
        val edtName = view.findViewById<EditText>(R.id.edtDialogName)
        val edtPhone = view.findViewById<EditText>(R.id.edtDialogPhone)
        val edtTable = view.findViewById<EditText>(R.id.edtDialogTable)
        val edtNote = view.findViewById<EditText>(R.id.edtDialogNote)

        AlertDialog.Builder(this@Order) // Chống văng
            .setView(view)
            .setCancelable(false)
            .setPositiveButton("ĐẶT MÓN") { _, _ ->
                customerName = edtName.text.toString().trim()
                customerPhone = edtPhone.text.toString().trim()
                tableNumber = edtTable.text.toString().trim()
                customerNote = edtNote.text.toString().trim()

                if (tableNumber.isNotEmpty()) confirmAndOrder()
                else {
                    Toast.makeText(this@Order, "Bắt buộc nhập số bàn!", Toast.LENGTH_SHORT).show()
                    showUserInfoDialog()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmAndOrder() {
        val database = FirebaseDatabase.getInstance(DB_URL)
        val orderRef = database.getReference("Orders")
        val ts = System.currentTimeMillis()

        lastTotalAmount = 0L
        for (item in cartList) {
            val giaRaw = item.child("gia").value.toString()
            val giaMon = giaRaw.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            lastTotalAmount += giaMon

            val data = HashMap<String, Any?>()
            data["tenKH"] = customerName
            data["sdt"] = customerPhone
            data["soBan"] = tableNumber
            data["tenMon"] = item.key
            data["gia"] = giaMon
            data["status"] = "waiting"
            data["timestamp"] = ts
            data["ghiChu"] = customerNote

            orderRef.push().setValue(data)
        }

        cartList.clear()
        btnViewCart.text = "Giỏ hàng (0)"
        Toast.makeText(this@Order, "Đơn hàng bàn $tableNumber đã gửi đi!", Toast.LENGTH_LONG).show()
    }

    private fun showPaymentDialog() {
        // Tính tổng tiền dựa trên giỏ hàng hoặc đơn vừa đặt gần nhất
        var total = 0L
        if (cartList.isNotEmpty()) {
            for (i in cartList) {
                val giaRaw = i.child("gia").value.toString()
                total += giaRaw.replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            }
        } else {
            total = lastTotalAmount
        }

        // Nếu chưa có món nào và cũng chưa đặt đơn nào thì không cho thanh toán
        if (total == 0L) {
            Toast.makeText(this@Order, "Chưa có hóa đơn cần thanh toán!", Toast.LENGTH_SHORT).show()
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null)
        view.findViewById<TextView>(R.id.tvTotalPayment).text = "${String.format("%,d", total)} VNĐ"

        val dialog = AlertDialog.Builder(this@Order).setView(view).create()

        // --- NÚT CHUYỂN KHOẢN ---
        view.findViewById<Button>(R.id.btnTransfer).setOnClickListener {
            if (tableNumber.isNotEmpty()) {
                val database = FirebaseDatabase.getInstance(DB_URL)
                val notiRef = database.getReference("Notifications")

                val notiData = HashMap<String, Any>()
                notiData["message"] = "Bàn số $tableNumber đã chuyển khoản, vui lòng chụp bill và xác nhận"
                notiData["table"] = tableNumber
                notiData["timestamp"] = System.currentTimeMillis()

                notiRef.setValue(notiData).addOnSuccessListener {
                    AlertDialog.Builder(this@Order)
                        .setTitle("Thông báo")
                        .setMessage("Đã gửi yêu cầu thanh toán chuyển khoản cho bàn $tableNumber. Vui lòng chuẩn bị sẵn hình ảnh xác nhận.")
                        .setPositiveButton("Đồng ý", null)
                        .show()
                }.addOnFailureListener {
                    Toast.makeText(this@Order, "Lỗi kết nối: ${it.message}", Toast.LENGTH_SHORT).show()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this@Order, "Vui lòng 'Đặt món' trước để có số bàn!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                showUserInfoDialog() // Bắt nhập thông tin bàn nếu chưa có
            }
        }

        // --- NÚT TIỀN MẶT ---
        view.findViewById<Button>(R.id.btnCash).setOnClickListener {
            if (tableNumber.isNotEmpty()) {
                val database = FirebaseDatabase.getInstance(DB_URL)
                val notiRef = database.getReference("Notifications")

                val notiData = HashMap<String, Any>()
                notiData["message"] = "Vui lòng ra bàn $tableNumber thanh toán"
                notiData["table"] = tableNumber
                notiData["timestamp"] = System.currentTimeMillis()

                notiRef.setValue(notiData).addOnSuccessListener {
                    Toast.makeText(this@Order, "Đã gửi yêu cầu cho nhân viên!", Toast.LENGTH_LONG).show()
                }
                dialog.dismiss()
            } else {
                Toast.makeText(this@Order, "Vui lòng 'Đặt món' trước để có số bàn!", Toast.LENGTH_LONG).show()
                dialog.dismiss()
                showUserInfoDialog()
            }
        }

        dialog.show()
    }
}