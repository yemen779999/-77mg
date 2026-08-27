package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.Account
import com.example.data.model.DeletedAccount
import com.example.data.model.DeletedTransaction
import com.example.data.model.Transaction
import com.example.data.model.InventoryItem
import com.example.data.model.NotificationLog
import com.example.data.model.MaterialItem
import com.example.data.model.Invoice
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class LedgerViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: LedgerRepository
    private val prefs = application.getSharedPreferences("pro_ledger_prefs", Context.MODE_PRIVATE)
    private val securePrefs = com.example.utils.SecurePreferences(application)

    // --- 3D Effects & Animation Settings ---
    private val _is3DEffectsEnabled = MutableStateFlow(prefs.getBoolean("3d_effects_enabled", true))
    val is3DEffectsEnabled: StateFlow<Boolean> = _is3DEffectsEnabled.asStateFlow()

    fun update3DEffectsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("3d_effects_enabled", enabled).apply()
        _is3DEffectsEnabled.value = enabled
    }

    private val _animationLevel = MutableStateFlow(
        try {
            com.example.ui.theme.AnimationLevel.valueOf(
                prefs.getString("animation_level", com.example.ui.theme.AnimationLevel.FULL.name) ?: com.example.ui.theme.AnimationLevel.FULL.name
            )
        } catch (_: Exception) {
            com.example.ui.theme.AnimationLevel.FULL
        }
    )
    val animationLevel: StateFlow<com.example.ui.theme.AnimationLevel> = _animationLevel.asStateFlow()

    fun updateAnimationLevel(level: com.example.ui.theme.AnimationLevel) {
        prefs.edit().putString("animation_level", level.name).apply()
        _animationLevel.value = level
    }

    // --- Premium Inventory System ---
    private val _inventoryItems = MutableStateFlow<List<InventoryItem>>(listOf())
    val inventoryItems: StateFlow<List<InventoryItem>> = _inventoryItems.asStateFlow()

    // App Security Settings
    private val _isSecurityEnabled = MutableStateFlow(prefs.getBoolean("security_enabled", false))
    val isSecurityEnabled: StateFlow<Boolean> = _isSecurityEnabled.asStateFlow()

    private val _securityPin = MutableStateFlow(prefs.getString("security_pin", "") ?: "")
    val securityPin: StateFlow<String> = _securityPin.asStateFlow()

    fun verifyEnteredPin(enteredPin: String): Boolean {
        return securePrefs.verifyPin(enteredPin) || enteredPin == _securityPin.value
    }

    // Cloud Admin & Sync Settings
    private val _cloudClientId = MutableStateFlow(prefs.getString("cloud_client_id", "anas-pro-${(1000..9999).random()}") ?: "anas-pro-client-7700")
    val cloudClientId: StateFlow<String> = _cloudClientId.asStateFlow()

    private val _isCloudFrozen = MutableStateFlow(prefs.getBoolean("is_cloud_frozen", false))
    val isCloudFrozen: StateFlow<Boolean> = _isCloudFrozen.asStateFlow()

    private val _lastCloudSync = MutableStateFlow(prefs.getString("last_cloud_sync", "لم يتم المزامنة بعد ⚪") ?: "لم يتم المزامنة بعد ⚪")
    val lastCloudSync: StateFlow<String> = _lastCloudSync.asStateFlow()

    private val _cloudServerUrl = MutableStateFlow(prefs.getString("cloud_server_url", "https://anaspro-cloud-sync.mockapi.io") ?: "https://anaspro-cloud-sync.mockapi.io")
    val cloudServerUrl: StateFlow<String> = _cloudServerUrl.asStateFlow()

    private val _isCloudSyncing = MutableStateFlow(false)
    val isCloudSyncing: StateFlow<Boolean> = _isCloudSyncing.asStateFlow()

    fun updateCloudSettings(clientId: String, serverUrl: String) {
        prefs.edit().apply {
            putString("cloud_client_id", clientId)
            putString("cloud_server_url", serverUrl)
            apply()
        }
        _cloudClientId.value = clientId
        _cloudServerUrl.value = serverUrl
    }

    fun setCloudFrozen(frozen: Boolean) {
        prefs.edit().putBoolean("is_cloud_frozen", frozen).apply()
        _isCloudFrozen.value = frozen
    }

    fun syncWithCloud(onResult: (Boolean, String) -> Unit) {
        if (_isCloudSyncing.value) return
        _isCloudSyncing.value = true
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val accountsList = allAccounts.value
                val transactionsList = allTransactions.value
                
                val rootJson = JSONObject().apply {
                    put("client_id", _cloudClientId.value)
                    put("business_name", _businessName.value)
                    put("business_phone", _businessPhone.value)
                    put("backup_date", System.currentTimeMillis())
                    put("accounts_count", accountsList.size)
                    put("transactions_count", transactionsList.size)
                    
                    val accountsArray = JSONArray()
                    for (acc in accountsList) {
                        accountsArray.put(JSONObject().apply {
                            put("id", acc.id)
                            put("name", acc.name)
                            put("phone", acc.phone)
                            put("type", acc.type)
                            put("createdAt", acc.createdAt)
                            put("creditLimit", acc.creditLimit)
                            put("tag", acc.tag)
                            put("initialBalance", acc.initialBalance)
                        })
                    }
                    put("accounts", accountsArray)

                    val transactionsArray = JSONArray()
                    for (tx in transactionsList) {
                        transactionsArray.put(JSONObject().apply {
                            put("id", tx.id)
                            put("accountId", tx.accountId)
                            put("day", tx.day)
                            put("date", tx.date)
                            put("details", tx.details)
                            put("quantity", tx.quantity)
                            put("unitPrice", tx.unitPrice)
                            put("addition", tx.addition)
                            put("total", tx.total)
                            put("isPayment", tx.isPayment)
                        })
                    }
                    put("transactions", transactionsArray)
                }

                val payload = rootJson.toString()
                val urlString = _cloudServerUrl.value
                
                if (urlString.contains("mockapi.io") || urlString.contains("example.com") || urlString.isBlank()) {
                    kotlinx.coroutines.delay(1500)
                    val formatNow = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val isLockedId = _cloudClientId.value.contains("freeze", ignoreCase = true) || _cloudClientId.value.contains("تجميد", ignoreCase = true)
                    
                    _lastCloudSync.value = "$formatNow ✅ (حجم البيانات: ${payload.length} حرف)"
                    prefs.edit().putString("last_cloud_sync", _lastCloudSync.value).apply()
                    
                    setCloudFrozen(isLockedId)
                    _isCloudSyncing.value = false
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, "تمت المحاكاة السحابية بنجاح! المعرف: ${_cloudClientId.value}. حالة الحساب: ${if (isCloudFrozen.value) "مجمّد 🔴" else "نشط 🟢"}")
                    }
                    return@launch
                }

                val url = java.net.URL(urlString)
                val conn = url.openConnection() as java.net.HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.setRequestProperty("Accept", "application/json")
                conn.doOutput = true
                conn.connectTimeout = 8000
                conn.readTimeout = 8000
                
                java.io.OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(payload)
                    writer.flush()
                }

                val responseCode = conn.responseCode
                if (responseCode in 200..299) {
                    val responseText = conn.inputStream.bufferedReader().use { it.readText() }
                    val responseJson = JSONObject(responseText)
                    
                    val serverStatus = responseJson.optString("account_status", "active")
                    val isFrozen = serverStatus.equals("frozen", ignoreCase = true) || responseJson.optBoolean("is_frozen", false)
                    
                    val formatNow = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.getDefault()).format(Date())
                    _lastCloudSync.value = "$formatNow ✅ • مفعّل"
                    prefs.edit().putString("last_cloud_sync", _lastCloudSync.value).apply()
                    setCloudFrozen(isFrozen)
                    _isCloudSyncing.value = false
                    
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(true, "تم مزامنة البيانات بنجاح كود 200! المعرف: ${_cloudClientId.value}")
                    }
                } else {
                    _isCloudSyncing.value = false
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        onResult(false, "فشل خادم السحاب بالرد بكود: $responseCode")
                    }
                }
                conn.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
                _isCloudSyncing.value = false
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    onResult(false, "خطأ بالاتصال بالخادم السحابي: ${e.localizedMessage}")
                }
            }
        }
    }

    private var autoSyncJob: kotlinx.coroutines.Job? = null
    fun triggerAutoSync() {
        autoSyncJob?.cancel()
        autoSyncJob = viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            kotlinx.coroutines.delay(1500) // الانتظار لضمان اكتمال تحديث قاعدة البيانات ومنع تكرار الإرسال
            syncWithCloud { success, msg ->
                android.util.Log.d("AnasProCloudSync", "المزامنة الخلفية التلقائية: نجاح=$success، السيرفر: $msg")
            }
        }
    }

    fun updateSecuritySettings(enabled: Boolean, pin: String) {
        prefs.edit().apply {
            putBoolean("security_enabled", enabled)
            putString("security_pin", pin)
            apply()
        }
        securePrefs.saveHashedPin(pin)
        _isSecurityEnabled.value = enabled
        _securityPin.value = pin
    }

    // Business Profile Settings
    private val _businessName = MutableStateFlow(prefs.getString("business_name", "المحاسب الشامل • Pro Ledger") ?: "المحاسب الشامل • Pro Ledger")
    val businessName: StateFlow<String> = _businessName.asStateFlow()

    private val _businessPhone = MutableStateFlow(prefs.getString("business_phone", "770000000") ?: "770000000")
    val businessPhone: StateFlow<String> = _businessPhone.asStateFlow()

    private val _businessAddress = MutableStateFlow(prefs.getString("business_address", "صنعاء - اليمن") ?: "صنعاء - اليمن")
    val businessAddress: StateFlow<String> = _businessAddress.asStateFlow()

    // Multi-currency Settings
    private val _defaultCurrency = MutableStateFlow(prefs.getString("default_currency", "YER") ?: "YER")
    val defaultCurrency: StateFlow<String> = _defaultCurrency.asStateFlow()

    fun updateDefaultCurrency(currency: String) {
        prefs.edit().putString("default_currency", currency).apply()
        _defaultCurrency.value = currency
    }

    fun getDefaultExchangeRate(from: String, to: String): Double {
        if (from == to) return 1.0
        // Standard conversions (defaulting to YER base)
        if (to == "YER") {
            return when (from) {
                "USD" -> prefs.getFloat("rate_usd_yer", 1600f).toDouble()
                "SAR" -> prefs.getFloat("rate_sar_yer", 422f).toDouble()
                else -> 1.0
            }
        } else if (to == "USD") {
            return when (from) {
                "YER" -> 1.0 / prefs.getFloat("rate_usd_yer", 1600f).toDouble()
                "SAR" -> 1.0 / 3.75
                else -> 1.0
            }
        } else if (to == "SAR") {
            return when (from) {
                "YER" -> 1.0 / prefs.getFloat("rate_sar_yer", 422f).toDouble()
                "USD" -> 3.75
                else -> 1.0
            }
        }
        return 1.0
    }

    fun updateStandardRates(usdToYer: Double, sarToYer: Double) {
        prefs.edit().apply {
            putFloat("rate_usd_yer", usdToYer.toFloat())
            putFloat("rate_sar_yer", sarToYer.toFloat())
            apply()
        }
    }

    // PDF Customization Settings
    private val _pdfLogo = MutableStateFlow(prefs.getString("pdf_logo", "🏢") ?: "🏢")
    val pdfLogo: StateFlow<String> = _pdfLogo.asStateFlow()

    private val _pdfHeaderShowLogo = MutableStateFlow(prefs.getBoolean("pdf_header_show_logo", true))
    val pdfHeaderShowLogo: StateFlow<Boolean> = _pdfHeaderShowLogo.asStateFlow()

    private val _pdfHeaderCustomTitle = MutableStateFlow(prefs.getString("pdf_header_custom_title", "") ?: "")
    val pdfHeaderCustomTitle: StateFlow<String> = _pdfHeaderCustomTitle.asStateFlow()

    private val _pdfHeaderCustomSubtitle = MutableStateFlow(prefs.getString("pdf_header_custom_subtitle", "") ?: "")
    val pdfHeaderCustomSubtitle: StateFlow<String> = _pdfHeaderCustomSubtitle.asStateFlow()

    private val _pdfFooterCustomText = MutableStateFlow(prefs.getString("pdf_footer_custom_text", "تم التوليد تلقائياً بواسطة تطبيق المحاسب الشامل (Pro Ledger)") ?: "تم التوليد تلقائياً بواسطة تطبيق المحاسب الشامل (Pro Ledger)")
    val pdfFooterCustomText: StateFlow<String> = _pdfFooterCustomText.asStateFlow()

    private val _pdfShowSignature = MutableStateFlow(prefs.getBoolean("pdf_show_signature", true))
    val pdfShowSignature: StateFlow<Boolean> = _pdfShowSignature.asStateFlow()

    private val _pdfFontStyle = MutableStateFlow(prefs.getString("pdf_font_style", "DEFAULT") ?: "DEFAULT")
    val pdfFontStyle: StateFlow<String> = _pdfFontStyle.asStateFlow()

    private val _pdfFontSize = MutableStateFlow(prefs.getString("pdf_font_size", "MEDIUM") ?: "MEDIUM")
    val pdfFontSize: StateFlow<String> = _pdfFontSize.asStateFlow()

    private val _pdfThemeColor = MutableStateFlow(prefs.getString("pdf_theme_color", "SLATE") ?: "SLATE")
    val pdfThemeColor: StateFlow<String> = _pdfThemeColor.asStateFlow()

    private val _pdfColDetailsLabel = MutableStateFlow(prefs.getString("pdf_col_details_label", "التفاصيل والبيان") ?: "التفاصيل والبيان")
    val pdfColDetailsLabel: StateFlow<String> = _pdfColDetailsLabel.asStateFlow()

    private val _pdfColQtyVisible = MutableStateFlow(prefs.getBoolean("pdf_col_qty_visible", true))
    val pdfColQtyVisible: StateFlow<Boolean> = _pdfColQtyVisible.asStateFlow()

    private val _pdfColQtyLabel = MutableStateFlow(prefs.getString("pdf_col_qty_label", "الكمية") ?: "الكمية")
    val pdfColQtyLabel: StateFlow<String> = _pdfColQtyLabel.asStateFlow()

    private val _pdfColPriceVisible = MutableStateFlow(prefs.getBoolean("pdf_col_price_visible", true))
    val pdfColPriceVisible: StateFlow<Boolean> = _pdfColPriceVisible.asStateFlow()

    private val _pdfColPriceLabel = MutableStateFlow(prefs.getString("pdf_col_price_label", "السعر") ?: "السعر")
    val pdfColPriceLabel: StateFlow<String> = _pdfColPriceLabel.asStateFlow()

    private val _pdfColAdditionVisible = MutableStateFlow(prefs.getBoolean("pdf_col_addition_visible", true))
    val pdfColAdditionVisible: StateFlow<Boolean> = _pdfColAdditionVisible.asStateFlow()

    private val _pdfColAdditionLabel = MutableStateFlow(prefs.getString("pdf_col_addition_label", "الإضافي") ?: "الإضافي")
    val pdfColAdditionLabel: StateFlow<String> = _pdfColAdditionLabel.asStateFlow()

    private val _pdfColTotalVisible = MutableStateFlow(prefs.getBoolean("pdf_col_total_visible", true))
    val pdfColTotalVisible: StateFlow<Boolean> = _pdfColTotalVisible.asStateFlow()

    private val _pdfColTotalLabel = MutableStateFlow(prefs.getString("pdf_col_total_label", "الإجمالي") ?: "الإجمالي")
    val pdfColTotalLabel: StateFlow<String> = _pdfColTotalLabel.asStateFlow()

    fun updatePdfTemplateSettings(
        logo: String,
        showLogo: Boolean,
        customTitle: String,
        customSubtitle: String,
        customFooter: String,
        showSignature: Boolean,
        fontStyle: String,
        fontSize: String,
        themeColor: String,
        colDetailsLabel: String,
        colQtyVisible: Boolean,
        colQtyLabel: String,
        colPriceVisible: Boolean,
        colPriceLabel: String,
        colAdditionVisible: Boolean,
        colAdditionLabel: String,
        colTotalVisible: Boolean,
        colTotalLabel: String
    ) {
        prefs.edit().apply {
            putString("pdf_logo", logo)
            putBoolean("pdf_header_show_logo", showLogo)
            putString("pdf_header_custom_title", customTitle)
            putString("pdf_header_custom_subtitle", customSubtitle)
            putString("pdf_footer_custom_text", customFooter)
            putBoolean("pdf_show_signature", showSignature)
            putString("pdf_font_style", fontStyle)
            putString("pdf_font_size", fontSize)
            putString("pdf_theme_color", themeColor)
            putString("pdf_col_details_label", colDetailsLabel)
            putBoolean("pdf_col_qty_visible", colQtyVisible)
            putString("pdf_col_qty_label", colQtyLabel)
            putBoolean("pdf_col_price_visible", colPriceVisible)
            putString("pdf_col_price_label", colPriceLabel)
            putBoolean("pdf_col_addition_visible", colAdditionVisible)
            putString("pdf_col_addition_label", colAdditionLabel)
            putBoolean("pdf_col_total_visible", colTotalVisible)
            putString("pdf_col_total_label", colTotalLabel)
            apply()
        }
        _pdfLogo.value = logo
        _pdfHeaderShowLogo.value = showLogo
        _pdfHeaderCustomTitle.value = customTitle
        _pdfHeaderCustomSubtitle.value = customSubtitle
        _pdfFooterCustomText.value = customFooter
        _pdfShowSignature.value = showSignature
        _pdfFontStyle.value = fontStyle
        _pdfFontSize.value = fontSize
        _pdfThemeColor.value = themeColor
        _pdfColDetailsLabel.value = colDetailsLabel
        _pdfColQtyVisible.value = colQtyVisible
        _pdfColQtyLabel.value = colQtyLabel
        _pdfColPriceVisible.value = colPriceVisible
        _pdfColPriceLabel.value = colPriceLabel
        _pdfColAdditionVisible.value = colAdditionVisible
        _pdfColAdditionLabel.value = colAdditionLabel
        _pdfColTotalVisible.value = colTotalVisible
        _pdfColTotalLabel.value = colTotalLabel
    }

    fun getPdfTemplateConfig() = PdfTemplateConfig(
        logo = pdfLogo.value,
        showLogo = pdfHeaderShowLogo.value,
        customTitle = pdfHeaderCustomTitle.value,
        customSubtitle = pdfHeaderCustomSubtitle.value,
        customFooter = pdfFooterCustomText.value,
        showSignature = pdfShowSignature.value,
        fontStyle = pdfFontStyle.value,
        fontSize = pdfFontSize.value,
        themeColor = pdfThemeColor.value,
        colDetailsLabel = pdfColDetailsLabel.value,
        colQtyVisible = pdfColQtyVisible.value,
        colQtyLabel = pdfColQtyLabel.value,
        colPriceVisible = pdfColPriceVisible.value,
        colPriceLabel = pdfColPriceLabel.value,
        colAdditionVisible = pdfColAdditionVisible.value,
        colAdditionLabel = pdfColAdditionLabel.value,
        colTotalVisible = pdfColTotalVisible.value,
        colTotalLabel = pdfColTotalLabel.value
    )

    // Dynamic AI Suggestion Chips / Topics Configuration
    private val defaultAiSuggestions = listOf(
        "من هو العميل الأكثر مديونية في متجري؟",
        "قدم لي تقريراً تحليلياً شاملاً للديون مقارنة بالمستحقات",
        "اكتب لي رسالة واتساب ودية ومحترفة للتذكير بالدين لعميل",
        "اقترح علي أفكار عملية ومدروسة لزيادة حجم مبيعاتي"
    )

    private val _aiSuggestions = MutableStateFlow<List<String>>(emptyList())
    val aiSuggestions: StateFlow<List<String>> = _aiSuggestions.asStateFlow()

    private fun loadAiSuggestions() {
        val saved = prefs.getString("ai_suggestions", null)
        if (saved == null) {
            _aiSuggestions.value = defaultAiSuggestions
            saveAiSuggestionsToPrefs(defaultAiSuggestions)
        } else {
            try {
                val array = JSONArray(saved)
                val list = mutableListOf<String>()
                for (i in 0 until array.length()) {
                    list.add(array.getString(i))
                }
                _aiSuggestions.value = list
            } catch (e: Exception) {
                _aiSuggestions.value = defaultAiSuggestions
            }
        }
    }

    private fun saveAiSuggestionsToPrefs(list: List<String>) {
        val array = JSONArray()
        list.forEach { array.put(it) }
        prefs.edit().putString("ai_suggestions", array.toString()).apply()
    }

    fun addAiSuggestion(suggestion: String) {
        val updated = _aiSuggestions.value + suggestion
        _aiSuggestions.value = updated
        saveAiSuggestionsToPrefs(updated)
    }

    fun updateAiSuggestion(index: Int, newSuggestion: String) {
        val current = _aiSuggestions.value.toMutableList()
        if (index in current.indices) {
            current[index] = newSuggestion
            _aiSuggestions.value = current
            saveAiSuggestionsToPrefs(current)
        }
    }

    fun deleteAiSuggestion(index: Int) {
        val current = _aiSuggestions.value.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _aiSuggestions.value = current
            saveAiSuggestionsToPrefs(current)
        }
    }

    fun resetAiSuggestions() {
        _aiSuggestions.value = defaultAiSuggestions
        saveAiSuggestionsToPrefs(defaultAiSuggestions)
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = LedgerRepository(database.accountDao(), database.transactionDao(), database.trashDao(), database.notificationLogDao())
        autoPruneTrash()
        loadAiSuggestions()
        loadInventory()
    }

    // Streams of data
    val allAccounts: StateFlow<List<Account>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeletedAccounts: StateFlow<List<DeletedAccount>> = repository.allDeletedAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDeletedTransactions: StateFlow<List<DeletedTransaction>> = repository.allDeletedTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun restoreAccount(deletedAccount: DeletedAccount) {
        viewModelScope.launch {
            repository.restoreAccount(deletedAccount)
        }
    }

    fun restoreTransaction(deletedTransaction: DeletedTransaction) {
        viewModelScope.launch {
            repository.restoreTransaction(deletedTransaction)
        }
    }

    fun removeDeletedAccountPermanently(id: Int) {
        viewModelScope.launch {
            repository.removeDeletedAccountPermanently(id)
        }
    }

    fun removeDeletedTransactionPermanently(id: Int) {
        viewModelScope.launch {
            repository.removeDeletedTransactionPermanently(id)
        }
    }

    fun clearTrash() {
        viewModelScope.launch {
            repository.clearTrash()
        }
    }

    fun autoPruneTrash() {
        viewModelScope.launch {
            val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24L * 60L * 60L * 1000L)
            repository.pruneTrash(thirtyDaysAgo)
        }
    }

    // Combine Accounts and Transactions to compute reactive balances
    val accountsWithBalance: StateFlow<List<AccountWithBalance>> = combine(allAccounts, allTransactions) { accounts, txs ->
        accounts.map { account ->
            val accountTxs = txs.filter { it.accountId == account.id }
            val balance = account.initialBalance + calculateBalance(accountTxs)
            AccountWithBalance(account, balance, accountTxs.size)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected Account Context for Statement
    private val _selectedAccountId = MutableStateFlow<Int?>(null)
    val selectedAccountId: StateFlow<Int?> = _selectedAccountId.asStateFlow()

    val selectedAccount: StateFlow<Account?> = _selectedAccountId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.getAccountById(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedAccountTransactions: StateFlow<List<Transaction>> = _selectedAccountId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.getTransactionsForAccountAsc(id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectAccount(accountId: Int?) {
        _selectedAccountId.value = accountId
    }

    // Business Logic calculations
    private fun calculateBalance(txs: List<Transaction>): Double {
        val charges = txs.filter { !it.isPayment }.sumOf { it.total * it.exchangeRate }
        val payments = txs.filter { it.isPayment }.sumOf { it.total * it.exchangeRate }
        return charges - payments
    }

    // CRUD For Accounts
    fun createAccount(name: String, phone: String, type: String, creditLimit: Double = 0.0, tag: String = "", initialBalance: Double = 0.0, onFinished: (Int) -> Unit = {}) {
        viewModelScope.launch {
            val account = Account(name = name, phone = phone, type = type, creditLimit = creditLimit, tag = tag, initialBalance = initialBalance)
            val newId = repository.insertAccount(account)
            onFinished(newId.toInt())
            triggerAutoSync()
        }
    }

    fun updateAccount(account: Account) {
        viewModelScope.launch {
            repository.updateAccount(account)
            triggerAutoSync()
        }
    }

    fun deleteAccount(account: Account) {
        viewModelScope.launch {
            repository.deleteAccount(account)
            if (_selectedAccountId.value == account.id) {
                _selectedAccountId.value = null
            }
            triggerAutoSync()
        }
    }

    // CRUD For Transactions
    fun addTransaction(
        accountId: Int,
        details: String,
        quantity: Double,
        unitPrice: Double,
        addition: Double,
        isPayment: Boolean,
        customDateString: String? = null,
        currency: String = "YER",
        exchangeRate: Double = 1.0,
        dueDate: String = ""
    ) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayFormat = SimpleDateFormat("EEEE", Locale("ar")) // Arabic day name
            val date = customDateString ?: sdf.format(Date())
            
            // Map day of week
            val parsedDate = if (customDateString != null) sdf.parse(customDateString) ?: Date() else Date()
            val dayName = dayFormat.format(parsedDate)

            val total = if (isPayment) unitPrice else (quantity * unitPrice) + addition
            val transaction = Transaction(
                accountId = accountId,
                day = dayName,
                date = date,
                details = details,
                quantity = if (isPayment) 1.0 else quantity,
                unitPrice = unitPrice,
                addition = if (isPayment) 0.0 else addition,
                total = total,
                isPayment = isPayment,
                currency = currency,
                exchangeRate = exchangeRate,
                dueDate = dueDate
            )
            repository.insertTransaction(transaction)
            triggerAutoSync()
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            triggerAutoSync()
        }
    }

    fun updateTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            triggerAutoSync()
        }
    }

    fun deleteTransactionById(txId: Int) {
        viewModelScope.launch {
            repository.deleteTransactionById(txId)
            triggerAutoSync()
        }
    }

    // Save profile settings
    fun updateBusinessProfile(name: String, phone: String, address: String) {
        prefs.edit().apply {
            putString("business_name", name)
            putString("business_phone", phone)
            putString("business_address", address)
            apply()
        }
        _businessName.value = name
        _businessPhone.value = phone
        _businessAddress.value = address
        triggerAutoSync()
    }

    // Database JSON backup export
    fun exportDatabaseJson(onCompleted: (File?) -> Unit) {
        viewModelScope.launch {
            try {
                val accountsList = allAccounts.value
                val transactionsList = allTransactions.value

                val rootJson = JSONObject().apply {
                    put("backup_version", 1)
                    put("backup_date", System.currentTimeMillis())
                    put("business_name", businessName.value)
                    put("business_phone", businessPhone.value)
                    put("business_address", businessAddress.value)

                    val accountsArray = JSONArray()
                    for (acc in accountsList) {
                        val accJson = JSONObject().apply {
                            put("id", acc.id)
                            put("name", acc.name)
                            put("phone", acc.phone)
                            put("type", acc.type)
                            put("createdAt", acc.createdAt)
                            put("creditLimit", acc.creditLimit)
                            put("tag", acc.tag)
                            put("initialBalance", acc.initialBalance)
                        }
                        accountsArray.put(accJson)
                    }
                    put("accounts", accountsArray)

                    val transactionsArray = JSONArray()
                    for (tx in transactionsList) {
                        val txJson = JSONObject().apply {
                            put("id", tx.id)
                            put("accountId", tx.accountId)
                            put("day", tx.day)
                            put("date", tx.date)
                            put("details", tx.details)
                            put("quantity", tx.quantity)
                            put("unitPrice", tx.unitPrice)
                            put("addition", tx.addition)
                            put("total", tx.total)
                            put("isPayment", tx.isPayment)
                            put("timestamp", tx.timestamp)
                            put("currency", tx.currency)
                            put("exchangeRate", tx.exchangeRate)
                        }
                        transactionsArray.put(txJson)
                    }
                    put("transactions", transactionsArray)
                }

                val backupDir = getApplication<Application>().filesDir
                val file = File(backupDir, "ProLedger_Backup_${System.currentTimeMillis() / 1000}.json")
                val fos = FileOutputStream(file)
                fos.write(rootJson.toString(2).toByteArray())
                fos.close()

                onCompleted(file)
            } catch (e: Exception) {
                e.printStackTrace()
                onCompleted(null)
            }
        }
    }

    // Database JSON backup restore
    fun restoreDatabaseJson(jsonString: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                val rootJson = JSONObject(jsonString)
                val profileName = rootJson.optString("business_name", businessName.value)
                val profilePhone = rootJson.optString("business_phone", businessPhone.value)
                val profileAddress = rootJson.optString("business_address", businessAddress.value)

                updateBusinessProfile(profileName, profilePhone, profileAddress)

                val accountsJsonArray = rootJson.getJSONArray("accounts")
                val transactionsJsonArray = rootJson.getJSONArray("transactions")

                // Map old Account ID to new Account ID to prevent conflicts and ensure consistent FK
                val accountIdMapping = mutableMapOf<Int, Int>()

                // Restore Accounts
                for (i in 0 until accountsJsonArray.length()) {
                    val accObj = accountsJsonArray.getJSONObject(i)
                    val oldId = accObj.getInt("id")
                    val name = accObj.getString("name")
                    val phone = accObj.getString("phone")
                    val type = accObj.getString("type")
                    val createdAt = accObj.optLong("createdAt", System.currentTimeMillis())
                    val creditLimit = accObj.optDouble("creditLimit", 0.0)
                    val tag = accObj.optString("tag", "")
                    val initialBalance = accObj.optDouble("initialBalance", 0.0)

                    val account = Account(
                        name = name,
                        phone = phone,
                        type = type,
                        createdAt = createdAt,
                        creditLimit = creditLimit,
                        tag = tag,
                        initialBalance = initialBalance
                    )
                    val newId = repository.insertAccount(account).toInt()
                    accountIdMapping[oldId] = newId
                }

                // Restore Transactions
                for (i in 0 until transactionsJsonArray.length()) {
                    val txObj = transactionsJsonArray.getJSONObject(i)
                    val oldAccountId = txObj.getInt("accountId")
                    val newAccountId = accountIdMapping[oldAccountId]

                    if (newAccountId != null) {
                        val day = txObj.getString("day")
                        val date = txObj.getString("date")
                        val details = txObj.getString("details")
                        val quantity = txObj.getDouble("quantity")
                        val unitPrice = txObj.getDouble("unitPrice")
                        val addition = txObj.getDouble("addition")
                        val total = txObj.getDouble("total")
                        val isPayment = txObj.getBoolean("isPayment")
                        val timestamp = txObj.optLong("timestamp", System.currentTimeMillis())

                        val transaction = Transaction(
                            accountId = newAccountId,
                            day = day,
                            date = date,
                            details = details,
                            quantity = quantity,
                            unitPrice = unitPrice,
                            addition = addition,
                            total = total,
                            isPayment = isPayment,
                            timestamp = timestamp,
                            currency = txObj.optString("currency", "YER"),
                            exchangeRate = txObj.optDouble("exchangeRate", 1.0)
                        )
                        repository.insertTransaction(transaction)
                    }
                }

                onCompleted(true)
            } catch (e: Exception) {
                e.printStackTrace()
                onCompleted(false)
            }
        }
    }

    // Windows Local Sync Server and Manager
    private var syncServer: com.example.utils.WindowsSyncServer? = null

    private val _isServerRunning = MutableStateFlow(false)
    val isServerRunning: StateFlow<Boolean> = _isServerRunning.asStateFlow()

    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    fun toggleSyncServer(enabled: Boolean) {
        if (enabled) {
            if (syncServer == null) {
                syncServer = com.example.utils.WindowsSyncServer(getApplication(), this)
            }
            syncServer?.start { running, url ->
                _isServerRunning.value = running
                _serverUrl.value = url
            }
        } else {
            syncServer?.stop { running ->
                _isServerRunning.value = running
                _serverUrl.value = ""
            }
        }
    }

    private val geminiRepository = com.example.data.repository.GeminiRepository()

    suspend fun parseSmartEntry(prompt: String, isOfflineMode: Boolean): Result<String> {
        return geminiRepository.parseSmartEntryTransaction(
            prompt = prompt,
            accounts = allAccounts.value,
            isOfflineMode = isOfflineMode
        )
    }

    fun loadInventory() {
        val json = prefs.getString("inventory_items", "[]") ?: "[]"
        try {
            val list = mutableListOf<InventoryItem>()
            val array = JSONArray(json)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    InventoryItem(
                        id = obj.getString("id"),
                        barcode = obj.getString("barcode"),
                        name = obj.getString("name"),
                        purchasePrice = obj.optDouble("purchasePrice", 0.0),
                        salePrice = obj.optDouble("salePrice", 0.0),
                        stockQuantity = obj.optDouble("stockQuantity", 0.0),
                        unit = obj.optString("unit", "حبة")
                    )
                )
            }
            _inventoryItems.value = list
        } catch (e: Exception) {
            e.printStackTrace()
            _inventoryItems.value = listOf()
        }
    }

    fun saveInventory(list: List<InventoryItem>) {
        val array = JSONArray()
        list.forEach { item ->
            val obj = JSONObject().apply {
                put("id", item.id)
                put("barcode", item.barcode)
                put("name", item.name)
                put("purchasePrice", item.purchasePrice)
                put("salePrice", item.salePrice)
                put("stockQuantity", item.stockQuantity)
                put("unit", item.unit)
            }
            array.put(obj)
        }
        prefs.edit().putString("inventory_items", array.toString()).apply()
        _inventoryItems.value = list
    }

    fun addInventoryItem(name: String, barcode: String, purchasePrice: Double, salePrice: Double, quantity: Double, unit: String) {
        val current = _inventoryItems.value.toMutableList()
        val existingIndex = current.indexOfFirst { it.barcode == barcode && barcode.isNotBlank() }
        if (existingIndex != -1) {
            val item = current[existingIndex]
            current[existingIndex] = item.copy(
                name = name,
                purchasePrice = purchasePrice,
                salePrice = salePrice,
                stockQuantity = item.stockQuantity + quantity,
                unit = unit
            )
        } else {
            current.add(InventoryItem(barcode = barcode, name = name, purchasePrice = purchasePrice, salePrice = salePrice, stockQuantity = quantity, unit = unit))
        }
        saveInventory(current)
    }

    fun updateInventoryItem(updated: InventoryItem) {
        val current = _inventoryItems.value.map { if (it.id == updated.id) updated else it }
        saveInventory(current)
    }

    fun deleteInventoryItem(id: String) {
        val current = _inventoryItems.value.filter { it.id != id }
        saveInventory(current)
    }

    fun sellItemFromStock(barcode: String, qty: Double) {
        val current = _inventoryItems.value.toMutableList()
        val index = current.indexOfFirst { it.barcode == barcode }
        if (index != -1) {
            val item = current[index]
            val newQty = (item.stockQuantity - qty).coerceAtLeast(0.0)
            current[index] = item.copy(stockQuantity = newQty)
            saveInventory(current)
        }
    }

    // --- Biometric Authentication Settings ---
    private val _isBiometricEnabled = MutableStateFlow(prefs.getBoolean("biometric_enabled", false))
    val isBiometricEnabled: StateFlow<Boolean> = _isBiometricEnabled.asStateFlow()

    fun updateBiometricSettings(enabled: Boolean) {
        prefs.edit().putBoolean("biometric_enabled", enabled).apply()
        _isBiometricEnabled.value = enabled
    }

    // --- Yemeni Quick Message Broadcasts ---
    fun getYemeniMessageTemplates(clientName: String, balance: Double, currency: String): List<String> {
        val formattedBal = String.format(Locale.US, "%,.2f", Math.abs(balance))
        val direction = if (balance >= 0) "مطلوب منكم سداد" else "لكم طرفنا رصيد دائن"
        val currLabel = when (currency) {
            "USD" -> "دولار"
            "SAR" -> "سعودي"
            "YER" -> "ريال يمني"
            else -> currency
        }
        val bizName = _businessName.value
        val bizPhone = _businessPhone.value
        
        return listOf(
            "عشاق ومستهلكي $bizName نود إخطاركم عميلنا المحترم [$clientName] بأن حسابكم الحالي هو ($formattedBal) $currLabel ($direction). شكراً لتعاملكم الراقي وثقتكم الدائمة بنا.",
            "إشعار مالي هام من [$bizName]: الأخ [$clientName]، يرجى الفحص والمراجعة؛ رصيد حسابكم لدينا حالياً هو ($formattedBal) $currLabel. للمراجعة أو الاستفسار اتصل بنا ($bizPhone).",
            "مرحباً [$clientName]، بموجب المطابقة الحسابية السنوية لشركة [$bizName]، يفيدكم نظامنا المحاسبي الآلي بأن رصيدكم المرحل هو ($formattedBal) $currLabel. دمتم ذخرًا لنا."
        )
    }

    // --- Dynamic Annual Fiscal Closing & Carryover ---
    fun performYearlyClosing(selectedClosingDate: String, onCompleted: (String) -> Unit) {
        viewModelScope.launch {
            val currentAccountsWithBalances = accountsWithBalance.value
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val dayFormat = SimpleDateFormat("EEEE", Locale("ar"))
            val todayStr = sdf.format(Date())
            val todayDayName = dayFormat.format(Date())

            for (accBal in currentAccountsWithBalances) {
                val accId = accBal.account.id
                val finalNetBal = accBal.balance

                repository.deleteTransactionsForAccount(accId)

                if (finalNetBal != 0.0) {
                    val openingTx = Transaction(
                        accountId = accId,
                        day = todayDayName,
                        date = selectedClosingDate.ifBlank { todayStr },
                        details = "📦 [رصيد مرحل وإقفال سنوي تلقائي للعام الجديد] لغاية $selectedClosingDate",
                        quantity = 1.0,
                        unitPrice = Math.abs(finalNetBal),
                        addition = 0.0,
                        total = Math.abs(finalNetBal),
                        isPayment = finalNetBal < 0,
                        currency = _defaultCurrency.value,
                        exchangeRate = 1.0,
                        dueDate = ""
                    )
                    repository.insertTransaction(openingTx)
                }
            }
            triggerAutoSync()
            onCompleted("تمت عملية الإقفال الحسابي السنوي بنجاح! تم تصفية كافة القيود والترحيل بأرصدة افتتاحية جديدة لـ ${currentAccountsWithBalances.size} حساب.")
        }
    }

    // --- User Roles & Client Portal ---
    private val _currentUserRole = MutableStateFlow(prefs.getString("current_user_role", "ADMIN") ?: "ADMIN")
    val currentUserRole: StateFlow<String> = _currentUserRole.asStateFlow()

    fun updateCurrentUserRole(role: String) {
        prefs.edit().putString("current_user_role", role).apply()
        _currentUserRole.value = role
    }

    private val _clientAccountId = MutableStateFlow<Int?>(if (prefs.contains("client_account_id")) prefs.getInt("client_account_id", -1).takeIf { it != -1 } else null)
    val clientAccountId: StateFlow<Int?> = _clientAccountId.asStateFlow()

    fun updateClientAccountId(id: Int?) {
        if (id != null) {
            prefs.edit().putInt("client_account_id", id).apply()
        } else {
            prefs.edit().remove("client_account_id").apply()
        }
        _clientAccountId.value = id
    }

    // --- Cloud Materials and Invoices ---
    val cloudMaterials: StateFlow<List<MaterialItem>> = _inventoryItems.map { list ->
        list.map {
            MaterialItem(
                id = it.id,
                name = it.name,
                count = it.stockQuantity.toInt(),
                unitPrice = it.salePrice,
                total = it.stockQuantity * it.salePrice
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudInvoices: StateFlow<List<Invoice>> = combine(allTransactions, allAccounts) { txs, accs ->
        val accMap = accs.associateBy { it.id }
        txs.filter { !it.isPayment }.take(50).map { tx ->
            Invoice(
                invoiceId = "INV-${tx.id}",
                clientName = accMap[tx.accountId]?.name ?: "عميل رقم ${tx.accountId}",
                count = tx.quantity.toInt().coerceAtLeast(1),
                unitPrice = tx.unitPrice,
                total = tx.total,
                date = tx.timestamp
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- WhatsApp Gateway & Automation Settings ---
    val allNotificationLogs: StateFlow<List<NotificationLog>> = repository.allNotificationLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _whatsappGatewayType = MutableStateFlow(prefs.getString("wa_gateway_type", "META_CLOUD_API") ?: "META_CLOUD_API")
    val whatsappGatewayType: StateFlow<String> = _whatsappGatewayType.asStateFlow()

    private val _whatsappApiUrl = MutableStateFlow(prefs.getString("wa_api_url", "") ?: "")
    val whatsappApiUrl: StateFlow<String> = _whatsappApiUrl.asStateFlow()

    private val _whatsappToken = MutableStateFlow(prefs.getString("wa_token", "") ?: "")
    val whatsappToken: StateFlow<String> = _whatsappToken.asStateFlow()

    private val _whatsappInstanceId = MutableStateFlow(prefs.getString("wa_instance_id", "") ?: "")
    val whatsappInstanceId: StateFlow<String> = _whatsappInstanceId.asStateFlow()

    private val _whatsappMetaPhoneNumberId = MutableStateFlow(prefs.getString("wa_meta_phone_id", "") ?: "")
    val whatsappMetaPhoneNumberId: StateFlow<String> = _whatsappMetaPhoneNumberId.asStateFlow()

    private val _whatsappMetaWabaId = MutableStateFlow(prefs.getString("wa_meta_waba_id", "") ?: "")
    val whatsappMetaWabaId: StateFlow<String> = _whatsappMetaWabaId.asStateFlow()

    private val _whatsappDefaultCountryCode = MutableStateFlow(prefs.getString("wa_country_code", "967") ?: "967")
    val whatsappDefaultCountryCode: StateFlow<String> = _whatsappDefaultCountryCode.asStateFlow()

    private val _whatsappPaymentInstructions = MutableStateFlow(prefs.getString("wa_payment_instructions", "يرجى التحويل إلى حساب الكريمي: 123456789 أو الحضور للمحل.") ?: "يرجى التحويل إلى حساب الكريمي: 123456789 أو الحضور للمحل.")
    val whatsappPaymentInstructions: StateFlow<String> = _whatsappPaymentInstructions.asStateFlow()

    private val _whatsappAutoSendInvoice = MutableStateFlow(prefs.getBoolean("wa_auto_send_invoice", false))
    val whatsappAutoSendInvoice: StateFlow<Boolean> = _whatsappAutoSendInvoice.asStateFlow()

    private val _whatsappAutoSendPayment = MutableStateFlow(prefs.getBoolean("wa_auto_send_payment", false))
    val whatsappAutoSendPayment: StateFlow<Boolean> = _whatsappAutoSendPayment.asStateFlow()

    private val _whatsappAutoSendDueDate = MutableStateFlow(prefs.getBoolean("wa_auto_send_due_date", false))
    val whatsappAutoSendDueDate: StateFlow<Boolean> = _whatsappAutoSendDueDate.asStateFlow()

    private val _whatsappTemplateInvoiceSummary = MutableStateFlow(prefs.getString("wa_tmpl_summary", "") ?: "")
    val whatsappTemplateInvoiceSummary: StateFlow<String> = _whatsappTemplateInvoiceSummary.asStateFlow()

    private val _whatsappTemplatePaymentReminder = MutableStateFlow(prefs.getString("wa_tmpl_reminder", "") ?: "")
    val whatsappTemplatePaymentReminder: StateFlow<String> = _whatsappTemplatePaymentReminder.asStateFlow()

    private val _whatsappTemplateInvoice = MutableStateFlow(prefs.getString("wa_tmpl_invoice", "") ?: "")
    val whatsappTemplateInvoice: StateFlow<String> = _whatsappTemplateInvoice.asStateFlow()

    private val _whatsappTemplateDueDate = MutableStateFlow(prefs.getString("wa_tmpl_due_date", "") ?: "")
    val whatsappTemplateDueDate: StateFlow<String> = _whatsappTemplateDueDate.asStateFlow()

    private val _whatsappTemplatePayment = MutableStateFlow(prefs.getString("wa_tmpl_payment", "") ?: "")
    val whatsappTemplatePayment: StateFlow<String> = _whatsappTemplatePayment.asStateFlow()

    fun updateWhatsAppSettings(
        gatewayType: String,
        apiUrl: String,
        token: String,
        instanceId: String,
        metaPhoneNumberId: String,
        metaWabaId: String,
        countryCode: String,
        paymentInstructions: String,
        autoSendInvoice: Boolean,
        autoSendPayment: Boolean,
        autoSendDueDate: Boolean,
        templateInvoiceSummary: String,
        templatePaymentReminder: String,
        templateInvoice: String,
        templateDueDate: String,
        templatePayment: String
    ) {
        prefs.edit().apply {
            putString("wa_gateway_type", gatewayType)
            putString("wa_api_url", apiUrl)
            putString("wa_token", token)
            putString("wa_instance_id", instanceId)
            putString("wa_meta_phone_id", metaPhoneNumberId)
            putString("wa_meta_waba_id", metaWabaId)
            putString("wa_country_code", countryCode)
            putString("wa_payment_instructions", paymentInstructions)
            putBoolean("wa_auto_send_invoice", autoSendInvoice)
            putBoolean("wa_auto_send_payment", autoSendPayment)
            putBoolean("wa_auto_send_due_date", autoSendDueDate)
            putString("wa_tmpl_summary", templateInvoiceSummary)
            putString("wa_tmpl_reminder", templatePaymentReminder)
            putString("wa_tmpl_invoice", templateInvoice)
            putString("wa_tmpl_due_date", templateDueDate)
            putString("wa_tmpl_payment", templatePayment)
            apply()
        }
        _whatsappGatewayType.value = gatewayType
        _whatsappApiUrl.value = apiUrl
        _whatsappToken.value = token
        _whatsappInstanceId.value = instanceId
        _whatsappMetaPhoneNumberId.value = metaPhoneNumberId
        _whatsappMetaWabaId.value = metaWabaId
        _whatsappDefaultCountryCode.value = countryCode
        _whatsappPaymentInstructions.value = paymentInstructions
        _whatsappAutoSendInvoice.value = autoSendInvoice
        _whatsappAutoSendPayment.value = autoSendPayment
        _whatsappAutoSendDueDate.value = autoSendDueDate
        _whatsappTemplateInvoiceSummary.value = templateInvoiceSummary
        _whatsappTemplatePaymentReminder.value = templatePaymentReminder
        _whatsappTemplateInvoice.value = templateInvoice
        _whatsappTemplateDueDate.value = templateDueDate
        _whatsappTemplatePayment.value = templatePayment
    }

    fun deleteNotificationLog(log: NotificationLog) {
        viewModelScope.launch {
            repository.deleteNotificationLog(log)
        }
    }

    fun clearAllNotificationLogs() {
        viewModelScope.launch {
            repository.clearNotificationLogs()
        }
    }

    fun sendPaymentReminder(
        context: Context,
        account: Account,
        balance: Double,
        onApiComplete: (Boolean, String) -> Unit
    ) {
        val message = com.example.utils.WhatsAppHelper.generatePaymentReminderMessage(
            template = _whatsappTemplatePaymentReminder.value,
            account = account,
            totalBalance = balance,
            businessName = _businessName.value,
            businessPhone = _businessPhone.value,
            currency = _defaultCurrency.value,
            paymentInstructions = _whatsappPaymentInstructions.value
        )
        sendCustomNotification(
            context = context,
            account = account,
            message = message,
            type = "PAYMENT_REMINDER",
            onApiComplete = onApiComplete
        )
    }

    fun sendInvoiceSummary(
        context: Context,
        account: Account,
        transactions: List<Transaction> = emptyList(),
        onApiComplete: (Boolean, String) -> Unit
    ) {
        val txs = if (transactions.isNotEmpty()) transactions else allTransactions.value.filter { it.accountId == account.id }
        val message = com.example.utils.WhatsAppHelper.generateInvoiceSummaryMessage(
            template = _whatsappTemplateInvoiceSummary.value,
            account = account,
            transactions = txs,
            businessName = _businessName.value,
            businessPhone = _businessPhone.value,
            currency = _defaultCurrency.value,
            paymentInstructions = _whatsappPaymentInstructions.value
        )
        sendCustomNotification(
            context = context,
            phone = account.phone,
            clientName = account.name,
            message = message,
            type = "INVOICE_SUMMARY",
            accountId = account.id,
            onApiComplete = onApiComplete
        )
    }

    fun sendWhatsAppTransactionNotification(
        context: Context,
        account: Account,
        transaction: Transaction,
        onApiComplete: (Boolean, String) -> Unit
    ) {
        val isPayment = transaction.isPayment
        val template = if (isPayment) _whatsappTemplatePayment.value else _whatsappTemplateInvoice.value
        val msgType = if (isPayment) "PAYMENT_RECEIPT" else "INVOICE_RECEIPT"
        
        val message = if (isPayment) {
            com.example.utils.WhatsAppHelper.generatePaymentReceiptMessage(
                template = template,
                account = account,
                transaction = transaction,
                businessName = _businessName.value,
                businessPhone = _businessPhone.value,
                currency = transaction.currency
            )
        } else {
            com.example.utils.WhatsAppHelper.generateInvoiceReceiptMessage(
                template = template,
                account = account,
                transaction = transaction,
                businessName = _businessName.value,
                businessPhone = _businessPhone.value,
                currency = transaction.currency
            )
        }
        
        sendCustomNotification(
            context = context,
            account = account,
            message = message,
            type = msgType,
            onApiComplete = onApiComplete
        )
    }

    fun sendWhatsAppDueDateNotification(
        context: Context,
        account: Account,
        transaction: Transaction,
        onApiComplete: (Boolean, String) -> Unit
    ) {
        val message = com.example.utils.WhatsAppHelper.generateDueDateMessage(
            template = _whatsappTemplateDueDate.value,
            account = account,
            transaction = transaction,
            businessName = _businessName.value,
            businessPhone = _businessPhone.value,
            currency = transaction.currency
        )
        sendCustomNotification(
            context = context,
            account = account,
            message = message,
            type = "DUE_DATE_REMINDER",
            onApiComplete = onApiComplete
        )
    }

    fun sendCustomNotification(
        context: Context,
        phone: String,
        clientName: String,
        message: String,
        type: String = "CUSTOM",
        accountId: Int = 0,
        onApiComplete: (Boolean, String) -> Unit
    ) {
        val gateway = _whatsappGatewayType.value
        if (gateway == "INTENT") {
            com.example.utils.WhatsAppHelper.sendViaIntent(context, phone, message)
            viewModelScope.launch {
                repository.insertNotificationLog(
                    NotificationLog(
                        accountId = accountId,
                        clientName = clientName,
                        clientPhone = phone,
                        type = type,
                        message = message,
                        status = "SENT_INTENT",
                        gateway = "INTENT",
                        responseMsg = "تم فتح تطبيق واتساب للمشاركة المباشرة"
                    )
                )
            }
            onApiComplete(true, "تم فتح تطبيق واتساب للمشاركة")
        } else {
            viewModelScope.launch {
                val result = com.example.utils.WhatsAppHelper.sendViaApi(
                    gatewayType = gateway,
                    apiUrl = _whatsappApiUrl.value,
                    token = _whatsappToken.value,
                    instanceId = _whatsappInstanceId.value,
                    phone = phone,
                    message = message,
                    defaultCountryCode = _whatsappDefaultCountryCode.value,
                    metaPhoneNumberId = _whatsappMetaPhoneNumberId.value,
                    metaWabaId = _whatsappMetaWabaId.value
                )
                val success = result.isSuccess
                val responseText = result.getOrNull() ?: (result.exceptionOrNull()?.message ?: "خطأ غير معروف")
                
                repository.insertNotificationLog(
                    NotificationLog(
                        accountId = accountId,
                        clientName = clientName,
                        clientPhone = phone,
                        type = type,
                        message = message,
                        status = if (success) "SUCCESS" else "FAILED",
                        gateway = gateway,
                        responseMsg = responseText
                    )
                )
                
                onApiComplete(success, responseText)
            }
        }
    }

    fun sendCustomNotification(
        context: Context,
        account: Account,
        message: String,
        type: String = "CUSTOM",
        onApiComplete: (Boolean, String) -> Unit
    ) {
        sendCustomNotification(
            context = context,
            phone = account.phone,
            clientName = account.name,
            message = message,
            type = type,
            accountId = account.id,
            onApiComplete = onApiComplete
        )
    }

    fun sendBatchPaymentReminders(
        context: Context,
        accountsWithBalance: List<AccountWithBalance>,
        onProgress: (Int, Int, String) -> Unit,
        onComplete: (Int, Int, String) -> Unit
    ) {
        viewModelScope.launch {
            var successCount = 0
            var failCount = 0
            val total = accountsWithBalance.size
            
            for (i in accountsWithBalance.indices) {
                val item = accountsWithBalance[i]
                val account = item.account
                val balance = item.balance
                onProgress(i + 1, total, account.name)
                
                val message = com.example.utils.WhatsAppHelper.generatePaymentReminderMessage(
                    template = _whatsappTemplatePaymentReminder.value,
                    account = account,
                    totalBalance = balance,
                    businessName = _businessName.value,
                    businessPhone = _businessPhone.value,
                    currency = _defaultCurrency.value,
                    paymentInstructions = _whatsappPaymentInstructions.value
                )
                
                val gateway = _whatsappGatewayType.value
                if (gateway == "INTENT") {
                    com.example.utils.WhatsAppHelper.sendViaIntent(context, account.phone, message)
                    successCount++
                    repository.insertNotificationLog(
                        NotificationLog(
                            accountId = account.id,
                            clientName = account.name,
                            clientPhone = account.phone,
                            type = "BATCH_REMINDER",
                            message = message,
                            status = "SENT_INTENT",
                            gateway = "INTENT",
                            responseMsg = "تم فتح تطبيق واتساب"
                        )
                    )
                } else {
                    val result = com.example.utils.WhatsAppHelper.sendViaApi(
                        gatewayType = gateway,
                        apiUrl = _whatsappApiUrl.value,
                        token = _whatsappToken.value,
                        instanceId = _whatsappInstanceId.value,
                        phone = account.phone,
                        message = message,
                        defaultCountryCode = _whatsappDefaultCountryCode.value,
                        metaPhoneNumberId = _whatsappMetaPhoneNumberId.value,
                        metaWabaId = _whatsappMetaWabaId.value
                    )
                    if (result.isSuccess) {
                        successCount++
                    } else {
                        failCount++
                    }
                    repository.insertNotificationLog(
                        NotificationLog(
                            accountId = account.id,
                            clientName = account.name,
                            clientPhone = account.phone,
                            type = "BATCH_REMINDER",
                            message = message,
                            status = if (result.isSuccess) "SUCCESS" else "FAILED",
                            gateway = gateway,
                            responseMsg = result.getOrNull() ?: (result.exceptionOrNull()?.message ?: "")
                        )
                    )
                }
                kotlinx.coroutines.delay(600)
            }
            val summary = "اكتمل الإرسال: ناجح $successCount من إجمالي $total (فشل $failCount)"
            onComplete(successCount, failCount, summary)
        }
    }

    // ==========================================
    // JSON EXPORT & IMPORT BACKUP SYSTEM
    // ==========================================

    fun exportDatabaseToJson(
        context: Context,
        onResult: (Boolean, String, File?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val accs = allAccounts.value
                val txs = allTransactions.value
                val invs = _inventoryItems.value
                val mats = emptyList<com.example.data.model.MaterialItem>()
                val invoices = emptyList<com.example.data.model.Invoice>()

                val jsonContent = com.example.utils.JsonBackupHelper.createBackupJson(
                    businessName = _businessName.value,
                    businessPhone = _businessPhone.value,
                    defaultCurrency = _defaultCurrency.value,
                    accounts = accs,
                    transactions = txs,
                    inventoryItems = invs,
                    materials = mats,
                    invoices = invoices
                )

                val file = com.example.utils.JsonBackupHelper.saveJsonBackupFile(context, jsonContent)
                if (file != null && file.exists()) {
                    onResult(true, "تم تصدير النسخة الاحتياطية بنجاح (${accs.size} حساب، ${txs.size} حركة)", file)
                } else {
                    onResult(false, "تعذر حفظ ملف النسخة الاحتياطية", null)
                }
            } catch (e: Exception) {
                onResult(false, "خطأ أثناء تصدير JSON: ${e.localizedMessage}", null)
            }
        }
    }

    fun shareJsonBackup(context: Context, file: File) {
        com.example.utils.JsonBackupHelper.shareJsonBackup(context, file)
    }

    fun importDatabaseFromJson(
        context: Context,
        uri: android.net.Uri,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val jsonString = com.example.utils.JsonBackupHelper.readJsonFromUri(context, uri)
                if (jsonString.isNullOrBlank()) {
                    onResult(false, "تعذر قراءة ملف JSON أو الملف فارغ")
                    return@launch
                }

                val backupData = com.example.utils.JsonBackupHelper.parseJsonBackup(jsonString)
                if (backupData == null) {
                    onResult(false, "صيغة ملف JSON غير صالحة أو غير متوافقة")
                    return@launch
                }

                val summary = com.example.utils.JsonBackupHelper.restoreDatabaseFromJson(repository, backupData)
                onResult(true, summary.message)
            } catch (e: Exception) {
                onResult(false, "خطأ أثناء استيراد البيانات: ${e.localizedMessage}")
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        syncServer?.stop { }
    }
}

data class AccountWithBalance(
    val account: Account,
    val balance: Double,
    val transactionCount: Int
)

data class PdfTemplateConfig(
    val logo: String = "🏢",
    val showLogo: Boolean = true,
    val customTitle: String = "",
    val customSubtitle: String = "",
    val customFooter: String = "تم التوليد تلقائياً بواسطة تطبيق المحاسب anas برو (Pro Ledger)",
    val showSignature: Boolean = true,
    val fontStyle: String = "DEFAULT", // DEFAULT, MONOSPACE, SANS_SERIF, SERIF
    val fontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE
    val themeColor: String = "SLATE", // SLATE, NAVY, EMERALD, BURGUNDY, GOLDEN
    val colDetailsLabel: String = "التفاصيل والبيان",
    val colQtyVisible: Boolean = true,
    val colQtyLabel: String = "الكمية",
    val colPriceVisible: Boolean = true,
    val colPriceLabel: String = "السعر",
    val colAdditionVisible: Boolean = true,
    val colAdditionLabel: String = "الإضافي",
    val colTotalVisible: Boolean = true,
    val colTotalLabel: String = "الإجمالي"
)
