package com.example.quanlynhahang

import com.google.firebase.database.DataSnapshot

// Model món ăn gom nhóm (Đã sửa để hỗ trợ gom món x2, x3)
data class DishItem(
    val idList: MutableList<String>, // Lưu danh sách các ID của những món trùng tên
    val tenMon: String,
    var soLuong: Int,
    val snapshot: DataSnapshot
)

// Model chứa thông tin bàn (THÊM 2 BIẾN isPaid và allServed ĐỂ QUẢN LÝ MÀU)
data class TableGroup(
    val soBan: String,
    val ghiChu: String,
    val timestamp: Long,
    val items: MutableList<DishItem>,

    // Thêm 2 thuộc tính này để kết hợp logic màu sắc
    val isPaid: Boolean = false,    // true khi đã bấm nút thu tiền (trong nhanvien.kt)
    val allServed: Boolean = false  // true khi bếp không còn món "waiting" (trong Bep.kt)
)