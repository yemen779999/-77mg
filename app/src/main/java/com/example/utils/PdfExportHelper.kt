package com.example.utils

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import com.example.data.model.Transaction
import com.example.ui.viewmodel.PdfTemplateConfig
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExportHelper {

    fun generateAccountStatementPdf(
        context: Context,
        businessName: String,
        businessPhone: String,
        businessAddress: String,
        accountName: String,
        accountPhone: String,
        accountType: String,
        transactions: List<Transaction>,
        currentBalance: Double,
        config: PdfTemplateConfig,
        defaultCurrency: String = "YER"
    ): File? {
        val pdfDocument = PdfDocument()
        
        // A4 page size in points (595 x 842)
        val pageWidth = 595
        val pageHeight = 842
        
        var pageNumber = 1
        var myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var myPage = pdfDocument.startPage(myPageInfo)
        var canvas = myPage.canvas

        // Apply Custom PDF Font Style
        val selectedTypeface = when (config.fontStyle) {
            "MONOSPACE" -> Typeface.MONOSPACE
            "SANS_SERIF" -> Typeface.SANS_SERIF
            "SERIF" -> Typeface.SERIF
            else -> Typeface.DEFAULT
        }

        // Apply Custom PDF Font Size Multiplier
        val sizeMultiplier = when (config.fontSize) {
            "SMALL" -> 0.85f
            "LARGE" -> 1.15f
            else -> 1.0f // MEDIUM
        }

        // Resolve custom professional color theme
        val headerColor = when (config.themeColor) {
            "NAVY" -> "#1E3A8A"
            "EMERALD" -> "#14532D"
            "BURGUNDY" -> "#6B1D2F"
            "GOLDEN" -> "#78350F"
            else -> "#1E293B" // SLATE
        }
        val tableHeaderBgColor = when (config.themeColor) {
            "NAVY" -> "#2563EB"
            "EMERALD" -> "#15803D"
            "BURGUNDY" -> "#881337"
            "GOLDEN" -> "#9A3412"
            else -> "#334155" // SLATE
        }

        val paint = Paint()
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            textSize = 10f * sizeMultiplier
            typeface = Typeface.create(selectedTypeface, Typeface.NORMAL)
        }

        // Draw header background (Dark slate theme)
        paint.color = Color.parseColor(headerColor)
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, paint)

        // Draw Custom Logo Image or Emoji/Symbol if enabled
        val logoOffset = if (config.showLogo && config.logo.isNotEmpty()) {
            if (config.logo.startsWith("content://") || config.logo.startsWith("file://")) {
                var drawn = false
                try {
                    val uri = Uri.parse(config.logo)
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        val bmp = BitmapFactory.decodeStream(stream)
                        if (bmp != null) {
                            val scaledBmp = Bitmap.createScaledBitmap(bmp, 55, 55, true)
                            canvas.drawBitmap(scaledBmp, 30f, 25f, null)
                            drawn = true
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (!drawn) {
                    val logoPaint = Paint().apply { textSize = 28f }
                    canvas.drawText("🏢", 30f, 65f, logoPaint)
                }
                65
            } else {
                val logoPaint = Paint().apply { textSize = 32f }
                canvas.drawText(config.logo, 30f, 65f, logoPaint)
                50 // Add offset for header elements so they don't overlap with the logo
            }
        } else {
            0
        }

        // Draw header custom or dynamic business title
        val titleText = config.customTitle.ifBlank { businessName }
        val titleTextPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 18f * sizeMultiplier
            typeface = Typeface.create(selectedTypeface, Typeface.BOLD)
        }
        drawTextRtl(canvas, titleText, pageWidth - 30, 30, titleTextPaint, pageWidth - 60 - logoOffset)
        
        // Draw Header custom subtitle
        val subtitleText = config.customSubtitle.ifBlank { "تقرير كشف حساب مالي - نظام المحاسب الشامل" }
        val subtitlePaint = TextPaint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 10f * sizeMultiplier
            typeface = Typeface.create(selectedTypeface, Typeface.NORMAL)
        }
        drawTextRtl(canvas, subtitleText, pageWidth - 30, 55, subtitlePaint, pageWidth - 60 - logoOffset)
        
        // Header Left contact info
        val descPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 9f * sizeMultiplier
            typeface = Typeface.create(selectedTypeface, Typeface.NORMAL)
        }
        val headerLeftDesc = "الموقع: $businessAddress\nالهاتف: $businessPhone"
        drawMultilineText(canvas, headerLeftDesc, 30 + logoOffset, 30, descPaint, 250, Layout.Alignment.ALIGN_NORMAL)

        // Account Details Section (Sub-header)
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRect(30f, 130f, (pageWidth - 30).toFloat(), 185f, paint)

        val boldTextPaint = TextPaint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f * sizeMultiplier
            typeface = Typeface.create(selectedTypeface, Typeface.BOLD)
        }

        val df = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val dateString = df.format(Date())

        val customerInfoRight = "الحساب: $accountName ($accountType)"
        val customerInfoLeft = "رقم الهاتف: $accountPhone\nتاريخ التصدير: $dateString"

        drawTextRtl(canvas, customerInfoRight, pageWidth - 45, 145, boldTextPaint, 300)
        drawMultilineText(canvas, customerInfoLeft, 45, 145, textPaint, 230, Layout.Alignment.ALIGN_NORMAL)

        // Draw Dynamic Currency Balance Info
        val balanceColor = if (currentBalance >= 0) "#15803D" else "#B91C1C"
        val balanceTitle = if (accountType == "مورد") "إجمالي الرصيد الذي له علينا:" else "إجمالي الرصيد المطلوب منه:"
        val currLabel = when (defaultCurrency) {
            "USD" -> "$"
            "SAR" -> "ر.س"
            "YER" -> "ريال يمني"
            else -> defaultCurrency
        }
        val balanceText = "$balanceTitle ${String.format(Locale.US, "%.2f", Math.abs(currentBalance))} $currLabel"
        
        val balancePaint = TextPaint().apply {
            color = Color.parseColor(balanceColor)
            textSize = 12f * sizeMultiplier
            typeface = Typeface.create(selectedTypeface, Typeface.BOLD)
        }
        drawTextRtl(canvas, balanceText, pageWidth - 45, 170, balancePaint, 500)

        // DYNAMIC TABLE COLUMN COMPUTATION BASED ON LAYOUT CONFIG
        val headerList = mutableListOf<String>()
        val widthList = mutableListOf<Int>()
        
        headerList.add("اليوم")
        widthList.add(50)
        
        headerList.add("التاريخ")
        widthList.add(65)
        
        // Sum up optional column widths to assign remainder space dynamically to Details/ البيان
        var optionalSum = 0
        if (config.colQtyVisible) optionalSum += 45
        if (config.colPriceVisible) optionalSum += 55
        if (config.colAdditionVisible) optionalSum += 55
        if (config.colTotalVisible) optionalSum += 70
        
        val detailsWidth = 535 - 115 - optionalSum // Remaining width of A4 bounds
        
        headerList.add(config.colDetailsLabel)
        widthList.add(detailsWidth)
        
        if (config.colQtyVisible) {
            headerList.add(config.colQtyLabel)
            widthList.add(45)
        }
        if (config.colPriceVisible) {
            headerList.add(config.colPriceLabel)
            widthList.add(55)
        }
        if (config.colAdditionVisible) {
            headerList.add(config.colAdditionLabel)
            widthList.add(55)
        }
        if (config.colTotalVisible) {
            headerList.add(config.colTotalLabel)
            widthList.add(70)
        }
        
        val colHeaders = headerList.toTypedArray()
        val colWidths = widthList.toIntArray()

        val tableTop = 205f
        
        // Draw Table Header Background
        paint.color = Color.parseColor(tableHeaderBgColor)
        canvas.drawRect(30f, tableTop, (pageWidth - 30).toFloat(), tableTop + 24f, paint)

        // Draw Table Headers
        var currentX = 30
        val tableHeaderPaint = TextPaint().apply {
            color = Color.WHITE
            textSize = 9f * sizeMultiplier
            typeface = Typeface.create(selectedTypeface, Typeface.BOLD)
        }
        for (i in colHeaders.indices) {
            val cellWidth = colWidths[i]
            val centerOffset = cellWidth / 2
            drawTextCentered(canvas, colHeaders[i], currentX + centerOffset, tableTop + 15, tableHeaderPaint)
            currentX += cellWidth
        }

        // Draw Table Rows
        var currentY = tableTop + 24f
        val maxRowsPerPage = 18
        var rowCount = 0

        val rowBgPaint = Paint()
        val rowBorderPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            style = Paint.Style.STROKE
            strokeWidth = 0.5f
        }

        for (tx in transactions) {
            if (rowCount >= maxRowsPerPage) {
                pdfDocument.finishPage(myPage)
                
                pageNumber++
                myPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                myPage = pdfDocument.startPage(myPageInfo)
                canvas = myPage.canvas
                
                // Draw new page header indicator
                paint.color = Color.parseColor(headerColor)
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 45f, paint)
                
                val pagenumPaint = TextPaint().apply {
                    color = Color.WHITE
                    textSize = 10f * sizeMultiplier
                    typeface = Typeface.create(selectedTypeface, Typeface.NORMAL)
                }
                drawTextRtl(canvas, "تابع كشف الحساب: $accountName - صفحة $pageNumber", pageWidth - 30, 25, pagenumPaint, pageWidth - 100)
                
                // Redraw table header on new page
                val newTableTop = 60f
                paint.color = Color.parseColor(tableHeaderBgColor)
                canvas.drawRect(30f, newTableTop, (pageWidth - 30).toFloat(), newTableTop + 24f, paint)
                
                currentX = 30
                for (i in colHeaders.indices) {
                    val cellWidth = colWidths[i]
                    val centerOffset = cellWidth / 2
                    drawTextCentered(canvas, colHeaders[i], currentX + centerOffset, newTableTop + 15, tableHeaderPaint)
                    currentX += cellWidth
                }
                
                currentY = newTableTop + 24f
                rowCount = 0
            }

            // Draw alternate row backgrounds
            if (rowCount % 2 == 1) {
                rowBgPaint.color = Color.parseColor("#F8FAFC")
            } else {
                rowBgPaint.color = Color.WHITE
            }
            canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 22f, rowBgPaint)
            // Draw column borders
            canvas.drawRect(30f, currentY, (pageWidth - 30).toFloat(), currentY + 22f, rowBorderPaint)

            // Compile cell values dynamically
            val isPay = tx.isPayment
            val cellValuesList = mutableListOf<String>()
            
            cellValuesList.add(tx.day)
            cellValuesList.add(tx.date)
            
            // Append foreign currency amount to Details string if transaction is in foreign currency
            val currencySuffix = if (tx.currency != defaultCurrency) {
                val symbol = when(tx.currency) {
                    "USD" -> "$"
                    "SAR" -> "ر.س"
                    "YER" -> "ر.ي"
                    else -> tx.currency
                }
                " (${String.format(Locale.US, "%.1f", tx.total)} $symbol)"
            } else {
                ""
            }
            cellValuesList.add(tx.details + currencySuffix)
            
            if (config.colQtyVisible) {
                cellValuesList.add(if (isPay) "-" else String.format(Locale.US, "%.1f", tx.quantity))
            }
            if (config.colPriceVisible) {
                cellValuesList.add(if (isPay) "-" else String.format(Locale.US, "%.2f", tx.unitPrice))
            }
            if (config.colAdditionVisible) {
                cellValuesList.add(if (isPay) "-" else String.format(Locale.US, "%.2f", tx.addition))
            }
            if (config.colTotalVisible) {
                // Total converted into Default App Currency inside PDF statement
                val defaultTotalMultiplier = tx.total * tx.exchangeRate
                cellValuesList.add(String.format(Locale.US, "%.2f", defaultTotalMultiplier) + (if (isPay) " (دفعة)" else ""))
            }

            val cellValues = cellValuesList.toTypedArray()

            // Draw Table Row Cells
            currentX = 30
            val cellPaint = TextPaint().apply {
                color = if (isPay) Color.parseColor("#15803D") else Color.parseColor("#1E293B")
                textSize = 8.5f * sizeMultiplier
                typeface = if (isPay) Typeface.create(selectedTypeface, Typeface.BOLD) else Typeface.create(selectedTypeface, Typeface.NORMAL)
            }
            
            for (i in cellValues.indices) {
                val cellWidth = colWidths[i]
                val text = cellValues[i]
                
                // Particular case for long/wrapping details cells
                if (i == 2) {
                    val maxLen = if (optionalSum == 0) 55 else 38
                    val truncated = if (text.length > maxLen) text.take(maxLen - 3) + "..." else text
                    drawTextCentered(canvas, truncated, currentX + (cellWidth / 2), currentY + 14, cellPaint)
                } else {
                    drawTextCentered(canvas, text, currentX + (cellWidth / 2), currentY + 14, cellPaint)
                }
                currentX += cellWidth
            }

            currentY += 22f
            rowCount++
        }

        // Draw bottom footer note
        val footerY = pageHeight - 50f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(30f, footerY, (pageWidth - 30).toFloat(), footerY, paint)

        val footerPaint = TextPaint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8f * sizeMultiplier
            typeface = Typeface.create(selectedTypeface, Typeface.NORMAL)
        }
        val customFooterText = config.customFooter.ifBlank { "تم التوليد تلقائياً بواسطة تطبيق المحاسب الشامل (Pro Ledger)" }
        drawTextRtl(canvas, customFooterText, pageWidth - 30, (footerY + 15).toInt(), footerPaint, 300)
        
        // Show signature stamp if enabled
        if (config.showSignature) {
            drawMultilineText(canvas, "التوقيع/الختم: .......................................", 30, (footerY + 15).toInt(), footerPaint, 200, Layout.Alignment.ALIGN_NORMAL)
        }

        pdfDocument.finishPage(myPage)

        val cleanAccountName = accountName.replace(Regex("[^a-zA-Z0-9\\u0600-\\u06FF]"), "_")
        val f = File(context.cacheDir, "Statement_$cleanAccountName.pdf")
        try {
            val fos = FileOutputStream(f)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            return f
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
        }
        return null
    }

    private fun drawTextRtl(canvas: Canvas, text: String, rx: Int, y: Int, textPaint: TextPaint, width: Int) {
        if (width <= 0) return
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
            .setAlignment(Layout.Alignment.ALIGN_OPPOSITE)
            .setLineSpacing(0f, 1.0f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate((rx - width).toFloat(), y.toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawMultilineText(canvas: Canvas, text: String, x: Int, y: Int, textPaint: TextPaint, width: Int, alignment: Layout.Alignment) {
        if (width <= 0) return
        val layout = StaticLayout.Builder.obtain(text, 0, text.length, textPaint, width)
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.1f)
            .setIncludePad(false)
            .build()
        canvas.save()
        canvas.translate(x.toFloat(), y.toFloat())
        layout.draw(canvas)
        canvas.restore()
    }

    private fun drawTextCentered(canvas: Canvas, text: String, centerX: Int, centerY: Float, textPaint: TextPaint) {
        val textWidth = textPaint.measureText(text)
        val rx = centerX - (textWidth / 2)
        val ry = centerY - ((textPaint.descent() + textPaint.ascent()) / 2)
        canvas.drawText(text, rx, ry, textPaint)
    }

    fun sharePdf(context: Context, pdfFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val pdfUri = FileProvider.getUriForFile(context, authority, pdfFile)
        
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, pdfUri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooserIntent = Intent.createChooser(intent, "مشاركة كشف الحساب كـ PDF").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooserIntent)
    }
}
