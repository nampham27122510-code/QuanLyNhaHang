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
        // SỬA: Thêm ánh xạ cho TextView hiển thị thời gian
        val tvTime: TextView = v.findViewById(R.id.tvThoiGianCho)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_bep, parent, false)
        return ViewHolder(v)
    }

    // 1. Hàm nạp dữ liệu đầy đủ (gọi khi lần đầu hiển thị hoặc notifyDataSetChanged)
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        holder.tvBan.text = "BÀN: ${item.soBan}"
        holder.tvSl.text = "x${item.soLuong}"

        // Cập nhật thời gian ngay lập tức
        updateTimeDisplay(holder.tvTime, item.timestamp)

        if (item.isPaid) {
            holder.tvMon.text = "${item.tenMon}\n(ĐÃ THANH TOÁN)"
            holder.card.setCardBackgroundColor(Color.parseColor("#FFF9C4"))
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

    // 2. SỬA: Hàm nạp dữ liệu từng phần (gọi bởi notifyItemRangeChanged với payload "UPDATE_TIME")
    override fun onBindViewHolder(holder: ViewHolder, position: Int, payloads: MutableList<Any>) {
        if (payloads.contains("UPDATE_TIME")) {
            // Chỉ cập nhật duy nhất dòng thời gian, giữ nguyên các thông tin khác
            updateTimeDisplay(holder.tvTime, list[position].timestamp)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    // 3. THÊM: Logic tính toán thời gian chờ thực tế
    private fun updateTimeDisplay(tv: TextView, timestamp: Long) {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        // Chuyển đổi mili giây sang Phút và Giây
        val minutes = (diff / (1000 * 60)).toInt()
        val seconds = ((diff / 1000) % 60).toInt()

        // Hiển thị dạng "5p 30s"
        tv.text = "⏱ ${minutes}p ${seconds}s"

        // Cảnh báo đỏ nếu khách đợi quá lâu (ví dụ trên 15 phút)
        if (minutes >= 15) {
            tv.setTextColor(Color.RED)
        } else {
            tv.setTextColor(Color.parseColor("#F44336")) // Màu đỏ mặc định
        }
    }

    override fun getItemCount() = list.size
}