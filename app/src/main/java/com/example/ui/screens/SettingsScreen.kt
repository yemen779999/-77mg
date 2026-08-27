package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AnimationLevel
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimensions
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.AppTypography
import com.example.ui.theme.GlassCard
import com.example.ui.theme.rememberHapticFeedback
import com.example.ui.theme.threeDTiltEffect
import com.example.ui.viewmodel.LedgerViewModel
import java.io.File
import java.util.*

enum class SettingsCategory(val title: String, val icon: ImageVector) {
    CATEGORIES("جميع الإعدادات", Icons.Default.Category),
    PROFILE("هوية المنشأة", Icons.Default.Business),
    SECURITY("الأمان والحماية", Icons.Default.Lock),
    APPEARANCE("المظهر والمؤثرات 🎨", Icons.Default.Palette),
    CLOUD("المزامنة السحابية", Icons.Default.CloudSync),
    CURRENCY("العملات والصرف", Icons.Default.AttachMoney),
    PDF("قوالب الفواتير والـ PDF", Icons.Default.PictureAsPdf),
    JSON_BACKUP("النسخ الاحتياطي وتصدير JSON 💾", Icons.Default.Save),
    TRASH("سلة المحذوفات", Icons.Default.Delete),
    ABOUT("حول التطبيق", Icons.Default.Info)
}

@Composable
fun SettingsScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTrash: () -> Unit
) {
    var activeCategory by remember { mutableStateOf(SettingsCategory.CATEGORIES) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AppSpacing.normal)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.normal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (activeCategory != SettingsCategory.CATEGORIES) {
                IconButton(onClick = { activeCategory = SettingsCategory.CATEGORIES }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "رجوع للقائمة")
                }
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }

            Text(
                text = if (activeCategory == SettingsCategory.CATEGORIES) "إعدادات النظام والمنشأة ⚙️" else activeCategory.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right
            )
        }

        if (activeCategory == SettingsCategory.CATEGORIES) {
            // Categorized Overview Menu
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                SettingsMenuCard(
                    title = "هوية المنشأة والمسؤول 🏢",
                    subtitle = "اسم المحل والتواصل والعنوان الخارجي",
                    icon = Icons.Default.Business,
                    onClick = { activeCategory = SettingsCategory.PROFILE }
                )
                SettingsMenuCard(
                    title = "أمان التطبيق وقفل الـ PIN 🔒",
                    subtitle = "تفعيل القفل السري والبصمة لمنع العبث",
                    icon = Icons.Default.Lock,
                    onClick = { activeCategory = SettingsCategory.SECURITY }
                )
                SettingsMenuCard(
                    title = "المظهر والمؤثرات البصرية 🎨",
                    subtitle = "مستوى التحريك (Animation Level) وتأثيرات الـ 3D والزجاج GlassCard",
                    icon = Icons.Default.Palette,
                    onClick = { activeCategory = SettingsCategory.APPEARANCE }
                )
                SettingsMenuCard(
                    title = "المزامنة السحابية والويندوز ☁️",
                    subtitle = "ربط وتأمين خادم المزامنة والسحابة",
                    icon = Icons.Default.CloudSync,
                    onClick = { activeCategory = SettingsCategory.CLOUD }
                )
                SettingsMenuCard(
                    title = "العملات والتحويل المالي 💵",
                    subtitle = "ضبط العملة القياسية وأسعار الصرف",
                    icon = Icons.Default.AttachMoney,
                    onClick = { activeCategory = SettingsCategory.CURRENCY }
                )
                SettingsMenuCard(
                    title = "تصميم الفواتير والـ PDF 📄",
                    subtitle = "تأطير الهيدر والتوقيع والألوان واللوجو",
                    icon = Icons.Default.PictureAsPdf,
                    onClick = { activeCategory = SettingsCategory.PDF }
                )
                SettingsMenuCard(
                    title = "النسخ الاحتياطي وتصدير JSON 💾",
                    subtitle = "تصدير واستيراد قاعدة البيانات والعمليات بصيغة JSON",
                    icon = Icons.Default.Save,
                    onClick = { activeCategory = SettingsCategory.JSON_BACKUP }
                )
                SettingsMenuCard(
                    title = "سلة المحذوفات المسترجعة 🗑️",
                    subtitle = "استرجاع الحسابات والعمليات المحذوفة",
                    icon = Icons.Default.Delete,
                    onClick = onNavigateToTrash
                )
                SettingsMenuCard(
                    title = "حول التطبيق والنسخة ℹ️",
                    subtitle = "معلومات الإصدار والحقوق والترخيص",
                    icon = Icons.Default.Info,
                    onClick = { activeCategory = SettingsCategory.ABOUT }
                )
            }
        } else {
            // Category Detail Content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (activeCategory) {
                    SettingsCategory.PROFILE -> ProfileSettingsSection(viewModel)
                    SettingsCategory.SECURITY -> SecuritySettingsSection(viewModel)
                    SettingsCategory.APPEARANCE -> AppearanceSettingsSection(viewModel)
                    SettingsCategory.CLOUD -> CloudSettingsSection(viewModel)
                    SettingsCategory.CURRENCY -> CurrencySettingsSection(viewModel)
                    SettingsCategory.PDF -> PdfSettingsSection(viewModel)
                    SettingsCategory.JSON_BACKUP -> JsonBackupSettingsSection(viewModel)
                    SettingsCategory.ABOUT -> AboutSettingsSection()
                    else -> {}
                }
            }
        }
    }
}

@Composable
fun SettingsMenuCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AppShapes.large,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.normal),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(AppShapes.medium)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileSettingsSection(viewModel: LedgerViewModel) {
    val businessName by viewModel.businessName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState()
    val businessAddress by viewModel.businessAddress.collectAsState()

    var tempBizName by remember { mutableStateOf(businessName) }
    var tempBizPhone by remember { mutableStateOf(businessPhone) }
    var tempBizAddress by remember { mutableStateOf(businessAddress) }
    var showSavedMessage by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        OutlinedTextField(
            value = tempBizName,
            onValueChange = { tempBizName = it },
            label = { Text("اسم المحل أو المنشأة التجارية") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )
        OutlinedTextField(
            value = tempBizPhone,
            onValueChange = { tempBizPhone = it },
            label = { Text("هاتف التواصل أو رقم الواتساب") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )
        OutlinedTextField(
            value = tempBizAddress,
            onValueChange = { tempBizAddress = it },
            label = { Text("العنوان الرئيسي للمكتب/المحل") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )
        Button(
            onClick = {
                viewModel.updateBusinessProfile(tempBizName, tempBizPhone, tempBizAddress)
                showSavedMessage = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("حفظ بيانات المنشأة 💾", fontWeight = FontWeight.Bold)
        }
        if (showSavedMessage) {
            Text(
                text = "✅ تم حفظ بيانات المنشأة التجارية بنجاح!",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun SecuritySettingsSection(viewModel: LedgerViewModel) {
    val isSecurityEnabled by viewModel.isSecurityEnabled.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    val isBiometricEnabled by viewModel.isBiometricEnabled.collectAsState()

    var tempPinEnabled by remember { mutableStateOf(isSecurityEnabled) }
    var tempPin by remember { mutableStateOf(securityPin) }
    var msg by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = tempPinEnabled,
                onCheckedChange = { isChecked ->
                    tempPinEnabled = isChecked
                    if (!isChecked) {
                        viewModel.updateSecuritySettings(false, "")
                    }
                }
            )
            Text("تأمين التطبيق برمز PIN الخاص بك", fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        if (tempPinEnabled) {
            OutlinedTextField(
                value = tempPin,
                onValueChange = { tempPin = it },
                label = { Text("رمز الـ PIN المكوّن من 4 أرقام") },
                modifier = Modifier.fillMaxWidth(),
                shape = AppShapes.medium,
                singleLine = true
            )
        }

        Button(
            onClick = {
                if (tempPinEnabled && tempPin.isBlank()) {
                    msg = "يرجى تحديد رمز PIN المكون من 4 أرقام"
                } else {
                    viewModel.updateSecuritySettings(tempPinEnabled, tempPin)
                    msg = if (tempPinEnabled) "تم تفعيل القفل بنجاح" else "تم إيقاف القفل"
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("حفظ إعدادات الأمان 🔑", fontWeight = FontWeight.Bold)
        }

        if (msg.isNotEmpty()) {
            Text(msg, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.medium)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                .padding(AppSpacing.medium),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Switch(
                checked = isBiometricEnabled,
                onCheckedChange = { viewModel.updateBiometricSettings(it) }
            )
            Text("تفعيل البصمة للفتح الفوري 🔐", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CloudSettingsSection(viewModel: LedgerViewModel) {
    val cloudClientId by viewModel.cloudClientId.collectAsState()
    val cloudServerUrl by viewModel.cloudServerUrl.collectAsState()
    val lastCloudSync by viewModel.lastCloudSync.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()

    var tempClientId by remember { mutableStateOf(cloudClientId) }
    var tempServerUrl by remember { mutableStateOf(cloudServerUrl) }
    var syncStatus by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        OutlinedTextField(
            value = tempClientId,
            onValueChange = { tempClientId = it },
            label = { Text("معرف العميل (Client ID)") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )
        OutlinedTextField(
            value = tempServerUrl,
            onValueChange = { tempServerUrl = it },
            label = { Text("عنوان خادم المزامنة (Cloud URL)") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium,
            singleLine = true
        )

        Button(
            onClick = {
                viewModel.updateCloudSettings(tempClientId, tempServerUrl)
                viewModel.syncWithCloud { success, result ->
                    syncStatus = result
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isCloudSyncing,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text(if (isCloudSyncing) "جاري الإرسال..." else "حفظ ومزامنة فورية ⚡", fontWeight = FontWeight.Bold)
        }

        if (syncStatus.isNotEmpty()) {
            Text(syncStatus, fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Right)
        }

        Text("آخر مزامنة: $lastCloudSync", fontSize = 11.sp, color = Color.Gray, textAlign = TextAlign.Right)
    }
}

@Composable
fun CurrencySettingsSection(viewModel: LedgerViewModel) {
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    var tempCurrency by remember { mutableStateOf(defaultCurrency) }
    var usdRate by remember { mutableStateOf(viewModel.getDefaultExchangeRate("USD", "YER")) }
    var sarRate by remember { mutableStateOf(viewModel.getDefaultExchangeRate("SAR", "YER")) }
    var status by remember { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        Text("العملة الافتراضية:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
        ) {
            listOf("YER", "USD", "SAR").forEach { curr ->
                val isSelected = tempCurrency == curr
                ElevatedButton(
                    onClick = {
                        tempCurrency = curr
                        viewModel.updateDefaultCurrency(curr)
                    },
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(curr, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        OutlinedTextField(
            value = usdRate.toString(),
            onValueChange = { usdRate = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("سعر صرف USD مقابل YER") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )
        OutlinedTextField(
            value = sarRate.toString(),
            onValueChange = { sarRate = it.toDoubleOrNull() ?: 0.0 },
            label = { Text("سعر صرف SAR مقابل YER") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )

        Button(
            onClick = {
                viewModel.updateStandardRates(usdRate, sarRate)
                status = "تم تحديث أسعار الصرف بنجاح"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("تحديث أسعار الصرف 💰", fontWeight = FontWeight.Bold)
        }

        if (status.isNotEmpty()) {
            Text(status, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun PdfSettingsSection(viewModel: LedgerViewModel) {
    val pdfLogo by viewModel.pdfLogo.collectAsState()
    val pdfCustomTitle by viewModel.pdfHeaderCustomTitle.collectAsState()
    val pdfCustomFooter by viewModel.pdfFooterCustomText.collectAsState()

    var tempLogo by remember { mutableStateOf(pdfLogo) }
    var tempTitle by remember { mutableStateOf(pdfCustomTitle) }
    var tempFooter by remember { mutableStateOf(pdfCustomFooter) }
    var saved by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        OutlinedTextField(
            value = tempLogo,
            onValueChange = { tempLogo = it },
            label = { Text("لوجو/رمز الفاتورة (مثال: 🏢)") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )
        OutlinedTextField(
            value = tempTitle,
            onValueChange = { tempTitle = it },
            label = { Text("عنوان الفاتورة المخصص") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )
        OutlinedTextField(
            value = tempFooter,
            onValueChange = { tempFooter = it },
            label = { Text("تذييل الفاتورة المخصص") },
            modifier = Modifier.fillMaxWidth(),
            shape = AppShapes.medium
        )

        Button(
            onClick = {
                viewModel.updatePdfTemplateSettings(
                    logo = tempLogo,
                    showLogo = true,
                    customTitle = tempTitle,
                    customSubtitle = "",
                    customFooter = tempFooter,
                    showSignature = true,
                    fontStyle = "DEFAULT",
                    fontSize = "MEDIUM",
                    themeColor = "SLATE",
                    colDetailsLabel = "التفاصيل والبيان",
                    colQtyVisible = true,
                    colQtyLabel = "الكمية",
                    colPriceVisible = true,
                    colPriceLabel = "السعر",
                    colAdditionVisible = true,
                    colAdditionLabel = "الإضافي",
                    colTotalVisible = true,
                    colTotalLabel = "الإجمالي"
                )
                saved = true
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("حفظ قالب الفاتورة 📄", fontWeight = FontWeight.Bold)
        }

        if (saved) {
            Text("✅ تم حفظ قالب الفاتورة بنجاح", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
fun AboutSettingsSection() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
    ) {
        Icon(
            imageVector = Icons.Default.AccountBalanceWallet,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(64.dp)
        )
        Text("المحاسب anas برو • Pro Ledger", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.primary)
        Text("الإصدار: 1.0.0 (إنتاجي)", fontSize = 12.sp, color = Color.Gray)
        Text("جميع الحقوق محفوظة © 2026", fontSize = 11.sp, color = Color.Gray)
    }
}

@Composable
fun JsonBackupSettingsSection(viewModel: LedgerViewModel) {
    val context = LocalContext.current
    val accounts by viewModel.allAccounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val inventory by viewModel.inventoryItems.collectAsState()

    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var exportStatusMessage by remember { mutableStateOf<String?>(null) }
    var importStatusMessage by remember { mutableStateOf<String?>(null) }
    var lastExportedFile by remember { mutableStateOf<File?>(null) }

    // File Picker for JSON import
    val jsonPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isImporting = true
            importStatusMessage = "جاري قراءة واستعادة ملف JSON..."
            viewModel.importDatabaseFromJson(context, uri) { success, msg ->
                isImporting = false
                importStatusMessage = if (success) "✅ $msg" else "❌ $msg"
                if (success) {
                    Toast.makeText(context, "تمت استعادة البيانات بنجاح", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.large)
    ) {
        // Overview Summary 3D Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .threeDTiltEffect(maxRotationDegrees = 5f)
                .shadow(8.dp, shape = AppShapes.large),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = AppShapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DataObject,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "نظام النسخ الاحتياطي بصيغة JSON",
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "يتيح لك تصدير قاعدة البيانات الكاملة (الحسابات، المعاملات، والمخزون) بصيغة JSON عالمية لسهولة النقل والمشاركة والأرشفة على أي جهاز آخر.",
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Right
                )

                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${accounts.size}", style = AppTypography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        Text(text = "حساب مسجل", style = AppTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${transactions.size}", style = AppTypography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.SuccessGreen)
                        Text(text = "حركة مالية", style = AppTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${inventory.size}", style = AppTypography.titleLarge, fontWeight = FontWeight.Bold, color = AppColors.WarningAmber)
                        Text(text = "صنف مخزون", style = AppTypography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Section 1: Export JSON 3D Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .threeDTiltEffect(maxRotationDegrees = 4f)
                .shadow(6.dp, shape = AppShapes.large),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = AppShapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, shape = AppShapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileUpload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تصدير نسخة احتياطية (JSON Export)",
                            style = AppTypography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "توليد ملف .json يحتوي على كافة البيانات",
                            style = AppTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        isExporting = true
                        exportStatusMessage = "جاري إنشاء ملف النسخة الاحتياطية..."
                        viewModel.exportDatabaseToJson(context) { success, msg, file ->
                            isExporting = false
                            exportStatusMessage = if (success) "✅ $msg" else "❌ $msg"
                            lastExportedFile = file
                            if (success && file != null) {
                                Toast.makeText(context, "تم حفظ الملف بنجاح", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isExporting,
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimensions.buttonHeight)
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isExporting) "جاري التصدير..." else "إنشاء وتصدير ملف JSON 📤",
                        style = AppTypography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (lastExportedFile != null) {
                    OutlinedButton(
                        onClick = {
                            viewModel.shareJsonBackup(context, lastExportedFile!!)
                        },
                        shape = AppShapes.button,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimensions.buttonHeight)
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "مشاركة الملف عبر واتساب أو البريد 🔗",
                            style = AppTypography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (exportStatusMessage != null) {
                    Text(
                        text = exportStatusMessage!!,
                        style = AppTypography.bodySmall,
                        color = if (exportStatusMessage!!.startsWith("✅")) AppColors.SuccessGreen else AppColors.DangerRed,
                        textAlign = TextAlign.Right,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }

        // Section 2: Import JSON 3D Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .threeDTiltEffect(maxRotationDegrees = 4f)
                .shadow(6.dp, shape = AppShapes.large),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = AppShapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(AppColors.WarningAmberLight, shape = AppShapes.small),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FileOpen,
                            contentDescription = null,
                            tint = AppColors.WarningAmber,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "استيراد واسترجاع (JSON Import)",
                            style = AppTypography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "استعادة البيانات من ملف .json تم تصديره مسبقاً",
                            style = AppTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Button(
                    onClick = {
                        try {
                            jsonPickerLauncher.launch("application/json")
                        } catch (e: Exception) {
                            try {
                                jsonPickerLauncher.launch("*/*")
                            } catch (ex: Exception) {
                                Toast.makeText(context, "تعذر فتح منتقي الملفات", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    enabled = !isImporting,
                    shape = AppShapes.button,
                    colors = ButtonDefaults.buttonColors(containerColor = AppColors.PrimaryRoyalNavy),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(AppDimensions.buttonHeight)
                ) {
                    if (isImporting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(imageVector = Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isImporting) "جاري الاستيراد..." else "اختيار ملف JSON واستعادة البيانات 📥",
                        style = AppTypography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                if (importStatusMessage != null) {
                    Text(
                        text = importStatusMessage!!,
                        style = AppTypography.bodySmall,
                        color = if (importStatusMessage!!.startsWith("✅")) AppColors.SuccessGreen else AppColors.DangerRed,
                        textAlign = TextAlign.Right,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun AppearanceSettingsSection(viewModel: LedgerViewModel) {
    val performHaptic = rememberHapticFeedback()
    val is3DEnabled by viewModel.is3DEffectsEnabled.collectAsState()
    val currentAnimLevel by viewModel.animationLevel.collectAsState()

    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
        // 3D & GlassCard Toggle Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = AppShapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "تأثيرات الـ 3D والزجاج 🔮",
                            style = AppTypography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "تفعيل المظهر ثلاثي الأبعاد والتأطير الزجاجي الشفاف GlassCard والحركة التفاعلية",
                            style = AppTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = is3DEnabled,
                        onCheckedChange = {
                            performHaptic()
                            viewModel.update3DEffectsEnabled(it)
                        }
                    )
                }
            }
        }

        // Animation Levels Selector Card
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = AppShapes.large,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AppSpacing.normal),
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                Text(
                    text = "مستوى وسرعة التحريك (Animation Level) ⚡",
                    style = AppTypography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "تحديد سرعة ومستوى الحركات والانتقالات داخل جميع شاشات وقوائم التطبيق",
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimationLevel.values().forEach { level ->
                        val isSelected = currentAnimLevel == level
                        Button(
                            onClick = {
                                performHaptic()
                                viewModel.updateAnimationLevel(level)
                            },
                            modifier = Modifier.weight(1f),
                            shape = AppShapes.medium,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                                contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        ) {
                            Text(
                                text = level.label.split(" ").first(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Live GlassCard Preview
        Text(
            text = "معاينة حية لكرت الزجاج (GlassCard Live Preview) 👁️",
            style = AppTypography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 8.dp)
        )

        GlassCard(
            modifier = Modifier.fillMaxWidth(),
            elevation = 8.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(AppShapes.medium)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Column {
                    Text(
                        text = "عنصر زجاجي ذكي GlassCard",
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (is3DEnabled) "التأثير الزجاجي والـ 3D مفعّل الآن بنجاح ✨" else "النمط العادي المباشر مفعّل ⚪",
                        style = AppTypography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
