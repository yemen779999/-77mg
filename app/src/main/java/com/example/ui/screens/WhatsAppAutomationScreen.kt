package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.NotificationLog
import com.example.ui.viewmodel.AccountWithBalance
import com.example.ui.viewmodel.LedgerViewModel
import com.example.utils.WhatsAppHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhatsAppAutomationScreen(
    viewModel: LedgerViewModel,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val allAccounts by viewModel.allAccounts.collectAsState()
    val allTransactions by viewModel.allTransactions.collectAsState()
    val notificationLogs by viewModel.allNotificationLogs.collectAsState()

    val gatewayType by viewModel.whatsappGatewayType.collectAsState()
    val apiUrl by viewModel.whatsappApiUrl.collectAsState()
    val token by viewModel.whatsappToken.collectAsState()
    val instanceId by viewModel.whatsappInstanceId.collectAsState()
    val metaPhoneId by viewModel.whatsappMetaPhoneNumberId.collectAsState()
    val metaWabaId by viewModel.whatsappMetaWabaId.collectAsState()
    val countryCode by viewModel.whatsappDefaultCountryCode.collectAsState()
    val paymentInstructions by viewModel.whatsappPaymentInstructions.collectAsState()
    val autoSendInvoice by viewModel.whatsappAutoSendInvoice.collectAsState()
    val autoSendPayment by viewModel.whatsappAutoSendPayment.collectAsState()
    val autoSendDueDate by viewModel.whatsappAutoSendDueDate.collectAsState()
    val templateInvoiceSummary by viewModel.whatsappTemplateInvoiceSummary.collectAsState()
    val templatePaymentReminder by viewModel.whatsappTemplatePaymentReminder.collectAsState()
    val templateInvoice by viewModel.whatsappTemplateInvoice.collectAsState()
    val templateDueDate by viewModel.whatsappTemplateDueDate.collectAsState()
    val templatePayment by viewModel.whatsappTemplatePayment.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "⏰ تذكيرات السداد",
        "🧾 ملخصات الفواتير",
        "📋 سجل الإشعارات",
        "⚙️ إعدادات البوابة"
    )

    // SnackBar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Dialog States
    var showBatchReminderDialog by remember { mutableStateOf(false) }
    var isBatchRunning by remember { mutableStateOf(false) }
    var batchCurrentIndex by remember { mutableIntStateOf(0) }
    var batchTotalCount by remember { mutableIntStateOf(0) }
    var batchCurrentClient by remember { mutableStateOf("") }
    var batchResultSummary by remember { mutableStateOf<String?>(null) }

    var previewMessageDialogData by remember { mutableStateOf<Pair<Account, String>?>(null) }
    var showTestApiDialog by remember { mutableStateOf(false) }

    // Calculate balances for debtors
    val accountsWithBalance = remember(allAccounts, allTransactions) {
        allAccounts.map { acc ->
            val txs = allTransactions.filter { it.accountId == acc.id }
            val debit = txs.filter { !it.isPayment }.sumOf { it.total }
            val credit = txs.filter { it.isPayment }.sumOf { it.total }
            val bal = (acc.initialBalance + debit) - credit
            AccountWithBalance(acc, bal, txs.size)
        }
    }

    val debtorsList = remember(accountsWithBalance) {
        accountsWithBalance.filter { it.balance > 0 }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "أتمتة إشعارات WhatsApp Business 📲",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "البوابة الحالية: ${
                                when (gatewayType) {
                                    "META_CLOUD_API" -> "Meta Cloud API (الرسمية)"
                                    "ULTRAMSG" -> "UltraMsg Gateway"
                                    "EVOLUTION_API" -> "Evolution API"
                                    "TWILIO" -> "Twilio WhatsApp"
                                    "GENERIC_POST" -> "Webhook POST"
                                    else -> "تطبيق واتساب اليدوي"
                                }
                            }",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                actions = {
                    IconButton(onClick = { showTestApiDialog = true }) {
                        Icon(Icons.Default.Send, contentDescription = "اختبار البوابة")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable / Fixed Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTabIndex,
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTabIndex) {
                0 -> PaymentRemindersTab(
                    debtorsList = debtorsList,
                    defaultCurrency = defaultCurrency,
                    gatewayType = gatewayType,
                    onSendInstantReminder = { acc, bal ->
                        viewModel.sendPaymentReminder(
                            context = context,
                            account = acc,
                            balance = bal,
                            onApiComplete = { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                    },
                    onPreviewReminder = { acc, bal ->
                        val msg = WhatsAppHelper.generatePaymentReminderMessage(
                            template = templatePaymentReminder,
                            account = acc,
                            totalBalance = bal,
                            currency = defaultCurrency,
                            businessName = businessName,
                            businessPhone = businessPhone,
                            paymentInstructions = paymentInstructions
                        )
                        previewMessageDialogData = Pair(acc, msg)
                    },
                    onStartBatchReminders = {
                        showBatchReminderDialog = true
                        isBatchRunning = false
                        batchResultSummary = null
                    }
                )
                1 -> InvoiceSummariesTab(
                    allAccounts = allAccounts,
                    allTransactions = allTransactions,
                    defaultCurrency = defaultCurrency,
                    templateInvoiceSummary = templateInvoiceSummary,
                    businessName = businessName,
                    businessPhone = businessPhone,
                    paymentInstructions = paymentInstructions,
                    onSendSummary = { acc ->
                        viewModel.sendInvoiceSummary(
                            context = context,
                            account = acc,
                            onApiComplete = { success, msg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                    }
                )
                2 -> NotificationLogsTab(
                    logs = notificationLogs,
                    onDeleteLog = { log -> viewModel.deleteNotificationLog(log) },
                    onClearAll = { viewModel.clearAllNotificationLogs() },
                    onResend = { log ->
                        viewModel.sendCustomNotification(
                            context = context,
                            phone = log.clientPhone,
                            clientName = log.clientName,
                            message = log.message,
                            type = log.type,
                            onApiComplete = { success, msg ->
                                scope.launch { snackbarHostState.showSnackbar(msg) }
                            }
                        )
                    }
                )
                3 -> GatewaySettingsTab(
                    viewModel = viewModel,
                    gatewayType = gatewayType,
                    apiUrl = apiUrl,
                    token = token,
                    instanceId = instanceId,
                    metaPhoneId = metaPhoneId,
                    metaWabaId = metaWabaId,
                    countryCode = countryCode,
                    paymentInstructions = paymentInstructions,
                    autoSendInvoice = autoSendInvoice,
                    autoSendPayment = autoSendPayment,
                    autoSendDueDate = autoSendDueDate,
                    templateInvoiceSummary = templateInvoiceSummary,
                    templatePaymentReminder = templatePaymentReminder,
                    templateInvoice = templateInvoice,
                    templateDueDate = templateDueDate,
                    templatePayment = templatePayment,
                    onSaved = {
                        scope.launch {
                            snackbarHostState.showSnackbar("✓ تم حفظ إعدادات الواتساب بنجاح!")
                        }
                    },
                    onTestApi = { showTestApiDialog = true }
                )
            }
        }
    }

    // Batch Reminders Modal Dialog
    if (showBatchReminderDialog) {
        AlertDialog(
            onDismissRequest = {
                if (!isBatchRunning) showBatchReminderDialog = false
            },
            icon = { Icon(Icons.Default.Send, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text(
                    text = "إرسال تذكير سداد جماعي 🚀",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (debtorsList.isEmpty()) {
                        Text("لا يوجد عملاء عليهم أرصدة مدينة حالياً في السجل.")
                    } else if (batchResultSummary != null) {
                        Text(
                            text = batchResultSummary ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    } else if (isBatchRunning) {
                        CircularProgressIndicator(modifier = Modifier.size(48.dp))
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "جاري إرسال التذكيرات ($batchCurrentIndex من $batchTotalCount)...",
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "العميل الحالي: $batchCurrentClient",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { if (batchTotalCount > 0) batchCurrentIndex.toFloat() / batchTotalCount else 0f },
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text(
                            text = "سيتم إرسال رسائل تذكير سداد مخصصة لكل عميل يتضمن رصيده الحالي وطرق السداد المعتمدة لعدد ${debtorsList.size} عميل مدين.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "البوابة المستخدمة: ${if (gatewayType == "META_CLOUD_API") "Meta Cloud API" else gatewayType}",
                                modifier = Modifier.padding(10.dp),
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            },
            confirmButton = {
                if (batchResultSummary != null || debtorsList.isEmpty()) {
                    Button(onClick = { showBatchReminderDialog = false }) {
                        Text("إغلاق")
                    }
                } else if (!isBatchRunning) {
                    Button(
                        onClick = {
                            isBatchRunning = true
                            viewModel.sendBatchPaymentReminders(
                                context = context,
                                accountsWithBalance = debtorsList,
                                onProgress = { cur, tot, client ->
                                    batchCurrentIndex = cur
                                    batchTotalCount = tot
                                    batchCurrentClient = client
                                },
                                onComplete = { succ, fail, summary ->
                                    isBatchRunning = false
                                    batchResultSummary = summary
                                }
                            )
                        }
                    ) {
                        Text("بدء الإرسال الجماعي الآن")
                    }
                }
            },
            dismissButton = {
                if (!isBatchRunning && batchResultSummary == null && debtorsList.isNotEmpty()) {
                    TextButton(onClick = { showBatchReminderDialog = false }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }

    // Preview & Custom Edit Dialog
    if (previewMessageDialogData != null) {
        val (account, defaultMsg) = previewMessageDialogData!!
        var editableMessage by remember { mutableStateOf(defaultMsg) }

        AlertDialog(
            onDismissRequest = { previewMessageDialogData = null },
            title = {
                Text(
                    text = "معاينة وتخصيص نص التذكير ✉️",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "العميل: ${account.name} (${account.phone})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = editableMessage,
                        onValueChange = { editableMessage = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp, max = 280.dp),
                        textStyle = MaterialTheme.typography.bodySmall,
                        label = { Text("نص الرسالة المخصصة") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val acc = account
                        val msg = editableMessage
                        previewMessageDialogData = null
                        viewModel.sendCustomNotification(
                            context = context,
                            phone = acc.phone,
                            clientName = acc.name,
                            message = msg,
                            type = "PAYMENT_REMINDER",
                            onApiComplete = { success, resultMsg ->
                                scope.launch {
                                    snackbarHostState.showSnackbar(resultMsg)
                                }
                            }
                        )
                    }
                ) {
                    Text("إرسال الآن")
                }
            },
            dismissButton = {
                TextButton(onClick = { previewMessageDialogData = null }) {
                    Text("إلغاء")
                }
            }
        )
    }

    // Test API Dialog
    if (showTestApiDialog) {
        var testPhone by remember { mutableStateOf(businessPhone) }
        var testMessage by remember {
            mutableStateOf("تجربة إرسال إشعار ناجح من تطبيق المحاسب anas برو عبر بوابة WhatsApp API! 🚀\nتاريخ الاختبار: ${SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US).format(Date())}")
        }
        var isTesting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { if (!isTesting) showTestApiDialog = false },
            title = {
                Text(
                    text = "اختبار اتصال بوابة الواتساب 🧪",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "أدخل رقم هاتف لاستلام رسالة اختبار للتأكد من صحة إعدادات البوابة والتوكن.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = testPhone,
                        onValueChange = { testPhone = it },
                        label = { Text("رقم هاتف المستلم") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = testMessage,
                        onValueChange = { testMessage = it },
                        label = { Text("نص الرسالة التجريبية") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isTesting = true
                        viewModel.sendCustomNotification(
                            context = context,
                            phone = testPhone,
                            clientName = "اختبار البوابة",
                            message = testMessage,
                            type = "TEST",
                            onApiComplete = { success, msg ->
                                isTesting = false
                                showTestApiDialog = false
                                scope.launch {
                                    snackbarHostState.showSnackbar(msg)
                                }
                            }
                        )
                    },
                    enabled = !isTesting && testPhone.isNotBlank()
                ) {
                    if (isTesting) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("إرسال رسالة الاختبار")
                }
            },
            dismissButton = {
                if (!isTesting) {
                    TextButton(onClick = { showTestApiDialog = false }) {
                        Text("إلغاء")
                    }
                }
            }
        )
    }
}

// ----------------------------------------------------
// 1. PAYMENT REMINDERS TAB
// ----------------------------------------------------
@Composable
fun PaymentRemindersTab(
    debtorsList: List<AccountWithBalance>,
    defaultCurrency: String,
    gatewayType: String,
    onSendInstantReminder: (Account, Double) -> Unit,
    onPreviewReminder: (Account, Double) -> Unit,
    onStartBatchReminders: () -> Unit
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    val filteredList = remember(debtorsList, searchQuery) {
        if (searchQuery.isBlank()) debtorsList
        else debtorsList.filter { it.account.name.contains(searchQuery, ignoreCase = true) || it.account.phone.contains(searchQuery) }
    }

    val totalDebtSum = remember(debtorsList) { debtorsList.sumOf { it.balance } }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary & Batch Action Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "إجمالي الديون والتحصيلات المستحقة",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = "${String.format(Locale.US, "%,.2f", totalDebtSum)} $defaultCurrency",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "${debtorsList.size}",
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = onStartBatchReminders,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp),
                        enabled = debtorsList.isNotEmpty()
                    ) {
                        Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("🚀 إرسال تذكير سداد جماعي لجميع المدينين (${debtorsList.size})")
                    }
                }
            }
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("بحث عن عميل مدين بالاسم أو رقم الهاتف...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )
        }

        if (filteredList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (searchQuery.isBlank()) "لا يوجد أي عملاء عليهم ديون حالياً! 🎉" else "لا توجد نتائج مطابقة للبحث.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            items(filteredList, key = { it.account.id }) { item ->
                DebtorCard(
                    account = item.account,
                    balance = item.balance,
                    defaultCurrency = defaultCurrency,
                    onInstantSend = { onSendInstantReminder(item.account, item.balance) },
                    onPreview = { onPreviewReminder(item.account, item.balance) },
                    onCall = {
                        try {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${item.account.phone}"))
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            Toast.makeText(context, "تعذر فتح تطبيق الهاتف", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DebtorCard(
    account: Account,
    balance: Double,
    defaultCurrency: String,
    onInstantSend: () -> Unit,
    onPreview: () -> Unit,
    onCall: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "📱 ${account.phone.ifBlank { "بدون رقم هاتف" }}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${String.format(Locale.US, "%,.2f", balance)} $defaultCurrency",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "رصيد مستحق",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalButton(
                    onClick = onInstantSend,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("إرسال فوري ⚡", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("معاينة وتعديل", fontSize = 12.sp)
                }

                if (account.phone.isNotBlank()) {
                    IconButton(
                        onClick = onCall,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = "اتصال", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------
// 2. INVOICE SUMMARIES TAB
// ----------------------------------------------------
@Composable
fun InvoiceSummariesTab(
    allAccounts: List<Account>,
    allTransactions: List<com.example.data.model.Transaction>,
    defaultCurrency: String,
    templateInvoiceSummary: String,
    businessName: String,
    businessPhone: String,
    paymentInstructions: String,
    onSendSummary: (Account) -> Unit
) {
    val context = LocalContext.current
    var selectedAccount by remember { mutableStateOf<Account?>(allAccounts.firstOrNull()) }
    var searchQuery by remember { mutableStateOf("") }
    var isExpandedDropdown by remember { mutableStateOf(false) }

    val accountTransactions = remember(selectedAccount, allTransactions) {
        if (selectedAccount == null) emptyList()
        else allTransactions.filter { it.accountId == selectedAccount!!.id }
    }

    val generatedMessage = remember(selectedAccount, accountTransactions, templateInvoiceSummary, businessName, businessPhone, paymentInstructions) {
        if (selectedAccount == null) ""
        else {
            WhatsAppHelper.generateInvoiceSummaryMessage(
                template = templateInvoiceSummary,
                account = selectedAccount!!,
                transactions = accountTransactions,
                businessName = businessName,
                businessPhone = businessPhone,
                currency = defaultCurrency,
                paymentInstructions = paymentInstructions
            )
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "اختر العميل لتوليد ملخص الفواتير وكشف الحساب:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))

            // Account Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedCard(
                    onClick = { isExpandedDropdown = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = selectedAccount?.name ?: "اختر عميل...",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "الهاتف: ${selectedAccount?.phone?.ifBlank { "غير مسجل" } ?: "-"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                DropdownMenu(
                    expanded = isExpandedDropdown,
                    onDismissRequest = { isExpandedDropdown = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    allAccounts.forEach { acc ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(acc.name, fontWeight = FontWeight.Medium)
                                    Text(acc.phone, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            },
                            onClick = {
                                selectedAccount = acc
                                isExpandedDropdown = false
                            }
                        )
                    }
                }
            }
        }

        if (selectedAccount != null) {
            val totalInvoiced = accountTransactions.filter { !it.isPayment }.sumOf { it.total }
            val totalPaid = accountTransactions.filter { it.isPayment }.sumOf { it.total }
            val balance = (selectedAccount!!.initialBalance + totalInvoiced) - totalPaid

            item {
                // Client Statement Summary Strip
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        title = "إجمالي الفواتير",
                        value = "${String.format(Locale.US, "%,.2f", totalInvoiced)} $defaultCurrency",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "إجمالي المسدد",
                        value = "${String.format(Locale.US, "%,.2f", totalPaid)} $defaultCurrency",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "صافي الرصيد",
                        value = "${String.format(Locale.US, "%,.2f", Math.abs(balance))} $defaultCurrency",
                        modifier = Modifier.weight(1f),
                        color = if (balance > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }
            }

            item {
                Text(
                    text = "معاينة رسالة ملخص الفواتير (نص كشف الحساب):",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = generatedMessage,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 20.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Invoice Summary", generatedMessage)
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "تم نسخ نص الملخص للحافظة ✓", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("نسخ النص")
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = { onSendSummary(selectedAccount!!) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إرسال ملخص الفاتورة وكشف الحساب للعميل (WhatsApp)")
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        }
    }
}

// ----------------------------------------------------
// 3. NOTIFICATION LOGS TAB
// ----------------------------------------------------
@Composable
fun NotificationLogsTab(
    logs: List<NotificationLog>,
    onDeleteLog: (NotificationLog) -> Unit,
    onClearAll: () -> Unit,
    onResend: (NotificationLog) -> Unit
) {
    var filterStatus by remember { mutableStateOf("ALL") }
    val filteredLogs = remember(logs, filterStatus) {
        when (filterStatus) {
            "SUCCESS" -> logs.filter { it.status == "SUCCESS" }
            "FAILED" -> logs.filter { it.status == "FAILED" }
            "INTENT" -> logs.filter { it.status == "SENT_INTENT" }
            else -> logs
        }
    }

    var showClearDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "سجل الإشعارات التلقائية (${logs.size})",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                if (logs.isNotEmpty()) {
                    TextButton(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("مسح الكل", fontSize = 12.sp)
                    }
                }
            }
        }

        // Filter chips
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = filterStatus == "ALL",
                    onClick = { filterStatus = "ALL" },
                    label = { Text("الكل (${logs.size})") }
                )
                FilterChip(
                    selected = filterStatus == "SUCCESS",
                    onClick = { filterStatus = "SUCCESS" },
                    label = { Text("ناجحة (${logs.count { it.status == "SUCCESS" }})") }
                )
                FilterChip(
                    selected = filterStatus == "FAILED",
                    onClick = { filterStatus = "FAILED" },
                    label = { Text("فاشلة (${logs.count { it.status == "FAILED" }})") }
                )
            }
        }

        if (filteredLogs.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("لا توجد إشعارات مسجلة حالياً 📭", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            items(filteredLogs, key = { it.id }) { log ->
                NotificationLogCard(
                    log = log,
                    onDelete = { onDeleteLog(log) },
                    onResend = { onResend(log) }
                )
            }
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("مسح كافة سجلات الإشعارات؟") },
            text = { Text("هل أنت متأكد من رغبتك في حذف جميع سجلات إرسال الواتساب السابقة؟ لا يمكن التراجع عن هذا الإجراء.") },
            confirmButton = {
                Button(
                    onClick = {
                        onClearAll()
                        showClearDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("نعم، مسح الكل")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text("إلغاء")
                }
            }
        )
    }
}

@Composable
fun NotificationLogCard(
    log: NotificationLog,
    onDelete: () -> Unit,
    onResend: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy/MM/dd hh:mm a", Locale.US) }
    val dateStr = remember(log.timestamp) { dateFormat.format(Date(log.timestamp)) }
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = when (log.status) {
                            "SUCCESS" -> Color(0xFF2E7D32).copy(alpha = 0.15f)
                            "FAILED" -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = when (log.status) {
                                    "SUCCESS" -> "✓"
                                    "FAILED" -> "✕"
                                    else -> "📱"
                                },
                                fontWeight = FontWeight.Bold,
                                color = when (log.status) {
                                    "SUCCESS" -> Color(0xFF2E7D32)
                                    "FAILED" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = log.clientName.ifBlank { "بدون اسم" },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "${log.clientPhone} • $dateStr",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = when (log.type) {
                            "INVOICE_SUMMARY" -> "ملخص فاتورة"
                            "PAYMENT_REMINDER" -> "تذكير سداد"
                            "INVOICE_RECEIPT" -> "إشعار فاتورة"
                            "PAYMENT_RECEIPT" -> "سند قبض"
                            "BATCH_REMINDER" -> "تذكير جماعي"
                            else -> "إشعار مخصص"
                        },
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = log.message,
                style = MaterialTheme.typography.bodySmall,
                maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickable { isExpanded = !isExpanded }
            )

            if (log.responseMsg.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "الرد: ${log.responseMsg}",
                    fontSize = 11.sp,
                    color = if (log.status == "SUCCESS") Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                if (log.status == "FAILED") {
                    TextButton(onClick = onResend) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعادة المحاولة", fontSize = 11.sp)
                    }
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "حذف", modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

// ----------------------------------------------------
// 4. GATEWAY SETTINGS TAB
// ----------------------------------------------------
@Composable
fun GatewaySettingsTab(
    viewModel: LedgerViewModel,
    gatewayType: String,
    apiUrl: String,
    token: String,
    instanceId: String,
    metaPhoneId: String,
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
    templatePayment: String,
    onSaved: () -> Unit,
    onTestApi: () -> Unit
) {
    var selectedGateway by remember { mutableStateOf(gatewayType) }
    var currentApiUrl by remember { mutableStateOf(apiUrl) }
    var currentToken by remember { mutableStateOf(token) }
    var currentInstanceId by remember { mutableStateOf(instanceId) }
    var currentMetaPhoneId by remember { mutableStateOf(metaPhoneId) }
    var currentMetaWabaId by remember { mutableStateOf(metaWabaId) }
    var currentCountryCode by remember { mutableStateOf(countryCode) }
    var currentPaymentInstructions by remember { mutableStateOf(paymentInstructions) }

    var currentAutoSendInvoice by remember { mutableStateOf(autoSendInvoice) }
    var currentAutoSendPayment by remember { mutableStateOf(autoSendPayment) }
    var currentAutoSendDueDate by remember { mutableStateOf(autoSendDueDate) }

    var currentTemplateInvoiceSummary by remember { mutableStateOf(templateInvoiceSummary) }
    var currentTemplatePaymentReminder by remember { mutableStateOf(templatePaymentReminder) }
    var currentTemplateInvoice by remember { mutableStateOf(templateInvoice) }
    var currentTemplateDueDate by remember { mutableStateOf(templateDueDate) }
    var currentTemplatePayment by remember { mutableStateOf(templatePayment) }

    var showTemplatesSection by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "اختر بوابة وخادم إرسال رسائل WhatsApp 🌐",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Gateway Selector
            val gateways = listOf(
                "META_CLOUD_API" to "Meta WhatsApp Cloud API (الرسمية)",
                "ULTRAMSG" to "UltraMsg Gateway",
                "EVOLUTION_API" to "Evolution API / Baileys",
                "TWILIO" to "Twilio WhatsApp API",
                "GENERIC_POST" to "Custom JSON Webhook (POST)",
                "INTENT" to "تطبيق واتساب اليدوي (Intent)"
            )

            gateways.forEach { (id, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { selectedGateway = id }
                        .padding(vertical = 4.dp, horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = selectedGateway == id,
                        onClick = { selectedGateway = id }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = label,
                        fontWeight = if (selectedGateway == id) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Gateway Credentials Form
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "بيانات الربط والاعتماد للبوابة المختارة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )

                    when (selectedGateway) {
                        "META_CLOUD_API" -> {
                            OutlinedTextField(
                                value = currentMetaPhoneId,
                                onValueChange = { currentMetaPhoneId = it },
                                label = { Text("Phone Number ID (معرف رقم الهاتف)") },
                                placeholder = { Text("مثال: 107441860505300") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentToken,
                                onValueChange = { currentToken = it },
                                label = { Text("Permanent Access Token (رمز الوصول الدائم)") },
                                placeholder = { Text("EAAG...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentMetaWabaId,
                                onValueChange = { currentMetaWabaId = it },
                                label = { Text("WhatsApp Business Account ID (WABA ID - اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentApiUrl,
                                onValueChange = { currentApiUrl = it },
                                label = { Text("رابط نقطة النهاية (Graph API Endpoint)") },
                                placeholder = { Text("https://graph.facebook.com/v20.0") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        "ULTRAMSG" -> {
                            OutlinedTextField(
                                value = currentInstanceId,
                                onValueChange = { currentInstanceId = it },
                                label = { Text("Instance ID (معرف النسخة)") },
                                placeholder = { Text("instance12345") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentToken,
                                onValueChange = { currentToken = it },
                                label = { Text("Token (رمز المرور)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentApiUrl,
                                onValueChange = { currentApiUrl = it },
                                label = { Text("API URL (افتراضي: https://api.ultramsg.com)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        "EVOLUTION_API" -> {
                            OutlinedTextField(
                                value = currentApiUrl,
                                onValueChange = { currentApiUrl = it },
                                label = { Text("Evolution Server URL") },
                                placeholder = { Text("https://evolution.example.com") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentInstanceId,
                                onValueChange = { currentInstanceId = it },
                                label = { Text("Instance Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentToken,
                                onValueChange = { currentToken = it },
                                label = { Text("API Key") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        "TWILIO" -> {
                            OutlinedTextField(
                                value = currentInstanceId,
                                onValueChange = { currentInstanceId = it },
                                label = { Text("Twilio Account SID") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentToken,
                                onValueChange = { currentToken = it },
                                label = { Text("Twilio Auth Token") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentApiUrl,
                                onValueChange = { currentApiUrl = it },
                                label = { Text("Twilio From Number (مثال: +14155238886)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        "GENERIC_POST" -> {
                            OutlinedTextField(
                                value = currentApiUrl,
                                onValueChange = { currentApiUrl = it },
                                label = { Text("Webhook Endpoint URL") },
                                placeholder = { Text("https://your-server.com/api/send-whatsapp") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = currentToken,
                                onValueChange = { currentToken = it },
                                label = { Text("Bearer Token / API Key (اختياري)") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                        else -> {
                            Text(
                                text = "يتم الإرسال عبر تطبيق واتساب المثبت على الهاتف مباشرة بدون الحاجة لإعداد خادم API.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    OutlinedTextField(
                        value = currentCountryCode,
                        onValueChange = { currentCountryCode = it },
                        label = { Text("رمز الدولة الافتراضي (Default Country Code)") },
                        placeholder = { Text("967 لليمن، 966 للسعودية، 20 لمصر...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currentPaymentInstructions,
                        onValueChange = { currentPaymentInstructions = it },
                        label = { Text("حسابات وطرق السداد (تُدرج تلقائياً في الرسائل)") },
                        placeholder = { Text("مثال: حساب بنك الكريمي: 1234567 | حساب النجم: 89101112") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = false
                    )
                }
            }
        }

        // Auto-Trigger switches
        item {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "محفزات الأتمتة الفورية:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إرسال فاتورة تلقائية عند إضافة مبيعات", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("يتم إرسال إشعار فوري للعميل فور حفظ الفاتورة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = currentAutoSendInvoice,
                            onCheckedChange = { currentAutoSendInvoice = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("إرسال سند قبض تلقائي عند سداد دفعة", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("يتم إرسال إشعار استلام وقيد المبلغ فور تسجيل السداد", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = currentAutoSendPayment,
                            onCheckedChange = { currentAutoSendPayment = it }
                        )
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تنبيهات الاستحقاق المؤتمتة", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                            Text("إرسال تذكيرات بمواعيد السداد المستحقة", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = currentAutoSendDueDate,
                            onCheckedChange = { currentAutoSendDueDate = it }
                        )
                    }
                }
            }
        }

        // Templates Accordion
        item {
            OutlinedCard(
                onClick = { showTemplatesSection = !showTemplatesSection },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "📝 تخصيص قوالب الرسائل والنصوص (${if (showTemplatesSection) "إخفاء" else "إظهار"})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Icon(
                        if (showTemplatesSection) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null
                    )
                }
            }
        }

        if (showTemplatesSection) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "العلامات الديناميكية المتاحة: {CLIENT} {TOTAL_DEBT} {TOTAL_PAID} {BALANCE} {INVOICE_COUNT} {RECENT_TRANSACTIONS} {PAYMENT_INFO} {BUSINESS} {BUSINESS_PHONE}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )

                    OutlinedTextField(
                        value = currentTemplateInvoiceSummary,
                        onValueChange = { currentTemplateInvoiceSummary = it },
                        label = { Text("قالب ملخص الفواتير وكشف الحساب") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = currentTemplatePaymentReminder,
                        onValueChange = { currentTemplatePaymentReminder = it },
                        label = { Text("قالب تذكير السداد والديون") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 140.dp),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = currentTemplateInvoice,
                        onValueChange = { currentTemplateInvoice = it },
                        label = { Text("قالب الفاتورة الفردية الفورية") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )

                    OutlinedTextField(
                        value = currentTemplatePayment,
                        onValueChange = { currentTemplatePayment = it },
                        label = { Text("قالب سند القبض الفوري") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        // Action Buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onTestApi,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فحص البوابة 🧪")
                }

                Button(
                    onClick = {
                        viewModel.updateWhatsAppSettings(
                            gatewayType = selectedGateway,
                            apiUrl = currentApiUrl,
                            token = currentToken,
                            instanceId = currentInstanceId,
                            countryCode = currentCountryCode,
                            autoSendInvoice = currentAutoSendInvoice,
                            autoSendDueDate = currentAutoSendDueDate,
                            templateInvoice = currentTemplateInvoice,
                            templateDueDate = currentTemplateDueDate,
                            templatePayment = currentTemplatePayment,
                            metaPhoneNumberId = currentMetaPhoneId,
                            metaWabaId = currentMetaWabaId,
                            paymentInstructions = currentPaymentInstructions,
                            autoSendPayment = currentAutoSendPayment,
                            templateInvoiceSummary = currentTemplateInvoiceSummary,
                            templatePaymentReminder = currentTemplatePaymentReminder
                        )
                        onSaved()
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ الإعدادات 💾")
                }
            }
        }
    }
}
