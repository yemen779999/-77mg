package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    foreignKeys = [
        ForeignKey(
            entity = Account::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["accountId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val accountId: Int,
    val day: String, // e.g., "الجمعة" or "الأحد"
    val date: String, // e.g., "2026-06-09"
    val details: String, // البيان والتفاصيل
    val quantity: Double, // العدد/الكمية
    val unitPrice: Double, // سعر الحبة
    val addition: Double, // الزيادة/الإضافات
    val total: Double, // (الكمية * السعر) + الإضافات
    val isPayment: Boolean = false, // هل هي دفعة مسددة / واصل؟
    val timestamp: Long = System.currentTimeMillis(),
    val currency: String = "YER",
    val exchangeRate: Double = 1.0,
    val dueDate: String = ""
)
