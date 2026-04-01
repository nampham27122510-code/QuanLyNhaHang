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
        val tvTime: TextView = v.findViewById(R.id.tvThoiGianCho)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bep, parent, false)
        return ViewHolder(v)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvBan.text = "BÀN: ${item.soBan}"
        holder.tvSl.text = "x${item.soLuong}"
        holder.tvMon.text = item.tenMon

        updateTimeDisplay(holder.tvTime, item.timestamp)

        // Nếu khách đã thanh toán, đổi màu thẻ sang màu vàng nhạt
        if (item.isPaid) {
            holder.card.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
            holder.tvMon.text = "${item.tenMon}\n(ĐÃ THANH TOÁN)"
        } else {
            holder.card.setCardBackgroundColor(Color.WHITE)
        }

        holder.itemView.setOnClickListener { onDoneClick(item) }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("UPDATE_TIME")) {
            updateTimeDisplay(holder.tvTime, list[position].timestamp)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    private fun updateTimeDisplay(tv: TextView, timestamp: Long) {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = (diff / (1000 * 60)).toInt()
        val seconds = ((diff / 1000) % 60).toInt()
        tv.text = "⏱ ${minutes}p ${seconds}s"
        if (minutes >= 10) tv.setTextColor(Color.RED) else tv.setTextColor(Color.BLACK)
    }

    override fun getItemCount() = list.size
}