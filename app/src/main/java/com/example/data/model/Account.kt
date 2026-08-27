package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String,
    val type: String, // "مورد" or "مشتري"
    val createdAt: Long = System.currentTimeMillis(),
    val creditLimit: Double = 0.0,
    val tag: String = "",
    val initialBalance: Double = 0.0
)
