package com.example.financetracker.data.local

import androidx.room.ColumnInfo

data class CategoryTuple(
    @ColumnInfo(name = "categoryName") val categoryName: String,
    @ColumnInfo(name = "total") val total: Long
)
