package com.example.quanlynhahang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.DataSnapshot

class GiaoMonAdapter(
    private val list: List<DataSnapshot>,
    private val onConfirm: (DataSnapshot) -> Unit
) : RecyclerView.Adapter<GiaoMonAdapter.GiaoViewHolder>() {

    class GiaoViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tenMon: TextView = v.findViewById(R.id.tvTenMonGiao)
        val soBan: TextView = v.findViewById(R.id.tvSoBanGiao)
        val btnGiao: Button = v.findViewById(R.id.btnXacNhanGiao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiaoViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_giao_mon, parent, false)
        return GiaoViewHolder(v)
    }

    override fun onBindViewHolder(holder: GiaoViewHolder, position: Int) {
        val data = list[position]

        // Lấy dữ liệu an toàn để tránh null
        val ten = data.child("tenMon").value?.toString() ?: "Món không tên"
        val ban = data.child("soBan").value?.toString() ?: "0"

        holder.tenMon.text = ten
        holder.soBan.text = "Bàn: $ban"

        holder.btnGiao.setOnClickListener {
            onConfirm(data) // Gửi snapshot về Activity nhanvien để xử lý thanh toán
        }
    }

    override fun getItemCount(): Int = list.size
}