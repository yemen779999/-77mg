package com.example.data.model

import java.util.UUID

data class InventoryItem(
    val id: String = UUID.randomUUID().toString(),
    val barcode: String,
    val name: String,
    val purchasePrice: Double,
    val salePrice: Double,
    val stockQuantity: Double,
    val unit: String = "حبة"
)
