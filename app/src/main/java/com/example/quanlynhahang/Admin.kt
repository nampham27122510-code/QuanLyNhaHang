package com.example.quanlynhahang

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Admin : AppCompatActivity() {

    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var barChartTuan: BarChart
    private lateinit var pieChartMon: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // 1. Ánh xạ các nút - Đảm bảo ID khớp với activity_admin.xml
        val cardRevenue = findViewById<CardView>(R.id.cardRevenueThang)
        val cardKho = findViewById<CardView>(R.id.cardQuanLyKho)
        val cardMenu = findViewById<CardView>(R.id.cardQuanLyMenu)
        val cardBaoCao = findViewById<CardView>(R.id.cardBaoCao)

        // 2. Ánh xạ biểu đồ (Sửa lại ID không dấu nếu cần)
        barChartTuan = findViewById(R.id.barChartTuần)
        pieChartMon = findViewById(R.id.pieChartMonAn)

        val database = FirebaseDatabase.getInstance(DB_URL)

        cardRevenue.setOnClickListener {
            hienThiDialog("Thông báo", "Dữ liệu doanh thu đang được cập nhật tự động.")
        }

        cardKho.setOnClickListener {
            // Đảm bảo bạn đã tạo file Kho.kt
            startActivity(Intent(this, Kho::class.java))
        }

        cardMenu.setOnClickListener {
            // Đảm bảo bạn đã tạo file QuanLyMenu.kt (hoặc QuanLyMenuActivity)
            startActivity(Intent(this, QuanLyMenu::class.java))
        }

        cardBaoCao.setOnClickListener {
            // Đảm bảo bạn đã tạo file BaoCao.kt
            startActivity(Intent(this, BaoCao::class.java))
        }

        loadBarChartTuan(database.getReference("Revenue"))
        loadPieChartMonAn(database.getReference("Orders"))
    }

    private fun loadBarChartTuan(dbRef: DatabaseReference) {
        val thangNay = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        dbRef.addValueEventListener(object : ValueEventListener { // Dùng addValueEventListener để cập nhật Realtime
            override fun onDataChange(snapshot: DataSnapshot) {
                val doanhThuTuan = floatArrayOf(0f, 0f, 0f, 0f)

                for (ngaySnap in snapshot.children) {
                    val ngayKey = ngaySnap.key ?: ""
                    if (ngayKey.startsWith(thangNay)) {
                        // Giả định Key là "2026-03-18" -> lấy "18"
                        val ngayStr = ngayKey.split("-").last().toIntOrNull() ?: 0
                        val tien = ngaySnap.child("total").value.toString().toFloatOrNull() ?: 0f

                        when (ngayStr) {
                            in 1..7 -> doanhThuTuan[0] += tien
                            in 8..14 -> doanhThuTuan[1] += tien
                            in 15..21 -> doanhThuTuan[2] += tien
                            else -> doanhThuTuan[3] += tien
                        }
                    }
                }

                val entries = ArrayList<BarEntry>()
                for (i in 0..3) {
                    entries.add(BarEntry((i + 1).toFloat(), doanhThuTuan[i]))
                }

                val dataSet = BarDataSet(entries, "Doanh thu tuần (VNĐ)")
                dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()

                barChartTuan.data = BarData(dataSet)
                barChartTuan.description.isEnabled = false
                barChartTuan.animateY(1000)
                barChartTuan.invalidate()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadPieChartMonAn(orderRef: DatabaseReference) {
        orderRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mapMonAn = HashMap<String, Int>()

                for (ds in snapshot.children) {
                    val status = ds.child("status").value.toString()
                    // Lọc những món đã phục vụ hoặc thanh toán
                    if (status == "cooked" || status == "paid") {
                        val tenMon = ds.child("tenMon").value.toString()
                        mapMonAn[tenMon] = (mapMonAn[tenMon] ?: 0) + 1
                    }
                }

                val pieEntries = ArrayList<PieEntry>()
                for ((mon, count) in mapMonAn) {
                    pieEntries.add(PieEntry(count.toFloat(), mon))
                }

                val dataSet = PieDataSet(pieEntries, "")
                dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
                dataSet.valueTextSize = 12f

                pieChartMon.data = PieData(dataSet)
                pieChartMon.centerText = "Món bán chạy"
                pieChartMon.animateXY(1000, 1000)
                pieChartMon.invalidate()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun hienThiDialog(tieuDe: String, noiDung: String) {
        AlertDialog.Builder(this)
            .setTitle(tieuDe)
            .setMessage(noiDung)
            .setPositiveButton("Đã hiểu", null)
            .show()
    }
}