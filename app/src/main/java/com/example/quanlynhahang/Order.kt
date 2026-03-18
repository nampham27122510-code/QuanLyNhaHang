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

    private var tableNumber = ""
    private var lastTotalAmount = 0L
    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"

    private var doubleBackToExitPressedOnce = false

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
                // Khi nhấn Login/Nội bộ: Chuyển sang MainActivity
                R.id.nav_login -> {
                    startActivity(Intent(this, MainActivity::class.java))
                }
            }
            drawerLayout.closeDrawers()
            true
        }

        btnViewCart.setOnClickListener {
            if (cartList.isEmpty()) Toast.makeText(this, "Giỏ trống!", Toast.LENGTH_SHORT).show()
            else if (tableNumber.isEmpty()) showUserInfoDialog()
            else confirmAndOrder()
        }

        // LOGIC: Nhấn Back 2 lần thoát App (Vì là màn hình LAUNCHER)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (doubleBackToExitPressedOnce) {
                    finishAffinity()
                    return
                }
                doubleBackToExitPressedOnce = true
                Toast.makeText(this@Order, "Nhấn lần nữa để thoát ứng dụng", Toast.LENGTH_SHORT).show()
                Handler(Looper.getMainLooper()).postDelayed({ doubleBackToExitPressedOnce = false }, 2000)
            }
        })
    }

    private fun showUserInfoDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.activity_dialog_user_info, null)
        val edtTable = view.findViewById<EditText>(R.id.edtDialogTable)
        AlertDialog.Builder(this).setView(view).setCancelable(false)
            .setPositiveButton("ĐẶT MÓN") { _, _ ->
                tableNumber = edtTable.text.toString().trim()
                if (tableNumber.isNotEmpty()) confirmAndOrder()
                else showUserInfoDialog()
            }.setNegativeButton("Hủy", null).show()
    }

    private fun confirmAndOrder() {
        val orderRef = FirebaseDatabase.getInstance(DB_URL).getReference("Orders")
        for (item in cartList) {
            val gia = item.child("gia").value.toString().replace(Regex("[^0-9]"), "").toLongOrNull() ?: 0L
            val data = HashMap<String, Any?>()
            data["soBan"] = tableNumber
            data["tenMon"] = item.key
            data["gia"] = gia
            data["status"] = "waiting"
            data["timestamp"] = System.currentTimeMillis()
            orderRef.push().setValue(data)
        }
        cartList.clear()
        btnViewCart.text = "Giỏ hàng (0)"
        Toast.makeText(this, "Đã gửi đơn bàn $tableNumber!", Toast.LENGTH_SHORT).show()
    }

    private fun showPaymentDialog() {
        val view = LayoutInflater.from(this).inflate(R.layout.dialog_payment, null)
        val dialog = AlertDialog.Builder(this).setView(view).create()
        view.findViewById<Button>(R.id.btnCash).setOnClickListener {
            sendNoti("Yêu cầu tiền mặt bàn $tableNumber")
            dialog.dismiss()
        }
        view.findViewById<Button>(R.id.btnTransfer).setOnClickListener {
            sendNoti("Bàn $tableNumber đã chuyển khoản")
            dialog.dismiss()
        }
        dialog.show()
    }

    private fun sendNoti(msg: String) {
        val ref = FirebaseDatabase.getInstance(DB_URL).getReference("Notifications")
        val data = HashMap<String, Any>()
        data["message"] = msg
        data["table"] = tableNumber
        ref.setValue(data)
    }
}