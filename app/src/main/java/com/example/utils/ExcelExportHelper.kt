package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ExcelExportHelper {

    fun generateExcelFile(context: Context, accountName: String, transactions: List<Transaction>, currentBalance: Double, defaultCurrency: String): File? {
        val cleanAccountName = accountName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]"), "_")
        val fileName = "Statement_${cleanAccountName}_${System.currentTimeMillis()}.csv"
        val file = File(context.cacheDir, fileName)

        try {
            val fos = FileOutputStream(file)
            
            // Write BOM for UTF-8 to support Arabic characters in Excel
            fos.write(0xEF)
            fos.write(0xBB)
            fos.write(0xBF)

            val writer = fos.writer(Charsets.UTF_8)
            
            // Standard Excel separator metadata directive
            writer.write("sep=,\n")
            
            // Header
            writer.write("كشف حساب: \"${accountName.replace("\"", "\"\"")}\"\n")
            writer.write("تاريخ الإصدار: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())}\n")
            val balanceStatus = if (currentBalance >= 0) "له (دائن)" else "عليه (مدين)"
            writer.write("الرصيد الحالي: ${Math.abs(currentBalance)} $defaultCurrency ($balanceStatus)\n\n")

            // Table headers
            writer.write("التاريخ,اليوم,التفاصيل والبيان,الكمية/العدد,السعر,الزيادات,المبلغ الإجمالي,نوع الحركة\n")

            // Rows
            transactions.forEach { tx ->
                val type = if (tx.isPayment) "مقبوضات (دائن)" else "مشتريات (مدين)"
                val qty = if (tx.isPayment) "-" else tx.quantity.toString()
                val price = if (tx.isPayment) "-" else tx.unitPrice.toString()
                val addition = if (tx.isPayment) "-" else tx.addition.toString()
                // Escape commas, quotes and newlines to prevent CSV breakdown
                val details = tx.details.replace("\"", "\"\"").replace("\n", " ").replace("\r", "").replace(",", "،")

                writer.write("${tx.date},${tx.day},\"${details}\",${qty},${price},${addition},${tx.total},${type}\n")
            }

            writer.flush()
            writer.close()
            fos.close()

            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun shareExcel(context: Context, file: File) {
        val authority = "${context.packageName}.fileprovider"
        val uri = FileProvider.getUriForFile(context, authority, file)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(intent, "مشاركة كشف الحساب كـ Excel/CSV").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}
