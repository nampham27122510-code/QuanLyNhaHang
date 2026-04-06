package com.example.quanlynhahang.com.example.quanlynhahang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlynhahang.R
import com.google.firebase.database.DataSnapshot

class ThanhToanAdapter(
    private val list: MutableList<DataSnapshot>,
    private val onConfirm: (table: String, total: Long, key: String) -> Unit
) : RecyclerView.Adapter<ThanhToanAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvThongTin: TextView = v.findViewById(R.id.tvTenMonGiao)
        val tvSoBan: TextView = v.findViewById(R.id.tvSoBanGiao)
        val btnXacNhan: Button = v.findViewById(R.id.btnXacNhanGiao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_giao_mon_adapter, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val snapshot = list[position]

        // 1. Lấy số bàn
        val soBan = snapshot.child("table").value?.toString() ?: "N/A"

        // 2. LOGIC FIX: Kiểm tra cả hai trường 'totalPrice' và 'total'
        val tongTien = if (snapshot.hasChild("totalPrice")) {
            snapshot.child("totalPrice").getValue(Long::class.java) ?: 0L
        } else {
            snapshot.child("total").getValue(Long::class.java) ?: 0L
        }

        // 3. Hiển thị lên giao diện
        holder.tvThongTin.text = "💰 Tổng: ${String.format("%,d", tongTien)} VNĐ"
        holder.tvSoBan.text = "Bàn số: $soBan"

        // Đổi chữ nút thành "XÁC NHẬN" cho đúng chức năng thanh toán
        holder.btnXacNhan.text = "XÁC NHẬN"

        holder.btnXacNhan.setOnClickListener {
            onConfirm(soBan, tongTien, snapshot.key ?: "")
        }
    }

    override fun getItemCount(): Int = list.size
}