package com.example.quanlynhahang
import com.google.firebase.database.DataSnapshot

// Model chứa thông tin món lẻ để xử lý logic của Nam
data class DishItem(
    val id: String,
    val tenMon: String,
    val snapshot: DataSnapshot // Giữ snapshot để phục vụ logic trừ kho
)

// Model chứa thông tin cả bàn
data class TableGroup(
    val soBan: String,
    val ghiChu: String,
    val timestamp: Long,
    val items: MutableList<DishItem>
)