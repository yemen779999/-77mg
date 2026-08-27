package com.example.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.model.*
import com.example.data.repository.LedgerRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

data class JsonBackupData(
    val exportDate: String,
    val businessName: String,
    val businessPhone: String,
    val defaultCurrency: String,
    val accounts: List<Account>,
    val transactions: List<Transaction>,
    val inventoryItems: List<InventoryItem>,
    val materials: List<MaterialItem>,
    val invoices: List<Invoice>
)

data class JsonImportSummary(
    val accountsCount: Int,
    val transactionsCount: Int,
    val inventoryCount: Int,
    val materialsCount: Int,
    val invoicesCount: Int,
    val message: String
)

object JsonBackupHelper {

    /**
     * Converts current app database records into a clean, formatted JSON string.
     */
    fun createBackupJson(
        businessName: String,
        businessPhone: String,
        defaultCurrency: String,
        accounts: List<Account>,
        transactions: List<Transaction>,
        inventoryItems: List<InventoryItem> = emptyList(),
        materials: List<MaterialItem> = emptyList(),
        invoices: List<Invoice> = emptyList()
    ): String {
        val root = JSONObject()

        // 1. Metadata header
        val meta = JSONObject().apply {
            put("appName", "المحاسب الشامل Pro Ledger")
            put("appVersion", "1.0.0")
            put("exportTimestamp", System.currentTimeMillis())
            put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date()))
            put("businessName", businessName)
            put("businessPhone", businessPhone)
            put("defaultCurrency", defaultCurrency)
        }
        root.put("meta", meta)

        // 2. Accounts Array
        val accountsArray = JSONArray()
        accounts.forEach { acc ->
            val accObj = JSONObject().apply {
                put("id", acc.id)
                put("name", acc.name)
                put("phone", acc.phone)
                put("type", acc.type)
                put("createdAt", acc.createdAt)
                put("creditLimit", acc.creditLimit)
                put("tag", acc.tag)
                put("initialBalance", acc.initialBalance)
            }
            accountsArray.put(accObj)
        }
        root.put("accounts", accountsArray)

        // 3. Transactions Array
        val txArray = JSONArray()
        transactions.forEach { tx ->
            val txObj = JSONObject().apply {
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
                put("dueDate", tx.dueDate)
            }
            txArray.put(txObj)
        }
        root.put("transactions", txArray)

        // 4. Inventory Array
        val inventoryArray = JSONArray()
        inventoryItems.forEach { item ->
            val itemObj = JSONObject().apply {
                put("id", item.id)
                put("name", item.name)
                put("barcode", item.barcode)
                put("purchasePrice", item.purchasePrice)
                put("salePrice", item.salePrice)
                put("stockQuantity", item.stockQuantity)
                put("unit", item.unit)
            }
            inventoryArray.put(itemObj)
        }
        root.put("inventory", inventoryArray)

        // 5. Materials Array
        val materialsArray = JSONArray()
        materials.forEach { mat ->
            val matObj = JSONObject().apply {
                put("id", mat.id)
                put("name", mat.name)
                put("count", mat.count)
                put("unitPrice", mat.unitPrice)
                put("total", mat.total)
            }
            materialsArray.put(matObj)
        }
        root.put("materials", materialsArray)

        // 6. Invoices Array
        val invoicesArray = JSONArray()
        invoices.forEach { inv ->
            val invObj = JSONObject().apply {
                put("invoiceId", inv.invoiceId)
                put("clientName", inv.clientName)
                put("count", inv.count)
                put("unitPrice", inv.unitPrice)
                put("total", inv.total)
                put("date", inv.date)
            }
            invoicesArray.put(invObj)
        }
        root.put("invoices", invoicesArray)

        return root.toString(2)
    }

    /**
     * Writes the backup JSON string to application storage cache / documents.
     */
    suspend fun saveJsonBackupFile(context: Context, jsonString: String): File? = withContext(Dispatchers.IO) {
        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val fileName = "ProLedger_Backup_$timeStamp.json"
            val backupDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "backups")
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            val file = File(backupDir, fileName)
            FileOutputStream(file).use { fos ->
                fos.write(jsonString.toByteArray(Charsets.UTF_8))
                fos.flush()
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Shares JSON backup file using standard Android Intent.
     */
    fun shareJsonBackup(context: Context, file: File) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "نسخة احتياطية محاسبية JSON - ${file.name}")
                putExtra(Intent.EXTRA_TEXT, "نسخة احتياطية من تطبيق المحاسب الشامل (Pro Ledger).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "مشاركة النسخة الاحتياطية JSON"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Reads text content from a content Uri (e.g. from File Picker).
     */
    suspend fun readJsonFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                stream.bufferedReader(Charsets.UTF_8).use { reader ->
                    reader.readText()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Parses JSON string into typed data structures and validates validity.
     */
    fun parseJsonBackup(jsonString: String): JsonBackupData? {
        return try {
            val root = JSONObject(jsonString)
            val meta = root.optJSONObject("meta")
            val businessName = meta?.optString("businessName", "") ?: ""
            val businessPhone = meta?.optString("businessPhone", "") ?: ""
            val defaultCurrency = meta?.optString("defaultCurrency", "YER") ?: "YER"
            val exportDate = meta?.optString("exportDate", "") ?: ""

            // Accounts
            val accountsList = mutableListOf<Account>()
            val accountsArray = root.optJSONArray("accounts") ?: JSONArray()
            for (i in 0 until accountsArray.length()) {
                val obj = accountsArray.getJSONObject(i)
                accountsList.add(
                    Account(
                        id = obj.optInt("id", 0),
                        name = obj.optString("name", "بدون اسم"),
                        phone = obj.optString("phone", ""),
                        type = obj.optString("type", "مشتري"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        creditLimit = obj.optDouble("creditLimit", 0.0),
                        tag = obj.optString("tag", ""),
                        initialBalance = obj.optDouble("initialBalance", 0.0)
                    )
                )
            }

            // Transactions
            val txList = mutableListOf<Transaction>()
            val txArray = root.optJSONArray("transactions") ?: JSONArray()
            for (i in 0 until txArray.length()) {
                val obj = txArray.getJSONObject(i)
                txList.add(
                    Transaction(
                        id = obj.optInt("id", 0),
                        accountId = obj.optInt("accountId", 0),
                        day = obj.optString("day", ""),
                        date = obj.optString("date", ""),
                        details = obj.optString("details", ""),
                        quantity = obj.optDouble("quantity", 1.0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        addition = obj.optDouble("addition", 0.0),
                        total = obj.optDouble("total", 0.0),
                        isPayment = obj.optBoolean("isPayment", false),
                        timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                        currency = obj.optString("currency", "YER"),
                        exchangeRate = obj.optDouble("exchangeRate", 1.0),
                        dueDate = obj.optString("dueDate", "")
                    )
                )
            }

            // Inventory
            val invList = mutableListOf<InventoryItem>()
            val invArray = root.optJSONArray("inventory") ?: JSONArray()
            for (i in 0 until invArray.length()) {
                val obj = invArray.getJSONObject(i)
                invList.add(
                    InventoryItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        barcode = obj.optString("barcode", ""),
                        name = obj.optString("name", ""),
                        purchasePrice = obj.optDouble("purchasePrice", 0.0),
                        salePrice = obj.optDouble("salePrice", 0.0),
                        stockQuantity = obj.optDouble("stockQuantity", 0.0),
                        unit = obj.optString("unit", "حبة")
                    )
                )
            }

            // Materials
            val matList = mutableListOf<MaterialItem>()
            val matArray = root.optJSONArray("materials") ?: JSONArray()
            for (i in 0 until matArray.length()) {
                val obj = matArray.getJSONObject(i)
                matList.add(
                    MaterialItem(
                        id = obj.optString("id", UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        count = obj.optInt("count", 0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        total = obj.optDouble("total", 0.0)
                    )
                )
            }

            // Invoices
            val invListInvoices = mutableListOf<Invoice>()
            val invoicesArray = root.optJSONArray("invoices") ?: JSONArray()
            for (i in 0 until invoicesArray.length()) {
                val obj = invoicesArray.getJSONObject(i)
                invListInvoices.add(
                    Invoice(
                        invoiceId = obj.optString("invoiceId", UUID.randomUUID().toString()),
                        clientName = obj.optString("clientName", ""),
                        count = obj.optInt("count", 0),
                        unitPrice = obj.optDouble("unitPrice", 0.0),
                        total = obj.optDouble("total", 0.0),
                        date = obj.optLong("date", System.currentTimeMillis())
                    )
                )
            }

            JsonBackupData(
                exportDate = exportDate,
                businessName = businessName,
                businessPhone = businessPhone,
                defaultCurrency = defaultCurrency,
                accounts = accountsList,
                transactions = txList,
                inventoryItems = invList,
                materials = matList,
                invoices = invListInvoices
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Imports data into Room database through LedgerRepository safely with ID mapping.
     */
    suspend fun restoreDatabaseFromJson(
        repository: LedgerRepository,
        backupData: JsonBackupData
    ): JsonImportSummary = withContext(Dispatchers.IO) {
        var restoredAccounts = 0
        var restoredTransactions = 0

        // Map old account ID to new inserted account ID
        val accountIdMap = mutableMapOf<Int, Int>()

        // 1. Restore Accounts
        for (acc in backupData.accounts) {
            try {
                val newAcc = Account(
                    id = 0, // Auto-generate new safe ID
                    name = acc.name,
                    phone = acc.phone,
                    type = acc.type,
                    createdAt = acc.createdAt,
                    creditLimit = acc.creditLimit,
                    tag = acc.tag,
                    initialBalance = acc.initialBalance
                )
                val insertedId = repository.insertAccount(newAcc).toInt()
                accountIdMap[acc.id] = insertedId
                restoredAccounts++
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // 2. Restore Transactions with mapped Account IDs
        for (tx in backupData.transactions) {
            try {
                val targetAccountId = accountIdMap[tx.accountId] ?: tx.accountId
                if (targetAccountId > 0) {
                    val newTx = Transaction(
                        id = 0,
                        accountId = targetAccountId,
                        day = tx.day,
                        date = tx.date,
                        details = tx.details,
                        quantity = tx.quantity,
                        unitPrice = tx.unitPrice,
                        addition = tx.addition,
                        total = tx.total,
                        isPayment = tx.isPayment,
                        timestamp = tx.timestamp,
                        currency = tx.currency,
                        exchangeRate = tx.exchangeRate,
                        dueDate = tx.dueDate
                    )
                    repository.insertTransaction(newTx)
                    restoredTransactions++
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        JsonImportSummary(
            accountsCount = restoredAccounts,
            transactionsCount = restoredTransactions,
            inventoryCount = backupData.inventoryItems.size,
            materialsCount = backupData.materials.size,
            invoicesCount = backupData.invoices.size,
            message = "تمت استعادة البيانات بنجاح: $restoredAccounts حساب، $restoredTransactions حركة مالية."
        )
    }
}
