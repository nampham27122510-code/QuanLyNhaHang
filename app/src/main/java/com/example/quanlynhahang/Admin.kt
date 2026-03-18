package com.example.quanlynhahang

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
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

    // Khai báo 2 loại biểu đồ
    private lateinit var barChartTuan: BarChart
    private lateinit var pieChartMon: PieChart

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        // 1. Ánh xạ các nút chức năng CardView
        val cardRevenue = findViewById<CardView>(R.id.cardRevenueThang)
        val cardKho = findViewById<CardView>(R.id.cardQuanLyKho)
        val cardMenu = findViewById<CardView>(R.id.cardQuanLyMenu)
        val cardBaoCao = findViewById<CardView>(R.id.cardBaoCao)

        // 2. Ánh xạ biểu đồ
        barChartTuan = findViewById(R.id.barChartTuần)
        pieChartMon = findViewById(R.id.pieChartMonAn)

        val database = FirebaseDatabase.getInstance(DB_URL)

        // --- SỰ KIỆN CLICK NÚT ---
        cardRevenue.setOnClickListener {
            hienThiDialog("Thông báo", "Biểu đồ chi tiết đang được hiển thị bên dưới.")
        }

        cardKho.setOnClickListener {
            startActivity(Intent(this, Kho::class.java))
        }

        cardMenu.setOnClickListener {
            startActivity(Intent(this, QuanLyMenu::class.java))
        }

        cardBaoCao.setOnClickListener {
            startActivity(Intent(this, BaoCao::class.java))
        }

        // 3. Tự động tải dữ liệu lên biểu đồ khi mở màn hình
        loadBarChartTuan(database.getReference("Revenue"))
        loadPieChartMonAn(database.getReference("Orders"))
    }

    // BIỂU ĐỒ CỘT: Thống kê doanh thu theo 4 tuần trong tháng
    private fun loadBarChartTuan(dbRef: DatabaseReference) {
        val thangNay = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val doanhThuTuan = floatArrayOf(0f, 0f, 0f, 0f) // Tuần 1, 2, 3, 4

                for (ngaySnap in snapshot.children) {
                    if (ngaySnap.key?.startsWith(thangNay) == true) {
                        val ngayStr = ngaySnap.key!!.split("-").last().toInt()
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
                entries.add(BarEntry(1f, doanhThuTuan[0]))
                entries.add(BarEntry(2f, doanhThuTuan[1]))
                entries.add(BarEntry(3f, doanhThuTuan[2]))
                entries.add(BarEntry(4f, doanhThuTuan[3]))

                val dataSet = BarDataSet(entries, "Doanh thu tuần (VNĐ)")
                dataSet.colors = ColorTemplate.MATERIAL_COLORS.toList()
                dataSet.valueTextSize = 10f

                barChartTuan.data = BarData(dataSet)
                barChartTuan.description.text = "Tháng này"
                barChartTuan.animateY(1000)
                barChartTuan.invalidate()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // BIỂU ĐỒ TRÒN: Tỉ lệ các món được order hôm nay
    private fun loadPieChartMonAn(orderRef: DatabaseReference) {
        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val mapMonAn = HashMap<String, Int>()

                for (ds in snapshot.children) {
                    val status = ds.child("status").value.toString()
                    // Chỉ đếm những món đã thanh toán hoặc đã bưng ra (không tính món bị hủy)
                    if (status != "waiting") {
                        val tenMon = ds.child("tenMon").value.toString()
                        mapMonAn[tenMon] = mapMonAn.getOrDefault(tenMon, 0) + 1
                    }
                }

                val pieEntries = ArrayList<PieEntry>()
                for ((mon, count) in mapMonAn) {
                    pieEntries.add(PieEntry(count.toFloat(), mon))
                }

                val dataSet = PieDataSet(pieEntries, "")
                dataSet.colors = ColorTemplate.COLORFUL_COLORS.toList()
                dataSet.valueTextSize = 12f
                dataSet.sliceSpace = 3f

                val pieData = PieData(dataSet)
                pieChartMon.data = pieData
                pieChartMon.centerText = "Tỉ lệ món ăn"
                pieChartMon.setCenterTextSize(16f)
                pieChartMon.animateXY(1000, 1000)
                pieChartMon.invalidate()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun hienThiDialog(tieuDe: String, noiDung: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(tieuDe)
        builder.setMessage(noiDung)
        builder.setPositiveButton("Đã hiểu", null)
        builder.show()
    }
}