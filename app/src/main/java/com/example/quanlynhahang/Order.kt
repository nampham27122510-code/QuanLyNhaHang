package com.example.quanlynhahang

import android.content.Intent
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
import com.google.zxing.integration.android.IntentIntegrator

class Order : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var foodAdapter: FoodAdapter
    private lateinit var btnViewCart: ExtendedFloatingActionButton
    private val foodList = mutableListOf<DataSnapshot>()
    private val cartList = mutableListOf<DataSnapshot>()

    private var currentTableNumber = ""
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order)

        drawerLayout = findViewById(R.id.drawer_layout)
        btnViewCart = findViewById(R.id.btnViewCart)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        val rvFood = findViewById<RecyclerView>(R.id.rvFood)
        val btnOpenMenu = findViewById<ImageButton>(R.id.btnOpenMenu)

        database = FirebaseDatabase.getInstance(DB_URL)

        rvFood.layoutManager = GridLayoutManager(this, 2)
        foodAdapter = FoodAdapter(foodList) { snapshot ->
            cartList.add(snapshot)
            btnViewCart.text = "Giỏ hàng (${cartList.size})"
        }
        rvFood.adapter = foodAdapter

        database.getReference("Menu").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()
                for (ds in snapshot.children) foodList.add(ds)
                foodAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        btnOpenMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }

        btnViewCart.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Giỏ hàng trống!", Toast.LENGTH_SHORT).show()
            } else {
                showCartDetailDialog()
            }
        }

        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan_qr -> startScanningQR()
                R.id.nav_payment -> showPaymentDialog()
                R.id.nav_login -> startActivity(Intent(this, MainActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }
    }

    // HIỂN THỊ GIỎ HÀNG VÀ XÓA MÓN (KHÔNG CHỒNG CỬA SỔ)
    private fun showCartDetailDialog() {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Giỏ hàng (Chạm để xóa)")

        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        cartList.forEach { adapter.add(it.key.toString()) }

        val listView = ListView(this)
        listView.adapter = adapter

        builder.setView(listView)
        builder.setPositiveButton("XÁC NHẬN") { _, _ -> showUserInfoDialog() }
        builder.setNegativeButton("CHỌN THÊM", null)

        val alertDialog = builder.create()

        listView.setOnItemClickListener { _, _, position, _ ->
            cartList.removeAt(position)
            btnViewCart.text = "Giỏ hàng (${cartList.size})"

            if (cartList.isEmpty()) {
                alertDialog.dismiss()
                btnViewCart.text = "Giỏ hàng (0)"
            } else {
                adapter.clear()
                cartList.forEach { adapter.add(it.key.toString()) }
                adapter.notifyDataSetChanged()
            }
        }
        alertDialog.show()
    }

    // NHẬP THÔNG TIN VÀ GỬI ĐƠN
    private fun showUserInfoDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_dialog_user_info, null)
        val edtTen = view.findViewById<EditText>(R.id.edtTenKhach)
        val edtSdt = view.findViewById<EditText>(R.id.edtSDT)
        val edtGhiChu = view.findViewById<EditText>(R.id.edtGhiChu)
        val spnSoBan = view.findViewById<Spinner>(R.id.spnSoBan)
        val btnGuiDon = view.findViewById<Button>(R.id.btnGuiDon)

        val dsBan = (1..30).map { "Bàn $it" }
        val adapterBan = ArrayAdapter(this, android.R.layout.simple_spinner_item, dsBan)
        adapterBan.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spnSoBan.adapter = adapterBan

        if (currentTableNumber.isNotEmpty()) {
            try { spnSoBan.setSelection(currentTableNumber.toInt() - 1) } catch (e: Exception) {}
        }

        val alertDialog = AlertDialog.Builder(this).setView(view).create()

        // Tính năng Reset hệ thống (Nhấn giữ)
        btnGuiDon.setOnLongClickListener {
            val dbReset = FirebaseDatabase.getInstance(DB_URL).reference
            dbReset.child("Orders").removeValue()
            dbReset.child("Notifications_Pay").removeValue()
            Toast.makeText(this, "♻️ HỆ THỐNG ĐÃ RESET!", Toast.LENGTH_SHORT).show()
            alertDialog.dismiss()
            true
        }

        btnGuiDon.setOnClickListener {
            val soBan = spnSoBan.selectedItem.toString().replace("Bàn ", "")
            currentTableNumber = soBan
            val note = edtGhiChu.text.toString().trim().ifEmpty { "Không có" }

            val orderRef = database.getReference("Orders")
            for (snapshot in cartList) {
                val data = HashMap<String, Any>()
                data["tenKhach"] = edtTen.text.toString().ifEmpty { "Khách" }
                data["sdt"] = edtSdt.text.toString().ifEmpty { "N/A" }
                data["ghiChu"] = note
                data["soBan"] = soBan
                data["tenMon"] = snapshot.key.toString()
                data["gia"] = snapshot.child("gia").value.toString().toLongOrNull() ?: 0L
                data["status"] = "waiting"
                data["isPaid"] = false
                data["timestamp"] = ServerValue.TIMESTAMP
                orderRef.push().setValue(data)
            }

            Toast.makeText(this, "Gửi đơn Bàn $soBan thành công!", Toast.LENGTH_SHORT).show()
            cartList.clear()
            btnViewCart.text = "Giỏ hàng (0)"
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    // KHÔI PHỤC LOGIC THANH TOÁN
    private fun showPaymentDialog() {
        if (currentTableNumber.isEmpty()) {
            Toast.makeText(this, "Vui lòng chọn bàn trước khi thanh toán!", Toast.LENGTH_LONG).show()
            showUserInfoDialog()
            return
        }

        val view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null)
        val tvTotal = view.findViewById<TextView>(R.id.tvTotalPayment)
        val btnTransfer = view.findViewById<Button>(R.id.btnTransfer)
        val btnCash = view.findViewById<Button>(R.id.btnCash)

        val alertDialog = AlertDialog.Builder(this).setView(view).create()

        database.getReference("Orders").orderByChild("soBan").equalTo(currentTableNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var total = 0L
                    for (ds in snapshot.children) {
                        if (ds.child("isPaid").value != true) {
                            total += ds.child("gia").getValue(Long::class.java) ?: 0L
                        }
                    }
                    tvTotal.text = "${String.format("%,d", total)} VNĐ"

                    btnTransfer.setOnClickListener {
                        sendPaymentRequest("Transfer", total)
                        alertDialog.dismiss()
                    }
                    btnCash.setOnClickListener {
                        sendPaymentRequest("Cash", total)
                        alertDialog.dismiss()
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        alertDialog.show()
    }

    private fun sendPaymentRequest(method: String, total: Long) {
        val data = HashMap<String, Any>()
        data["table"] = currentTableNumber
        data["method"] = method
        data["totalPrice"] = total
        data["timestamp"] = ServerValue.TIMESTAMP

        database.getReference("Notifications_Pay").child("ban_$currentTableNumber")
            .setValue(data).addOnSuccessListener {
                Toast.makeText(this, "Đã báo nhân viên Bàn $currentTableNumber thanh toán!", Toast.LENGTH_LONG).show()
            }
    }

    private fun startScanningQR() {
        IntentIntegrator(this).setPrompt("Quét mã bàn").setOrientationLocked(false).initiateScan()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data)
        if (result?.contents != null) {
            currentTableNumber = result.contents.replace("ban_", "")
            showUserInfoDialog()
        } else {
            super.onActivityResult(requestCode, resultCode, data)
        }
    }
}