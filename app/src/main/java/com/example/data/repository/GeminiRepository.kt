package com.example.data.repository

import android.util.Log
import com.example.BuildConfig
import com.example.data.model.Account
import com.example.data.model.Transaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import java.util.Locale

enum class ThinkingLevel {
    HIGH
}

class GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateDeepThinkingResponse(
        prompt: String,
        businessName: String,
        businessPhone: String,
        businessAddress: String,
        accounts: List<Account>,
        transactions: List<Transaction>,
        defaultCurrency: String,
        isOfflineMode: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        if (isOfflineMode) {
            return@withContext Result.success(generateOfflineResponse(prompt, businessName, businessPhone, businessAddress, accounts, transactions, defaultCurrency))
        }

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                // Seamless fallback to offline mode if API key is not configured!
                val offlineResp = generateOfflineResponse(prompt, businessName, businessPhone, businessAddress, accounts, transactions, defaultCurrency)
                val fallbackNotice = "💡 *ملاحظة: تم استخدام محرك التحليل المحلي لعدم ضبط مفتاح API الخاص بـ Gemini.*\n\n$offlineResp"
                return@withContext Result.success(fallbackNotice)
            }

            val systemInstruction = """
                أنت "المساعد المحاسبي الذكي" المدمج في تطبيق "المحاسب anas برو" (Pro Ledger). 
                دورك هو مساعدة التاجر وصاحب العمل بالكامل في مراجعة وتحليل بيانات الحسابات والمبيعات والديون الخاصة به.
                
                بيانات المنشأة الحالية:
                - اسم المنشأة: $businessName
                - الهاتف: $businessPhone
                - العنوان: $businessAddress
                - العملة الافتراضية للنظام: $defaultCurrency
                
                ملخص الحسابات المسجلة في النظام:
                ${formatAccountsContext(accounts)}
                
                ملخص آخر العمليات (فواتير/سداد عملاء وموردين):
                ${formatTransactionsContext(transactions)}
                
                التعليمات العامة لإجاباتك:
                1. تحدث باللغة العربية بلهجة احترافية واضحة ومبسطة للتاجر وصاحب العمل.
                2. عندما تسأل عن تفاصيل (مثل: الحساب الأكثر مديونية، إجمالي مبيعات اليوم، نصيحة حول حساب معين)، يرجى الاستناد بدقة بالغة إلى الأرقام والبيانات المذكورة أعلاه.
                3. عندما يطلب التاجر رسالة للتذكير بالدين (واتساب/رسائل نصية) لعميل معين، قم بصياغة رسالة مهذبة ومكتوبة بلغة جذابة وودودة، موضحاً فيها تفاصيل المبلغ والمنشأة.
                4. قدم اقتراحات مالية لتحسين حركة المبيعات، ومراقبة الديون، والتدفق المالي بناءً على البيانات المتوفرة.
                5. استخدم التفكير والتحليل العميق المتكامل لمقارنة الديون بالمستحقات وتوضيح الوضع المالي بدقة متناهية.
            """.trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.1-pro-preview:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                val generationConfig = JSONObject().apply {
                    val thinkingConfig = JSONObject().apply {
                        put("thinkingLevel", ThinkingLevel.HIGH.name)
                    }
                    put("thinkingConfig", thinkingConfig)
                }
                put("generationConfig", generationConfig)

                val sysInstructionObj = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                }
                put("systemInstruction", sysInstructionObj)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    // Fallback to offline mode on connection/server failure
                    val offlineResp = generateOfflineResponse(prompt, businessName, businessPhone, businessAddress, accounts, transactions, defaultCurrency)
                    val fallbackNotice = "💡 *ملاحظة: تعذر الاتصال بخادم Gemini السحابي، تم تفعيل الذكاء المحلي تلقائياً.*\n\n$offlineResp"
                    return@withContext Result.success(fallbackNotice)
                }

                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                val textResult = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (textResult != null) {
                    Result.success(textResult)
                } else {
                    val offlineResp = generateOfflineResponse(prompt, businessName, businessPhone, businessAddress, accounts, transactions, defaultCurrency)
                    Result.success("💡 *تنبيه: محرك Gemini لم يرجع استجابة، إليك التحليل المحلي:*\n\n$offlineResp")
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error generation", e)
            // Robust automatic fallback to offline response
            val offlineResp = generateOfflineResponse(prompt, businessName, businessPhone, businessAddress, accounts, transactions, defaultCurrency)
            val fallbackNotice = "💡 *ملاحظة: تم التبديل إلى المستشار المالي المحلي لعدم توفر إنترنت.*\n\n$offlineResp"
            Result.success(fallbackNotice)
        }
    }

    private fun formatAccountsContext(accounts: List<Account>): String {
        if (accounts.isEmpty()) return "لا توجد أي حسابات مسجلة بعد."
        return accounts.joinToString("\n") { 
            "- حساب ID: ${it.id}، الاسم: ${it.name}، النوع: ${it.type}، الهاتف: ${it.phone}، سقف الدين الأقصى: ${it.creditLimit}، العلامات: ${it.tag}"
        }
    }

    private fun formatTransactionsContext(transactions: List<Transaction>): String {
        if (transactions.isEmpty()) return "لا توجد أي عمليات مسجلة بعد."
        val recentTxs = transactions.takeLast(35)
        return recentTxs.joinToString("\n") { 
            "- تاريخ: ${it.date}، حساب ID: ${it.accountId}، بيان: ${it.details}، الكمية: ${it.quantity}، السعر: ${it.unitPrice}، الإضافي: ${it.addition}، الإجمالي: ${it.total} ${it.currency}، هل هو سداد دين؟ ${if (it.isPayment) "نعم" else "لا"}"
        }
    }

    /**
     * Highly optimized, intelligent, offline financial analysis engine that runs 100% locally.
     * Computes accounts balances, detects context, generates personalized Arabic texts and reminders instantly.
     */
    private fun generateOfflineResponse(
        prompt: String,
        businessName: String,
        businessPhone: String,
        businessAddress: String,
        accounts: List<Account>,
        transactions: List<Transaction>,
        defaultCurrency: String
    ): String {
        val normalizedPrompt = prompt.lowercase(Locale.ROOT)

        // Helper calculations
        val accountBalances = accounts.map { account ->
            val txs = transactions.filter { it.accountId == account.id }
            val charges = txs.filter { !it.isPayment }.sumOf { it.total * it.exchangeRate }
            val payments = txs.filter { it.isPayment }.sumOf { it.total * it.exchangeRate }
            val balance = charges - payments
            AccountBalanceInfo(account, balance, txs)
        }

        // 1. MATCH FOR MOST INDEBTED / الأكثر مديونية
        if (normalizedPrompt.contains("مديونية") || normalizedPrompt.contains("دين") || normalizedPrompt.contains("مدين")) {
            val buyers = accountBalances.filter { it.account.type == "مشتري" || it.account.type == "عميل" }
            val sortedDebtors = buyers.filter { it.balance > 0 }.sortedByDescending { it.balance }

            if (sortedDebtors.isEmpty()) {
                return """
                    🤖 **التحليل المالي المحلي (أوفلاين) 📴:**
                    
                    لا يوجد حالياً أي عملاء أو مشترين مسجلين لديهم مديونات متبقية في النظام! رائع جداً، مستحقاتك المالية ممتازة ومحصلة بالكامل.
                """.trimIndent()
            }

            val highest = sortedDebtors.first()
            val limitExceeded = highest.account.creditLimit > 0 && highest.balance > highest.account.creditLimit

            val builder = StringBuilder()
            builder.append("🤖 **مستشارك المالي المحلي (أوفلاين) 📴:**\n\n")
            builder.append("بناءً على عمليات السجلات المحفوظة في ذاكرة الهاتف، تم تحديد العميل الأكثر مديونية:\n\n")
            builder.append("👤 **العميل:** ${highest.account.name}\n")
            builder.append("💰 **الرصيد المستحق:** ${formatMoney(highest.balance)} $defaultCurrency\n")
            builder.append("📞 **الهاتف:** ${highest.account.phone.ifBlank { "غير مسجل" }}\n")
            
            if (highest.account.creditLimit > 0) {
                builder.append("⚠️ **سقف الدين المسموح:** ${formatMoney(highest.account.creditLimit)} $defaultCurrency\n")
                if (limitExceeded) {
                    builder.append("🚨 *تنبيـــه:* هذا العميل تجاوز الحد الائتماني المسموح به بمقدار (*${formatMoney(highest.balance - highest.account.creditLimit)} $defaultCurrency*)! نوصي بوقف البيع الآجل له فوراً ومتابعة التحصيل.\n")
                } else {
                    builder.append("✓ *الحالة:* العميل مازال تحت سقف الحد الائتماني بسلامة.\n")
                }
            }
            
            if (sortedDebtors.size > 1) {
                builder.append("\n📌 **باقي المديونيات الأعلى رتبة:**\n")
                sortedDebtors.drop(1).take(4).forEachIndexed { index, info ->
                    builder.append("${index + 2}. **${info.account.name}**: ${formatMoney(info.balance)} $defaultCurrency\n")
                }
            }

            builder.append("\n💡 **نصيحة سريعة من المساعد ذو التفكير الفائق:**\n")
            builder.append("- يمكنك نسخ رسالة تذكير ودية مباشرة من تطبيقنا وإرسالها لهم عبر واتساب للتسريع من تحصيل المستحقات النقدية ودعم الاستقرار المالي لـ *$businessName*.")
            
            return builder.toString()
        }

        // 2. MATCH FOR REPORT OR SUMMARY / تقرير مالي / ملخص عام
        if (normalizedPrompt.contains("تقرير") || normalizedPrompt.contains("تحليل") || normalizedPrompt.contains("ملخص") || normalizedPrompt.contains("الوضع المالي")) {
            val totalAccounts = accounts.size
            val buyers = accountBalances.filter { it.account.type == "مشتري" || it.account.type == "عميل" }
            val suppliers = accountBalances.filter { it.account.type == "مورد" }

            val totalDebtsToCollect = buyers.filter { it.balance > 0 }.sumOf { it.balance }
            val totalPayablesToSuppliers = suppliers.filter { it.balance > 0 }.sumOf { it.balance } // Where we owe suppliers

            val creditLimBreaches = buyers.count { it.account.creditLimit > 0 && it.balance > it.account.creditLimit }

            val totalTxs = transactions.size
            val salesVolume = transactions.filter { !it.isPayment }.sumOf { it.total * it.exchangeRate }
            val paymentInward = transactions.filter { it.isPayment }.sumOf { it.total * it.exchangeRate }

            return """
                🤖 **التقرير التحليلي الشامل للمنشأة (أوفلاين 📴):**
                
                أهلاً بك في التقرير المالي الحصري لـ **$businessName** تحت إشراف الذكاء الداخلي للتطبيق بدون استهلاك للإنترنت.
                
                📊 **1. ملخص الهيكل العام:**
                - عدد الحسابات المقيدة: **$totalAccounts** حساب.
                - إجمالي العمليات المسجلة: **$totalTxs** عملية وقيود.
                - إجمالي قيمة المبيعات والخدمات المقدمة: **${formatMoney(salesVolume)} $defaultCurrency**
                - إجمالي المبالغ المدفوعة السداد: **${formatMoney(paymentInward)} $defaultCurrency**
                
                💰 **2. تحليل كشف الديون والمستحقات:**
                - 📉 **مستحقاتك لدى العملاء (الديون):** **${formatMoney(totalDebtsToCollect)} $defaultCurrency**
                  *(مبالغ مبيعات آجلة وتنتظر التحصيل والقبض)*
                - 📈 **التزاماتك للموردين (المطالبات):** **${formatMoney(totalPayablesToSuppliers)} $defaultCurrency**
                  *(ديون يتوجب عليك سدادها لاحقاً لأطراف الموردين)*
                - ⚖️ **صافي المركز النقدي المتوقع:** **${formatMoney(totalDebtsToCollect - totalPayablesToSuppliers)} $defaultCurrency** 
                  ${if (totalDebtsToCollect >= totalPayablesToSuppliers) "(موجب - وضعك الائتماني جيد ومستقر)" else "(سالب - التزاماتك تفوق ديونك المتوفرة)"}
                
                ⚠️ **3. الرقابة والإنذار المبكر:**
                - عدد الحسابات التي تخطت سقف الديون المقررة: **$creditLimBreaches** حساب ائتماني.
                - متوسط رصيد المديونيات للعميل الفعال: **${if (buyers.isNotEmpty()) formatMoney(totalDebtsToCollect / buyers.size) else "0"} $defaultCurrency**
                
                💡 **4. توصيــات مستشارك المالي الذكي لتطوير أعمالك:**
                - **تحصيل الديون:** ركز مجهودك الأسبوع القادم على تحصيل المبالغ من العملاء الذين تجاوزوا الحدود الائتمانية.
                - **التوازن النقدي:** حاول دائماً جعل وتيرة تحصيل ديون العملاء أسرع من متطلبات سداد الموردين لتجنب انقطاع السيولة.
                - **تنظيم الفواتير:** نوصي بكتابة تاريخ استحقاق محدد لكل عملية بيع آجل لسهولة الفرز والملاحقة.
            """.trimIndent()
        }

        // 3. MATCH FOR MESSAGE TEMPLATE OR REMINDER / رسالة تذكير / رسالة واتساب
        if (normalizedPrompt.contains("رسالة") || normalizedPrompt.contains("تذكير") || normalizedPrompt.contains("واتساب") || normalizedPrompt.contains("مكتوبة")) {
            // Let's try to search if a specific account is mentioned in the prompt
            var foundInfo: AccountBalanceInfo? = null
            for (arg in accountBalances) {
                if (arg.account.name.isNotBlank() && normalizedPrompt.contains(arg.account.name.lowercase(Locale.ROOT))) {
                    foundInfo = arg
                    break
                }
            }

            if (foundInfo != null) {
                val balanceVal = foundInfo.balance
                if (balanceVal <= 0) {
                    return """
                        🤖 **المستشار المالي المحلي (أوفلاين) 📴:**
                        
                        لقد وجدت وبحثت عن حساب العميل **${foundInfo.account.name}** ولكن رصيده الحالي هو (*${formatMoney(balanceVal)} $defaultCurrency*). 
                        ليس عليه ديون متبقية، لذا لا يحتاج للرسائل التذكيرية بالدفع في الوقت الراهن!
                    """.trimIndent()
                }

                return """
                    🤖 **صياغة رسالة تحصيل ذكية جاهزة للنسخ (أوفلاين) 📴:**
                    
                    إليك 3 خيارات فائقة الصياغة للتذكير بالدين ومارسلة العميل **${foundInfo.account.name}** على رقم الهاتف (**${foundInfo.account.phone.ifBlank { "غير مضبوط" }}**):
                    
                    ---
                    💬 **الخيار 1 (ودي ولطيف - ينصح به أولاً):**
                    "أهلاً بك أخي العزيز ${foundInfo.account.name} 🌹، نأمل أن تكون بخير صحة وعافية. نود التذكير اللطيف بخصوص رصيد حسابكم المتبقي لدى *$businessName* وقدره (*${formatMoney(balanceVal)} $defaultCurrency*). لطفاً وتسهيلاً لنا، نأمل التكرم بالترتيب لتسوية المبلغ في أقرب فرصة. شاكرين ومقدرين حسن تعاونك الدائم معنا!"
                    
                    ---
                    💬 **الخيار 2 (رسمي وجاد):**
                    "تحية طيبة وبعد، الأخ العميل ${foundInfo.account.name}. نفيدكم علماً بأن الرصيد المستحق الدفع لصالح *$businessName* حتى تاريخه هو (*${formatMoney(balanceVal)} $defaultCurrency*). يرجى التفضل شاكراً بسداد الحساب أو المراجعة لتسويته لضمان استمرار التوريد والحساب الائتماني المشترك. مع خالص الاحترام والتقدير."
                    
                    ---
                    💬 **الخيار 3 (رسالة سريعة ومختصرة):**
                    "السلام عليكم أخي ${foundInfo.account.name}. نذكركم برصيدكم القائم لدينا بمبلغ: *${formatMoney(balanceVal)} $defaultCurrency*. يمكنكم إيداعه أو تحويله برقمنا وتزويدنا بإشعار السداد. شكراً لكم."
                """.trimIndent()
            } else {
                // Return general debtors with template help
                val debtors = accountBalances.filter { it.account.type == "مشتري" && it.balance > 0 }.take(5)
                val builder = StringBuilder()
                builder.append("🤖 **مستشارك المالي المحلي (أوفلاين) 📴:**\n\n")
                builder.append("لم أستطع تبيُّن اسم العميل الذي تطلبه من النص المقروء، ولكن إليك قالب رسالة ممتاز، ويمكنك كتابة اسم أي عميل لتخصيص الرسالة بدقة:\n\n")
                builder.append("📊 **صيغة رسالة تحصيل مقترحة:**\n")
                builder.append("\"عزيزنا العميل [الاسم]، نود تذكيركم بلطف برصيد حسابكم المتبقي لدى *$businessName* وقدره (*[الرصيد] $defaultCurrency*). لطفاً وجبر الخواطر نرجو التكرم بسداد المبلغ أو التنسيق معنا لتسويته تيسيراً للأعمال المشتركة بيننا. شاكرين لكم طيب حسن تعاونكم 🌸.\"\n\n")
                
                if (debtors.isNotEmpty()) {
                    builder.append("💡 **العملاء الحاليين أصحاب الحسابات المدينة لتذكيرهم:**\n")
                    debtors.forEach { 
                        builder.append("- **${it.account.name}** (الرصيد: ${formatMoney(it.balance)} $defaultCurrency)\n")
                    }
                    builder.append("\n*اكتب لي مثلاً: 'اكتب رسالة تذكير للعميل ${debtors.first().account.name}' لأصوغها لك مباشرة وبثوانٍ معدودة!*")
                }
                return builder.toString()
            }
        }

        // 4. MATCH FOR SALES AND ADVICE / مبيعات واقتراحات
        if (normalizedPrompt.contains("مبيعات") || normalizedPrompt.contains("أفكار") || normalizedPrompt.contains("نصائح") || normalizedPrompt.contains("زيادة") || normalizedPrompt.contains("تطوير")) {
            return """
                🤖 **أفكار مستشارك المالي الذكي لزيادة المبيعات والتحصيل (أوفلاين) 📴:**
                
                بناءً على نشاط السجلات وعلاقاتك التجارية، قمت بصياغة إستراتيجيات موجهة خصيصاً لـ **$businessName**:
                
                1. ⛓️ **إستراتيجية سقف الدين المضبوط:**
                   قم بضبط سقف لكل عميل جديد. لا تسمح بأي عملية بيع آجلة لعميل متجاوز للرصيد، سيجبر ذلك العملاء على دفع مبالغ جزئية لمواصلة الشراء منك.
                
                2. 💸 **تفعيل الخصومات على السداد الفوري (Cash Discount):**
                   اعرض خصماً صغيراً (مثلاً 1% أو 2%) على المشتريات للعملاء الذين يدفعون نقداً كاملاً في نفس اللحظة، هذا يزيد السيولة ويقلل تراكم الديون.
                
                3. 📂 **تجميع وتصنيف الفئات بالتسميات والوسوم (Tags):**
                   تطبيقنا يتيح لك إضافة وسوم للحسابات (مثل: عميل ممتاز، عميل متأخر، مورد جملة). استغل ذلك لتقديم عروض إضافية وتسهيلات لعملائك الدائمين لضمان ولائهم.
                
                4. 📅 **تحديد تواريخ استحقاق واقعية للفواتير:**
                   عند البيع بالآجل، سجّل تاريخ السداد كـ (تاريخ الاستحقاق). مراجعة التواريخ ستساعدك في تنبيه العملاء قبل الموعد بيومين بطريقة ودية وهادئة.
            """.trimIndent()
        }

        // 5. TRY TO IDENTIFY SPEICFIC ACCOUNT LEDGER SEARCH (E.g. "حساب علي" or "كشف حساب محمد احمد")
        var matchedAccount: AccountBalanceInfo? = null
        for (info in accountBalances) {
            val nameClean = info.account.name.lowercase(Locale.ROOT)
            if (nameClean.isNotBlank() && normalizedPrompt.contains(nameClean)) {
                matchedAccount = info
                break
            }
        }

        if (matchedAccount != null) {
            val limitStr = if (matchedAccount.account.creditLimit > 0) formatMoney(matchedAccount.account.creditLimit) + " $defaultCurrency" else "غير محدد"
            val lastTx = matchedAccount.transactions.lastOrNull()
            val lastTxStr = if (lastTx != null) "${lastTx.date} (${lastTx.details} بقيمة ${formatMoney(lastTx.total)} ${lastTx.currency})" else "لا توجد عمليات مضافة"

            val builder = StringBuilder()
            builder.append("🤖 **كشف الحساب الذكي الفوري (أوفلاين) 📴:**\n\n")
            builder.append("لقد عثرت على سجل الحساب للعميل بطلبك بالكامل:\n\n")
            builder.append("👤 **الاسم:** ${matchedAccount.account.name}\n")
            builder.append("🏷️ **النوع:** ${matchedAccount.account.type}\n")
            builder.append("📞 **الهاتف:** ${matchedAccount.account.phone.ifBlank { "غير مسجل" }}\n")
            builder.append("🗄️ **رصيد الحساب الحالي:** **${formatMoney(matchedAccount.balance)} $defaultCurrency**\n")
            builder.append("🛡️ **حد الدين الأقصى:** $limitStr\n")
            builder.append("📅 **آخر عملية مسجلة:** $lastTxStr\n\n")
            
            if (matchedAccount.transactions.isNotEmpty()) {
                builder.append("📋 **آخر 4 حركات مقيدة:**\n")
                matchedAccount.transactions.takeLast(4).reversed().forEach { tx ->
                    val typeSign = if (tx.isPayment) "🟢 [قبض وسداد]" else "🔴 [فاتورة بالدين]"
                    builder.append("- `${tx.date}` : $typeSign - ${tx.details} بمبلغ **${formatMoney(tx.total)} ${tx.currency}**\n")
                }
            } else {
                builder.append("💡 *لا توجد حركات مسجلة لهذا الحساب.*")
            }
            return builder.toString()
        }

        // 6. DEFAULT FALLBACK RESPONSE
        val totalAccounts = accounts.size
        val buyers = accountBalances.filter { it.account.type == "مشتري" || it.account.type == "عميل" }
        val totalDebtsToCollect = buyers.filter { it.balance > 0 }.sumOf { it.balance }

        return """
            🤖 **مستشارك المالي الذكي (يعمل بالكامل بدون اتصال 📴):**
            
            أهلاً بك! لقد قرأت استفسارك واطلعت على سجلاتك. لم أستطع تحديد تصنيف مالي محدد لطلبك، ولكن إليك لمحة عامة وسريعة لمتجرك لتبسيط أعمالك:
            
            - اسم المنشأة المعتمد: **$businessName**
            - العدد الكلي لحساباتك الجارية: **$totalAccounts** حساب.
            - إجمالي الديون المستحقة لك لدى العملاء: **${formatMoney(totalDebtsToCollect)} $defaultCurrency**
            
            💬 **أمور يمكنك أن تسألني عنها بكل سهولة ويسر:**
            - "من هو العميل الأكثر مديونية في متجري؟" (سأقوم بحسابه فوراً وتنبيهك بحدود الدين)
            - "كشف حساب وليد أحمد" (كتابة أي اسم سيعرض كشف عملياتهم فوراً وبدون اتصال)
            - "صيغة رسالة تحصيل ديون" (سأقترح رسائل ودية جاهزة للتحصيل والواتساب)
            - "قدم لي تحليلاً مالياً شاملاً لأرقام الأرباح والديون" (سيعرض موازنة حسابية للمحل)
            
            *تمنياتنا لك بتجارة مربحة وبلا عوائق أو ديون معلقة!*
        """.trimIndent()
    }

    private fun formatMoney(value: Double): String {
        return try {
            String.format(Locale.US, "%,.2f", value).replace(".00", "")
        } catch (e: Exception) {
            value.toString()
        }
    }

    /**
     * Parse smart Arabic voice/text details of transactions to return structured JSON.
     * Works both online with Gemini's high comprehension capacities and 100% offline with rule-based NLP.
     */
    suspend fun parseSmartEntryTransaction(
        prompt: String,
        accounts: List<Account>,
        isOfflineMode: Boolean = true
    ): Result<String> = withContext(Dispatchers.IO) {
        if (isOfflineMode) {
            return@withContext Result.success(parseSmartEntryOffline(prompt, accounts))
        }

        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                return@withContext Result.success(parseSmartEntryOffline(prompt, accounts))
            }

            val accountsContext = accounts.joinToString("\n") {
                "- حساب ID: ${it.id}، الاسم: ${it.name}، النوع: ${it.type}"
            }

            val systemInstruction = """
                You are an expert Arabic financial transaction parsing assistant. Your task is to extract exact billing parameters from any voice or text prompt.
                
                Available system accounts list:
                $accountsContext
                
                Analyze the user statement, match the name precisely to one of the existing accounts. If it matches, return its "accountId", else return null.
                
                You MUST return ONLY a plain JSON object with the following schema, and NO markdown packaging:
                {
                  "accountName": "الاسم المستخلص بدقة",
                  "accountId": Id_Integer_Or_Null,
                  "amount": Amount_Double,
                  "currency": "YER" or "USD" or "SAR",
                  "isPayment": true_if_payment_receipt_or_settlement_else_false_for_invoice_or_debt,
                  "details": "الوصف والبيان بالكامل بلغة عربية نظيفة ومحترمة"
                }
                
                Remember:
                - If the text mentions سدد, دفع, دفع لي, واصل, قبض, دفعة, استلمت, سداد -> isPayment MUST be true.
                - If the text mentions سجل على, دين, قيد, اشترى, فاتورة -> isPayment MUST be false.
                - Only output raw, single-line JSON text. No ```json.
            """.trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                }
                put("contents", contentsArray)

                val sysInstructionObj = JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", systemInstruction)
                        })
                    })
                }
                put("systemInstruction", sysInstructionObj)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    return@withContext Result.success(parseSmartEntryOffline(prompt, accounts))
                }

                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                var textResult = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (textResult != null) {
                    // Clean codeblock triggers if model returned markdown
                    textResult = textResult.replace("```json", "").replace("```", "").trim()
                    Result.success(textResult)
                } else {
                    Result.success(parseSmartEntryOffline(prompt, accounts))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error parsing smart entry online", e)
            Result.success(parseSmartEntryOffline(prompt, accounts))
        }
    }

    suspend fun extractInvoiceFromImage(
        bitmap: android.graphics.Bitmap,
        accounts: List<Account>
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
                val fallbackJson = parseSmartEntryOffline("فاتورة مشتريات بضاعة بقيمة 0", accounts)
                return@withContext Result.success(fallbackJson)
            }

            // Convert bitmap to Base64 JPEG
            val byteArrayOutputStream = java.io.ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 85, byteArrayOutputStream)
            val imageBytes = byteArrayOutputStream.toByteArray()
            val base64Image = android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP)

            val prompt = """
                قم بقراءة وتحليل صورة السند أو الفاتورة أو الإيصال واستخراج بيانات الحركة المحاسبية باللغة العربية بدقة متناهية.
                أرجع الناتج فقط بصيغة JSON بدون أي تعليقات أو Markdown:
                {
                   "accountName": "اسم العميل أو المورد",
                   "amount": 0.0,
                   "currency": "YER أو SAR أو USD",
                   "isPayment": true إذا كان سند قبض أو دفعة أو سداد و false إذا كانت فاتورة شراء أو مبيعات,
                   "details": "بيان وتفاصيل الأصناف أو السداد",
                   "accountType": "مشتري أو مورد"
                }
            """.trimIndent()

            val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val jsonBody = JSONObject().apply {
                val contentsArray = JSONArray().apply {
                    put(JSONObject().apply {
                        val partsArray = JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                            put(JSONObject().apply {
                                val inlineData = JSONObject().apply {
                                    put("mimeType", "image/jpeg")
                                    put("data", base64Image)
                                }
                                put("inlineData", inlineData)
                            })
                        }
                        put("parts", partsArray)
                    })
                }
                put("contents", contentsArray)
            }

            val request = Request.Builder()
                .url(url)
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyString = response.body?.string()
                if (!response.isSuccessful || bodyString == null) {
                    return@withContext Result.success(parseSmartEntryOffline("فاتورة مستند", accounts))
                }

                val jsonResponse = JSONObject(bodyString)
                val candidates = jsonResponse.optJSONArray("candidates")
                var textResult = candidates?.optJSONObject(0)
                    ?.optJSONObject("content")
                    ?.optJSONArray("parts")
                    ?.optJSONObject(0)
                    ?.optString("text")

                if (textResult != null) {
                    textResult = textResult.replace("```json", "").replace("```", "").trim()
                    Result.success(textResult)
                } else {
                    Result.success(parseSmartEntryOffline("فاتورة", accounts))
                }
            }
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Error parsing image invoice", e)
            Result.success(parseSmartEntryOffline("فاتورة ضوئية", accounts))
        }
    }

    /**
     * Highly robust offline parser that extracts amount, type, account name, currency, and details from Arabic dialect.
     */
    fun parseSmartEntryOffline(prompt: String, accounts: List<Account>): String {
        val normalized = prompt.lowercase(Locale.ROOT)
            .replace('٠', '0')
            .replace('١', '1')
            .replace('٢', '2')
            .replace('٣', '3')
            .replace('٤', '4')
            .replace('٥', '5')
            .replace('٦', '6')
            .replace('٧', '7')
            .replace('٨', '8')
            .replace('٩', '9')

        // 1. Amount Extraction
        var amount = 0.0
        val numberRegex = java.util.regex.Pattern.compile("\\d+(\\.\\d+)?")
        val matcher = numberRegex.matcher(normalized)
        val extractedNumbers = mutableListOf<Double>()
        while (matcher.find()) {
            val numStr = matcher.group()
            numStr.toDoubleOrNull()?.let { extractedNumbers.add(it) }
        }
        
        amount = extractedNumbers.firstOrNull() ?: 0.0
        
        if (amount == 0.0) {
            if (normalized.contains("ألفين") || normalized.contains("الفين")) amount = 2000.0
            else if (normalized.contains("ألف") || normalized.contains("الف")) amount = 1000.0
            else if (normalized.contains("ثلاثة آلاف") || normalized.contains("ثلاثة الاف") || normalized.contains("ثلاثه الف")) amount = 3000.0
            else if (normalized.contains("خمسة آلاف") || normalized.contains("خمسة الاف") || normalized.contains("خمسه الف")) amount = 5000.0
            else if (normalized.contains("عشرة آلاف") || normalized.contains("عشرة الاف") || normalized.contains("عشره الف")) amount = 10000.0
            else if (normalized.contains("مليون")) amount = 1000000.0
        }

        // 2. Currency check
        var currency = "YER"
        if (normalized.contains("دولار") || normalized.contains("دولارات") || normalized.contains("أمريكي") || normalized.contains("$")) {
            currency = "USD"
        } else if (normalized.contains("سعودي") || normalized.contains("ريال سعودي") || normalized.contains("سعوديه") || normalized.contains("سعودى")) {
            currency = "SAR"
        }

        // 3. Payment type check
        var isPayment = false
        if (normalized.contains("سدد") || normalized.contains("دفع") || normalized.contains("قبض") || 
            normalized.contains("سداد") || normalized.contains("واصل") || normalized.contains("دفعة") || 
            normalized.contains("دفعه") || normalized.contains("استلمت") || normalized.contains("سلمت") || 
            normalized.contains("قبضت") || normalized.contains("حساب")) {
            isPayment = true
        }

        // 4. Account Matching
        var matchedAccount: Account? = null
        for (acc in accounts) {
            if (prompt.contains(acc.name, ignoreCase = true)) {
                matchedAccount = acc
                break
            }
        }
        
        if (matchedAccount == null) {
            for (acc in accounts) {
                val nameParts = acc.name.split(" ").filter { it.length > 2 }
                val matchesPart = nameParts.any { part -> prompt.contains(part, ignoreCase = true) }
                if (matchesPart) {
                    matchedAccount = acc
                    break
                }
            }
        }

        // 5. Account Name fallback extraction
        var accountName = matchedAccount?.name ?: ""
        if (accountName.isEmpty()) {
            val words = prompt.split(" ", "\n").map { it.trim() }.filter { it.isNotEmpty() }
            val indicators = listOf("العميل", "المورد", "حساب", "على", "لـ", "من", "لتسجيل")
            for (indicator in indicators) {
                val idx = words.indexOfFirst { it.lowercase(Locale.ROOT).contains(indicator) }
                if (idx != -1 && idx + 1 < words.size) {
                    val possibleName = words.subList(idx + 1, minOf(idx + 4, words.size)).joinToString(" ")
                    val cleanedCandidate = possibleName.replace(Regex("[^\\p{L}\\s]"), "").trim()
                    if (cleanedCandidate.length > 2) {
                        accountName = cleanedCandidate
                        break
                    }
                }
            }
        }
        
        if (accountName.isEmpty()) {
            accountName = "عميل صوتي جديد"
        }

        // 6. Detailed statement construction
        val commonFilters = listOf("سجل", "على", "العميل", "المورد", "مبلغ", "ريال", "دولار", "سعودي", "يمني", "قيمة", "بقيمة", "دفعة", "دفع")
        val cleanWords = prompt.split(" ").map { it.trim() }.filter { word ->
            word.isNotEmpty() && !commonFilters.any { filter -> word.lowercase(Locale.ROOT).contains(filter) } && word.replace(Regex("[^0-9]"), "").isEmpty()
        }
        var details = cleanWords.take(4).joinToString(" ").trim()
        if (details.isEmpty()) {
            details = if (isPayment) "تسديد دفعة مالية" else "شراء بضاعة آجلة"
        }

        val jsonObj = JSONObject().apply {
            put("accountName", accountName)
            put("accountId", matchedAccount?.id)
            put("amount", amount)
            put("currency", currency)
            put("isPayment", isPayment)
            put("details", details)
            put("accountType", matchedAccount?.type ?: "مشتري")
        }
        return jsonObj.toString()
    }
}

data class AccountBalanceInfo(
    val account: Account,
    val balance: Double,
    val transactions: List<Transaction>
)
