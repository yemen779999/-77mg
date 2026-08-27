package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deleted_accounts")
data class DeletedAccount(
    @PrimaryKey val id: Int,
    val name: String,
    val phone: String,
    val type: String,
    val createdAt: Long,
    val creditLimit: Double,
    val tag: String,
    val deletedAt: Long = System.currentTimeMillis(),
    val initialBalance: Double = 0.0
)
