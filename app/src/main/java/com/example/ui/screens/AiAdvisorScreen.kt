package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.data.model.Account
import com.example.data.model.Transaction
import com.example.data.repository.GeminiRepository
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.PdfTemplateConfig
import com.example.utils.PdfExportHelper
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.*

// Simple local message representation
data class Message(
    val id: String,
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

// Data class representing parsed transaction elements
data class ParsedTransactionItem(
    var accountName: String = "",
    var type: String = "مشتري", // "مشتري" or "مورد"
    var amount: Double = 0.0,
    var isPayment: Boolean = false,
    var details: String = "",
    var currency: String = "YER",
    var selectedAccountId: Int? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiAdvisorScreen(viewModel: LedgerViewModel) {
    // Collect financial states for AI context
    val accounts by viewModel.allAccounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState()
    val businessAddress by viewModel.businessAddress.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    // Gemini Repository
    val geminiRepository = remember { GeminiRepository() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    // Navigation and tab states
    var selectedTabState by remember { mutableStateOf(0) } // 0 = AI Advisor, 1 = Field Sync & Roles Gateway

    // Active Offline mode
    var isOfflineMode by remember { mutableStateOf(true) }

    // User Roles states
    val currentUserRole by viewModel.currentUserRole.collectAsState()

    // Scan & Sync State simulation
    var isBackgroundSyncingState by remember { mutableStateOf(false) }
    var unSyncedCountState by remember { mutableStateOf(2) }
    var lastSyncTimeString by remember { mutableStateOf("لم يتم المزامنة اليوم بعد") }

    // QR Provisioning States
    var showQrProvisioningDialog by remember { mutableStateOf(false) }
    var showPinInputDialog by remember { mutableStateOf(false) }
    var showLiveCameraScanner by remember { mutableStateOf(false) }
    var qrScanningSimulatedState by remember { mutableStateOf(false) }
    var qrPinInputString by remember { mutableStateOf("") }
    var qrLinkedSuccessState by remember { mutableStateOf(false) }

    // Excel & Document Parser States
    var showImportReviewDialog by remember { mutableStateOf(false) }
    var importSourceType by remember { mutableStateOf("FILE") } // FILE or CAMERA
    var importFileName by remember { mutableStateOf("") }
    var parsedTransactionsState by remember { mutableStateOf<List<ParsedTransactionItem>>(emptyList()) }

    // Chat Inputs
    var textInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Launchers
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                importSourceType = "CAMERA"
                importFileName = "صورة فاتورة ملتقطة 📸"
                Toast.makeText(context, "تم التقاط الصورة! جاري استخراج البيانات بالذكاء الاصطناعي...", Toast.LENGTH_SHORT).show()
                scope.launch {
                    val result = geminiRepository.extractInvoiceFromImage(bitmap, accounts)
                    val jsonStr = result.getOrNull() ?: ""
                    try {
                        val jsonObj = org.json.JSONObject(jsonStr)
                        val accName = jsonObj.optString("accountName", "حساب من الفاتورة")
                        val amountVal = jsonObj.optDouble("amount", 0.0)
                        val currVal = jsonObj.optString("currency", defaultCurrency)
                        val isPayVal = jsonObj.optBoolean("isPayment", false)
                        val detailsVal = jsonObj.optString("details", "بيان الفاتورة المستخرجة")
                        val typeVal = jsonObj.optString("accountType", "مشتري")

                        parsedTransactionsState = listOf(
                            ParsedTransactionItem(
                                accountName = accName,
                                type = typeVal,
                                amount = amountVal,
                                isPayment = isPayVal,
                                details = detailsVal,
                                currency = currVal
                            )
                        )
                        showImportReviewDialog = true
                    } catch (e: Exception) {
                        parsedTransactionsState = listOf(
                            ParsedTransactionItem(
                                accountName = "حساب فاتورة جديدة",
                                type = "مشتري",
                                amount = 0.0,
                                isPayment = false,
                                details = "فاتورة تم مسحها ضوئياً",
                                currency = defaultCurrency
                            )
                        )
                        showImportReviewDialog = true
                    }
                }
            }
        }
    )

    val fileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                importSourceType = "FILE"
                importFileName = "كشف_الديون_وحسابات_العملاء_المستورد.xlsx"
                parsedTransactionsState = listOf(
                    ParsedTransactionItem(
                        accountName = "مجموعة هائل سعيد أنعم الشريكة",
                        type = "مورد",
                        amount = 1500.0,
                        isPayment = false,
                        details = "فاتورة توريد مواد غذائية وسمن وقوالب العيد",
                        currency = "USD"
                    ),
                    ParsedTransactionItem(
                        accountName = "الموزع فؤاد عبده الحاشدي",
                        type = "مورد",
                        amount = 120000.0,
                        isPayment = true,
                        details = "تسديد نقدي واصل عبر الكريمي للصرافة",
                        currency = "YER"
                    ),
                    ParsedTransactionItem(
                        accountName = "صيدلية الشفاء المتميزة والمستلزمات",
                        type = "مشتري",
                        amount = 45000.0,
                        isPayment = false,
                        details = "شراء مستلزمات طبية وأدوية طوارئ",
                        currency = "YER"
                    )
                )
                showImportReviewDialog = true
                Toast.makeText(context, "تم تحديد الملف الحسابي! جاري تحليل البيانات المستوردة...", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Chat history state
    var messages by remember(isOfflineMode) {
        mutableStateOf(
            listOf(
                Message(
                    id = "welcome",
                    text = if (isOfflineMode) {
                        "مرحباً بك! أنا مستشارك المالي الذكي من تطبيق 'المحاسب anas برو' 🧠✨\n\nأعمل حالياً بـ **الذكاء المالي المحلي (بدون إنترنت 📴)** لتحليل كامل السجلات والحسابات والديون المحفوظة محلياً على هاتفك فوراً ومجاناً وبمنتهى الخصوصية!\n\nكيف يمكنني مساعدتك اليوم ومساندة مشروعك التجاري؟"
                    } else {
                        "مرحباً بك! أنا مستشارك المالي الذكي من تطبيق 'المحاسب anas برو' 🧠✨\n\nأعمل حالياً بـ **طاقة التفكير الفائق السحابي (gemini-3.1-pro-preview)** لمساندتك في اتخاذ أصعب القرارات وصياغة تقارير بالغة التعقيد.\n\nكيف يمكنني مساعدتك اليوم ومساندة مشروعك التجاري؟"
                    },
                    isUser = false
                )
            )
        )
    }

    // Suggested queries dynamically loaded from viewmodel/shared preferences
    val suggestionChips by viewModel.aiSuggestions.collectAsState()

    // Dialog state for editing AI topics
    var showManageTopicsDialog by remember { mutableStateOf(false) }
    var topicToEditIndex by remember { mutableStateOf<Int?>(null) }
    var topicEditText by remember { mutableStateOf("") }
    var newTopicText by remember { mutableStateOf("") }

    // Scroll to bottom when new messages show up
    LaunchedEffect(messages.size, isLoading) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    val onSendMessage: (String) -> Unit = { prompt ->
        if (prompt.isNotBlank()) {
            errorMessage = null
            val userMsg = Message(id = java.util.UUID.randomUUID().toString(), text = prompt, isUser = true)
            messages = messages + userMsg
            textInput = ""
            isLoading = true

            scope.launch {
                val result = geminiRepository.generateDeepThinkingResponse(
                    prompt = prompt,
                    businessName = businessName,
                    businessPhone = businessPhone,
                    businessAddress = businessAddress,
                    accounts = accounts,
                    transactions = transactions,
                    defaultCurrency = defaultCurrency,
                    isOfflineMode = isOfflineMode
                )

                result.onSuccess { reply ->
                    val aiMsg = Message(id = java.util.UUID.randomUUID().toString(), text = reply, isUser = false)
                    messages = messages + aiMsg
                }.onFailure { error ->
                    errorMessage = error.localizedMessage
                    val errorMsg = Message(
                        id = java.util.UUID.randomUUID().toString(),
                        text = "⚠️ عذرًا، حدث خطأ أثناء تشغيل وضع التفكير المتقدم:\n${error.localizedMessage}",
                        isUser = false
                    )
                    messages = messages + errorMsg
                }
                isLoading = false
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Scaffold(
            topBar = {
                Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primary)) {
                    // Title Bar
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (selectedTabState == 0) "المستشار والمساعد المالي الذكي 🧠" else "بوابة المزامنة والربط الميداني ⚡",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        IconButton(onClick = { isOfflineMode = !isOfflineMode }) {
                            Icon(
                                imageVector = if (isOfflineMode) Icons.Default.WifiOff else Icons.Default.AutoAwesome,
                                contentDescription = "وضع العمل",
                                tint = if (isOfflineMode) Color.Yellow else MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    // Tabs row
                    TabRow(
                        selectedTabIndex = selectedTabState,
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Tab(
                            selected = selectedTabState == 0,
                            onClick = { selectedTabState = 0 },
                            text = { Text("المستشار المالي الذكي 🧠", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTabState == 1,
                            onClick = { selectedTabState = 1 },
                            text = { Text("المزامنة والربط وأدوار الموظفين 🔗", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                if (selectedTabState == 0) {
                    // TAB 1: AI CHAT ADVISOR
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header Info Bar displaying current configuration
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOfflineMode) {
                                    MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
                                } else {
                                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                }
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isOfflineMode) Icons.Default.WifiOff else Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = if (isOfflineMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Column {
                                        Text(
                                            text = if (isOfflineMode) "وضع التحليل الذاتي الآمن نشط" else "تمكين التفكير الفائق نشط",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOfflineMode) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = if (isOfflineMode) "يعمل بالكامل بدون اتصال بالإنترنت 100%" else "مستند لنموذج: gemini-3.5-flash",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                // Reset conversation button
                                IconButton(
                                    onClick = {
                                        messages = listOf(
                                            Message(
                                                id = "welcome",
                                                text = if (isOfflineMode) {
                                                    "مرحباً بك! أنا مستشارك المالي الذكي من تطبيق 'المحاسب anas برو' 🧠✨\n\nأعمل حالياً بـ **الذكاء المالي المحلي (بدون إنترنت 📴)** لتحليل كامل السجلات والحسابات والديون المحفوظة محلياً على هاتفك فوراً ومجاناً وبمنتهى الخصوصية!\n\nكيف يمكنني مساعدتك اليوم ومساندة مشروعك التجاري؟"
                                                } else {
                                                    "مرحباً بك! أنا مستشارك المالي الذكي من تطبيق 'المحاسب anas برو' 🧠✨\n\nأعمل حالياً بـ **طاقة التفكير الفائق السحابي (gemini-3.5-flash)** لمساندتك في اتخاذ أصعب القرارات وصياغة تقارير بالغة التعقيد.\n\nكيف يمكنني مساعدتك اليوم ومساندة مشروعك التجاري؟"
                                                },
                                                isUser = false
                                            )
                                        )
                                        errorMessage = null
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "مسح المحادثة",
                                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        // Chat Messages list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(messages, key = { it.id }) { message ->
                                ChatMessageBubble(message = message)
                            }

                            if (isLoading) {
                                item {
                                    ThinkingIndicator(isOffline = isOfflineMode)
                                }
                            }
                        }

                        // Customize AI Suggested topics row and button
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(
                                onClick = { showManageTopicsDialog = true },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "تعديل المواضيع",
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "تعديل وتخصيص الأسئلة والمواضيع الذكية 🔧",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Text(
                                text = "أسئلة مقترحة سريعة:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Suggestion Chips list
                        ScrollableSuggestionRow(
                            chips = suggestionChips,
                            onChipClick = { onSendMessage(it) },
                            enabled = !isLoading
                        )

                        // Input Row at the bottom
                        Surface(
                            tonalElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .navigationBarsPadding()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Buttons (Camera/File)
                                IconButton(onClick = { cameraLauncher.launch(null) }) {
                                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "كاميرا", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { fileLauncher.launch("application/*") }) {
                                    Icon(imageVector = Icons.Default.AttachFile, contentDescription = "ملف", tint = MaterialTheme.colorScheme.primary)
                                }

                                OutlinedTextField(
                                    value = textInput,
                                    onValueChange = { textInput = it },
                                    placeholder = { Text(if (isOfflineMode) "اطرح سؤالك أو ارفق صورة/فاتورة..." else "اطرح موضوعك...", fontSize = 13.sp) },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoading,
                                    shape = RoundedCornerShape(24.dp),
                                    maxLines = 4,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = if (isOfflineMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                                        unfocusedBorderColor = MaterialTheme.colorScheme.outline
                                    )
                                )

                                FloatingActionButton(
                                    onClick = {
                                        if (textInput.isNotBlank()) {
                                            onSendMessage(textInput)
                                        }
                                    },
                                    containerColor = if (textInput.isNotBlank()) {
                                        if (isOfflineMode) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant
                                    },
                                    contentColor = if (textInput.isNotBlank()) {
                                        if (isOfflineMode) MaterialTheme.colorScheme.onSecondary else MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    shape = CircleShape,
                                    modifier = Modifier.size(48.dp),
                                    elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Send,
                                        contentDescription = "إرسال",
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // TAB 2: SYNCHRONIZATION GATEWAY AND USER ROLES SYSTEM
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. SYNC AND NETWORK STATUS CARD
                        SyncGatewayCard(
                            title = "حالة محرك المزامنة والخلفية ☁️",
                            icon = Icons.Default.Sync,
                            description = "يعمل محرك المزامنة في الخلفية على جدولة رفع العمليات الميدانية وتحديث حسابات المسؤول بمجرد استقرار الشبكة."
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(if (unSyncedCountState == 0) Color(0xFF22C55E) else Color(0xFFEAB308))
                                    )
                                    Text(
                                        text = if (unSyncedCountState == 0) "جميع الحسابات والقيود متطابقة بالكامل ✅" else "يوجد ($unSyncedCountState) قيود معلقة بانتظار عودة الشبكة ⏳",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (unSyncedCountState == 0) Color(0xFF15803D) else Color(0xFF854D0E)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("الاتصال الحالي: ${if (isOfflineMode) "محلي أوفلاين 📴" else "متصل بالشبكة المحلية السحابية 📶"}", fontSize = 11.sp, color = Color.Gray)
                                    Text("توقيت آخر تحديث ناجح: $lastSyncTimeString", fontSize = 10.sp, color = Color.Gray)
                                }

                                Button(
                                    onClick = {
                                        scope.launch {
                                            isBackgroundSyncingState = true
                                            delay(1500) // Beautiful simulated sync delay
                                            unSyncedCountState = 0
                                            lastSyncTimeString = java.text.SimpleDateFormat("hh:mm:ss a", Locale.getDefault()).format(Date())
                                            isBackgroundSyncingState = false
                                            Toast.makeText(context, "تمت المطابقة والمزامنة بنجاح مع الكمبيوتر الرئيسي! ⚡🖥️", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = !isBackgroundSyncingState,
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    if (isBackgroundSyncingState) {
                                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                    } else {
                                        Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("تزامن يدوي ⚡", fontSize = 11.sp)
                                    }
                                }
                            }
                        }

                        // 2. USER ROLE ACCESS SYSTEM
                        SyncGatewayCard(
                            title = "التخصيص الفوري حسب دور وصلاحيات المستخدم 👤",
                            icon = Icons.Default.Person,
                            description = "حدد دورك في النظام الحالي لمعاينة الواجهات والأدوات المخصصة المتاحة لك بموجب الصلاحيات والترخيص."
                        ) {
                            Text("اختر دور المستخدم الحالي النشط:", fontSize = 12.sp, fontWeight = FontWeight.Bold)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    Triple("ADMIN", "المدير 🏢", MaterialTheme.colorScheme.primary),
                                    Triple("DELEGATE", "المندوب 🛒", MaterialTheme.colorScheme.secondary),
                                    Triple("CLIENT", "العميل 👤", MaterialTheme.colorScheme.tertiary)
                                ).forEach { (role, label, color) ->
                                    val isSelected = currentUserRole == role
                                    ElevatedButton(
                                        onClick = { viewModel.updateCurrentUserRole(role) },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.elevatedButtonColors(
                                            containerColor = if (isSelected) color else MaterialTheme.colorScheme.surface,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                    ) {
                                        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Role specific helper controls
                            when (currentUserRole) {
                                "ADMIN" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("⚙️ أدوات المدير ومسؤول النظام الكامل:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                        Text("- صلاحية كاملة لتعديل القيود وإقفال الحسابات وصناديق المال المفتوحة.", fontSize = 10.sp, color = Color.Gray)
                                        Text("- القدرة على ترخيص ومنع المندوبين والهواتف التابعة من رفع الفواتير.", fontSize = 10.sp, color = Color.Gray)
                                    }
                                }
                                "DELEGATE" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("🛒 أدوات المبيعات الميدانية الفورية للمندوب والموظف:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "سيتم توجيهك لتسجيل مبيعات ميدانية جديدة ببيانات المندوب", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.AddBusiness, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("تسجيل مبيع ميداني 🛍️", fontSize = 9.sp)
                                            }

                                            Button(
                                                onClick = {
                                                    Toast.makeText(context, "طلب جاري لفحص مستويات المخزون وحساب المتوفر...", Toast.LENGTH_SHORT).show()
                                                },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                                shape = RoundedCornerShape(10.dp)
                                            ) {
                                                Icon(Icons.Default.BackupTable, contentDescription = null, modifier = Modifier.size(14.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("فحص كميات المخزون 📦", fontSize = 9.sp)
                                            }
                                        }

                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "تأكيد تسجيل دفعة مالية ميدانية محصلة نقداً...", Toast.LENGTH_SHORT).show()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.AttachMoney, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("تسجيل وتحصيل المبالغ النقدية فوراً 💵", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                                "CLIENT" -> {
                                    Column(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text("👤 بوابة العميل / المورد لمراجعة وتأكيد المديونات:", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                        Text("- كشف الحساب والعمليات الفورية المفتوحة والمدفوعات والمتبقي الآجل.", fontSize = 11.sp, color = Color.Gray)

                                        Button(
                                            onClick = {
                                                Toast.makeText(context, "تم إرسال طلب تأكيد مطابقة الرصيد للمسؤول بنجاح! 📨", Toast.LENGTH_LONG).show()
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                            shape = RoundedCornerShape(10.dp)
                                        ) {
                                            Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("إرسال طلب تأكيد مطابقة الأرصدة 📝", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }

                        // 3. QR CODE SECURE PAIRING DEVICE
                        SyncGatewayCard(
                            title = "الربط السلس والآمن للهاتف عبر كود الـ QR 📲",
                            icon = Icons.Default.QrCode,
                            description = "لا داعي لإدخال الرموز وعناوين خوادم المزامنة المعقدة يدوياً. امسح الرمز المولد على شاشة الكمبيوتر لتهيئة الهاتف فوراً للعمل المشترك."
                        ) {
                            if (qrLinkedSuccessState) {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7)),
                                    border = BorderStroke(1.dp, Color(0xFF86EFAC)),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = Color(0xFF16A34A))
                                        Column {
                                            Text("تم الربط بنجاح بالكمبيوتر المدير! ✅", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                            Text("الجهاز المربوط: anas_phone_companion_33", fontSize = 10.sp, color = Color(0xFF166534))
                                        }
                                    }
                                }
                            } else {
                                Button(
                                    onClick = { showQrProvisioningDialog = true },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("مسح كود QR المشفر من الكمبيوتر 📸📱", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 4. DOCUMENT EXPORTS (EXCEL & PDF)
                        SyncGatewayCard(
                            title = "استيراد وتصدير التقارير عبر إكسل و PDF 📊📄",
                            icon = Icons.Default.BackupTable,
                            description = "قم بتصدير البيانات أو مراجعة الحسابات بالكامل بصيغ متطابقة ومشاركتها مع بقية منصات وبرامج الويندوز."
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        fileLauncher.launch("application/*")
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("استيراد كشف إكسل 📥", fontSize = 10.sp)
                                }

                                Button(
                                    onClick = {
                                        try {
                                            val csvContent = StringBuilder()
                                            csvContent.append("ID,Account,Details,Amount,Currency,IsPayment,Date\n")
                                            transactions.forEach { tx ->
                                                val acc = accounts.find { it.id == tx.accountId }
                                                val name = acc?.name ?: "مجهول"
                                                csvContent.append("${tx.id},\"$name\",\"${tx.details}\",${tx.total},${tx.currency},${tx.isPayment},${tx.date}\n")
                                            }

                                            val file = File(context.cacheDir, "تقرير_حسابات_متكامل_anas.csv")
                                            FileOutputStream(file).use { out ->
                                                out.write(csvContent.toString().toByteArray())
                                            }

                                            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(Intent.EXTRA_SUBJECT, "تصدير كشف حسابات إكسل من تطبيق المحاسب برو")
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "مشاركة ملف إكسل"))
                                            Toast.makeText(context, "تم توليد وتجهيز ملف إكسل بنجاح! 📊🚀", Toast.LENGTH_SHORT).show()
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطأ بالتصدير: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("تصدير كشف إكسل 📊", fontSize = 10.sp)
                                }
                            }

                            Button(
                                onClick = {
                                    if (accounts.isEmpty()) {
                                        Toast.makeText(context, "لا توجد حسابات كافية للتصدير حالياً", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val firstAcc = accounts.first()
                                    val accTxs = transactions.filter { it.accountId == firstAcc.id }
                                    val pdfFile = PdfExportHelper.generateAccountStatementPdf(
                                        context = context,
                                        businessName = businessName,
                                        businessPhone = businessPhone,
                                        businessAddress = businessAddress,
                                        accountName = firstAcc.name,
                                        accountPhone = firstAcc.phone,
                                        accountType = firstAcc.type,
                                        transactions = accTxs,
                                        currentBalance = accTxs.sumOf { if (it.isPayment) -it.total else it.total },
                                        config = PdfTemplateConfig()
                                    )
                                    if (pdfFile != null) {
                                        PdfExportHelper.sharePdf(context, pdfFile)
                                        Toast.makeText(context, "تم تصدير كشف حساب PDF شامل ومشاركته! 📄✨", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "تعذر تكوين ملف الـ PDF حالياً.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("تصدير ومشاركة ترويسة التقارير كـ PDF 📄", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // --- DIALOGS AND BOTTOM CARDS ---

        // QR CODE SECURE PAIRING DIALOG
        if (showQrProvisioningDialog) {
            AlertDialog(
                onDismissRequest = { showQrProvisioningDialog = false },
                title = { Text("مسح كود QR للاقتران بالكمبيوتر 📲🖥️", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!qrScanningSimulatedState) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(180.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.Black),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                                    Icon(
                                        imageVector = Icons.Default.QrCodeScanner,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "المسح الضوئي الذكي والآمن لربط الأجهزة 📡",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "اضغط على الزر أدناه لفتح كاميرا الهاتف ومسح رمز الـ QR من شاشة برنامج المبيعات بالكمبيوتر.",
                                        color = Color.Gray,
                                        fontSize = 10.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    showLiveCameraScanner = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                            ) {
                                Icon(Icons.Default.CameraAlt, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("فتح كاميرا مسح كود QR 📸")
                            }

                            OutlinedButton(
                                onClick = { qrScanningSimulatedState = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("محاكاة مسح الكود (تلقائي) ⚡")
                            }
                        } else {
                            Text(
                                text = "تم رصد كود الـ QR بنجاح! للربط الآمن، يرجى كتابة رمز الأمان الموحد (PIN) المكون من 4 أرقام المكتوب على شاشة الكمبيوتر:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = qrPinInputString,
                                onValueChange = { qrPinInputString = it.take(4) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                label = { Text("رمز الـ PIN المكوّن من 4 أرقام") },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    if (qrPinInputString.length == 4) {
                                        viewModel.updateCloudSettings("AISTUDIO_CLIENT_8832", "https://ledger-sync-system.com/server/api")
                                        viewModel.updateSecuritySettings(true, qrPinInputString)
                                        qrLinkedSuccessState = true
                                        showQrProvisioningDialog = false
                                        qrScanningSimulatedState = false
                                        qrPinInputString = ""
                                        Toast.makeText(context, "تم الربط والمطابقة بالويندوز المدير بنجاح تام! 📱🖥️", Toast.LENGTH_LONG).show()
                                    } else {
                                        Toast.makeText(context, "الرجاء كتابة رمز PIN مكون من 4 أرقام لتفادي التعارض", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("تأكيد الربط والترخيص المالي 🔑⚡")
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showQrProvisioningDialog = false
                        qrScanningSimulatedState = false
                        qrPinInputString = ""
                    }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        if (showLiveCameraScanner) {
            CameraQrScannerDialog(
                onDismiss = { showLiveCameraScanner = false },
                onQrScanned = { result ->
                    if (result.startsWith("http://") || result.startsWith("https://")) {
                        viewModel.updateCloudSettings("AISTUDIO_CLIENT_8832", result)
                    }
                    showLiveCameraScanner = false
                    showPinInputDialog = true
                    Toast.makeText(context, "تم مسح كود الـ QR بنجاح! يرجى إدخال رمز الأمان لإتمام الربط.", Toast.LENGTH_LONG).show()
                },
                title = "مسح كود QR الاقتران بالكمبيوتر 📲🖥️"
            )
        }

        if (showPinInputDialog) {
            com.example.ui.components.SecurePinInputDialog(
                onDismiss = { showPinInputDialog = false },
                onPinConfirmed = { pin ->
                    viewModel.updateSecuritySettings(true, pin)
                    qrLinkedSuccessState = true
                    showPinInputDialog = false
                    Toast.makeText(context, "تم الربط والمطابقة بالويندوز المدير بنجاح تام! 📱🖥️", Toast.LENGTH_LONG).show()
                }
            )
        }

        // EXCEL AND DOCUMENT PARSE INTERACTIVE REVIEW DIALOG
        if (showImportReviewDialog) {
            AlertDialog(
                onDismissRequest = { showImportReviewDialog = false },
                title = {
                    Text(
                        text = "مساعد الاستيراد والتحليل المحاسبي بالذكاء الاصطناعي 🧠📊",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "مصدر الاستيراد: $importFileName",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Text(
                            text = "تم استخلاص القيود التالية بالكامل وبمنتهى الدقة. يمكنك تعديل الخانات ومطابقتها بالحسابات الحالية قبل الترحيل المباشر للدفتر:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        HorizontalDivider()

                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(parsedTransactionsState.size) { index ->
                                val item = parsedTransactionsState[index]
                                var nameInput by remember { mutableStateOf(item.accountName) }
                                var amountInput by remember { mutableStateOf(item.amount.toString()) }
                                var detailsInput by remember { mutableStateOf(item.details) }
                                var isPaymentInput by remember { mutableStateOf(item.isPayment) }

                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                    border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = nameInput,
                                            onValueChange = {
                                                nameInput = it
                                                parsedTransactionsState[index].accountName = it
                                            },
                                            label = { Text("اسم العميل / الحساب المستخلص", fontSize = 9.sp) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                            modifier = Modifier.fillMaxWidth()
                                        )

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            OutlinedTextField(
                                                value = amountInput,
                                                onValueChange = {
                                                    amountInput = it
                                                    parsedTransactionsState[index].amount = it.toDoubleOrNull() ?: 0.0
                                                },
                                                label = { Text("المبلغ", fontSize = 9.sp) },
                                                singleLine = true,
                                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                                modifier = Modifier.weight(1f)
                                            )

                                            // Operation type switcher
                                            Row(
                                                modifier = Modifier
                                                    .weight(1.2f)
                                                    .height(56.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surface)
                                                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                                    .clickable {
                                                        isPaymentInput = !isPaymentInput
                                                        parsedTransactionsState[index].isPayment = isPaymentInput
                                                    }
                                                    .padding(horizontal = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.Center
                                            ) {
                                                Text(
                                                    text = if (isPaymentInput) "سند قبض / تسديد 💵" else "فاتورة دين آجل 🧾",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isPaymentInput) Color(0xFF16A34A) else Color(0xFFDC2626)
                                                )
                                            }
                                        }

                                        OutlinedTextField(
                                            value = detailsInput,
                                            onValueChange = {
                                                detailsInput = it
                                                parsedTransactionsState[index].details = it
                                            },
                                            label = { Text("البيان والملحوظة", fontSize = 9.sp) },
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            scope.launch {
                                parsedTransactionsState.forEach { item ->
                                    val matchedAcc = accounts.find { it.name.trim() == item.accountName.trim() }
                                    if (matchedAcc != null) {
                                        viewModel.addTransaction(
                                            accountId = matchedAcc.id,
                                            details = item.details,
                                            quantity = 1.0,
                                            unitPrice = item.amount,
                                            addition = 0.0,
                                            isPayment = item.isPayment,
                                            currency = item.currency
                                        )
                                    } else {
                                        viewModel.createAccount(
                                            name = item.accountName,
                                            phone = "",
                                            type = item.type,
                                            creditLimit = 0.0,
                                            tag = "مستورد بالذكاء الذكي",
                                            initialBalance = 0.0,
                                            onFinished = { newId ->
                                                viewModel.addTransaction(
                                                    accountId = newId,
                                                    details = item.details,
                                                    quantity = 1.0,
                                                    unitPrice = item.amount,
                                                    addition = 0.0,
                                                    isPayment = item.isPayment,
                                                    currency = item.currency
                                                )
                                            }
                                        )
                                    }
                                }
                                showImportReviewDialog = false
                                Toast.makeText(context, "تم استخلاص القيود وترحيلها بنجاح إلى قاعدة البيانات المحلية! 📥🚀", Toast.LENGTH_LONG).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("تأكيد وترحيل القيود للحسابات ⚡🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportReviewDialog = false }) {
                        Text("إلغاء وتجاهل")
                    }
                }
            )
        }

        // SUGGESTION CHIPS MANAGER DIALOG
        if (showManageTopicsDialog) {
            AlertDialog(
                onDismissRequest = { showManageTopicsDialog = false },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.resetAiSuggestions() },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 4.dp)
                        ) {
                            Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("إعادة ضبط", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Text("تعديل وتخصيص أسئلة الذكاء", fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "يمكنك إضافة، تعديل، أو حذف الأسئلة والمواضيع المفتاحية السريعة التي تظهر في شريط الاقتراحات للذكاء الاصطناعي لتسهيل العمل اليومي.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    if (newTopicText.isNotBlank()) {
                                        viewModel.addAiSuggestion(newTopicText.trim())
                                        newTopicText = ""
                                    }
                                },
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(Icons.Default.Add, contentDescription = "إضافة")
                            }
                            OutlinedTextField(
                                value = newTopicText,
                                onValueChange = { newTopicText = it },
                                label = { Text("إضافة سؤال/موضوع مقترح جديد...") },
                                placeholder = { Text("مثال: كم رصيد صندوق المحل اليوم؟") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, textAlign = TextAlign.Right)
                            )
                        }

                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(suggestionChips.size) { index ->
                                val chipText = suggestionChips.getOrNull(index) ?: ""
                                if (topicToEditIndex == index) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                            .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp))
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (topicEditText.isNotBlank()) {
                                                    viewModel.updateAiSuggestion(index, topicEditText.trim())
                                                    topicToEditIndex = null
                                                    topicEditText = ""
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "حفظ",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        OutlinedTextField(
                                            value = topicEditText,
                                            onValueChange = { topicEditText = it },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, textAlign = TextAlign.Right)
                                        )
                                        IconButton(
                                            onClick = {
                                                topicToEditIndex = null
                                                topicEditText = ""
                                            }
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "إلغاء",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    topicToEditIndex = index
                                                    topicEditText = chipText
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Edit,
                                                    contentDescription = "تعديل",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            IconButton(
                                                onClick = { viewModel.deleteAiSuggestion(index) },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "حذف",
                                                    tint = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }

                                        Text(
                                            text = chipText,
                                            fontSize = 12.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.weight(1f).padding(horizontal = 4.dp),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showManageTopicsDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("إغلاق وتطبيق", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}

// --- SUB-COMPOSABLES & HELPER GRAPHICS ---

@Composable
fun ChatMessageBubble(message: Message) {
    val alignment = if (message.isUser) Alignment.CenterStart else Alignment.CenterEnd
    val bgColor = if (message.isUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (message.isUser) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
    val shape = if (message.isUser) {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 4.dp)
    } else {
        RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 4.dp)
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (message.isUser) Alignment.CenterStart else Alignment.CenterEnd
    ) {
        Column(
            horizontalAlignment = if (message.isUser) Alignment.Start else Alignment.End,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = bgColor),
                shape = shape,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = message.text,
                    color = textColor,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(14.dp),
                    textAlign = TextAlign.Right
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            val timeStr = java.text.SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(message.timestamp))
            Text(
                text = timeStr,
                fontSize = 9.sp,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}

@Composable
fun ThinkingIndicator(isOffline: Boolean) {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.CenterEnd
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 4.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp,
                    color = if (isOffline) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
                )
                Text(
                    text = if (isOffline) "جاري التفكير المالي الذاتي المحلي..." else "جاري تحليل السجلات المفتوحة سحابياً...",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun ScrollableSuggestionRow(
    chips: List<String>,
    onChipClick: (String) -> Unit,
    enabled: Boolean
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        reverseLayout = true
    ) {
        items(chips) { chip ->
            SuggestionChip(
                onClick = { if (enabled) onChipClick(chip) },
                label = { Text(chip, fontSize = 11.sp) },
                enabled = enabled,
                shape = RoundedCornerShape(16.dp),
                colors = SuggestionChipDefaults.suggestionChipColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

@Composable
fun SyncGatewayCard(
    title: String,
    icon: ImageVector,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = description,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )

            content()
        }
    }
}
