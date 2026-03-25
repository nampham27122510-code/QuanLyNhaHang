package com.example.quanlynhahang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class ThanhToanAdapter(
    private val list: List<DataSnapshot>,
    private val onConfirm: (String, Long, String) -> Unit
) : RecyclerView.Adapter<ThanhToanAdapter.ThanhToanViewHolder>() {

    class ThanhToanViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val tvInfo: TextView = v.findViewById(R.id.tvInfoThanhToan)
        val btnXacNhan: Button = v.findViewById(R.id.btnXacNhanThanhToan)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThanhToanViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_thanh_toan, parent, false)
        return ThanhToanViewHolder(v)
    }

    override fun onBindViewHolder(holder: ThanhToanViewHolder, position: Int) {
        val data = list[position]
        val notiKey = data.key ?: ""
        // Lấy số bàn từ Notifications (trường 'table')
        val tableFromNoti = data.child("table").value?.toString()?.trim() ?: ""

        holder.tvInfo.text = "💰 BÀN $tableFromNoti: Đang tính..."

        if (tableFromNoti.isNotEmpty()) {
            val orderRef = FirebaseDatabase.getInstance().getReference("Orders")
            orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalAmount = 0L

                    for (ds in snapshot.children) {
                        // KHỚP CHÍNH XÁC VỚI ẢNH DB: trường 'soban' viết thường
                        val banTrongDB = ds.child("soban").value?.toString()?.trim() ?: ""

                        if (banTrongDB == tableFromNoti) {
                            val status = ds.child("status").value?.toString() ?: ""
                            if (status != "paid") {
                                val gia = ds.child("gia").value?.toString()?.toLongOrNull() ?: 0L
                                totalAmount += gia
                            }
                        }
                    }

                    // Hiển thị tiền và kích hoạt nút bấm ngay lập tức
                    holder.tvInfo.text = "💰 BÀN $tableFromNoti: $totalAmount VNĐ"

                    holder.btnXacNhan.setOnClickListener {
                        onConfirm(tableFromNoti, totalAmount, notiKey)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
        }
    }

    override fun getItemCount(): Int = list.size
}