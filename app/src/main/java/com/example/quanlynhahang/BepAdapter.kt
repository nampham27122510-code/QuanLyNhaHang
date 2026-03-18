package com.example.quanlynhahang

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView

class BepAdapter(private val list: MutableList<GroupedItem>) : RecyclerView.Adapter<BepAdapter.BepViewHolder>() {

    class BepViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val card: CardView = v.findViewById(R.id.cardMonAn)
        val tvNoiDung: TextView = v.findViewById(R.id.tvNoiDungMon)
        val tvThoiGian: TextView = v.findViewById(R.id.tvThoiGian)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BepViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bep, parent, false)
        return BepViewHolder(v)
    }

    override fun onBindViewHolder(holder: BepViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("UPDATE_TIME") && position < list.size) {
            val diffSec = (System.currentTimeMillis() - list[position].timestamp) / 1000
            holder.tvThoiGian.text = String.format("%02d:%02d", diffSec / 60, diffSec % 60)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun onBindViewHolder(holder: BepViewHolder, position: Int) {
        val item = list[position]

        // Reset View cực sạch để không bị lỗi bóng ma
        holder.itemView.apply {
            alpha = 1.0f
            scaleY = 1.0f
            isEnabled = true
        }
        holder.card.setCardBackgroundColor(Color.WHITE)
        holder.tvNoiDung.text = "[BÀN ${item.soBan}] ${item.tenMon} x${item.soLuong}"

        holder.itemView.setOnClickListener {
            // Lấy vị trí mới nhất ngay khi click
            val currentPos = holder.adapterPosition
            if (currentPos == RecyclerView.NO_POSITION) return@setOnClickListener

            // Khóa thẻ ngay để chống ấn nhanh
            holder.itemView.isEnabled = false
            holder.card.setCardBackgroundColor(Color.parseColor("#C8E6C9"))

            // 1. Animation tan biến nhanh (250ms)
            holder.itemView.animate()
                .alpha(0f)
                .scaleY(0f)
                .setDuration(250)
                .withEndAction {
                    // 2. Tìm lại index thật của item (tránh lỗi index khi ấn nhanh nhiều thẻ)
                    val latestIndex = list.indexOf(item)
                    if (latestIndex != -1) {
                        // Xóa cục bộ trên điện thoại trước
                        list.removeAt(latestIndex)
                        // Ép RecyclerView trôi các thẻ dưới lên
                        notifyItemRemoved(latestIndex)

                        // 3. Cập nhật Firebase ngầm sau
                        item.snapshots.forEach {
                            it.ref.child("status").setValue("cooked")
                        }
                    }
                }
                .start()
        }
    }

    override fun getItemCount(): Int = list.size
}