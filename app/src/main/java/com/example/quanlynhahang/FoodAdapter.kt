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

        // 1. Hiển thị tên món ăn từ Firebase
        holder.tvFoodName.text = monAn.key.toString()

        // 2. Định dạng giá tiền (ví dụ: 55,000 VNĐ cho Sandwich)
        val gia = monAn.child("gia").value.toString()
        holder.tvFoodPrice.text = String.format("%,d VNĐ", gia.toLongOrNull() ?: 0L)

        // 3. LOGIC XỬ LÝ ẢNH TỪ DRAWABLE
        // Lấy giá trị từ field "imageName" trên Firebase.
        // LƯU Ý: Firebase nên để là "sandwich", "pho_bo", "pho_ga" (không kèm đuôi .jpg)
        val rawImageName = monAn.child("imageName").value?.toString() ?: ""

        // Chuẩn hóa: viết thường, xóa khoảng trắng thừa
        val cleanedName = rawImageName.lowercase().trim()

        // Tìm ID tài nguyên trong thư mục drawable
        val imageResId = context.resources.getIdentifier(cleanedName, "drawable", context.packageName)

        if (imageResId != 0) {
            // Nếu tìm thấy ID hợp lệ, nạp ảnh bằng Glide
            Glide.with(context)
                .load(imageResId)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_report_image)
                .into(holder.imgFood)
        } else {
            // Nếu không tìm thấy (do sai tên trên Firebase), hiện ảnh mặc định
            holder.imgFood.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // 4. Xử lý sự kiện thêm vào giỏ hàng
        holder.btnAddToCart.setOnClickListener {
            onAddClick(monAn)
        }
    }

    override fun getItemCount(): Int = foodList.size
}