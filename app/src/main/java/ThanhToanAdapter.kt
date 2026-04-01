package com.example.quanlynhahang

import android.graphics.Color
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
        val tableId = data.child("table").value?.toString()?.trim() ?: ""
        val method = data.child("method").value?.toString() ?: "Normal"

        // HIỆN SỐ BÀN NGAY LẬP TỨC
        holder.tvInfo.text = "💰 BÀN $tableId: Đang quét hóa đơn..."
        setupButtonUI(holder.btnXacNhan, method)

        val orderRef = FirebaseDatabase.getInstance().getReference("Orders")
        orderRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalAmount = 0L
                var hasItem = false
                for (ds in snapshot.children) {
                    val banDB = ds.child("soBan").value?.toString() ?: ds.child("soban").value?.toString() ?: ""
                    if (banDB.trim() == tableId) {
                        val status = ds.child("status").value?.toString() ?: ""
                        if (status != "paid") {
                            val gia = ds.child("gia").value?.toString()?.toLongOrNull() ?: 0L
                            totalAmount += gia
                            hasItem = true
                        }
                    }
                }

                holder.tvInfo.text = if (hasItem) "💰 BÀN $tableId: ${String.format("%,d", totalAmount)} VNĐ"
                else "💰 BÀN $tableId: 0 VNĐ (Không đơn chưa trả)"

                holder.btnXacNhan.setOnClickListener {
                    onConfirm(tableId, totalAmount, notiKey)
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupButtonUI(btn: Button, method: String) {
        when (method) {
            "Transfer" -> { btn.setBackgroundColor(Color.parseColor("#1976D2")); btn.text = "XÁC NHẬN CK" }
            "Cash" -> { btn.setBackgroundColor(Color.parseColor("#388E3C")); btn.text = "THU TIỀN MẶT" }
            else -> { btn.setBackgroundColor(Color.parseColor("#4CAF50")); btn.text = "XÁC NHẬN" }
        }
    }

    override fun getItemCount(): Int = list.size
}