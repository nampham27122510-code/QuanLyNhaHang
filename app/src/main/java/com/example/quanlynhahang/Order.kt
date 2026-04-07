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
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_scan_qr -> startScanningQR()
                R.id.nav_payment -> showPaymentDialog()
                R.id.nav_login -> startActivity(Intent(this, MainActivity::class.java))
            }
            drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        btnViewCart.setOnClickListener {
            if (cartList.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn món!", Toast.LENGTH_SHORT).show()
            } else {
                showUserInfoDialog()
            }
        }
    }

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

        // Reset trạng thái bằng cách nhấn giữ nút Gửi đơn
        btnGuiDon.setOnLongClickListener {
            val databaseReset = FirebaseDatabase.getInstance(DB_URL).reference
            databaseReset.child("Orders").removeValue()
            databaseReset.child("Notifications_Pay").removeValue()
            Toast.makeText(this, "♻️ ĐÃ RESET TOÀN BỘ TRẠNG THÁI BÀN!", Toast.LENGTH_LONG).show()
            alertDialog.dismiss()
            true
        }

        btnGuiDon.setOnClickListener {
            val soBan = spnSoBan.selectedItem.toString().replace("Bàn ", "")
            currentTableNumber = soBan

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

                // CẬP NHẬT QUAN TRỌNG: Gửi kèm isPaid để tránh treo đỏ
                data["isPaid"] = false

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

        database.getReference("Orders").orderByChild("soBan").equalTo(currentTableNumber)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var total = 0L
                    for (ds in snapshot.children) {
                        // Tính tiền những món chưa thanh toán
                        if (ds.child("isPaid").value != true) {
                            total += ds.child("gia").getValue(Long::class.java) ?: 0L
                        }
                    }
                    tvTotal.text = "${String.format("%,d", total)} VNĐ"

                    btnTransfer.setOnClickListener {
                        sendPaymentRequest("Transfer", total)
                        dismissDialog(view)
                    }
                    btnCash.setOnClickListener {
                        sendPaymentRequest("Cash", total)
                        dismissDialog(view)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })

        AlertDialog.Builder(this).setView(view).show()
    }

    private fun dismissDialog(view: android.view.View) {
        val parent = view.parent
        // Code xử lý đóng dialog
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