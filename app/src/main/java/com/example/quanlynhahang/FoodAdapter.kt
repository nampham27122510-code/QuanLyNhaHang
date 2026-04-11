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

        // 1. Lấy tên món gốc từ Database (Ví dụ: "Bún Đậu Mắn Tôm")
        val tenMonGoc = item.key.toString()
        val giaMon = item.child("gia").value.toString()

        // HIỂN THỊ ĐÚNG TÊN MÓN (Dù database sai chính tả vẫn hiện đúng chữ đó)
        holder.tvName.text = tenMonGoc
        holder.tvPrice.text = "$giaMon VNĐ"

        // 2. LOGIC TÌM ẢNH: Chuyển về chữ thường để so sánh từ khóa "bún đậu"
        val imageName = item.child("imageName").value?.toString()?.trim() ?: ""

        val imageResource = when (imageName) {
            "bun_dau" -> R.drawable.bun_dau_mam_tom
            "pho_bo" -> R.drawable.pho_bo
            "pho_ga" -> R.drawable.pho_ga
            "sandwich" -> R.drawable.sandwich
            else -> R.drawable.logo
        }

        // 3. Load ảnh bằng Glide
        Glide.with(holder.itemView.context)
            .load(imageResource)
            .into(holder.imgFood)

        holder.btnAdd.setOnClickListener { onAddClick(item) }
    }

    override fun getItemCount() = list.size
}