package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.widget.Toast
import com.example.data.model.Account
import com.example.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object WhatsAppHelper {

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    /**
     * Clean and format phone numbers for WhatsApp.
     * Removes spaces, dashes, symbols and prepends the country code if needed.
     */
    fun formatPhoneNumber(phone: String, defaultCountryCode: String = "967"): String {
        var cleaned = phone.replace(Regex("[^0-9]"), "").trim()
        
        while (cleaned.startsWith("0")) {
            cleaned = cleaned.substring(1)
        }

        if (cleaned.isEmpty()) return ""

        val code = defaultCountryCode.replace("+", "").trim()

        // If number starts with country code already
        if (code.isNotEmpty() && cleaned.startsWith(code)) {
            return cleaned
        }

        // Specific local checks (e.g. Yemen 9 digits starting with 7, KSA 9 digits starting with 5, Egypt 10 digits starting with 1)
        if (code == "967" && cleaned.length == 9 && cleaned.startsWith("7")) {
            return code + cleaned
        }
        if (code == "966" && cleaned.length == 9 && cleaned.startsWith("5")) {
            return code + cleaned
        }
        if (code == "20" && cleaned.length == 10 && cleaned.startsWith("1")) {
            return code + cleaned
        }

        // If length looks like a local phone without code, prepend code
        return if (cleaned.length in 7..10 && code.isNotEmpty()) {
            code + cleaned
        } else {
            cleaned
        }
    }

    /**
     * Generate comprehensive Invoice Summary & Statement message for a client.
     */
    fun generateInvoiceSummaryMessage(
        template: String,
        account: Account,
        transactions: List<Transaction>,
        businessName: String,
        businessPhone: String,
        currency: String = "YER",
        paymentInstructions: String = ""
    ): String {
        val totalDebit = transactions.filter { !it.isPayment }.sumOf { it.total }
        val totalCredit = transactions.filter { it.isPayment }.sumOf { it.total }
        val netBalance = (account.initialBalance + totalDebit) - totalCredit
        val invoiceCount = transactions.count { !it.isPayment }
        val paymentCount = transactions.count { it.isPayment }

        val currSfx = when (currency.uppercase()) {
            "USD" -> "$"
            "SAR" -> "ر.س"
            else -> "ريال يمني"
        }

        val totalDebitStr = String.format(Locale.US, "%,.2f", totalDebit)
        val totalCreditStr = String.format(Locale.US, "%,.2f", totalCredit)
        val netBalStr = String.format(Locale.US, "%,.2f", Math.abs(netBalance))
        
        val balanceStatus = if (account.type == "مورد") {
            if (netBalance >= 0) "لكم علينا (المتبقي لصالحكم)" else "لنا عليكم (زيادة مسددة)"
        } else {
            if (netBalance >= 0) "عليكم لنا (المتبقي بذمتكم)" else "لكم علينا (دفعة فائضة)"
        }

        val todayDate = SimpleDateFormat("yyyy/MM/dd", Locale.US).format(Date())

        // Build recent transactions snippet (last 5)
        val recentTxs = transactions.takeLast(5).reversed()
        val txsListBuilder = StringBuilder()
        if (recentTxs.isNotEmpty()) {
            recentTxs.forEach { tx ->
                val typeIcon = if (tx.isPayment) "🟢 سند قبض" else "🧾 فاتورة"
                val amt = String.format(Locale.US, "%,.2f", tx.total)
                txsListBuilder.append("• ${tx.date} | $typeIcon: $amt $currSfx (${tx.details.ifBlank { "بدون بيان" }})\n")
            }
        } else {
            txsListBuilder.append("لا توجد حركات مسجلة حالياً.")
        }

        var msg = template
            .replace("{CLIENT}", account.name)
            .replace("{CLIENT_NAME}", account.name)
            .replace("{CLIENT_PHONE}", account.phone)
            .replace("{TOTAL_DEBT}", totalDebitStr)
            .replace("{TOTAL_INVOICED}", totalDebitStr)
            .replace("{TOTAL_PAID}", totalCreditStr)
            .replace("{BALANCE}", "$netBalStr $currSfx")
            .replace("{REMAINING_BALANCE}", "$netBalStr $currSfx")
            .replace("{BALANCE_STATUS}", balanceStatus)
            .replace("{INVOICE_COUNT}", invoiceCount.toString())
            .replace("{PAYMENT_COUNT}", paymentCount.toString())
            .replace("{CURRENCY}", currSfx)
            .replace("{DATE}", todayDate)
            .replace("{TODAY}", todayDate)
            .replace("{BUSINESS}", businessName)
            .replace("{BUSINESS_NAME}", businessName)
            .replace("{BUSINESS_PHONE}", businessPhone)
            .replace("{RECENT_TRANSACTIONS}", txsListBuilder.toString().trimEnd())
            .replace("{PAYMENT_INFO}", paymentInstructions.ifBlank { "يرجى التواصل معنا لتحديد طريقة السداد المناسبة." })

        return msg
    }

    /**
     * Generate friendly yet clear payment reminder message for debts / overdue dues.
     */
    fun generatePaymentReminderMessage(
        template: String,
        account: Account,
        totalBalance: Double,
        currency: String = "YER",
        dueDate: String = "",
        overdueDays: Int = 0,
        details: String = "",
        businessName: String,
        businessPhone: String,
        paymentInstructions: String = ""
    ): String {
        val currSfx = when (currency.uppercase()) {
            "USD" -> "$"
            "SAR" -> "ر.س"
            else -> "ريال يمني"
        }

        val balStr = String.format(Locale.US, "%,.2f", Math.abs(totalBalance))
        val dueStatus = when {
            overdueDays > 0 -> "⚠️ متأخرة منذ $overdueDays يوم"
            overdueDays == 0 && dueDate.isNotBlank() -> "🔔 تستحق اليوم"
            dueDate.isNotBlank() -> "📅 موعد الاستحقاق: $dueDate"
            else -> "مستحقة السداد"
        }

        var msg = template
            .replace("{CLIENT}", account.name)
            .replace("{CLIENT_NAME}", account.name)
            .replace("{CLIENT_PHONE}", account.phone)
            .replace("{TOTAL}", "$balStr $currSfx")
            .replace("{BALANCE}", "$balStr $currSfx")
            .replace("{REMAINING_BALANCE}", "$balStr $currSfx")
            .replace("{DUE_DATE}", if (dueDate.isNotBlank()) dueDate else "تاريخ الاستحقاق")
            .replace("{DUE_STATUS}", dueStatus)
            .replace("{DAYS_OVERDUE}", overdueDays.toString())
            .replace("{DETAILS}", details.ifBlank { "مشتريات وحركات سابقة مقيدة في الحساب" })
            .replace("{CURRENCY}", currSfx)
            .replace("{BUSINESS}", businessName)
            .replace("{BUSINESS_NAME}", businessName)
            .replace("{BUSINESS_PHONE}", businessPhone)
            .replace("{PAYMENT_INFO}", paymentInstructions.ifBlank { "نقداً أو عبر الحوالات المباشرة" })

        return msg
    }

    /**
     * Replaces standard placeholders in custom single transaction templates.
     */
    fun generateMessage(
        template: String,
        accountName: String,
        accountPhone: String,
        details: String,
        total: Double,
        currency: String,
        dueDate: String,
        businessName: String,
        businessPhone: String,
        transactionId: Int = 0,
        currentBalance: Double = 0.0,
        paymentInstructions: String = ""
    ): String {
        val currSfx = when (currency.uppercase()) {
            "USD" -> "$"
            "SAR" -> "ر.س"
            else -> "ريال"
        }
        val totalStr = String.format(Locale.US, "%,.2f", total).replace(Regex("\\.00$"), "")
        val balStr = String.format(Locale.US, "%,.2f", Math.abs(currentBalance)).replace(Regex("\\.00$"), "")

        return template
            .replace("{CLIENT}", accountName)
            .replace("{CLIENT_NAME}", accountName)
            .replace("{CLIENT_PHONE}", accountPhone)
            .replace("{DETAILS}", details.ifBlank { "عملية حسابية" })
            .replace("{CURRENCY}", currSfx)
            .replace("{BUSINESS}", businessName)
            .replace("{BUSINESS_NAME}", businessName)
            .replace("{BUSINESS_PHONE}", businessPhone)
            .replace("{ID}", if (transactionId > 0) transactionId.toString() else "-")
            .replace("{TRANSACTION_ID}", if (transactionId > 0) transactionId.toString() else "-")
            .replace("{TOTAL}", totalStr)
            .replace("{BALANCE}", "$balStr $currSfx")
            .replace("{REMAINING_BALANCE}", "$balStr $currSfx")
            .replace("{DUE_DATE}", if (dueDate.isNotBlank()) dueDate else "تاريخ الاستحقاق")
            .replace("{PAYMENT_INFO}", paymentInstructions)
    }

    fun generateInvoiceReceiptMessage(
        template: String,
        account: Account,
        transaction: Transaction,
        businessName: String,
        businessPhone: String,
        currency: String = "YER"
    ): String {
        val baseTemplate = template.ifBlank {
            "🧾 *فاتورة مبيعات جديدة*\nمرحباً {CLIENT}، تم تقييد فاتورة مشتريات جديدة بقيمة {TOTAL} {CURRENCY}.\nالبيان: {DETAILS}\nالتاريخ: {DUE_DATE}\nشكراً لتعاملكم معنا - {BUSINESS}"
        }
        return generateMessage(
            template = baseTemplate,
            accountName = account.name,
            accountPhone = account.phone,
            details = transaction.details,
            total = transaction.total,
            currency = currency,
            dueDate = transaction.date,
            businessName = businessName,
            businessPhone = businessPhone,
            transactionId = transaction.id
        )
    }

    fun generatePaymentReceiptMessage(
        template: String,
        account: Account,
        transaction: Transaction,
        businessName: String,
        businessPhone: String,
        currency: String = "YER"
    ): String {
        val baseTemplate = template.ifBlank {
            "🟢 *سند قبض وإشعار سداد*\nمرحباً {CLIENT}، تم استلام دفعة مالية بقيمة {TOTAL} {CURRENCY}.\nالبيان: {DETAILS}\nالتاريخ: {DUE_DATE}\nشكراً لسدادكم - {BUSINESS}"
        }
        return generateMessage(
            template = baseTemplate,
            accountName = account.name,
            accountPhone = account.phone,
            details = transaction.details,
            total = transaction.total,
            currency = currency,
            dueDate = transaction.date,
            businessName = businessName,
            businessPhone = businessPhone,
            transactionId = transaction.id
        )
    }

    fun generateDueDateMessage(
        template: String,
        account: Account,
        transaction: Transaction,
        businessName: String,
        businessPhone: String,
        currency: String = "YER"
    ): String {
        val baseTemplate = template.ifBlank {
            "⏰ *تذكير بموعد استحقاق*\nمرحباً {CLIENT}، نود تذكيركم بموعد استحقاق الفاتورة بقيمة {TOTAL} {CURRENCY}.\nتاريخ الاستحقاق: {DUE_DATE}\nالبيان: {DETAILS}\n{BUSINESS}"
        }
        return generateMessage(
            template = baseTemplate,
            accountName = account.name,
            accountPhone = account.phone,
            details = transaction.details,
            total = transaction.total,
            currency = currency,
            dueDate = transaction.dueDate.ifBlank { transaction.date },
            businessName = businessName,
            businessPhone = businessPhone,
            transactionId = transaction.id
        )
    }

    /**
     * Manual Share Mode: Opens WhatsApp application directly
     */
    fun sendViaIntent(context: Context, phone: String, message: String) {
        try {
            val formattedPhone = formatPhoneNumber(phone)
            val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${URLEncoder.encode(message, "UTF-8")}"
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse(url)
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                // Try WhatsApp Business or system browser
                val formattedPhone = formatPhoneNumber(phone)
                val url = "https://api.whatsapp.com/send?phone=$formattedPhone&text=${URLEncoder.encode(message, "UTF-8")}"
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    data = Uri.parse(url)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ex: Exception) {
                Toast.makeText(context, "لم يتم العثور على تطبيق واتساب مثبت على هذا الجهاز.", Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * Automated API Mode: Dispatches message in the background using the configured WhatsApp Gateway.
     * Supports:
     * - Meta WhatsApp Business Cloud API (Official)
     * - UltraMsg API
     * - Evolution API / Baileys
     * - Twilio WhatsApp API
     * - Custom Webhook (JSON POST & GET)
     */
    suspend fun sendViaApi(
        gatewayType: String,
        apiUrl: String,
        token: String,
        instanceId: String,
        phone: String,
        message: String,
        defaultCountryCode: String = "967",
        metaPhoneNumberId: String = "",
        metaWabaId: String = ""
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val formattedPhone = formatPhoneNumber(phone, defaultCountryCode)
            if (formattedPhone.isBlank()) {
                return@withContext Result.failure(Exception("رقم الهاتف فارغ أو غير صالح للإرسال."))
            }

            val encodedMessage = URLEncoder.encode(message, "UTF-8")
            val selectedGateway = gatewayType.uppercase().trim()

            when (selectedGateway) {
                // 1. Official Meta WhatsApp Business Cloud API
                "META_CLOUD_API", "META", "WHATSAPP_CLOUD" -> {
                    val phoneId = metaPhoneNumberId.ifBlank { instanceId.ifBlank { "1074418605053" } }.trim()
                    if (phoneId.isBlank() || token.isBlank()) {
                        return@withContext Result.failure(Exception("يرجى إدخال Phone Number ID ومفتاح Access Token الخاص بـ Meta Cloud API."))
                    }

                    val endpoint = if (apiUrl.isNotBlank() && apiUrl.startsWith("http")) {
                        apiUrl.trimEnd('/')
                    } else {
                        "https://graph.facebook.com/v20.0/$phoneId/messages"
                    }

                    val payloadJson = JSONObject().apply {
                        put("messaging_product", "whatsapp")
                        put("recipient_type", "individual")
                        put("to", formattedPhone)
                        put("type", "text")
                        put("text", JSONObject().apply {
                            put("preview_url", false)
                            put("body", message)
                        })
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val requestBody = payloadJson.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url(endpoint)
                        .post(requestBody)
                        .addHeader("Authorization", "Bearer $token")
                        .addHeader("Content-Type", "application/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        val respBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            val msgId = try {
                                JSONObject(respBody).getJSONArray("messages").getJSONObject(0).getString("id")
                            } catch (e: Exception) {
                                "WAMID-${System.currentTimeMillis()}"
                            }
                            Result.success("✓ تم الإرسال بنجاح عبر Meta WhatsApp Cloud API! (معرف: $msgId)")
                        } else {
                            val errorDetails = try {
                                val errObj = JSONObject(respBody).getJSONObject("error")
                                val errorMsg = errObj.optString("message", "خطأ غير معروف")
                                val errorDetailsStr = errObj.optJSONObject("error_data")?.optString("details", "") ?: ""
                                val errCode = errObj.optInt("code", response.code)
                                "خطأ Meta ($errCode): $errorMsg ${if (errorDetailsStr.isNotBlank()) "($errorDetailsStr)" else ""}"
                            } catch (e: Exception) {
                                "فشل الإرسال (رمز ${response.code}): $respBody"
                            }
                            Result.failure(Exception(errorDetails))
                        }
                    }
                }

                // 2. UltraMsg Gateway
                "ULTRAMSG" -> {
                    val cleanUrl = (if (apiUrl.isNotBlank()) apiUrl else "https://api.ultramsg.com").trimEnd('/')
                    val requestUrl = if (cleanUrl.contains(instanceId)) {
                        "$cleanUrl/messages/chat"
                    } else {
                        "$cleanUrl/$instanceId/messages/chat"
                    }

                    val mediaType = "application/x-www-form-urlencoded".toMediaType()
                    val bodyString = "token=$token&to=$formattedPhone&body=${URLEncoder.encode(message, "UTF-8")}"
                    val body = bodyString.toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url(requestUrl)
                        .post(body)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val respBody = response.body?.string() ?: ""
                        if (response.isSuccessful && respBody.contains("sent")) {
                            Result.success("✓ تم الإرسال بنجاح عبر بوابة UltraMsg!")
                        } else {
                            Result.failure(Exception("استجابة غير متوقعة من بوابة UltraMsg: رمز ${response.code}، الرد: $respBody"))
                        }
                    }
                }

                // 3. Evolution API / Baileys
                "EVOLUTION_API", "EVOLUTION" -> {
                    val cleanUrl = (if (apiUrl.isNotBlank()) apiUrl else "http://localhost:8080").trimEnd('/')
                    val instance = instanceId.ifBlank { "default" }
                    val endpoint = "$cleanUrl/message/sendText/$instance"

                    val json = JSONObject().apply {
                        put("number", formattedPhone)
                        put("text", message)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toString().toRequestBody(mediaType)

                    val request = Request.Builder()
                        .url(endpoint)
                        .post(body)
                        .addHeader("apikey", token)
                        .addHeader("Content-Type", "application/json")
                        .build()

                    client.newCall(request).execute().use { response ->
                        val respBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Result.success("✓ تم الإرسال بنجاح عبر Evolution API!")
                        } else {
                            Result.failure(Exception("خطأ في Evolution API: رمز ${response.code}، $respBody"))
                        }
                    }
                }

                // 4. Twilio WhatsApp API
                "TWILIO" -> {
                    val accountSid = instanceId.ifBlank { "AC_TWILIO_SID" }
                    val endpoint = "https://api.twilio.com/2010-04-01/Accounts/$accountSid/Messages.json"
                    
                    val mediaType = "application/x-www-form-urlencoded".toMediaType()
                    val fromPhone = apiUrl.ifBlank { "+14155238886" } // Twilio WhatsApp Sandbox / sender
                    val bodyString = "To=whatsapp:${URLEncoder.encode("+$formattedPhone", "UTF-8")}&From=whatsapp:${URLEncoder.encode(fromPhone, "UTF-8")}&Body=${URLEncoder.encode(message, "UTF-8")}"
                    val body = bodyString.toRequestBody(mediaType)

                    val authCredentials = "$accountSid:$token"
                    val authHeader = "Basic " + Base64.encodeToString(authCredentials.toByteArray(), Base64.NO_WRAP)

                    val request = Request.Builder()
                        .url(endpoint)
                        .post(body)
                        .addHeader("Authorization", authHeader)
                        .build()

                    client.newCall(request).execute().use { response ->
                        val respBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Result.success("✓ تم الإرسال بنجاح عبر بوابة Twilio WhatsApp!")
                        } else {
                            Result.failure(Exception("خطأ Twilio: رمز ${response.code}، الرد: $respBody"))
                        }
                    }
                }

                // 5. Generic Custom JSON POST Webhook
                "GENERIC_POST" -> {
                    val cleanUrl = apiUrl.trim()
                    if (cleanUrl.isBlank()) {
                        return@withContext Result.failure(Exception("يرجى إدخال رابط الويب الخاص بالبوابة (Webhook URL)."))
                    }

                    val json = JSONObject().apply {
                        put("to", formattedPhone)
                        put("phone", formattedPhone)
                        put("recipient", formattedPhone)
                        put("message", message)
                        put("msg", message)
                        put("body", message)
                        put("token", token)
                    }

                    val mediaType = "application/json; charset=utf-8".toMediaType()
                    val body = json.toString().toRequestBody(mediaType)

                    val reqBuilder = Request.Builder()
                        .url(cleanUrl)
                        .post(body)
                    
                    if (token.isNotBlank()) {
                        reqBuilder.addHeader("Authorization", "Bearer $token")
                        reqBuilder.addHeader("apikey", token)
                    }

                    client.newCall(reqBuilder.build()).execute().use { response ->
                        val respBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Result.success("✓ تم إرسال الطلب بنجاح عبر بوابة الويب المخصصة (POST)!")
                        } else {
                            Result.failure(Exception("خطأ في الاتصال بالبوابة (رمز ${response.code}): $respBody"))
                        }
                    }
                }

                // 6. Generic GET Webhook
                "GENERIC_GET" -> {
                    val finalUrl = apiUrl
                        .replace("{phone}", formattedPhone)
                        .replace("{to}", formattedPhone)
                        .replace("{message}", encodedMessage)
                        .replace("{msg}", encodedMessage)
                        .replace("{token}", token)
                        .replace("{key}", token)

                    val request = Request.Builder()
                        .url(finalUrl)
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        val respBody = response.body?.string() ?: ""
                        if (response.isSuccessful) {
                            Result.success("✓ تم إرسال الطلب بنجاح عبر بوابة HTTP GET!")
                        } else {
                            Result.failure(Exception("خطأ في الاتصال بالبوابة (رمز ${response.code}): $respBody"))
                        }
                    }
                }

                else -> {
                    Result.failure(Exception("نوع بوابة الواتساب غير محدد أو غير مدعوم: $gatewayType"))
                }
            }
        } catch (e: Exception) {
            Log.e("WhatsAppHelper", "Error sending WhatsApp notification", e)
            Result.failure(Exception("خطأ في الاتصال بالشبكة: ${e.localizedMessage ?: e.message}"))
        }
    }
}
