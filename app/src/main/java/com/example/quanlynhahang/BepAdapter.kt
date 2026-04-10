package com.example.quanlynhahang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class BepAdapter(
    private var list: List<TableGroup>,
    private val onFinishItem: (DishItem) -> Unit
) : RecyclerView.Adapter<BepAdapter.TableVH>() {

    class TableVH(v: View) : RecyclerView.ViewHolder(v) {
        val tvBan: TextView = v.findViewById(R.id.tvSoBanBep)
        val tvGhiChu: TextView = v.findViewById(R.id.tvGhiChuBan)
        val layoutItems: LinearLayout = v.findViewById(R.id.layoutContainerMonAn)
        val tvTime: TextView = v.findViewById(R.id.tvThoiGianCho)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TableVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_ban_bep, parent, false)
        return TableVH(v)
    }

    override fun onBindViewHolder(holder: TableVH, position: Int) {
        val table = list[position]

        holder.tvBan.text = "BÀN: ${table.soBan}"
        holder.tvGhiChu.text = "Ghi chú: ${table.ghiChu}"

        // Hiển thị thời gian chờ chi tiết (phút và giây)
        val diff = System.currentTimeMillis() - table.timestamp
        val minutes = (diff / 60000).toInt()
        val seconds = ((diff / 1000) % 60).toInt()
        holder.tvTime.text = "⏱ ${minutes}p ${seconds}s"

        holder.layoutItems.removeAllViews()

        table.items.forEach { dish ->
            val itemView = LayoutInflater.from(holder.itemView.context)
                .inflate(R.layout.item_dong_mon_an, holder.layoutItems, false)

            val tvName = itemView.findViewById<TextView>(R.id.tvTenMon)
            val cbDone = itemView.findViewById<CheckBox>(R.id.cbXongMon)

            tvName.text = dish.tenMon

            cbDone.setOnClickListener {
                itemView.visibility = View.GONE
                // Gọi logic trừ kho khi nhấn Checkbox
                onFinishItem(dish)
            }
            holder.layoutItems.addView(itemView)
        }
    }

    override fun getItemCount() = list.size
}