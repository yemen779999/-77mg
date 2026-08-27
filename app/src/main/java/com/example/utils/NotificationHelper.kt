package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.Account
import com.example.data.model.Transaction
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.Locale

object NotificationHelper {

    fun drawPremiumReceiptBitmap(
        context: Context,
        businessName: String,
        account: Account,
        transaction: Transaction,
        currentBalance: Double
    ): android.graphics.Bitmap {
        val width = 600
        val height = 750
        val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)
        
        // Background color
        val bgPaint = Paint().apply {
            color = Color.parseColor("#F8FAFC") // Ice slate background
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)
        
        // Header Banner Background
        val headerPaint = Paint().apply {
            color = Color.parseColor("#1E3A8A") // Royal Navy Blue
        }
        canvas.drawRect(0f, 0f, width.toFloat(), 130f, headerPaint)
        
        // Outer decorative border
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 6f
        }
        canvas.drawRect(3f, 3f, width.toFloat() - 3f, height.toFloat() - 3f, borderPaint)
        
        // Header Text - Title
        val textPaint = android.text.TextPaint().apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }
        
        textPaint.color = Color.WHITE
        textPaint.textSize = 22f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("إيصال مالي رسمي - نظام anas برو", width / 2f, 50f, textPaint)
        
        textPaint.color = Color.parseColor("#38BDF8") // Glacier Blue accent
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText(businessName, width / 2f, 85f, textPaint)
        
        textPaint.color = Color.parseColor("#94A3B8")
        textPaint.textSize = 11f
        canvas.drawText("رقم الهاتف: ${account.phone.ifEmpty { "-" }}", width / 2f, 110f, textPaint)
        
        // Content Area Elements
        // Card Frame
        val cardPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
        }
        val cardShadowPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.FILL
        }
        // Shadow effect
        canvas.drawRoundRect(40f, 160f, (width - 40).toFloat(), height - 120f, 16f, 16f, cardShadowPaint)
        canvas.drawRoundRect(38f, 158f, (width - 42).toFloat(), height - 124f, 16f, 16f, cardPaint)
        
        // Fill details inside receipt card (RTL lookalike align)
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.color = Color.parseColor("#475569") // Slate dark text
        
        // Account Name
        textPaint.textSize = 14f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("المستفيد / الحساب:", (width - 70).toFloat(), 205f, textPaint)
        
        textPaint.textSize = 18f
        textPaint.color = Color.parseColor("#0F172A")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(account.name, (width - 70).toFloat(), 235f, textPaint)
        
        // Section Divider
        val divPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 2f
        }
        canvas.drawLine(70f, 260f, (width - 70).toFloat(), 260f, divPaint)
        
        // Transaction Info
        textPaint.textSize = 13f
        textPaint.color = Color.parseColor("#475569")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("تفاصيل وحركة العملية:", (width - 70).toFloat(), 295f, textPaint)
        
        textPaint.textSize = 15f
        textPaint.color = Color.parseColor("#0F172A")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(transaction.details.ifEmpty { "عملية مالية بدون بيان" }, (width - 70).toFloat(), 320f, textPaint)
        
        // Labels Grid: Date & Type
        textPaint.textSize = 12f
        textPaint.color = Color.parseColor("#64748B")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("تاريخ القيد", (width - 70).toFloat(), 370f, textPaint)
        canvas.drawText("تصنيف جهة الحركة", 180f, 370f, textPaint)
        
        textPaint.textSize = 13f
        textPaint.color = Color.parseColor("#0F172A")
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(transaction.date, (width - 70).toFloat(), 395f, textPaint)
        canvas.drawText(if (account.type == "مورد") "مورد / دائن" else "عميل / مدين", 215f, 395f, textPaint)
        
        canvas.drawLine(70f, 420f, (width - 70).toFloat(), 420f, divPaint)
        
        // Big Amount Badge Highlight
        val isPay = transaction.isPayment
        val badgeColor = if (isPay) "#15803D" else "#B91C1C" // Green for payment/refund, red for debit
        val badgeBgColor = if (isPay) "#DCFCE7" else "#FEE2E2"
        val badgeText = if (isPay) "دفعة مسددة/مستلمة" else "مبيعات دائنة/مستحق"
        
        val badgePaint = Paint().apply {
            color = Color.parseColor(badgeBgColor)
            style = Paint.Style.FILL
        }
        canvas.drawRoundRect(70f, 445f, (width - 70).toFloat(), 550f, 12f, 12f, badgePaint)
        
        // Drawing text inside badge
        textPaint.textAlign = Paint.Align.CENTER
        textPaint.color = Color.parseColor(badgeColor)
        textPaint.textSize = 11f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(badgeText, width / 2f, 475f, textPaint)
        
        textPaint.textSize = 25f
        val formattedTotal = String.format(Locale.US, "%.2f", transaction.total)
        val currencySfx = when(transaction.currency) {
            "USD" -> "$"
            "SAR" -> "ر.س"
            else -> "ريال يمني"
        }
        canvas.drawText("$formattedTotal $currencySfx", width / 2f, 525f, textPaint)
        
        // Footer: Current Net balance summary
        textPaint.color = Color.parseColor("#475569")
        textPaint.textSize = 13f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("الرصيد الكلي المتبقي في ذمة الحساب", width / 2f, 580f, textPaint)
        
        val totalBalColor = if (currentBalance >= 0) "#15803D" else "#B91C1C"
        textPaint.color = Color.parseColor(totalBalColor)
        textPaint.textSize = 18f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        val balanceTypeLabel = if (account.type == "مورد") {
            if (currentBalance >= 0) "لكم علينا" else "لنا عليكم"
        } else {
            if (currentBalance >= 0) "عليكم لنا" else "لكم علينا"
        }
        val formattedBal = String.format(Locale.US, "%.2f", Math.abs(currentBalance))
        canvas.drawText("$formattedBal $currencySfx ($balanceTypeLabel)", width / 2f, 610f, textPaint)
        
        // Dynamic bottom seal / message
        textPaint.color = Color.parseColor("#64748B")
        textPaint.textSize = 10f
        textPaint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("شكراً لتعاملكم الراقي معنا! تم التوليد بواسطة نظام anas برو", width / 2f, 680f, textPaint)
        
        // System decorative footer banner line
        val accentLinePaint = Paint().apply {
            color = Color.parseColor("#D97706") // Amber Gold
        }
        canvas.drawRect(0f, height.toFloat() - 8f, width.toFloat(), height.toFloat(), accentLinePaint)
        
        return bitmap
    }

    fun shareReceiptWithImage(
        context: Context,
        businessName: String,
        account: Account,
        transaction: Transaction,
        currentBalance: Double
    ) {
        try {
            val bitmap = drawPremiumReceiptBitmap(context, businessName, account, transaction, currentBalance)
            
            // Save bitmap to cache directory
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs() // Create directory if it does not exist
            val file = File(cachePath, "receipt_${transaction.id}.png")
            val stream = FileOutputStream(file)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            // Get Uri of the file via FileProvider
            val fileUri: Uri = FileProvider.getUriForFile(
                context,
                "com.aistudio.proledger.xkyzrn.fileprovider",
                file
            )
            
            val shareMessage = generateShareMessage(businessName, account, transaction, currentBalance)
            
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, fileUri)
                putExtra(Intent.EXTRA_TEXT, shareMessage)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            val chooser = Intent.createChooser(intent, "مشاركة إيصال الحركة المالي بالصورة")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to text message WhatsApp share
            val textMsg = generateShareMessage(businessName, account, transaction, currentBalance)
            sendViaWhatsApp(context, account.phone, textMsg)
        }
    }

    fun generateShareMessage(
        businessName: String,
        account: Account,
        transaction: Transaction,
        currentBalance: Double
    ): String {
        val dateString = transaction.date
        val details = transaction.details
        val typeStr = if (transaction.isPayment) "دفعة مستلمة/مُسددة" else "عملية مبيعات/مشتريات جديدة"
        val totalAmount = String.format("%.2f", transaction.total)
        val balanceVal = String.format("%.2f", Math.abs(currentBalance))
        
        val balanceType = if (account.type == "مورد") {
            if (currentBalance >= 0) "لكم علينا (المتبقي لكم)" else "لنا عليكم (زيادة مسددة)"
        } else {
            if (currentBalance >= 0) "عليكم لنا (المتبقي عليكم)" else "لكم علينا (بدفعة زيادة)"
        }

        return """
📢 حركة حساب مالي من: $businessName
👤 العميل/الحساب: ${account.name}
📝 نوع الحركة: $typeStr
📅 التاريخ: $dateString
🏷️ التفاصيل: $details
💵 قيمة الحركة: $totalAmount ريال

💰 الرصيد الحالي: $balanceVal ريال ($balanceType)
شكراً لتعاملكم معنا!
        """.trimIndent()
    }

    fun sendViaWhatsApp(context: Context, phone: String, message: String) {
        try {
            // Clean phone number (needs country code like +967 or similar, let's keep phone, or try to clean it)
            val cleanedPhone = phone.filter { it.isDigit() }
            val url = "https://api.whatsapp.com/send?phone=$cleanedPhone&text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback to sharing generic if whatsapp is not installed or uri fails
            shareGeneric(context, message)
        }
    }

    fun sendViaSMS(context: Context, phone: String, message: String) {
        try {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$phone")
                putExtra("sms_body", message)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            shareGeneric(context, message)
        }
    }

    fun shareGeneric(context: Context, message: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
        }
        context.startActivity(Intent.createChooser(intent, "مشاركة الحركة المالية"))
    }
}
