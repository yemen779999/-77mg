package com.example.ui.screens

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.LedgerViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceSmartEntryDialog(
    viewModel: LedgerViewModel,
    onDismiss: () -> Unit,
    onEntrySaved: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts by viewModel.allAccounts.collectAsState()
    val performHaptic = com.example.ui.theme.rememberHapticFeedback()

    // Mode Toggle: Offline vs Online
    var isOfflineMode by remember { mutableStateOf(true) }

    // Text inputs
    var textInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }

    // Extracted results state
    var parsedJson by remember { mutableStateOf<JSONObject?>(null) }
    var parseError by remember { mutableStateOf<String?>(null) }

    // Editing states for extracted entities
    var extractedName by remember { mutableStateOf("") }
    var extractedAccountId by remember { mutableStateOf<Int?>(null) }
    var extractedAmountStr by remember { mutableStateOf("") }
    var extractedCurrency by remember { mutableStateOf("YER") }
    var extractedIsPayment by remember { mutableStateOf(false) }
    var extractedDetails by remember { mutableStateOf("") }

    // Initialize intent Speech-to-Text launcher
    val speechRecognizerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val spokenText = results?.firstOrNull() ?: ""
                if (spokenText.isNotBlank()) {
                    textInput = spokenText
                }
            }
        }
    )

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
        onResult = { bitmap ->
            if (bitmap != null) {
                // TODO: Handle bitmap (send to Gemini/process)
                Toast.makeText(context, "تم التقاط الصورة!", Toast.LENGTH_SHORT).show()
                textInput = "تحليل الصورة الملتقطة..."
            }
        }
    )

    fun startVoiceRecording() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar-YE") // Arabic voice extraction
            putExtra(RecognizerIntent.EXTRA_PROMPT, "تحدث الآن لتسجيل القيد أو الديون بالصوت... 🎙️")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "خاصية التعرف الصوتي غير متوفرة على هذا الهاتف.", Toast.LENGTH_SHORT).show()
        }
    }

    // Processing voice/written statement using online Gemini API or offline NLP module
    fun processStatement() {
        if (textInput.isBlank()) {
            Toast.makeText(context, "الرجاء كتابة أو قول بيان مالي أولاً.", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        parseError = null
        parsedJson = null

        scope.launch {
            val result = viewModel.parseSmartEntry(textInput, isOfflineMode)
            result.onSuccess { rawJson ->
                try {
                    val json = JSONObject(rawJson)
                    parsedJson = json

                    // Sync helper editing variables
                    extractedName = json.optString("accountName", "عميل صوتي جديد")
                    extractedAccountId = if (json.isNull("accountId")) null else json.optInt("accountId")
                    extractedAmountStr = json.optDouble("amount", 0.0).toString()
                    extractedCurrency = json.optString("currency", "YER")
                    extractedIsPayment = json.optBoolean("isPayment", false)
                    extractedDetails = json.optString("details", "قيد ذكي تلقائي")
                } catch (e: Exception) {
                    parseError = "تعذر استخلاص الحركات تلقائياً، يرجى كتابتها بجملة أوضح."
                }
            }.onFailure { err ->
                parseError = "خطأ في المعالجة: ${err.localizedMessage}"
            }
            isLoading = false
        }
    }

    // Persisting extracted values to DB
    fun saveExtractedEntry() {
        val amount = extractedAmountStr.toDoubleOrNull() ?: 0.0
        if (amount <= 0.0) {
            Toast.makeText(context, "الرجاء تحديد مبلغ مالي صحيح أكبر من الصفر.", Toast.LENGTH_SHORT).show()
            return
        }

        if (extractedName.isBlank()) {
            Toast.makeText(context, "الرجاء تحديد اسم للحساب.", Toast.LENGTH_SHORT).show()
            return
        }

        scope.launch {
            var targetId = extractedAccountId

            if (targetId == null) {
                // Determine whether user wants a customer or vendor automatically based on invoice direction
                val accountType = if (extractedIsPayment) "عميل" else "مشتري"
                // Account does not exist, create it on the fly!
                viewModel.createAccount(
                    name = extractedName,
                    phone = "",
                    type = accountType,
                    creditLimit = 0.0,
                    tag = "قيد_صوتي",
                    initialBalance = 0.0,
                    onFinished = { newId ->
                        targetId = newId
                    }
                )
                // Wait briefly for Account ID assignment or search db
                kotlinx.coroutines.delay(650)
                val updatedAccounts = viewModel.allAccounts.value
                val newlyCreatedAcc = updatedAccounts.find { it.name == extractedName }
                if (newlyCreatedAcc != null) {
                    targetId = newlyCreatedAcc.id
                }
            }

            // Save the Transaction!
            val finalId = targetId
            if (finalId != null) {
                viewModel.addTransaction(
                    accountId = finalId,
                    details = extractedDetails,
                    quantity = 1.0,
                    unitPrice = amount,
                    addition = 0.0,
                    isPayment = extractedIsPayment,
                    customDateString = null,
                    currency = extractedCurrency,
                    exchangeRate = viewModel.getDefaultExchangeRate(extractedCurrency, viewModel.defaultCurrency.value)
                )

                Toast.makeText(context, "✓ تم تحليل القيد وحفظه بنجاح فوري في السجلات!", Toast.LENGTH_LONG).show()
                onEntrySaved()
                onDismiss()
            } else {
                Toast.makeText(context, "فشل إنشاء الحساب تلقائياً، جرب السفر باسم آخر.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = { if (!isLoading) onDismiss() },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "مساعد القيد الصوتي والذكي لـ أنس برو 🎙️✨",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "تكلم بحريتك بالعامية (أوفلاين بدون إنترنت) أو بالـ AI السحابي، وسيقوم النظام فوراً بتحليل كلامك واستخلاص: الاسم، المبلغ، العملة، نوع العملية (فاتورة/تسديد) وقيدها تلقائياً!",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Right
                    )

                    // SEGMENTED TAB FOR OFFLINE / ONLINE SPEECH AI
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    performHaptic()
                                    isOfflineMode = true
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isOfflineMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (isOfflineMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.WifiOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("محلّي (أوفلاين 📴)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Button(
                                onClick = {
                                    performHaptic()
                                    isOfflineMode = false
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isOfflineMode) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    contentColor = if (!isOfflineMode) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text("ذكي متبحر (سحابي 🌐)", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // INPUT BOX + MIC BUTTON
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = textInput,
                            onValueChange = { textInput = it },
                            placeholder = {
                                Text(
                                    text = "تكلم بالصوت 🎙️، التقط صورة لفاتورة 📸، أو اختر ملفاً...",
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            maxLines = 4,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary
                            )
                        )

                        // MIC BUTTON
                        FloatingActionButton(
                            onClick = {
                                performHaptic()
                                startVoiceRecording()
                            },
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.primary,
                            shape = CircleShape,
                            modifier = Modifier.size(48.dp),
                            elevation = FloatingActionButtonDefaults.elevation(0.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "تحدث بصوتك", modifier = Modifier.size(24.dp))
                        }
                    }

                    // CAMERA AND FILE PICKER
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                performHaptic()
                                cameraLauncher.launch(null)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("صورة/فاتورة 📸", fontSize = 10.sp)
                        }

                        Button(
                            onClick = {
                                performHaptic()
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("استيراد ملف 📄", fontSize = 10.sp)
                        }
                    }

                    // PROCESS TRIGGERS BUTTON
                    Button(
                        onClick = {
                            performHaptic()
                            processStatement()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading && textInput.isNotBlank()
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري قراءة واستخراج القيد المالي...", fontSize = 12.sp)
                        } else {
                            Icon(Icons.Default.FlashOn, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("تحليل واستخراج البيانات بالذكاء الاصطناعي ✨", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // PARSE ERROR SHOWCASE
                    parseError?.let { err ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            shape = RoundedCornerShape(8.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Text(
                                text = err,
                                fontSize = 11.sp,
                                color = Color(0xFFB91C1C),
                                modifier = Modifier.padding(10.dp),
                                textAlign = TextAlign.Right
                            )
                        }
                    }

                    // EXTRACTED CARD REVIEW SECTION (DYNAMIC CONDITIONAL RENDER)
                    parsedJson?.let { _ ->
                        Column(
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                            
                            Text(
                                text = "🔍 مراجعة بطاقة القيد المستخلصة قبل الحفظ الأخير:",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    // 1. Matched Account Badge
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("الحساب المستهدَف:", fontSize = 11.sp, color = Color.Gray)
                                        
                                        val matchedAcc = accounts.find { it.id == extractedAccountId }
                                        if (matchedAcc != null) {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFDCFCE7))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF15803D), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(matchedAcc.name, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                                            }
                                        } else {
                                            Row(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFEF3C7))
                                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFB45309), modifier = Modifier.size(12.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("عميل جديد سيُنشأ: \"$extractedName\"", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB45309))
                                            }
                                        }
                                    }

                                    // 2. Amount and currency
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("القيمة / المبلغ المقيد:", fontSize = 11.sp, color = Color.Gray)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(extractedAmountStr, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                            Text(
                                                text = when (extractedCurrency) {
                                                    "YER" -> "ريال يمني"
                                                    "USD" -> "دولار أمريكي"
                                                    "SAR" -> "ريال سعودي"
                                                    else -> extractedCurrency
                                                },
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    // 3. Operation direction / balance influence
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("نوع المعاملة وسدادها:", fontSize = 11.sp, color = Color.Gray)
                                        if (extractedIsPayment) {
                                            Text(
                                                text = "🟢 دفعة مسددة وصال رصيد (واصل)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFF15803D)
                                            )
                                        } else {
                                            Text(
                                                text = "🔴 فاتورة جديدة وقيد بالدين (مستحق)",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Black,
                                                color = Color(0xFFB91C1C)
                                            )
                                        }
                                    }

                                    // 4. Details modification field
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("تفاصيل البيان والوصف:", fontSize = 11.sp, color = Color.Gray)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        OutlinedTextField(
                                            value = extractedDetails,
                                            onValueChange = { extractedDetails = it },
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(8.dp),
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp, textAlign = TextAlign.Right)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        performHaptic()
                        saveExtractedEntry()
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF15803D)),
                    enabled = (parsedJson != null) && !isLoading
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("⚡ تأكيد القيد وحفظه فوراً في السجلات", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        performHaptic()
                        onDismiss()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray),
                    enabled = !isLoading
                ) {
                    Text("إلغاء", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
