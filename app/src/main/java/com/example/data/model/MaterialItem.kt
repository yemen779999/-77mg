package com.example.data.model

data class MaterialItem(
    val id: String = "",
    val name: String = "",
    val count: Int = 0,          // العدد
    val unitPrice: Double = 0.0, // سعر الحبة
    val total: Double = 0.0      // الإجمالي (count * unitPrice)
)
