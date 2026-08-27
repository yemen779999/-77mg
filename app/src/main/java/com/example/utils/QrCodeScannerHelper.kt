package com.example.utils

import android.content.Context
import android.widget.Toast

object QrCodeScannerHelper {
    /**
     * Helper to show scanning success notifications or process scanner results.
     */
    fun processScannedResult(
        context: Context,
        result: String,
        onProcessed: (String) -> Unit
    ) {
        if (result.isNotBlank()) {
            onProcessed(result)
            Toast.makeText(context, "تم المسح بنجاح! 🎉", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "كود الـ QR غير صالح", Toast.LENGTH_SHORT).show()
        }
    }
}
