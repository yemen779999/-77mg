package com.example.ui.screens

import android.app.DatePickerDialog
import android.widget.DatePicker
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.Transaction
import com.example.ui.common.ThreeDUiStateLayout
import com.example.ui.common.UiState
import com.example.ui.theme.*
import com.example.ui.viewmodel.LedgerViewModel
import com.example.utils.NotificationHelper
import com.example.utils.PdfExportHelper
import java.text.SimpleDateFormat
import java.util.*

private fun formatDecimalValue(value: Double, decimals: Int = 2): String {
    if (value % 1.0 == 0.0) {
        return value.toLong().toString()
    }
    val pattern = "%.${decimals}f"
    val formatted = String.format(Locale.US, pattern, value)
    return formatted.trimEnd('0').trimEnd('.')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntriesScreen(
    viewModel: LedgerViewModel,
    initialAccountId: Int? = null
) {
    val context = LocalContext.current
    val performHaptic = rememberHapticFeedback()
    val accountsWithBalance by viewModel.accountsWithBalance.collectAsState()
    val selectedAccountId by viewModel.selectedAccountId.collectAsState()
    val selectedAccount by viewModel.selectedAccount.collectAsState()
    val transactions by viewModel.selectedAccountTransactions.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    // Dialog & Form State
    var showAddTxDialog by remember { mutableStateOf(false) }
    var details by remember { mutableStateOf("") }
    var quantityStr by remember { mutableStateOf("1") }
    var priceStr by remember { mutableStateOf("") }
    var additionStr by remember { mutableStateOf("0") }
    var isPayment by remember { mutableStateOf(false) } // Default: regular invoice
    var selectedDateString by remember { mutableStateOf("") } // Custom date if selected
    var formError by remember { mutableStateOf(false) }

    var docType by remember { mutableStateOf("فاتورة بيع نقد") }
    var showTilingCalc by remember { mutableStateOf(false) }
    var tileLength by remember { mutableStateOf("") }
    var tileWidth by remember { mutableStateOf("") }
    var tileCount by remember { mutableStateOf("") }
    var showStockLookup by remember { mutableStateOf(false) }
    val stockItems by viewModel.inventoryItems.collectAsState()
    var stockSearchQuery by remember { mutableStateOf("") }
    var selectedStockItem by remember { mutableStateOf<com.example.data.model.InventoryItem?>(null) }
    var showMockBarcodeScanner by remember { mutableStateOf(false) }
    var mockScannerBeep by remember { mutableStateOf(false) }

    // Edit Dialog & Form State
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editDetails by remember { mutableStateOf("") }
    var editQuantityStr by remember { mutableStateOf("1") }
    var editPriceStr by remember { mutableStateOf("") }
    var editAdditionStr by remember { mutableStateOf("0") }
    var editIsPayment by remember { mutableStateOf(false) }
    var editSelectedDateString by remember { mutableStateOf("") }
    var editTxCurrency by remember { mutableStateOf("YER") }
    var editTxExchangeRateStr by remember { mutableStateOf("1.0") }
    var editFormError by remember { mutableStateOf(false) }

    LaunchedEffect(editingTransaction) {
        editingTransaction?.let {
            editDetails = it.details
            editQuantityStr = it.quantity.toString()
            editPriceStr = it.unitPrice.toString()
            editAdditionStr = it.addition.toString()
            editIsPayment = it.isPayment
            editSelectedDateString = it.date
            editTxCurrency = it.currency
            editTxExchangeRateStr = it.exchangeRate.toString()
            editFormError = false
        }
    }

    // Automation Notification Prompt Dialog State
    var lastSavedTx by remember { mutableStateOf<Transaction?>(null) }
    var showNotificationPrompt by remember { mutableStateOf(false) }
    var showVoiceSmartEntryDialog by remember { mutableStateOf(false) }
    var showInvoiceTemplateDesignerDialog by remember { mutableStateOf(false) }

    // Synchronize initial account navigation (e.g. from Dashboard or Accounts list)
    LaunchedEffect(initialAccountId) {
        if (initialAccountId != null) {
            viewModel.selectAccount(initialAccountId)
        }
    }

    if (selectedAccountId == null) {
        // Step A: Choose an account first to enter its ledger statement
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            Text(
                text = "العمليات وحركة كشوف الحسابات",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
            
            Text(
                text = "الرجاء اختيار الحساب أو العميل لمعاينة كشف الحساب وتسجيل القيود اليومية:",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            // Smart Voice Entry Assistant Button Trigger
            Button(
                onClick = {
                    performHaptic()
                    showVoiceSmartEntryDialog = true
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(48.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("🎙️ نطق قيد أو مديونية بالصوت والذكاء الاصطناعي", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            val accountsUiState: UiState<List<com.example.ui.viewmodel.AccountWithBalance>> = remember(accountsWithBalance) {
                if (accountsWithBalance.isEmpty()) {
                    UiState.Empty(
                        title = "لا توجد حسابات مسجلة حالياً",
                        message = "يرجى الانتقال لصفحة 'إدارة الحسابات' لإضافة حسابك المالي أو العميل/المورد الأول.",
                        icon = Icons.Default.PeopleOutline
                    )
                } else {
                    UiState.Content(accountsWithBalance)
                }
            }

            ThreeDUiStateLayout(
                state = accountsUiState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { accList ->
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    items(accList) { accountItem ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .threeDTiltEffect(maxRotationDegrees = 5f)
                                .shadow(
                                    elevation = 4.dp,
                                    shape = AppShapes.large,
                                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                                )
                                .clickable {
                                    performHaptic()
                                    viewModel.selectAccount(accountItem.account.id)
                                },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = AppShapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.normal),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChevronLeft,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = accountItem.account.name,
                                        style = AppTypography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val defaultCurrencySymbol = when (viewModel.defaultCurrency.value) {
                                        "USD" -> "$"
                                        "SAR" -> "ر.س"
                                        "YER" -> "ر.ي"
                                        else -> viewModel.defaultCurrency.value
                                    }
                                    Text(
                                        text = "نوع الحساب: ${accountItem.account.type} | الرصيد: ${String.format(Locale.US, "%.2f", Math.abs(accountItem.balance))} $defaultCurrencySymbol",
                                        style = AppTypography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        // Step B: Account Selected -> Show Ledger Daily Operations inside a 30-day table cycle
        val currentAccount = selectedAccount
        if (currentAccount != null) {
            // Find current balance
            val currentAccountWithBalance = accountsWithBalance.find { it.account.id == currentAccount.id }
            val currentBalance = currentAccountWithBalance?.balance ?: 0.0

            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = currentAccount.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    text = "حساب ${currentAccount.type} - الهاتف: ${currentAccount.phone}",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                )
                            }
                        },
                        navigationIcon = {
                            IconButton(onClick = {
                                performHaptic()
                                viewModel.selectAccount(null)
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "الخروج",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        actions = {
                            // Print / Export PDF System Button with Custom Invoice Template Designer
                            IconButton(
                                onClick = {
                                    performHaptic()
                                    showInvoiceTemplateDesignerDialog = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PictureAsPdf,
                                    contentDescription = "تصميم الفاتورة وتصديرها كـ PDF",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = {
                            performHaptic()
                            details = ""
                            quantityStr = "1"
                            priceStr = ""
                            additionStr = "0"
                            isPayment = false
                            selectedDateString = ""
                            formError = false
                            showAddTxDialog = true
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("قيد / حركة جديدة", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Icon(Icons.Default.AddBusiness, contentDescription = "تسجيل قيد جديد")
                        }
                    }
                }
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .padding(paddingValues)
                        .padding(16.dp)
                ) {
                    val context = LocalContext.current
                    
                    // Action Toolbar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { 
                                performHaptic()
                                val excelFile = com.example.utils.ExcelExportHelper.generateExcelFile(context, currentAccount.name, transactions, currentBalance, viewModel.defaultCurrency.value)
                                if (excelFile != null) {
                                    com.example.utils.ExcelExportHelper.shareExcel(context, excelFile)
                                } else {
                                    android.widget.Toast.makeText(context, "فشل تصدير Excel", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF166534)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("تصدير Excel", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                performHaptic()
                                val defaultCurrencyLabel = when (viewModel.defaultCurrency.value) {
                                    "USD" -> "$"
                                    "SAR" -> "ر.س"
                                    "YER" -> "ر.ي"
                                    else -> viewModel.defaultCurrency.value
                                }
                                val header = "كشف حساب العميل: *${currentAccount.name}*\n" +
                                        "رقم الهاتف: *${currentAccount.phone}*\n" +
                                        "الرصيد الحالي: *${currentBalance} $defaultCurrencyLabel*\n\n" +
                                        "تفاصيل القيود الأخيرة:\n"
                                val entriesText = transactions.take(15).mapIndexed { i, tx ->
                                    val prefix = if (tx.isPayment) "🟢 واصل:" else "🔴 قيد:"
                                    "- ${tx.date} | $prefix *${tx.total} ${tx.currency}* (${tx.details})"
                                }.joinToString("\n")
                                val footer = "\n\nنشكركم لحسن التعامل.\n*${viewModel.businessName.value}*"
                                
                                val message = header + entriesText + footer
                                com.example.utils.WhatsAppHelper.sendViaIntent(context, currentAccount.phone, message)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("إرسال كشف واتساب", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { 
                                performHaptic()
                                val pdfFile = com.example.utils.PdfExportHelper.generateAccountStatementPdf(
                                    context = context,
                                    businessName = viewModel.businessName.value,
                                    businessPhone = viewModel.businessPhone.value,
                                    businessAddress = viewModel.businessAddress.value,
                                    accountName = currentAccount.name,
                                    accountPhone = currentAccount.phone,
                                    accountType = currentAccount.type,
                                    transactions = transactions,
                                    currentBalance = currentBalance,
                                    config = viewModel.getPdfTemplateConfig(),
                                    defaultCurrency = viewModel.defaultCurrency.value
                                )
                                if (pdfFile != null) {
                                    com.example.utils.PdfExportHelper.sharePdf(context, pdfFile)
                                } else {
                                    android.widget.Toast.makeText(context, "فشل التصدير", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("مشاركة كشف PDF", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        
                        Button(
                            onClick = { 
                                performHaptic()
                                val pdfFile = com.example.utils.PdfExportHelper.generateAccountStatementPdf(
                                    context = context,
                                    businessName = viewModel.businessName.value,
                                    businessPhone = viewModel.businessPhone.value,
                                    businessAddress = viewModel.businessAddress.value,
                                    accountName = currentAccount.name,
                                    accountPhone = currentAccount.phone,
                                    accountType = currentAccount.type,
                                    transactions = transactions,
                                    currentBalance = currentBalance,
                                    config = viewModel.getPdfTemplateConfig(),
                                    defaultCurrency = viewModel.defaultCurrency.value
                                )
                                if (pdfFile != null) {
                                    com.example.utils.PrintHelper.printPdf(context, pdfFile, "Statement_${currentAccount.name}")
                                } else {
                                    android.widget.Toast.makeText(context, "فشل الطباعة", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("طباعة الكشف", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Balance Showcase Card (Styled in High Density styling)
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (currentBalance >= 0) Color(0xFFDCFCE7) else Color(0xFFFEE2E2)
                        ),
                        shape = RoundedCornerShape(28.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, if (currentBalance >= 0) Color(0xFF86EFAC) else Color(0xFFFCA5A5)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val balanceColor = if (currentBalance >= 0) Color(0xFF15803D) else Color(0xFFB91C1C)
                            val balanceLabel = if (currentAccount.type == "مورد") {
                                if (currentBalance >= 0) "الرصيد المتبقي له علينا (له)" else "تم دفع زيادة له (عليه)"
                            } else {
                                if (currentBalance >= 0) "الرصيد المطلوب سداده منا (عليه)" else "الرصيد المسدد زيادة (له)"
                            }

                            val defaultCurrencySymbol = when (viewModel.defaultCurrency.value) {
                                "USD" -> "$"
                                "SAR" -> "ر.س"
                                "YER" -> "ر.ي"
                                else -> viewModel.defaultCurrency.value
                            }
                            Text(
                                text = "${String.format(Locale.US, "%,.2f", Math.abs(currentBalance))} $defaultCurrencySymbol",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = balanceColor
                            )

                            Text(
                                text = balanceLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = balanceColor
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "لتحميل كشف حساب رسمي ومشاركته اضغط على زر PDF بالأعلى 📄",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "سجل القيود اليومي (30 يوماً)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                    }

                    val transactionsUiState: UiState<List<Transaction>> = remember(transactions) {
                        if (transactions.isEmpty()) {
                            UiState.Empty(
                                title = "كشف الحساب فارغ حالياً",
                                message = "اضغط على 'قيد / حركة جديدة' لإضافة العمليات والقيود المالية الأولى في هذا الحساب.",
                                icon = Icons.Default.ReceiptLong,
                                actionLabel = "إضافة قيد / حركة جديدة ➕"
                            )
                        } else {
                            UiState.Content(transactions)
                        }
                    }

                    ThreeDUiStateLayout(
                        state = transactionsUiState,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        onEmptyAction = {
                            performHaptic()
                            details = ""
                            quantityStr = "1"
                            priceStr = ""
                            additionStr = "0"
                            isPayment = false
                            selectedDateString = ""
                            formError = false
                            showAddTxDialog = true
                        }
                    ) { txList ->
                        val scrollState = rememberScrollState()
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(scrollState)
                                    .threeDTiltEffect(maxRotationDegrees = 3f)
                                    .shadow(6.dp, shape = AppShapes.large)
                                    .background(MaterialTheme.colorScheme.surface, shape = AppShapes.large)
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, AppShapes.large)
                                    .clip(AppShapes.large)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    // Table Header Row with 3D gradient
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(
                                                brush = Brush.horizontalGradient(
                                                    colors = listOf(
                                                        MaterialTheme.colorScheme.primary,
                                                        AppColors.PrimaryRoyalNavy
                                                    )
                                                )
                                            )
                                            .padding(vertical = 10.dp)
                                    ) {
                                        val headers = arrayOf("اليوم", "التاريخ", "التفاصيل والبيان", "الكمية", "سعر الحبة", "الإضافات", "الإجمالي")
                                        val widths = arrayOf(50.dp, 75.dp, 160.dp, 50.dp, 65.dp, 60.dp, 80.dp)
                                        
                                        for (i in headers.indices) {
                                            Text(
                                                text = headers[i],
                                                style = AppTypography.labelMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White,
                                                modifier = Modifier.width(widths[i]),
                                                textAlign = TextAlign.Center
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(90.dp)) // Spacer for action buttons
                                    }

                                    // Table Data Rows
                                    LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                        items(txList.reversed()) { txItem ->
                                            val isPay = txItem.isPayment
                                            val rowBg = if (isPay) AppColors.SuccessGreenLight.copy(alpha = 0.4f) else Color.White
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .background(rowBg)
                                                    .padding(vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                val colWidths = arrayOf(50.dp, 75.dp, 160.dp, 50.dp, 65.dp, 60.dp, 80.dp)
                                            
                                                // Column 1: Day
                                                Text(
                                                    text = txItem.day,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.width(colWidths[0]),
                                                    textAlign = TextAlign.Center,
                                                    color = Color.Black
                                                )
                                                
                                                // Column 2: Date
                                                Text(
                                                    text = txItem.date,
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.width(colWidths[1]),
                                                    textAlign = TextAlign.Center,
                                                    color = Color.Black
                                                )

                                                // Column 3: Details
                                                Text(
                                                    text = txItem.details + (if (isPay) " (واصل دفعة)" else "") + (if (txItem.currency != defaultCurrency) {
                                                        val sym = when (txItem.currency) {
                                                            "USD" -> "$"
                                                            "SAR" -> "ر.س"
                                                            "YER" -> "ر.ي"
                                                            else -> txItem.currency
                                                        }
                                                        " (${txItem.total} $sym)"
                                                    } else ""),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.width(colWidths[2]).padding(horizontal = 4.dp),
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = if (isPay) FontWeight.SemiBold else FontWeight.Normal,
                                                    color = if (isPay) AppColors.SuccessGreen else Color.Black,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                // Column 4: Quantity
                                                Text(
                                                    text = if (isPay) "-" else formatDecimalValue(txItem.quantity, 1),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.width(colWidths[3]),
                                                    textAlign = TextAlign.Center,
                                                    color = Color.Black
                                                )

                                                // Column 5: Unit Price
                                                Text(
                                                    text = if (isPay) "-" else formatDecimalValue(txItem.unitPrice, 2),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.width(colWidths[4]),
                                                    textAlign = TextAlign.Center,
                                                    color = Color.Black
                                                )

                                                // Column 6: Addition
                                                Text(
                                                    text = if (isPay) "-" else formatDecimalValue(txItem.addition, 2),
                                                    fontSize = 11.sp,
                                                    modifier = Modifier.width(colWidths[5]),
                                                    textAlign = TextAlign.Center,
                                                    color = Color.Black
                                                )

                                                // Column 7: Total
                                                Text(
                                                    text = run {
                                                        val convertedTotalAmt = txItem.total * txItem.exchangeRate
                                                        val defaultCurrencyLabelFormatter = when (defaultCurrency) {
                                                            "USD" -> "$"
                                                            "SAR" -> "ر.س"
                                                            "YER" -> "ر.ي"
                                                            else -> defaultCurrency
                                                        }
                                                        formatDecimalValue(convertedTotalAmt, 2) + " " + defaultCurrencyLabelFormatter
                                                    },
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    modifier = Modifier.width(colWidths[6]),
                                                    textAlign = TextAlign.Center,
                                                    color = if (isPay) AppColors.SuccessGreen else Color.Black
                                                )

                                                // WhatsApp Send Invoice/Receipt quick icon
                                                IconButton(
                                                    onClick = {
                                                        performHaptic()
                                                        viewModel.sendWhatsAppTransactionNotification(
                                                            context = context,
                                                            account = currentAccount,
                                                            transaction = txItem,
                                                            onApiComplete = { success, msg ->
                                                                android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                            }
                                                        )
                                                    },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Send,
                                                        contentDescription = "إرسال التفاصيل عبر واتساب",
                                                        tint = AppColors.SuccessGreen,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // WhatsApp Send Due-Date Reminder quick icon (if due date is set)
                                                if (txItem.dueDate.isNotBlank()) {
                                                    IconButton(
                                                        onClick = {
                                                            performHaptic()
                                                            viewModel.sendWhatsAppDueDateNotification(
                                                                context = context,
                                                                account = currentAccount,
                                                                transaction = txItem,
                                                                onApiComplete = { success, msg ->
                                                                    android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()
                                                                }
                                                            )
                                                        },
                                                        modifier = Modifier.size(30.dp)
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Event,
                                                            contentDescription = "إرسال تذكير الاستحقاق",
                                                            tint = Color(0xFFD97706),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                    }
                                                }

                                                // Edit quick icon
                                                IconButton(
                                                    onClick = {
                                                        performHaptic()
                                                        editingTransaction = txItem
                                                    },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "تعديل القيد",
                                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }

                                                // Deletion quick icon
                                                IconButton(
                                                    onClick = {
                                                        performHaptic()
                                                        viewModel.deleteTransaction(txItem)
                                                    },
                                                    modifier = Modifier.size(30.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "حذف القيد",
                                                        tint = AppColors.DangerRed.copy(alpha = 0.8f),
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Custom Calendar Date Picker trigger helper using standard Android dialog
    val calendarInstance = Calendar.getInstance()
    val datePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            selectedDateString = sdf.format(cal.time)
        },
        calendarInstance.get(Calendar.YEAR),
        calendarInstance.get(Calendar.MONTH),
        calendarInstance.get(Calendar.DAY_OF_MONTH)
    )

    val editDatePickerDialog = DatePickerDialog(
        context,
        { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
            val cal = Calendar.getInstance()
            cal.set(Calendar.YEAR, year)
            cal.set(Calendar.MONTH, month)
            cal.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            editSelectedDateString = sdf.format(cal.time)
        },
        calendarInstance.get(Calendar.YEAR),
        calendarInstance.get(Calendar.MONTH),
        calendarInstance.get(Calendar.DAY_OF_MONTH)
    )

    var txCurrency by remember { mutableStateOf("YER") }
    var txExchangeRateStr by remember { mutableStateOf("1.0") }
    
    // Reset or update currency states when dialog gets shown
    LaunchedEffect(showAddTxDialog) {
        if (showAddTxDialog) {
            txCurrency = defaultCurrency
            txExchangeRateStr = "1.0"
        }
    }

    // Form Dialog to Add Transaction
    if (showAddTxDialog) {
        val activeAcc = selectedAccount
        if (activeAcc != null) {
            AlertDialog(
                onDismissRequest = { showAddTxDialog = false },
                title = {
                    Text(
                        text = "تسجيل قيد مالي جديد",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                         // Type choice (فاتورة مشتريات/مبيعات vs دفعة مسددة/واصل)
                         Text(
                             text = "نوع السند / الحركة المالية:",
                             fontWeight = FontWeight.Bold,
                             fontSize = 11.sp,
                             color = MaterialTheme.colorScheme.primary
                         )
 
                         androidx.compose.foundation.lazy.LazyRow(
                             modifier = Modifier
                                 .fillMaxWidth()
                                 .clip(RoundedCornerShape(8.dp))
                                 .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                 .padding(4.dp),
                             horizontalArrangement = Arrangement.spacedBy(4.dp)
                         ) {
                             val typesList = listOf(
                                 "فاتورة بيع نقد", "فاتورة بيع آجل", "فاتورة شراء نقد", "فاتورة شراء آجل",
                                 "سند قبض مالي", "سند صرف مالي", "سند صرف عملات", "قيد يومي عام"
                             )
                             items(typesList) { t ->
                                 val isSelected = docType == t
                                 Box(
                                     modifier = Modifier
                                         .clip(RoundedCornerShape(6.dp))
                                         .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                         .clickable { 
                                             docType = t
                                             isPayment = (t == "سند قبض مالي" || t == "سند صرف مالي" || t == "سند صرف عملات")
                                         }
                                         .padding(horizontal = 12.dp, vertical = 8.dp),
                                     contentAlignment = Alignment.Center
                                 ) {
                                     Text(
                                         text = t,
                                         fontWeight = FontWeight.Bold,
                                         fontSize = 10.sp,
                                         color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                     )
                                 }
                             }
                         }
 
                         // Warehouse & Tiling Triggers
                         Row(
                             modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                             horizontalArrangement = Arrangement.SpaceBetween,
                             verticalAlignment = Alignment.CenterVertically
                         ) {
                             TextButton(
                                 onClick = { 
                                     viewModel.loadInventory()
                                     showStockLookup = true 
                                 },
                                 colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                             ) {
                                 Icon(Icons.Default.Store, contentDescription = null, modifier = Modifier.size(16.dp))
                                 Spacer(modifier = Modifier.width(4.dp))
                                 Text("إدراج مادة مخزنية 📦", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                             }
 
                             TextButton(
                                 onClick = { showMockBarcodeScanner = true },
                                 colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFCA8A04))
                             ) {
                                 Icon(Icons.Default.QrCodeScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                                 Spacer(modifier = Modifier.width(4.dp))
                                 Text("مسح باركود 📸", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                             }
 
                             TextButton(
                                 onClick = { showTilingCalc = !showTilingCalc },
                                 colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                             ) {
                                 Icon(Icons.Default.Architecture, contentDescription = null, modifier = Modifier.size(16.dp))
                                 Spacer(modifier = Modifier.width(4.dp))
                                 Text("حاسبة تمتير 📐", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                             }
                         }
 
                         if (showTilingCalc) {
                             Card(
                                 modifier = Modifier.fillMaxWidth(),
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                             ) {
                                 Column(
                                     modifier = Modifier.padding(12.dp),
                                     verticalArrangement = Arrangement.spacedBy(8.dp),
                                     horizontalAlignment = Alignment.End
                                 ) {
                                     Text("تمتير البلاط والمقاسات (الطول × العرض × العدد)", fontSize = 10.sp, color = MaterialTheme.colorScheme.secondary, fontWeight = FontWeight.Bold)
                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.spacedBy(8.dp)
                                     ) {
                                         OutlinedTextField(
                                             value = tileCount,
                                             onValueChange = { tileCount = it },
                                             label = { Text("العدد", fontSize = 10.sp) },
                                             modifier = Modifier.weight(1f),
                                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                         )
                                         OutlinedTextField(
                                             value = tileWidth,
                                             onValueChange = { tileWidth = it },
                                             label = { Text("العرض م", fontSize = 10.sp) },
                                             modifier = Modifier.weight(1f),
                                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                         )
                                         OutlinedTextField(
                                             value = tileLength,
                                             onValueChange = { tileLength = it },
                                             label = { Text("الطول م", fontSize = 10.sp) },
                                             modifier = Modifier.weight(1f),
                                             keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                         )
                                     }
                                     val lengthD = tileLength.toDoubleOrNull() ?: 0.0
                                     val widthD = tileWidth.toDoubleOrNull() ?: 0.0
                                     val countD = tileCount.toDoubleOrNull() ?: 1.0
                                     val totalMeters = lengthD * widthD * countD
                                     
                                     Row(
                                         modifier = Modifier.fillMaxWidth(),
                                         horizontalArrangement = Arrangement.SpaceBetween,
                                         verticalAlignment = Alignment.CenterVertically
                                     ) {
                                         Button(
                                             onClick = {
                                                 quantityStr = String.format(Locale.US, "%.2f", totalMeters)
                                                 details = details.trim() + " [تمتير: ${tileLength}م × ${tileWidth}م × ${tileCount}ق]"
                                                 showTilingCalc = false
                                             },
                                             colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                             modifier = Modifier.wrapContentWidth()
                                         ) {
                                             Text("اعتماد المقاس ✅", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                         }
                                         Text(
                                             text = "الإجمالي: ${String.format(Locale.US, "%.2f", totalMeters)} م²",
                                             fontSize = 11.sp,
                                             fontWeight = FontWeight.Bold,
                                             color = MaterialTheme.colorScheme.primary
                                         )
                                     }
                                 }
                             }
                         }

                        // Custom Date Picker Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { datePickerDialog.show() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (selectedDateString.isEmpty()) "اليوم تلقائياً (اضغط للتغيير)" else selectedDateString,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Currency Selector Field
                        Text(
                            text = "العملة المستخدمة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // If selected currency is NOT default currency, let them specify custom rate
                            if (txCurrency != defaultCurrency) {
                                OutlinedTextField(
                                    value = txExchangeRateStr,
                                    onValueChange = { txExchangeRateStr = it },
                                    label = { Text("سعر الصرف لـ $defaultCurrency") },
                                    placeholder = { Text("مثال: 1600") },
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .weight(1.7f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf("YER", "USD", "SAR").forEach { curr ->
                                    val isSelected = txCurrency == curr
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable { 
                                                txCurrency = curr
                                                txExchangeRateStr = viewModel.getDefaultExchangeRate(curr, defaultCurrency).toString()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when(curr) {
                                                "YER" -> "ريال"
                                                "USD" -> "دولار"
                                                "SAR" -> "سعودي"
                                                else -> curr
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Details Field
                        OutlinedTextField(
                            value = details,
                            onValueChange = {
                                details = it
                                if (it.isNotBlank()) formError = false
                            },
                            label = { Text("بيان وتفاصيل القيد *") },
                            placeholder = { Text("مثال: مبيعات كلي كرتون عصير") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = formError,
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (!isPayment) {
                            // Quantity & Price & Additions Columns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = additionStr,
                                    onValueChange = { additionStr = it },
                                    label = { Text("زيادة إضافية") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = priceStr,
                                    onValueChange = { priceStr = it },
                                    label = { Text("سعر الحبة *") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = quantityStr,
                                    onValueChange = { quantityStr = it },
                                    label = { Text("الكمية") },
                                    modifier = Modifier.weight(0.8f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        } else {
                            // Payment Mode - single input for amount
                            OutlinedTextField(
                                value = priceStr,
                                onValueChange = { priceStr = it },
                                label = { Text("مبلغ الدفعة المستلمة/المسددة *") },
                                placeholder = { Text("مثال: 5000") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = formError,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsedPrice = priceStr.toDoubleOrNull()
                            if (details.isBlank() || parsedPrice == null) {
                                formError = true
                            } else {
                                val parsedQty = quantityStr.toDoubleOrNull() ?: 1.0
                                val parsedAdd = additionStr.toDoubleOrNull() ?: 0.0
                                val customDate = if (selectedDateString.isEmpty()) null else selectedDateString

                                val docPrefix = "[$docType] "
                                val finalDetailsText = if (details.trim().startsWith("[")) details.trim() else docPrefix + details.trim()
                                
                                // Decrease stock count if item is sold
                                selectedStockItem?.let { item ->
                                    if (docType.contains("بيع")) {
                                        viewModel.sellItemFromStock(item.barcode, parsedQty)
                                    }
                                }
                                selectedStockItem = null // reset

                                // Save Transaction with multi-currency fields
                                viewModel.addTransaction(
                                    accountId = activeAcc.id,
                                    details = finalDetailsText,
                                    quantity = parsedQty,
                                    unitPrice = parsedPrice,
                                    addition = parsedAdd,
                                    isPayment = isPayment,
                                    customDateString = customDate,
                                    currency = txCurrency,
                                    exchangeRate = txExchangeRateStr.toDoubleOrNull() ?: 1.0
                                )

                                // Temporary hold of transaction values to generate immediate notification
                                val mockSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val mockDaySdf = SimpleDateFormat("EEEE", Locale("ar"))
                                val activeDate = customDate ?: mockSdf.format(Date())
                                val parsedDateObj = if (customDate != null) mockSdf.parse(customDate) ?: Date() else Date()
                                val activeDay = mockDaySdf.format(parsedDateObj)
                                val calculatedTotal = if (isPayment) parsedPrice else (parsedQty * parsedPrice) + parsedAdd

                                lastSavedTx = Transaction(
                                    accountId = activeAcc.id,
                                    day = activeDay,
                                    date = activeDate,
                                    details = finalDetailsText,
                                    quantity = if (isPayment) 1.0 else parsedQty,
                                    unitPrice = parsedPrice,
                                    addition = if (isPayment) 0.0 else parsedAdd,
                                    total = calculatedTotal,
                                    isPayment = isPayment,
                                    currency = txCurrency,
                                    exchangeRate = txExchangeRateStr.toDoubleOrNull() ?: 1.0
                                )

                                showAddTxDialog = false
                                showNotificationPrompt = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "حفظ وإدراج القيد", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showAddTxDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }
    }

    // Form Dialog to Edit Transaction
    if (editingTransaction != null) {
        val activeAcc = selectedAccount
        if (activeAcc != null) {
            AlertDialog(
                onDismissRequest = { editingTransaction = null },
                title = {
                    Text(
                        text = "تعديل القيد المالي الحالي",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Type choice (فاتورة مشتريات/مبيعات vs دفعة مسددة/واصل)
                        Text(
                            text = "نوع الحركة المالية المعدلة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (editIsPayment) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { editIsPayment = true }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "دفعة مسددة (سداد/واصل)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (editIsPayment) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (!editIsPayment) MaterialTheme.colorScheme.primary else Color.Transparent)
                                    .clickable { editIsPayment = false }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "فاتورة (شراء/مبيعات)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (!editIsPayment) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // Custom Date Picker Selector
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .border(0.5.dp, Color.Gray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .clickable { editDatePickerDialog.show() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (editSelectedDateString.isEmpty()) "اضغط لتحديد التاريخ" else editSelectedDateString,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        // Currency Selector Field
                        Text(
                            text = "العملة المستخدمة:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // If selected currency is NOT default currency, let them specify custom rate
                            if (editTxCurrency != defaultCurrency) {
                                OutlinedTextField(
                                    value = editTxExchangeRateStr,
                                    onValueChange = { editTxExchangeRateStr = it },
                                    label = { Text("سعر الصرف لـ $defaultCurrency") },
                                    placeholder = { Text("مثال: 1600") },
                                    modifier = Modifier.weight(1.3f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .weight(1.7f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                listOf("YER", "USD", "SAR").forEach { curr ->
                                    val isSelected = editTxCurrency == curr
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                                            .clickable {
                                                editTxCurrency = curr
                                                editTxExchangeRateStr = viewModel.getDefaultExchangeRate(curr, defaultCurrency).toString()
                                            }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = when(curr) {
                                                "YER" -> "ريال"
                                                "USD" -> "دولار"
                                                "SAR" -> "سعودي"
                                                else -> curr
                                            },
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        // Details Field
                        OutlinedTextField(
                            value = editDetails,
                            onValueChange = {
                                editDetails = it
                                if (it.isNotBlank()) editFormError = false
                            },
                            label = { Text("بيان وتفاصيل القيد *") },
                            placeholder = { Text("مثال: مبيعات كلي كرتون عصير") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = editFormError,
                            shape = RoundedCornerShape(8.dp)
                        )

                        if (!editIsPayment) {
                            // Quantity & Price & Additions Columns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = editAdditionStr,
                                    onValueChange = { editAdditionStr = it },
                                    label = { Text("زيادة إضافية") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = editPriceStr,
                                    onValueChange = { editPriceStr = it },
                                    label = { Text("سعر الحبة *") },
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )

                                OutlinedTextField(
                                    value = editQuantityStr,
                                    onValueChange = { editQuantityStr = it },
                                    label = { Text("الكمية") },
                                    modifier = Modifier.weight(0.8f),
                                    shape = RoundedCornerShape(8.dp),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        } else {
                            // Payment Mode - single input for amount
                            OutlinedTextField(
                                value = editPriceStr,
                                onValueChange = { editPriceStr = it },
                                label = { Text("مبلغ الدفعة المستلمة/المسددة *") },
                                placeholder = { Text("مثال: 5000") },
                                modifier = Modifier.fillMaxWidth(),
                                isError = editFormError,
                                shape = RoundedCornerShape(8.dp),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val parsedPrice = editPriceStr.toDoubleOrNull()
                            val original = editingTransaction
                            if (editDetails.isBlank() || parsedPrice == null) {
                                editFormError = true
                            } else if (original != null) {
                                val parsedQty = editQuantityStr.toDoubleOrNull() ?: 1.0
                                val parsedAdd = editAdditionStr.toDoubleOrNull() ?: 0.0
                                val calculatedTotal = if (editIsPayment) parsedPrice else (parsedQty * parsedPrice) + parsedAdd

                                val mockSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val mockDaySdf = SimpleDateFormat("EEEE", Locale("ar"))
                                val dateToParse = editSelectedDateString.ifBlank { original.date }
                                val parsedDateObj = try {
                                    mockSdf.parse(dateToParse) ?: Date()
                                } catch(e: Exception) {
                                    Date()
                                }
                                val updatedDay = mockDaySdf.format(parsedDateObj)

                                val updated = original.copy(
                                    details = editDetails.trim(),
                                    quantity = if (editIsPayment) 1.0 else parsedQty,
                                    unitPrice = parsedPrice,
                                    addition = if (editIsPayment) 0.0 else parsedAdd,
                                    total = calculatedTotal,
                                    isPayment = editIsPayment,
                                    date = dateToParse,
                                    day = updatedDay,
                                    currency = editTxCurrency,
                                    exchangeRate = editTxExchangeRateStr.toDoubleOrNull() ?: 1.0
                                )

                                viewModel.updateTransaction(updated)
                                editingTransaction = null
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(text = "تعديل القيد وحفظه", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { editingTransaction = null }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        if (showInvoiceTemplateDesignerDialog) {
            val currentAccount = selectedAccount
            if (currentAccount != null) {
                val currentAccountWithBalance = accountsWithBalance.find { it.account.id == currentAccount.id }
                val currentBalance = currentAccountWithBalance?.balance ?: 0.0
                InvoiceTemplateDesignerDialog(
                    viewModel = viewModel,
                    currentAccount = currentAccount,
                    transactions = transactions,
                    currentBalance = currentBalance,
                    onDismiss = { showInvoiceTemplateDesignerDialog = false }
                )
            }
        }
    }

    // Notification automation prompt dialog
    if (showNotificationPrompt) {
        val currAcc = selectedAccount
        val savedTx = lastSavedTx
        if (currAcc != null && savedTx != null) {
            val accountsWithBalanceList by viewModel.accountsWithBalance.collectAsState()
            val matchedAcc = accountsWithBalanceList.find { it.account.id == currAcc.id }
            val balance = matchedAcc?.balance ?: 0.0

            AlertDialog(
                onDismissRequest = { showNotificationPrompt = false },
                title = {
                    Text(
                        text = "تم حفظ القيد بنجاح! 🎉",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "هل ترغب في أتمتة وإرسال إشعار فوري وتفصيلي للعميل '${currAcc.name}' عبر واتساب أو الرسائل القصيرة SMS بالحركة المبرمة اليوم ورصيده الكلي المتبقي؟",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                            textAlign = TextAlign.Right
                        )
                    }
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val messageToShare = NotificationHelper.generateShareMessage(
                            businessName = viewModel.businessName.value,
                            account = currAcc,
                            transaction = savedTx,
                            currentBalance = balance
                        )

                        Button(
                            onClick = {
                                NotificationHelper.shareReceiptWithImage(
                                    context = context,
                                    businessName = viewModel.businessName.value,
                                    account = currAcc,
                                    transaction = savedTx,
                                    currentBalance = balance
                                )
                                showNotificationPrompt = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("إرسال كـ إيصال مصوّر مميز (واتساب والمشاركة) 🖼️", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        Button(
                            onClick = {
                                NotificationHelper.sendViaWhatsApp(context, currAcc.phone, messageToShare)
                                showNotificationPrompt = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF22C55E)), // WA green
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("إرسال عبر واتساب (WhatsApp)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        Button(
                            onClick = {
                                NotificationHelper.sendViaSMS(context, currAcc.phone, messageToShare)
                                showNotificationPrompt = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("إرسال كـ رسالة نصية (SMS)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Icon(Icons.Default.Sms, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }

                        TextButton(
                            onClick = { showNotificationPrompt = false },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("تخطي وإرجاع لكشف الحساب", textAlign = TextAlign.Center)
                        }
                    }
                },
                dismissButton = {}
            )
        }

    }

    if (showVoiceSmartEntryDialog) {
        VoiceSmartEntryDialog(
            viewModel = viewModel,
            onDismiss = { showVoiceSmartEntryDialog = false },
            onEntrySaved = { }
        )
    }

    // --- Stock Lookup Dialogue ---
    if (showStockLookup) {
        AlertDialog(
            onDismissRequest = { showStockLookup = false },
            title = {
                Text(
                    text = "مخزن ومستودع المنتجات والمواد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = stockSearchQuery,
                        onValueChange = { stockSearchQuery = it },
                        label = { Text("بحث باسم السلعة أو الباركود 🔍") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    val filteredStock = stockItems.filter {
                        it.name.contains(stockSearchQuery, ignoreCase = true) || 
                        it.barcode.contains(stockSearchQuery)
                    }
                    
                    if (filteredStock.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(120.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("المخزن فارغ أو غير متطابق 📦", fontSize = 12.sp, color = Color.Gray)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = {
                                        viewModel.addInventoryItem("كابل سلك النخبة يمني", "1111", 400.0, 550.0, 200.0, "حبة")
                                        viewModel.addInventoryItem("بلاط سيراميك يمني ممتاز", "2222", 1200.0, 1600.0, 450.0, "متر")
                                        viewModel.addInventoryItem("أسمنت الوطنية كيس", "3333", 4500.0, 5200.0, 100.0, "كيس")
                                        viewModel.loadInventory()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text("شحن أصناف تجريبية فوراً ⚡", fontSize = 10.sp)
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredStock) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedStockItem = item
                                            details = item.name
                                            priceStr = item.salePrice.toString()
                                            showStockLookup = false
                                        },
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = androidx.compose.foundation.BorderStroke(0.5.dp, Color.Gray.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(horizontalAlignment = Alignment.Start) {
                                            Text("سعر البيع: ${item.salePrice}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Text("الكمية بالرفوف: ${item.stockQuantity} ${item.unit}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(item.name, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("باركود: ${item.barcode} | الوحدة: ${item.unit}", fontSize = 10.sp, color = Color.Gray)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStockLookup = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // --- Mock Barcode Scanner ---
    if (showMockBarcodeScanner) {
        AlertDialog(
            onDismissRequest = { showMockBarcodeScanner = false },
            title = {
                Text(
                    text = "قارئ الباركود الذكي عبر الكاميرا 📸",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "جاري مسح الباركود تلقائياً... وجه ممر الكاميرا الخلفي نحو الرمز الشريطي للسلعة",
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                    
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(130.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(2.dp)
                                .background(Color.Red)
                        )
                        Icon(
                            Icons.Default.QrCode,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.3f),
                            modifier = Modifier.size(64.dp)
                        )
                    }

                    var simulatedProgress by remember { mutableStateOf(0.0f) }
                    LaunchedEffect(Unit) {
                        while (simulatedProgress < 1.0f) {
                            kotlinx.coroutines.delay(100)
                            simulatedProgress += 0.1f
                        }
                        mockScannerBeep = true
                        val matchedItem = if (stockItems.isNotEmpty()) stockItems.random() else com.example.data.model.InventoryItem(barcode = "3333", name = "أسمنت الوطنية كيس", purchasePrice = 4500.0, salePrice = 5200.0, stockQuantity = 100.0, unit = "كيس")
                        selectedStockItem = matchedItem
                        details = matchedItem.name
                        priceStr = matchedItem.salePrice.toString()
                        showMockBarcodeScanner = false
                    }
                    
                    Text(
                        text = "محاكاة قراءة ذكية... بيب! 💡🔊",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showMockBarcodeScanner = false }) {
                    Text("إلغاء المسح")
                }
            }
        )
    }
}
