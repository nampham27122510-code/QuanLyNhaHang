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
import java.text.Normalizer
import java.util.regex.Pattern

class FoodAdapter(
    private val foodList: List<DataSnapshot>,
    private val onAddClick: (DataSnapshot) -> Unit
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgFood: ImageView = itemView.findViewById(R.id.imgFood)
        val tvFoodName: TextView = itemView.findViewById(R.id.tvFoodName)
        val tvFoodPrice: TextView = itemView.findViewById(R.id.tvFoodPrice)
        val btnAddToCart: Button = itemView.findViewById(R.id.btnAddToCart)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.activity_item_food, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val monAn = foodList[position]
        val context = holder.itemView.context

        // 1. Lấy tên món ăn (Key trên Firebase)
        val tenMon = monAn.key.toString()
        holder.tvFoodName.text = tenMon

        // 2. Định dạng giá tiền
        val gia = monAn.child("gia").value.toString()
        holder.tvFoodPrice.text = String.format("%,d VNĐ", gia.toLongOrNull() ?: 0L)

        // 3. LOGIC LẤY ẢNH TỰ ĐỘNG THEO TÊN MÓN
        // Chuyển "Bánh mì" -> "banh_mi", "Sandwich" -> "sandwich"
        val fileName = removeAccent(tenMon.lowercase().trim()).replace(" ", "_")

        // Tìm ID trong drawable
        val imageResId = context.resources.getIdentifier(fileName, "drawable", context.packageName)

        if (imageResId != 0) {
            Glide.with(context)
                .load(imageResId)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.imgFood)
        } else {
            // Nếu không tìm thấy file trùng tên món, hiện ảnh mặc định
            holder.imgFood.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.btnAddToCart.setOnClickListener {
            onAddClick(monAn)
        }
    }

    override fun getItemCount(): Int = foodList.size

    // Hàm bổ trợ: Chuyển tiếng Việt có dấu thành không dấu để khớp với tên file drawable
    private fun removeAccent(s: String): String {
        val temp = Normalizer.normalize(s, Normalizer.Form.NFD)
        val pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+")
        return pattern.matcher(temp).replaceAll("").replace('đ', 'd').replace('Đ', 'D')
    }
}