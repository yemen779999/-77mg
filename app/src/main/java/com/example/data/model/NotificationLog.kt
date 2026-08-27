package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notification_logs")
data class NotificationLog(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val accountId: Int = 0,
    val clientName: String = "",
    val clientPhone: String = "",
    val type: String = "INVOICE_SUMMARY", // INVOICE_SUMMARY, PAYMENT_REMINDER, INVOICE_RECEIPT, PAYMENT_RECEIPT, BATCH_REMINDER, TEST
    val message: String = "",
    val status: String = "SUCCESS", // SUCCESS, FAILED, SENT_INTENT
    val gateway: String = "META_CLOUD_API", // META_CLOUD_API, ULTRAMSG, EVOLUTION_API, TWILIO, GENERIC_POST, GENERIC_GET, INTENT
    val responseMsg: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
