package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_transactions")
data class DeletedTransaction(
    @PrimaryKey val id: Int,
    val accountId: Int,
    val day: String,
    val date: String,
    val details: String,
    val quantity: Double,
    val unitPrice: Double,
    val addition: Double,
    val total: Double,
    val isPayment: Boolean,
    val timestamp: Long,
    val currency: String,
    val exchangeRate: Double,
    val dueDate: String,
    val deletedAt: Long = System.currentTimeMillis()
)
