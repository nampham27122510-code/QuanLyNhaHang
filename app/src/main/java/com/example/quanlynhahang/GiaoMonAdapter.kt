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
        val tenMon = v.findViewById<TextView>(R.id.tvTenMonGiao)
        val soBan = v.findViewById<TextView>(R.id.tvSoBanGiao)
        val btnGiao = v.findViewById<Button>(R.id.btnXacNhanGiao)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GiaoViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_giao_mon, parent, false)
        return GiaoViewHolder(v)
    }

    override fun onBindViewHolder(holder: GiaoViewHolder, position: Int) {
        val data = list[position]
        holder.tenMon.text = data.child("tenMon").value.toString()
        holder.soBan.text = "Bàn: ${data.child("soBan").value.toString()}"

        holder.btnGiao.setOnClickListener {
            onConfirm(data) // Gọi hàm xử lý ngoài Activity nhanvien
        }
    }

    override fun getItemCount(): Int = list.size
}