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

        val tableFromNoti = data.child("table").value?.toString()?.trim() ?: ""
        val message = data.child("message").value?.toString() ?: "Yêu cầu thanh toán"
        val method = data.child("method").value?.toString() ?: "Normal"

        // HIỂN THỊ NGAY LẬP TỨC: Không đợi tính tiền để tránh lag
        holder.btnXacNhan.isEnabled = true
        holder.btnXacNhan.alpha = 1.0f

        // Chỉ hiển thị Bàn và nội dung yêu cầu (Tiền mặt/Chuyển khoản)
        holder.tvInfo.text = "💰 BÀN: $tableFromNoti\n📢 $message"

        // Thiết lập màu sắc nút dựa trên phương thức
        setupButtonUI(holder.btnXacNhan, method)

        // SỬA: Khi ấn xác nhận, truyền table và key sang Activity
        // Activity sẽ tự quét bảng Orders để tính tổng tiền và cộng vào Admin
        holder.btnXacNhan.setOnClickListener {
            // Truyền 0L vì Activity sẽ tự tính lại số tiền thực tế từ Database cho chính xác
            onConfirm(tableFromNoti, 0L, notiKey)
        }
    }

    private fun setupButtonUI(btn: Button, method: String) {
        when (method) {
            "Transfer" -> {
                btn.setBackgroundColor(Color.parseColor("#1976D2")) // Màu xanh dương
                btn.text = "XÁC NHẬN CK"
            }
            "Cash" -> {
                btn.setBackgroundColor(Color.parseColor("#388E3C")) // Màu xanh lá
                btn.text = "THU TIỀN MẶT"
            }
            else -> {
                btn.setBackgroundColor(Color.parseColor("#4CAF50"))
                btn.text = "XÁC NHẬN"
            }
        }
    }

    override fun getItemCount(): Int = list.size
}