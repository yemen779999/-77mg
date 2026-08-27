package com.example.ui.screens

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.Transaction
import com.example.ui.viewmodel.LedgerViewModel
import com.example.ui.viewmodel.PdfTemplateConfig
import com.example.utils.PdfExportHelper
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvoiceTemplateDesignerDialog(
    viewModel: LedgerViewModel,
    currentAccount: Account?,
    transactions: List<Transaction>,
    currentBalance: Double,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Global profile data
    val businessName by viewModel.businessName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState()
    val businessAddress by viewModel.businessAddress.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    // Shared preferences stored PDF customization configurations
    val pdfLogo by viewModel.pdfLogo.collectAsState()
    val pdfHeaderShowLogo by viewModel.pdfHeaderShowLogo.collectAsState()
    val pdfHeaderCustomTitle by viewModel.pdfHeaderCustomTitle.collectAsState()
    val pdfHeaderCustomSubtitle by viewModel.pdfHeaderCustomSubtitle.collectAsState()
    val pdfFooterCustomText by viewModel.pdfFooterCustomText.collectAsState()
    val pdfShowSignature by viewModel.pdfShowSignature.collectAsState()
    val pdfFontStyle by viewModel.pdfFontStyle.collectAsState()
    val pdfFontSize by viewModel.pdfFontSize.collectAsState()
    val pdfThemeColor by viewModel.pdfThemeColor.collectAsState()

    val pdfColDetailsLabel by viewModel.pdfColDetailsLabel.collectAsState()
    val pdfColQtyVisible by viewModel.pdfColQtyVisible.collectAsState()
    val pdfColQtyLabel by viewModel.pdfColQtyLabel.collectAsState()
    val pdfColPriceVisible by viewModel.pdfColPriceVisible.collectAsState()
    val pdfColPriceLabel by viewModel.pdfColPriceLabel.collectAsState()
    val pdfColAdditionVisible by viewModel.pdfColAdditionVisible.collectAsState()
    val pdfColAdditionLabel by viewModel.pdfColAdditionLabel.collectAsState()
    val pdfColTotalVisible by viewModel.pdfColTotalVisible.collectAsState()
    val pdfColTotalLabel by viewModel.pdfColTotalLabel.collectAsState()

    // Screen Edit States
    var editLogo by remember { mutableStateOf("") }
    var editShowLogo by remember { mutableStateOf(true) }
    var editTitle by remember { mutableStateOf("") }
    var editSubtitle by remember { mutableStateOf("") }
    var editFooter by remember { mutableStateOf("") }
    var editSignature by remember { mutableStateOf(true) }
    var editFontStyle by remember { mutableStateOf("DEFAULT") }
    var editFontSize by remember { mutableStateOf("MEDIUM") }
    var editThemeColor by remember { mutableStateOf("SLATE") }

    var editColDetails by remember { mutableStateOf("") }
    var editColQtyVis by remember { mutableStateOf(true) }
    var editColQtyLab by remember { mutableStateOf("") }
    var editColPriceVis by remember { mutableStateOf(true) }
    var editColPriceLab by remember { mutableStateOf("") }
    var editColAddVis by remember { mutableStateOf(true) }
    var editColAddLab by remember { mutableStateOf("") }
    var editColTotVis by remember { mutableStateOf(true) }
    var editColTotLab by remember { mutableStateOf("") }

    // Company profile edit states
    var profileName by remember { mutableStateOf("") }
    var profilePhone by remember { mutableStateOf("") }
    var profileAddress by remember { mutableStateOf("") }

    // Image Picker Launcher for Company Logo
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            editLogo = it.toString()
            editShowLogo = true
            Toast.makeText(context, "تم تحديد صورة الشعار بنجاح! 🖼️", Toast.LENGTH_SHORT).show()
        }
    }

    // Init state loaded
    LaunchedEffect(
        pdfLogo, pdfHeaderShowLogo, pdfHeaderCustomTitle, pdfHeaderCustomSubtitle,
        pdfFooterCustomText, pdfShowSignature, pdfFontStyle, pdfFontSize, pdfThemeColor,
        pdfColDetailsLabel, pdfColQtyVisible, pdfColQtyLabel, pdfColPriceVisible,
        pdfColPriceLabel, pdfColAdditionVisible, pdfColAdditionLabel, pdfColTotalVisible, pdfColTotalLabel,
        businessName, businessPhone, businessAddress
    ) {
        editLogo = pdfLogo
        editShowLogo = pdfHeaderShowLogo
        editTitle = pdfHeaderCustomTitle
        editSubtitle = pdfHeaderCustomSubtitle
        editFooter = pdfFooterCustomText
        editSignature = pdfShowSignature
        editFontStyle = pdfFontStyle
        editFontSize = pdfFontSize
        editThemeColor = pdfThemeColor

        editColDetails = pdfColDetailsLabel
        editColQtyVis = pdfColQtyVisible
        editColQtyLab = pdfColQtyLabel
        editColPriceVis = pdfColPriceVisible
        editColPriceLab = pdfColPriceLabel
        editColAddVis = pdfColAdditionVisible
        editColAddLab = pdfColAdditionLabel
        editColTotVis = pdfColTotalVisible
        editColTotLab = pdfColTotalLabel

        profileName = businessName
        profilePhone = businessPhone
        profileAddress = businessAddress
    }

    // Active sub-section tab index inside customization pane:
    // 0 = الترويسة والشعار (Header / Identity), 1 = تصميم القالب (Theme & Colors), 2 = تفاصيل الجدول (Table Columns)
    var selectedTab by remember { mutableStateOf(0) }

    // Save and export helper
    fun saveSettingsToDefaults() {
        viewModel.updateBusinessProfile(profileName.trim(), profilePhone.trim(), profileAddress.trim())
        viewModel.updatePdfTemplateSettings(
            logo = editLogo.trim(),
            showLogo = editShowLogo,
            customTitle = editTitle.trim(),
            customSubtitle = editSubtitle.trim(),
            customFooter = editFooter.trim(),
            showSignature = editSignature,
            fontStyle = editFontStyle,
            fontSize = editFontSize,
            themeColor = editThemeColor,
            colDetailsLabel = editColDetails.trim(),
            colQtyVisible = editColQtyVis,
            colQtyLabel = editColQtyLab.trim(),
            colPriceVisible = editColPriceVis,
            colPriceLabel = editColPriceLab.trim(),
            colAdditionVisible = editColAddVis,
            colAdditionLabel = editColAddLab.trim(),
            colTotalVisible = editColTotVis,
            colTotalLabel = editColTotLab.trim()
        )
        Toast.makeText(context, "✓ تم حفظ وتأكيد إعدادات تصميم الفاتورة كقالب افتراضي!", Toast.LENGTH_SHORT).show()
    }

    fun triggerPdfExport() {
        // Automatically save edits as temporary/actual configuration
        saveSettingsToDefaults()

        if (currentAccount == null) {
            Toast.makeText(context, "يرجى تحديد حساب محاسبي أولاً لتوليد كشف حسابه.", Toast.LENGTH_LONG).show()
            return
        }

        val config = PdfTemplateConfig(
            logo = editLogo.trim(),
            showLogo = editShowLogo,
            customTitle = editTitle.trim(),
            customSubtitle = editSubtitle.trim(),
            customFooter = editFooter.trim(),
            showSignature = editSignature,
            fontStyle = editFontStyle,
            fontSize = editFontSize,
            themeColor = editThemeColor,
            colDetailsLabel = editColDetails.trim(),
            colQtyVisible = editColQtyVis,
            colQtyLabel = editColQtyLab.trim(),
            colPriceVisible = editColPriceVis,
            colPriceLabel = editColPriceLab.trim(),
            colAdditionVisible = editColAddVis,
            colAdditionLabel = editColAddLab.trim(),
            colTotalVisible = editColTotVis,
            colTotalLabel = editColTotLab.trim()
        )

        val pdfFile = PdfExportHelper.generateAccountStatementPdf(
            context = context,
            businessName = profileName.trim(),
            businessPhone = profilePhone.trim(),
            businessAddress = profileAddress.trim(),
            accountName = currentAccount.name,
            accountPhone = currentAccount.phone,
            accountType = currentAccount.type,
            transactions = transactions,
            currentBalance = currentBalance,
            config = config,
            defaultCurrency = defaultCurrency
        )

        if (pdfFile != null) {
            PdfExportHelper.sharePdf(context, pdfFile)
            onDismiss()
        } else {
            Toast.makeText(context, "حدث خطأ أثناء تصدير ملف PDF، جرب مجدداً.", Toast.LENGTH_SHORT).show()
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "مصمم ومخصص قوالب الفواتير والـ PDF 🎨📄",
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
                        .heightIn(max = 500.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Text(
                        text = "خصص ترويسات التقارير وفواتير المبيعات، وعدّل الشعار ومعلومات التواصل، ثم راجع ورقة المعاينة الحية المباشرة للنموذج بالأسفل قبل التصدير النهائي.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                        lineHeight = 16.sp,
                        textAlign = TextAlign.Right
                    )

                    // LIVE INTERACTIVE HIGH-FIDELITY PREVIEW SHEET CARD
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(16.dp)),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Section Header Banner
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📱 لوحة معاينة الفاتورة الحيّة (A4):",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.Gray
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFFFEF3C7))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text("تحديث فوري ⚡", fontSize = 8.sp, color = Color(0xFFB45309), fontWeight = FontWeight.Bold)
                                }
                            }

                            // Dynamic Simulated Sheet Body
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(BorderStroke(0.5.dp, Color.LightGray), RoundedCornerShape(8.dp))
                                    .background(Color.White)
                                    .padding(8.dp)
                            ) {
                                // Simulated Top Headerband matching PDF Theme Color
                                val themeHex = when (editThemeColor) {
                                    "NAVY" -> "#1E3A8A"
                                    "EMERALD" -> "#14532D"
                                    "BURGUNDY" -> "#6B1D2F"
                                    "GOLDEN" -> "#78350F"
                                    else -> "#1E293B" // SLATE
                                }
                                val primaryThemeColor = Color(android.graphics.Color.parseColor(themeHex))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(primaryThemeColor)
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Left: Business Contact Info
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(text = "الهاتف: ${profilePhone.ifBlank { "770000000" }}", color = Color.White.copy(alpha = 0.85f), fontSize = 7.sp)
                                        Text(text = "الموقع: ${profileAddress.ifBlank { "اليمن" }}", color = Color.White.copy(alpha = 0.85f), fontSize = 7.sp)
                                    }

                                    // Right: Name Title & Logo
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = editTitle.ifBlank { profileName.ifBlank { "اسم المنشأة" } },
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White,
                                                fontSize = 9.sp
                                            )
                                            Text(
                                                text = editSubtitle.ifBlank { "سند كشف حساب محاسبي تفصيلي" },
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 7.sp
                                            )
                                        }

                                        if (editShowLogo && editLogo.isNotBlank()) {
                                            Box(
                                                modifier = Modifier
                                                    .size(28.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color.White.copy(alpha = 0.25f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (editLogo.startsWith("content://") || editLogo.startsWith("file://")) {
                                                    var loadedBitmap by remember(editLogo) { mutableStateOf<android.graphics.Bitmap?>(null) }
                                                    LaunchedEffect(editLogo) {
                                                        try {
                                                            val uri = Uri.parse(editLogo)
                                                            context.contentResolver.openInputStream(uri)?.use { stream ->
                                                                loadedBitmap = BitmapFactory.decodeStream(stream)
                                                            }
                                                        } catch (e: Exception) {
                                                            e.printStackTrace()
                                                        }
                                                    }
                                                    loadedBitmap?.let { bmp ->
                                                        Image(
                                                            bitmap = bmp.asImageBitmap(),
                                                            contentDescription = "Logo",
                                                            modifier = Modifier.fillMaxSize()
                                                        )
                                                    } ?: Icon(Icons.Default.Image, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                                } else {
                                                    Text(editLogo, fontSize = 14.sp)
                                                }
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(6.dp))

                                // Buyer details Box
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFFF8FAFC), RoundedCornerShape(4.dp))
                                        .border(BorderStroke(0.5.dp, Color(0xFFE2E8F0)), RoundedCornerShape(4.dp))
                                        .padding(6.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "إجمالي الرصيد المطلوب: " + if (currentBalance >= 0) "🟢 ${currentBalance} $defaultCurrency" else "🔴 ${currentBalance} $defaultCurrency",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "العميل: ${currentAccount?.name ?: "اسم الحساب المحدد"}",
                                            fontSize = 7.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Invoice details table header
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(primaryThemeColor.copy(alpha = 0.9f))
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (editColTotVis) {
                                        Text(editColTotLab.ifBlank { "الإجمالي" }, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = TextAlign.Left)
                                    }
                                    if (editColAddVis) {
                                        Text(editColAddLab.ifBlank { "الإضافي" }, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    if (editColPriceVis) {
                                        Text(editColPriceLab.ifBlank { "السعر" }, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    if (editColQtyVis) {
                                        Text(editColQtyLab.ifBlank { "الكمية" }, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    Text(editColDetails.ifBlank { "البيان والتفاصيل" }, color = Color.White, fontSize = 7.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1.8f), textAlign = TextAlign.Right)
                                }

                                // Simulated rows
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (editColTotVis) {
                                        Text("5,000 YER", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Left)
                                    }
                                    if (editColAddVis) {
                                        Text("0.0", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    if (editColPriceVis) {
                                        Text("5,000", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    if (editColQtyVis) {
                                        Text("1", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    Text("شراء بضاعة (مثال افتراضي)", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(1.8f), textAlign = TextAlign.Right)
                                }

                                HorizontalDivider(color = Color(0xFFE2E8F0), thickness = 0.5.dp)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    if (editColTotVis) {
                                        Text("2,000 YER", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(1f), textAlign = TextAlign.Left)
                                    }
                                    if (editColAddVis) {
                                        Text("0.0", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    if (editColPriceVis) {
                                        Text("2,000", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    if (editColQtyVis) {
                                        Text("1", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(0.8f), textAlign = TextAlign.Center)
                                    }
                                    Text("تسديد ومقبوضات (واصل)", color = Color.DarkGray, fontSize = 7.sp, modifier = Modifier.weight(1.8f), textAlign = TextAlign.Right)
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Footer & Custom signature box
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    if (editSignature) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("التوقيع والختم المعتمد", fontSize = 6.sp, color = Color.Gray)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(width = 54.dp, height = 18.dp)
                                                    .border(BorderStroke(0.5.dp, Color.LightGray), RoundedCornerShape(2.dp))
                                            )
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.width(1.dp))
                                    }

                                    Column(
                                        modifier = Modifier.weight(1f),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text(
                                            text = editFooter.ifBlank { "شكراً لتعاملكم الداعم والمستمر معنا!" },
                                            fontSize = 6.sp,
                                            color = Color.DarkGray,
                                            maxLines = 1,
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // TAB CONTROLS
                    TabRow(
                        selectedTabIndex = selectedTab,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant), RoundedCornerShape(12.dp))
                    ) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("الترويسة والاتصال", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("القالب والألوان", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        Tab(
                            selected = selectedTab == 2,
                            onClick = { selectedTab = 2 },
                            text = { Text("أعمدة الجدول", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 160.dp)
                    ) {
                        when (selectedTab) {
                            0 -> {
                                // TAP 0: HEADER & BUSINESS CONNECTIVITY SETTINGS
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "معلومات الاتصال وشعار الشركة المطبوعة:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    OutlinedTextField(
                                        value = profileName,
                                        onValueChange = { profileName = it },
                                        label = { Text("اسم الشركة / المحل التجاري") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = profilePhone,
                                            onValueChange = { profilePhone = it },
                                            label = { Text("رقم هاتف المنشأة للتواصل") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                        OutlinedTextField(
                                            value = profileAddress,
                                            onValueChange = { profileAddress = it },
                                            label = { Text("العنوان والموقع") },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Logo & Header override titles
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Checkbox(checked = editShowLogo, onCheckedChange = { editShowLogo = it })
                                            Text("عرض الشعار", fontSize = 11.sp)
                                        }

                                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            OutlinedTextField(
                                                value = editLogo,
                                                onValueChange = { editLogo = it },
                                                label = { Text("أيقونة أو مسار الشعار (مثل 🏢 أو صورة)") },
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(12.dp),
                                                singleLine = true,
                                                enabled = editShowLogo
                                            )
                                            Button(
                                                onClick = { logoPickerLauncher.launch("image/*") },
                                                enabled = editShowLogo,
                                                modifier = Modifier.fillMaxWidth(),
                                                shape = RoundedCornerShape(10.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer)
                                            ) {
                                                Icon(Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("رفع صورة الشعار 🖼️", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    OutlinedTextField(
                                        value = editTitle,
                                        onValueChange = { editTitle = it },
                                        label = { Text("رويسة الفاتورة المخصصة (يحل محل الاسم الافتراضي)") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    OutlinedTextField(
                                        value = editSubtitle,
                                        onValueChange = { editSubtitle = it },
                                        label = { Text("العنوان السطري الفرعي لتقرير الفاتورة") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )
                                }
                            }
                            1 -> {
                                // TAP 1: THEME TEMPLATE & TYPOGRAPHY & COLORS
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text(
                                        text = "اختر السمت واللون المميّز لملف الفاتورة والـ PDF:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                            .padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        listOf(
                                            Triple("SLATE", "رمادي", "#1E293B"),
                                            Triple("NAVY", "كحلي", "#1E3A8A"),
                                            Triple("EMERALD", "أخضر", "#14532D"),
                                            Triple("BURGUNDY", "عنابي", "#6B1D2F"),
                                            Triple("GOLDEN", "ذهبي", "#78350F")
                                        ).forEach { (v, label, colorHex) ->
                                            val isSelected = editThemeColor == v
                                            Column(
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .clickable { editThemeColor = v }
                                                    .background(if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                                                )
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(text = label, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                    // Size controls
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("حجم خط الطباعة:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("SMALL" to "صغير", "MEDIUM" to "متوسط", "LARGE" to "كبير").forEach { (v, l) ->
                                                    TextButton(
                                                        onClick = { editFontSize = v },
                                                        colors = ButtonDefaults.textButtonColors(
                                                            containerColor = if (editFontSize == v) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(l, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("قالب نمط الخط:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                listOf("DEFAULT" to "الافتراضي", "SERIF" to "Serif", "MONOSPACE" to "Mono").forEach { (v, l) ->
                                                    TextButton(
                                                        onClick = { editFontStyle = v },
                                                        colors = ButtonDefaults.textButtonColors(
                                                            containerColor = if (editFontStyle == v) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color.Transparent
                                                        ),
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Text(l, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            2 -> {
                                // TAP 2: TABLE COLUMNS LABELS & DISPLAY SETTINGS
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "تخصيص أسماء وأعمدة جدول الحساب وعرضها:",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    OutlinedTextField(
                                        value = editColDetails,
                                        onValueChange = { editColDetails = it },
                                        label = { Text("قيمة مسمى العمود: التفاصيل والبيان") },
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(12.dp),
                                        singleLine = true
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Switch(checked = editColQtyVis, onCheckedChange = { editColQtyVis = it })
                                        OutlinedTextField(
                                            value = editColQtyLab,
                                            onValueChange = { editColQtyLab = it },
                                            label = { Text("مسمى عمود: الكمية") },
                                            modifier = Modifier.weight(1f),
                                            enabled = editColQtyVis,
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Switch(checked = editColPriceVis, onCheckedChange = { editColPriceVis = it })
                                        OutlinedTextField(
                                            value = editColPriceLab,
                                            onValueChange = { editColPriceLab = it },
                                            label = { Text("مسمى عمود: السعر") },
                                            modifier = Modifier.weight(1f),
                                            enabled = editColPriceVis,
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Switch(checked = editColAddVis, onCheckedChange = { editColAddVis = it })
                                        OutlinedTextField(
                                            value = editColAddLab,
                                            onValueChange = { editColAddLab = it },
                                            label = { Text("مسمى عمود: الإضافات") },
                                            modifier = Modifier.weight(1f),
                                            enabled = editColAddVis,
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Switch(checked = editColTotVis, onCheckedChange = { editColTotVis = it })
                                        OutlinedTextField(
                                            value = editColTotLab,
                                            onValueChange = { editColTotLab = it },
                                            label = { Text("مسمى عمود: الإجمالي") },
                                            modifier = Modifier.weight(1f),
                                            enabled = editColTotVis,
                                            shape = RoundedCornerShape(12.dp),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Footer Text Settings always visible at the bottom of form
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editFooter,
                            onValueChange = { editFooter = it },
                            label = { Text("حاشية وتذييل كشف الحساب (Footer Term Notes)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Checkbox(checked = editSignature, onCheckedChange = { editSignature = it })
                            Text("إضافة قسم وحقل التوقيع والختم أسفل كشوفات الفواتير المطبوعة", fontSize = 11.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { triggerPdfExport() },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("طرد وتوليد PDF 🚀", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = { saveSettingsToDefaults() },
                        modifier = Modifier.weight(0.8f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("حفظ الافتراضي 💾", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Gray)
                ) {
                    Text("إلغاء", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
