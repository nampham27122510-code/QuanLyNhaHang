package com.example.quanlynhahang

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class Admin : AppCompatActivity() {

    private val DB_URL = "https://hethongnhahang-91d27-default-rtdb.asia-southeast1.firebasedatabase.app"
    private lateinit var database: FirebaseDatabase
    private lateinit var tvTodayRevenue: TextView

    private lateinit var barChartTuan: BarChart
    private lateinit var pieChartMonAn: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)
        database = FirebaseDatabase.getInstance(DB_URL)

        tvTodayRevenue = findViewById(R.id.tvTotalRevenueAdmin)
        barChartTuan = findViewById(R.id.barChartTuan)
        pieChartMonAn = findViewById(R.id.pieChartMonAn)

        listenToTodayRevenue()
        setupCharts()

        findViewById<CardView>(R.id.cardRevenueThang).setOnClickListener {
            hienThiDoanhThuThang()
        }

        findViewById<CardView>(R.id.cardQuanLyKho).setOnClickListener {
            startActivity(Intent(this, Kho::class.java))
        }

        findViewById<CardView>(R.id.cardQuanLyMenu).setOnClickListener {
            startActivity(Intent(this, QuanLyMenu::class.java))
        }

        // SỬA: Khi ấn Báo cáo, mở Sơ đồ 100 bàn để xem bàn trống/có khách
        findViewById<CardView>(R.id.cardBaoCao).setOnClickListener {
            startActivity(Intent(this, SoDoBan::class.java))
        }
    }

    // --- Giữ nguyên các hàm hienThiDoanhThuThang, setupCharts, veBarChartDoanhThuTuan, vePieChartTiLeMonAn, listenToTodayRevenue ---

    private fun hienThiDoanhThuThang() {
        database.getReference("Revenue").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    Toast.makeText(this@Admin, "Chưa có dữ liệu doanh thu!", Toast.LENGTH_SHORT).show()
                    return
                }
                val mapThang = mutableMapOf<String, Long>()
                for (ngayDs in snapshot.children) {
                    val ngay = ngayDs.key ?: continue
                    val tienNgay = ngayDs.child("total").getValue(Long::class.java) ?: 0L
                    if (ngay.length >= 7) {
                        val keyThang = ngay.substring(0, 7)
                        val hienTai = mapThang[keyThang] ?: 0L
                        mapThang[keyThang] = hienTai + tienNgay
                    }
                }
                val sortedMap = mapThang.toSortedMap(compareByDescending { it })
                val hienThi = StringBuilder()
                sortedMap.forEach { (thang, tong) ->
                    hienThi.append("📅 Tháng $thang: \n💰 ${String.format("%,d", tong)} VNĐ\n")
                    hienThi.append("--------------------------------\n")
                }
                AlertDialog.Builder(this@Admin)
                    .setTitle("📊 DOANH THU THEO THÁNG")
                    .setMessage(hienThi.toString())
                    .setPositiveButton("ĐÓNG", null)
                    .show()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupCharts() {
        veBarChartDoanhThuTuan()
        vePieChartTiLeMonAn()
    }

    private fun veBarChartDoanhThuTuan() {
        val entries = mutableListOf<BarEntry>()
        val labels = mutableListOf<String>()
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val displaySdf = SimpleDateFormat("dd/MM", Locale.getDefault())

        for (i in 0..6) {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -i)
            val dateStr = sdf.format(calendar.time)
            val labelStr = displaySdf.format(calendar.time)
            labels.add(0, labelStr)

            database.getReference("Revenue").child(dateStr).child("total")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val value = snapshot.getValue(Long::class.java) ?: 0L
                        entries.add(BarEntry((6 - i).toFloat(), value.toFloat()))
                        if (entries.size == 7) {
                            entries.sortBy { it.x }
                            val dataSet = BarDataSet(entries, "Doanh thu (VNĐ)")
                            dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
                            dataSet.valueTextSize = 10f
                            val barData = BarData(dataSet)
                            barChartTuan.data = barData
                            barChartTuan.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                            barChartTuan.xAxis.setDrawGridLines(false)
                            barChartTuan.description.isEnabled = false
                            barChartTuan.animateY(1000)
                            barChartTuan.invalidate()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun vePieChartTiLeMonAn() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        database.getReference("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val counts = mutableMapOf<String, Int>()
                for (ds in snapshot.children) {
                    val status = ds.child("status").value?.toString() ?: ""
                    if (status == "delivered" || status == "paid") {
                        val tenMon = ds.child("tenMon").value?.toString() ?: "Khác"
                        counts[tenMon] = counts.getOrDefault(tenMon, 0) + 1
                    }
                }
                if (counts.isEmpty()) return
                val entries = counts.map { PieEntry(it.value.toFloat(), it.key) }
                val dataSet = PieDataSet(entries, "")
                dataSet.colors = ColorTemplate.PASTEL_COLORS.toList()
                dataSet.sliceSpace = 3f
                dataSet.valueTextSize = 12f
                val pieData = PieData(dataSet)
                pieChartMonAn.data = pieData
                pieChartMonAn.centerText = "Món ăn\nhôm nay"
                pieChartMonAn.setCenterTextSize(16f)
                pieChartMonAn.description.isEnabled = false
                pieChartMonAn.animateXY(1000, 1000)
                pieChartMonAn.invalidate()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun listenToTodayRevenue() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        database.getReference("Revenue").child(today).child("total")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val total = snapshot.getValue(Long::class.java) ?: 0L
                    tvTodayRevenue.text = "💰 Hôm nay: ${String.format("%,d", total)} VNĐ"
                    veBarChartDoanhThuTuan()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }
}