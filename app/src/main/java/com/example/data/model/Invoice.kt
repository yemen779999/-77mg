package com.example.data.model

data class Invoice(
    val invoiceId: String = "",
    val clientName: String = "",
    val count: Int = 0,          // العدد
    val unitPrice: Double = 0.0, // سعر الحبة
    val total: Double = 0.0,     // الإجمالي
    val date: Long = System.currentTimeMillis()
)
