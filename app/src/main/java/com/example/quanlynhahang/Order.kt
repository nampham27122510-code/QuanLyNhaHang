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

    // Ghi nhớ bàn khách đang ngồi để thanh toán tự động
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

        // 1. RecyclerView (Không hiện Toast khi thêm món)
        rvFood.layoutManager = GridLayoutManager(this, 2)
        foodAdapter = FoodAdapter(foodList) { snapshot ->
            cartList.add(snapshot)
            btnViewCart.text = "Giỏ hàng (${cartList.size})"
        }
        rvFood.adapter = foodAdapter

        // 2. Load Menu từ Firebase
        database.getReference("Menu").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                foodList.clear()
                for (ds in snapshot.children) foodList.add(ds)
                foodAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 3. Drawer & Navigation
        btnOpenMenu.setOnClickListener { drawerLayout.openDrawer(GravityCompat.START) }
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan_qr -> startScanningQR()
                R.id.nav_payment -> showPaymentDialog() // Tự lấy số bàn và tính tiền
                R.id.nav_login -> startActivity(Intent(this, MainActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 4. Mở Dialog nhập liệu khi nhấn Giỏ hàng
        btnViewCart.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn món!", Toast.LENGTH_SHORT).show()
            } else {
                showUserInfoDialog()
            }
        }
    }

    // --- DIALOG NHẬP THÔNG TIN (CHỈ BẮT BUỘC SỐ BÀN) ---
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
            spnSoBan.setSelection(currentTableNumber.toInt() - 1)
        }

        val alertDialog = AlertDialog.Builder(this).setView(view).create()

        btnGuiDon.setOnClickListener {
            val soBan = spnSoBan.selectedItem.toString().replace("Bàn ", "")
            currentTableNumber = soBan // Ghi nhớ bàn để thanh toán

            val ten = edtTen.text.toString().trim().ifEmpty { "Khách tại bàn" }
            val sdt = edtSdt.text.toString().trim().ifEmpty { "N/A" }
            val note = edtGhiChu.text.toString().trim().ifEmpty { "Không có" }

            val orderRef = database.getReference("Orders")
            for (snapshot in cartList) {
                val data = HashMap<String, Any>()
                data["tenKhach"] = ten
                data["sdt"] = sdt
                data["ghiChu"] = note
                data["soBan"] = soBan
                data["tenMon"] = snapshot.key.toString()
                data["gia"] = snapshot.child("gia").value.toString().toLongOrNull() ?: 0L
                data["status"] = "waiting"
                data["timestamp"] = ServerValue.TIMESTAMP
                orderRef.push().setValue(data)
            }

            Toast.makeText(this, "Đã gửi đơn Bàn $soBan thành công!", Toast.LENGTH_LONG).show()
            cartList.clear()
            btnViewCart.text = "Giỏ hàng (0)"
            alertDialog.dismiss()
        }
        alertDialog.show()
    }

    // --- DIALOG THANH TOÁN (KHỚP XML CỦA NAM) ---
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

        // Tính tổng tiền các món của bàn này từ Firebase
        database.getReference("Orders").orderByChild("soBan").equalTo(currentTableNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var total = 0L
                    for (ds in snapshot.children) {
                        // Chỉ tính những món chưa thanh toán (status != paid)
                        if (ds.child("status").value != "paid") {
                            total += ds.child("gia").getValue(Long::class.java) ?: 0L
                        }
                    }
                    tvTotal.text = "${String.format("%,d", total)} VNĐ"
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        val alertDialog = AlertDialog.Builder(this).setView(view).create()

        // Xử lý nút Chuyển khoản
        btnTransfer.setOnClickListener {
            sendPaymentRequest("Chuyển khoản")
            alertDialog.dismiss()
        }

        // Xử lý nút Tiền mặt
        btnCash.setOnClickListener {
            sendPaymentRequest("Tiền mặt")
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun sendPaymentRequest(method: String) {
        val data = HashMap<String, Any>()
        data["table"] = currentTableNumber
        data["method"] = method
        data["timestamp"] = ServerValue.TIMESTAMP

        database.getReference("Notifications_Pay").child("ban_$currentTableNumber")
            .setValue(data).addOnSuccessListener {
                Toast.makeText(this, "Đã báo nhân viên Bàn $currentTableNumber thanh toán ($method)!", Toast.LENGTH_LONG).show()
            }
    }

    // --- QUÉT QR ---
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