package com.example.quanlynhahang

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.database.DataSnapshot

class FoodAdapter(
    private val list: List<DataSnapshot>,
    private val onAddClick: (DataSnapshot) -> Unit
) : RecyclerView.Adapter<FoodAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvFoodName)
        val tvPrice: TextView = view.findViewById(R.id.tvFoodPrice)
        val btnAdd: Button = view.findViewById(R.id.btnAddToCart)
        val imgFood: ImageView = view.findViewById(R.id.imgFood)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item_food, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val tenMon = item.key.toString()
        val giaMon = item.child("gia").value.toString()
        val urlAnh = item.child("hinhAnh").value?.toString() ?: "" // Link ảnh từ Firebase

        holder.tvName.text = tenMon
        holder.tvPrice.text = "$giaMon VNĐ"

        // SỬA LỖI HIỂN THỊ ẢNH: Dùng Glide để load ảnh từ link
        Glide.with(holder.itemView.context)
            .load(urlAnh)
            .placeholder(R.drawable.pho_bo) // Hiện ảnh này khi đang tải
            .error(R.drawable.pho_bo)       // Hiện ảnh này nếu link lỗi
            .into(holder.imgFood)

        holder.btnAdd.setOnClickListener { onAddClick(item) }
    }

    override fun getItemCount() = list.size
}