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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        // 1. Ánh xạ View
        drawerLayout = findViewById(R.id.drawer_layout)
        btnViewCart = findViewById(R.id.btnViewCart)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)
        val rvFood = findViewById<RecyclerView>(R.id.rvFood)
        val navView = findViewById<NavigationView>(R.id.nav_view)

        // 2. Cấu hình RecyclerView - Phải làm trước khi nạp data
        rvFood.layoutManager = GridLayoutManager(this, 2)
        foodAdapter = FoodAdapter(foodList) {
            cartList.add(it)
            btnViewCart.text = "Giỏ hàng (${cartList.size})"
        }
        rvFood.adapter = foodAdapter

        // 3. Kết nối Firebase tải Menu
        val database = FirebaseDatabase.getInstance("https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app")
        val menuRef = database.getReference("Menu")

        menuRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear() // Xóa danh sách cũ tránh trùng lặp
                if (snapshot.exists()) {
                    for (ds in snapshot.children) {
                        foodList.add(ds)
                    }
                }
                foodAdapter.notifyDataSetChanged() // Ép giao diện hiển thị món ngay
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@Order, "Lỗi kết nối: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })

        // 4. Sidebar
        btnOpenMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_payment -> showPaymentDialog()
                R.id.nav_service -> Toast.makeText(this, "Đã gọi phục vụ!", Toast.LENGTH_SHORT).show()
                R.id.nav_login -> {
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        // 5. Nút Giỏ hàng
        btnViewCart.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Chưa chọn món nào!", Toast.LENGTH_SHORT).show()
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

        AlertDialog.Builder(this)
            .setView(view)
            .setCancelable(false)
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
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun confirmAndOrder() {
        val orderRef = FirebaseDatabase.getInstance().getReference("Orders")
        val ts = System.currentTimeMillis()
        for (item in cartList) {
            val data = HashMap<String, Any?>()
            data["tenKH"] = customerName
            data["sdt"] = customerPhone
            data["soBan"] = tableNumber
            data["tenMon"] = item.key
            data["gia"] = item.child("gia").value
            data["status"] = "waiting"
            data["timestamp"] = ts
            data["ghiChu"] = customerNote
            orderRef.push().setValue(data)
        }

        cartList.clear() // Reset giỏ hàng
        btnViewCart.text = "Giỏ hàng (0)"
        Toast.makeText(this, "Đơn hàng bàn $tableNumber đã gửi đi!", Toast.LENGTH_LONG).show()
    }

    private fun showPaymentDialog() {
        var total = 0L
        for (i in cartList) total += i.child("gia").value.toString().toLongOrNull() ?: 0L

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null)
        view.findViewById<TextView>(R.id.tvTotalPayment).text = "${String.format("%,d", total)} VNĐ"

        val dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<Button>(R.id.btnTransfer).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://qr.napas.com.vn/pay")))
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnCash).setOnClickListener {
            Toast.makeText(this, "Thanh toán tại quầy - Bàn $tableNumber", Toast.LENGTH_LONG).show()
            dialog.dismiss()
        }
        dialog.show()
    }
}