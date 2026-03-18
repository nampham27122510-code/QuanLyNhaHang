package com.example.quanlynhahang

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BepAdapter(
    private val list: List<GroupedItem>,
    private val onDoneClick: (GroupedItem) -> Unit
) : RecyclerView.Adapter<BepAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBan: TextView = view.findViewById(R.id.tvSoBanBep)
        val tvMon: TextView = view.findViewById(R.id.tvTenMonBep)
        val tvSl: TextView = view.findViewById(R.id.tvSoLuongBep)
        val tvTime: TextView = view.findViewById(R.id.tvThoiGianCho)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_bep, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvBan.text = "BÀN: ${item.soBan}"
        holder.tvMon.text = item.tenMon
        holder.tvSl.text = "x${item.soLuong}"
        updateTime(holder, item.timestamp)
        holder.itemView.setOnClickListener { onDoneClick(item) }
    }

    // ĐẾM GIÂY KHÔNG LAG: Sử dụng Payload
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("UPDATE_TIME")) {
            updateTime(holder, list[position].timestamp)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun updateTime(holder: ViewHolder, ts: Long) {
        val totalSec = (System.currentTimeMillis() - ts) / 1000
        val m = totalSec / 60
        val s = totalSec % 60
        holder.tvTime.text = String.format("%02d:%02d", m, s)
        if (m >= 10) holder.tvTime.setTextColor(Color.RED)
        else holder.tvTime.setTextColor(Color.parseColor("#4CAF50"))
    }

    override fun getItemCount() = list.size
}