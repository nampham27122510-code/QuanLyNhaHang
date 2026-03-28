package com.example.quanlynhahang

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.OnBackPressedCallback
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
    private var doubleBackToExitPressedOnce = false

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
        database.getReference("Menu").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()
                for (ds in snapshot.children) foodList.add(ds)
                foodAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnOpenMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_payment -> showPaymentDialog()
                R.id.nav_service -> {
                    if (tableNumber.isNotEmpty()) {
                        // Gửi thông báo phục vụ thông thường
                        sendNotification("BÀN $tableNumber YÊU CẦU PHỤC VỤ!", "Service")
                    } else {
                        Toast.makeText(this, "Vui lòng nhập số bàn trước!", Toast.LENGTH_SHORT).show()
                        showUserInfoDialog()
                    }
                }
                R.id.nav_login -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        btnViewCart.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show()
            } else if (tableNumber.isEmpty()) {
                showUserInfoDialog()
            } else {
                confirmAndOrder()
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    if (doubleBackToExitPressedOnce) {
                        finishAffinity()
                        return
                    }
                    doubleBackToExitPressedOnce = true
                    Toast.makeText(this@Order, "Nhấn lần nữa để thoát", Toast.LENGTH_SHORT).show()
                    Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
                }
            }
        })
    }

    private fun showUserInfoDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_dialog_user_info, null)
        val edtName = view.findViewById<EditText>(R.id.edtDialogName)
        val edtPhone = view.findViewById<EditText>(R.id.edtDialogPhone)
        val edtTable = view.findViewById<EditText>(R.id.edtDialogTable)
        val edtNote = view.findViewById<EditText>(R.id.edtDialogNote)

        AlertDialog.Builder(this).setView(view).setCancelable(false)
            .setPositiveButton("ĐẶT MÓN") { _, _ ->
                customerName = edtName.text.toString().trim()
                customerPhone = edtPhone.text.toString().trim()
                tableNumber = edtTable.text.toString().trim()
                customerNote = edtNote.text.toString().trim()
                if (tableNumber.isNotEmpty()) confirmAndOrder()
                else {
                    Toast.makeText(this, "Bắt buộc nhập số bàn!", Toast.LENGTH_SHORT).show()
                    showUserInfoDialog()
                }
            }.setNegativeButton("Hủy", null).show()
    }

    private fun confirmAndOrder() {
        val db = FirebaseDatabase.getInstance(DB_URL)
        val orderRef = db.getReference("Orders")
        val warehouseRef = db.getReference("Warehouse")
        val ts = System.currentTimeMillis()
        var totalOrderPrice = 0L

        for (item in cartList) {
            val tenMon = item.key.toString()
            val idKho = item.child("idKho").value?.toString()?.trim() ?: ""
            val dinhMuc = item.child("dinhMuc").value?.toString()?.toDoubleOrNull() ?: 0.0
            val giaMon = item.child("gia").value.toString().replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L

            warehouseRef.child(idKho).get().addOnSuccessListener { snap ->
                val tonKhoHienTai = snap.value.toString().toDoubleOrNull() ?: 0.0

                if (tonKhoHienTai < dinhMuc) {
                    Toast.makeText(this, "Món $tenMon đã hết nguyên liệu!", Toast.LENGTH_LONG).show()
                } else {
                    totalOrderPrice += giaMon
                    lastTotalAmount = totalOrderPrice
                    warehouseRef.child(idKho).setValue(tonKhoHienTai - dinhMuc)

                    val data = HashMap<String, Any?>()
                    data["tenKH"] = customerName
                    data["sdt"] = customerPhone
                    data["soBan"] = tableNumber
                    data["tenMon"] = tenMon
                    data["gia"] = giaMon
                    data["status"] = "waiting"
                    data["timestamp"] = ts
                    data["idKho"] = idKho
                    data["dinhMuc"] = dinhMuc
                    data["ghiChu"] = customerNote

                    orderRef.push().setValue(data)
                }
            }
        }

        cartList.clear()
        btnViewCart.text = "Giỏ hàng (0)"
        Toast.makeText(this, "Đã gửi đơn bàn $tableNumber!", Toast.LENGTH_SHORT).show()
    }

    private fun showPaymentDialog() {
        var total = 0L
        if (cartList.isNotEmpty()) {
            for (i in cartList) {
                val giaRaw = i.child("gia").value.toString().replace(Regex("[^0-9]"), "")
                total += giaRaw.toLongOrNull() ?: 0L
            }
        } else {
            total = lastTotalAmount
        }

        if (total == 0L) {
            Toast.makeText(this, "Chưa có hóa đơn!", Toast.LENGTH_SHORT).show()
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalPayment)
        tvTotal.text = "${String.format("%,d", total)} VNĐ"

        val dialog = AlertDialog.Builder(this).setView(view).create()

        view.findViewById<Button>(R.id.btnTransfer).setOnClickListener {
            // SỬA: Gửi thông báo kèm phương thức Chuyển khoản
            sendNotification("Bàn $tableNumber yêu cầu CHUYỂN KHOẢN: ${tvTotal.text}", "Transfer")
            dialog.dismiss()
        }

        view.findViewById<Button>(R.id.btnCash).setOnClickListener {
            // SỬA: Gửi thông báo kèm phương thức Tiền mặt
            sendNotification("Bàn $tableNumber yêu cầu TIỀN MẶT: ${tvTotal.text}", "Cash")
            dialog.dismiss()
        }
        dialog.show()
    }

    // SỬA: Hàm nhận thêm tham số phương thức (paymentMethod)
    private fun sendNotification(msg: String, paymentMethod: String = "Normal") {
        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications")
        val data = HashMap<String, Any>()
        data["message"] = msg
        data["table"] = tableNumber
        data["method"] = paymentMethod // Thêm trường phương thức để bên nhân viên hiển thị icon/màu sắc
        data["timestamp"] = System.currentTimeMillis()

        ref.push().setValue(data).addOnSuccessListener {
            Toast.makeText(this, "Nhân viên đang đến!", Toast.LENGTH_SHORT).show()
        }
    }
}