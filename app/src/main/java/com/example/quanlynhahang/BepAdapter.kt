package com.example.quanlynhahang

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class BepAdapter(
    private val list: MutableList<GroupedItem>,
    private val onDoneClick: (GroupedItem) -> Unit
) : RecyclerView.Adapter<BepAdapter.ViewHolder>() {

    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val card: CardView = v.findViewById(R.id.cardMonAn)
        val tvBan: TextView = v.findViewById(R.id.tvSoBanBep)
        val tvMon: TextView = v.findViewById(R.id.tvTenMonBep)
        val tvSl: TextView = v.findViewById(R.id.tvSoLuongBep)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bep, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvBan.text = "BÀN: ${item.soBan}"
        holder.tvSl.text = "x${item.soLuong}"

        // HIỂN THỊ MÀU THẺ DỰA TRÊN THANH TOÁN
        if (item.isPaid) {
            holder.tvMon.text = "${item.tenMon}\n(ĐÃ THANH TOÁN)"
            holder.card.setCardBackgroundColor(Color.parseColor("#FFF9C4")) // Màu vàng nhạt
            holder.tvMon.setTextColor(Color.parseColor("#F57F17"))
        } else {
            holder.tvMon.text = item.tenMon
            holder.card.setCardBackgroundColor(Color.WHITE)
            holder.tvMon.setTextColor(Color.BLACK)
        }

        holder.itemView.setOnClickListener {
            onDoneClick(item)
        }
    }

    override fun getItemCount() = list.size
}