package com.example.quanlynhahang

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.PercentFormatter
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

        findViewById<CardView>(R.id.cardBaoCao).setOnClickListener {
            val intent = Intent(this, ThucTrangNhaHang::class.java)
            startActivity(intent)
        }
    }

    private fun listenToTodayRevenue() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        database.getReference("Revenue").child(today).child("total")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val total = snapshot.getValue(Long::class.java) ?: 0L
                    tvTodayRevenue.text = "💰 ${String.format("%,d", total)} VNĐ"
                    veBarChartDoanhThuTuan()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupCharts() {
        veBarChartDoanhThuTuan()
        vePieChartTiLeMonAn()
    }

    private fun vePieChartTiLeMonAn() {
        val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())

        // CHỈNH SỬA TẠI ĐÂY: Đọc từ OrderHistory thay vì Orders
        // Điều này giúp dữ liệu biểu đồ không bị mất sau khi thanh toán bàn
        database.getReference("OrderHistory").child(today).addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val counts = mutableMapOf<String, Int>()

                for (ds in snapshot.children) {
                    // Trong lịch sử, chúng ta thống kê tất cả các món đã từng được thanh toán
                    val tenMon = ds.child("tenMon").value?.toString() ?: "Khác"
                    counts[tenMon] = counts.getOrDefault(tenMon, 0) + 1
                }

                if (counts.isEmpty()) {
                    pieChartMonAn.clear()
                    pieChartMonAn.setNoDataText("Chưa có món nào hoàn tất thanh toán hôm nay.")
                    return
                }

                val entries = counts.map { PieEntry(it.value.toFloat(), it.key) }
                val dataSet = PieDataSet(entries, "")
                dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
                dataSet.sliceSpace = 3f
                dataSet.valueTextSize = 13f
                dataSet.valueTextColor = Color.WHITE

                val pieData = PieData(dataSet)
                pieData.setValueFormatter(PercentFormatter(pieChartMonAn))
                pieChartMonAn.apply {
                    data = pieData
                    setUsePercentValues(true)
                    centerText = "Tỉ lệ món ăn\nđã bán hôm nay"
                    setCenterTextSize(15f)
                    description.isEnabled = false
                    animateXY(800, 800)
                    invalidate()
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
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
            labels.add(0, displaySdf.format(calendar.time))

            database.getReference("Revenue").child(dateStr).child("total")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val value = snapshot.getValue(Long::class.java) ?: 0L
                        entries.add(BarEntry((6 - i).toFloat(), value.toFloat()))

                        if (entries.size == 7) {
                            entries.sortBy { it.x }
                            val dataSet = BarDataSet(entries, "Doanh thu (VNĐ)")
                            dataSet.color = Color.parseColor("#4CAF50")
                            barChartTuan.apply {
                                data = BarData(dataSet)
                                xAxis.valueFormatter = IndexAxisValueFormatter(labels)
                                xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
                                axisLeft.axisMinimum = 0f
                                animateY(1000)
                                invalidate()
                            }
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }
    }

    private fun hienThiDoanhThuThang() {
        database.getReference("Revenue").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mapThang = mutableMapOf<String, Long>()
                for (ngayDs in snapshot.children) {
                    val ngay = ngayDs.key ?: continue
                    val tienNgay = ngayDs.child("total").getValue(Long::class.java) ?: 0L
                    if (ngay.length >= 7) {
                        val keyThang = ngay.substring(0, 7)
                        mapThang[keyThang] = (mapThang[keyThang] ?: 0L) + tienNgay
                    }
                }
                val sortedMap = mapThang.toSortedMap(compareByDescending { it })
                val hienThi = StringBuilder()
                sortedMap.forEach { (thang, tong) ->
                    hienThi.append("📅 Tháng $thang: \n💰 ${String.format("%,d", tong)} VNĐ\n")
                    hienThi.append("--------------------------------\n")
                }
                AlertDialog.Builder(this@Admin).setTitle("📊 DOANH THU THEO THÁNG")
                    .setMessage(hienThi.toString()).setPositiveButton("ĐÓNG", null).show()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }
}