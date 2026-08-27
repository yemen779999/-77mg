package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.ui.theme.AppColors
import com.example.ui.theme.AppDimensions
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.threeDShadow
import com.example.ui.theme.threeDTiltEffect
import com.example.ui.theme.LocalWindowSizeClass
import com.example.ui.theme.WindowSizeClass
import com.example.ui.viewmodel.AccountWithBalance
import com.example.ui.viewmodel.LedgerViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    viewModel: LedgerViewModel,
    onNavigateToAccount: (Int) -> Unit
) {
    val context = LocalContext.current
    val windowSizeClass = LocalWindowSizeClass.current
    val performHaptic = com.example.ui.theme.rememberHapticFeedback()

    val accountsWithBalance by viewModel.accountsWithBalance.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterType by remember { mutableStateOf("الكل") }
    var showAddDialog by remember { mutableStateOf(false) }

    var newName by remember { mutableStateOf("") }
    var newPhone by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf("مشتري") }
    var shareTarget by remember { mutableStateOf("NONE") }
    var nameError by remember { mutableStateOf(false) }
    var newCreditLimit by remember { mutableStateOf("") }
    var newTagBy by remember { mutableStateOf("") }
    var newInitialBalance by remember { mutableStateOf("") }

    var editingAccount by remember { mutableStateOf<Account?>(null) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editType by remember { mutableStateOf("مشتري") }
    var editCreditLimit by remember { mutableStateOf("0.0") }
    var editTag by remember { mutableStateOf("") }
    var editInitialBalance by remember { mutableStateOf("0.0") }
    var editNameError by remember { mutableStateOf(false) }

    val contactPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickContact()
    ) { uri ->
        uri?.let {
            try {
                val cursor = context.contentResolver.query(it, null, null, null, null)
                cursor?.use { c ->
                    if (c.moveToFirst()) {
                        val nameIndex = c.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                        val idIndex = c.getColumnIndex(ContactsContract.Contacts._ID)
                        if (nameIndex >= 0 && idIndex >= 0) {
                            val contactName = c.getString(nameIndex)
                            val contactId = c.getString(idIndex)
                            
                            val hasPhoneIndex = c.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)
                            val hasPhone = if (hasPhoneIndex >= 0) c.getInt(hasPhoneIndex) else 0
                            var contactPhone = ""
                            if (hasPhone > 0) {
                                val phoneCursor = context.contentResolver.query(
                                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                                    null,
                                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                                    arrayOf(contactId),
                                    null
                                )
                                phoneCursor?.use { pc ->
                                    if (pc.moveToFirst()) {
                                        val numberIndex = pc.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                        if (numberIndex >= 0) {
                                            contactPhone = pc.getString(numberIndex)
                                        }
                                    }
                                }
                            }
                            
                            val cleanPhone = contactPhone.replace(" ", "").replace("-", "").replace("(", "").replace(")", "")
                            
                            if (editingAccount != null) {
                                editName = contactName
                                editPhone = cleanPhone
                            } else {
                                newName = contactName
                                newPhone = cleanPhone
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(context, "عذراً، فشل استرداد جهات الاتصال", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                contactPickerLauncher.launch(null)
            } catch (e: Exception) {
                Toast.makeText(context, "خطأ فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "الرجاء السماح بالوصول لجهات الاتصال لتسهيل إضافة الاسم والهاتف بسلاسة", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(editingAccount) {
        editingAccount?.let {
            editName = it.name
            editPhone = it.phone
            editType = it.type
            editCreditLimit = it.creditLimit.toString()
            editTag = it.tag
            editInitialBalance = it.initialBalance.toString()
            editNameError = false
        }
    }

    val filteredAccounts = accountsWithBalance.filter {
        val matchesQuery = it.account.name.contains(searchQuery, ignoreCase = true) ||
                it.account.phone.contains(searchQuery) ||
                it.account.tag.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (selectedFilterType) {
            "عملاء" -> it.account.type == "مشتري"
            "موردون" -> it.account.type == "مورد"
            "صناديق" -> it.account.type == "صندوق"
            else -> true
        }
        matchesQuery && matchesFilter
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    performHaptic()
                    newName = ""
                    newPhone = ""
                    selectedType = "مشتري"
                    shareTarget = "NONE"
                    nameError = false
                    newCreditLimit = ""
                    newTagBy = ""
                    newInitialBalance = ""
                    showAddDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(AppSpacing.normal)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = AppSpacing.normal),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
                ) {
                    Text(text = "إضافة حساب جديد", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Icon(imageVector = Icons.Default.Add, contentDescription = "إضافة حساب")
                }
            }
        },
        bottomBar = {},
        contentWindowInsets = WindowInsets(0)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(innerPadding)
                .padding(AppSpacing.normal)
        ) {
            Text(
                text = "إدارة الحسابات والعملاء",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.normal)
            )

            // Search input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.small),
                placeholder = {
                    Text(
                        text = "ابحث بالاسم أو رقم الهاتف أو الوسم...",
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "بحث",
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                shape = AppShapes.extraLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface
                ),
                singleLine = true
            )

            // Filter Tags Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = AppSpacing.normal),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.small)
            ) {
                listOf("الكل", "عملاء", "موردون", "صناديق").forEach { filterLabel ->
                    val isSelected = selectedFilterType == filterLabel
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            performHaptic()
                            selectedFilterType = filterLabel
                        },
                        label = { Text(filterLabel, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    )
                }
            }

            // Responsive Layout List/Grid
            if (filteredAccounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(AppSpacing.small)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Group,
                            contentDescription = "لا يوجد حسابات",
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
                        )
                        Text(
                            text = if (searchQuery.isEmpty()) "لم تقم بإضافة حسابات بعد.\nاضغط على الزر بالأسفل للبدء." else "لا توجد نتائج بحث مطابقة.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (windowSizeClass != WindowSizeClass.COMPACT) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    items(filteredAccounts) { accountItem ->
                        AccountItemCard(
                            item = accountItem,
                            defaultCurrency = defaultCurrency,
                            onClick = { onNavigateToAccount(accountItem.account.id) },
                            onDelete = { viewModel.deleteAccount(accountItem.account) },
                            onEdit = { editingAccount = accountItem.account }
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
                ) {
                    items(filteredAccounts) { accountItem ->
                        AccountItemCard(
                            item = accountItem,
                            defaultCurrency = defaultCurrency,
                            onClick = { onNavigateToAccount(accountItem.account.id) },
                            onDelete = { viewModel.deleteAccount(accountItem.account) },
                            onEdit = { editingAccount = accountItem.account }
                        )
                    }
                }
            }
        }
    }

    // Add Account Dialog
    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = {
                Text(
                    text = "إنشاء حساب ومستفيد جديد",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    horizontalAlignment = Alignment.End
                ) {
                    OutlinedTextField(
                        value = newName,
                        onValueChange = {
                            newName = it
                            if (it.isNotBlank()) nameError = false
                        },
                        label = { Text("الاسم الكامل للحساب *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = nameError,
                        shape = AppShapes.small,
                        singleLine = true
                    )
                    if (nameError) {
                        Text(
                            text = "الاسم حقل مطلوب ولا يمكن تركه فارغاً",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = newPhone,
                        onValueChange = { newPhone = it },
                        label = { Text("رقم الهاتف / الواتساب") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val isPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.READ_CONTACTS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (isPermissionGranted) {
                                        try {
                                            contactPickerLauncher.launch(null)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطأ فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPage,
                                    contentDescription = "استيراد من جهات الاتصال",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = newInitialBalance,
                        onValueChange = { newInitialBalance = it },
                        label = { Text("الرصيد الحالي / البادئ للافتتاح (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newCreditLimit,
                        onValueChange = { newCreditLimit = it },
                        label = { Text("سقف الدين الأقصى (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = newTagBy,
                        onValueChange = { newTagBy = it },
                        label = { Text("العلامات / الوسوم (مثال: هام، تجزئة)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        singleLine = true
                    )

                    Text(
                        text = "نوع الحساب المالي:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = AppSpacing.small)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.small)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(AppSpacing.extraSmall),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)
                    ) {
                        listOf("مشتري" to "مشتري / عـميل", "مورد" to "مورد / دائن", "صندوق" to "خزينة / صندوق").forEach { (typeKey, typeLabel) ->
                            val isSelected = selectedType == typeKey
                            val activeBg = if (typeKey == "صندوق") AppColors.WarningAmber else MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(AppShapes.small)
                                    .background(if (isSelected) activeBg else Color.Transparent)
                                    .clickable { selectedType = typeKey }
                                    .padding(vertical = AppSpacing.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newName.isBlank()) {
                            nameError = true
                        } else {
                            val limit = newCreditLimit.toDoubleOrNull() ?: 0.0
                            val initBal = newInitialBalance.toDoubleOrNull() ?: 0.0
                            viewModel.createAccount(newName.trim(), newPhone.trim(), selectedType, limit, newTagBy.trim(), initBal) { newId ->
                                val bizName = viewModel.businessName.value
                                val accountTypeText = if (selectedType == "مورد") "مورد (دائن لنا)" else "مشتري / عميل"
                                val formattedMessage = """
                                    *بيانات حساب جديد* 📋
                                    مرحباً بك *$newName*، تم تسجيل حسابك بنجاح.
                                    
                                    • رقم الحساب المالي: #$newId
                                    • نوع الحساب: $accountTypeText
                                    • رقم الهاتف: ${newPhone.ifBlank { "غير مسجل" }}
                                    
                                    ــــــــــــــــــــــــــــــــــــــــــــــــــــــــ
                                    *مقدم من منشأة:* $bizName 🏢
                                """.trimIndent()

                                if (newPhone.isNotBlank()) {
                                    if (shareTarget == "WHATSAPP") {
                                        shareToWhatsApp(context, newPhone.trim(), formattedMessage)
                                    } else if (shareTarget == "SMS") {
                                        shareViaSms(context, newPhone.trim(), formattedMessage)
                                    }
                                }
                            }
                            showAddDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "حفظ الحساب", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(text = "إلغاء")
                }
            }
        )
    }

    // Edit Account Dialog
    if (editingAccount != null) {
        AlertDialog(
            onDismissRequest = { editingAccount = null },
            title = {
                Text(
                    text = "تعديل بيانات الحساب",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.medium),
                    horizontalAlignment = Alignment.End
                ) {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = {
                            editName = it
                            if (it.isNotBlank()) editNameError = false
                        },
                        label = { Text("الاسم الكامل للحساب *") },
                        modifier = Modifier.fillMaxWidth(),
                        isError = editNameError,
                        shape = AppShapes.small,
                        singleLine = true
                    )
                    if (editNameError) {
                        Text(
                            text = "الاسم حقل مطلوب ولا يمكن تركه فارغاً",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = { Text("رقم الهاتف / الواتساب") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val isPermissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(
                                        context,
                                        android.Manifest.permission.READ_CONTACTS
                                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                                    if (isPermissionGranted) {
                                        try {
                                            contactPickerLauncher.launch(null)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "خطأ فتح جهات الاتصال", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        permissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContactPage,
                                    contentDescription = "استيراد من جهات الاتصال",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    )

                    OutlinedTextField(
                        value = editInitialBalance,
                        onValueChange = { editInitialBalance = it },
                        label = { Text("الرصيد الحالي / البادئ للافتتاح") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    Text(
                        text = "نوع الحساب المالي:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = AppSpacing.small)
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AppShapes.small)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                            .padding(AppSpacing.extraSmall),
                        horizontalArrangement = Arrangement.spacedBy(AppSpacing.extraSmall)
                    ) {
                        listOf("مشتري" to "مشتري / عـميل", "مورد" to "مورد / دائن", "صندوق" to "خزينة / صندوق").forEach { (typeKey, typeLabel) ->
                            val isSelected = editType == typeKey
                            val activeBg = if (typeKey == "صندوق") AppColors.WarningAmber else MaterialTheme.colorScheme.primary
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(AppShapes.small)
                                    .background(if (isSelected) activeBg else Color.Transparent)
                                    .clickable { editType = typeKey }
                                    .padding(vertical = AppSpacing.small),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = typeLabel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 11.sp,
                                    color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = editCreditLimit,
                        onValueChange = { editCreditLimit = it },
                        label = { Text("سقف الدين الأقصى (اختياري)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = editTag,
                        onValueChange = { editTag = it },
                        label = { Text("العلامات / الوسوم (مثال: هام، تجزئة)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = AppShapes.small,
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val original = editingAccount
                        if (editName.isBlank()) {
                            editNameError = true
                        } else if (original != null) {
                            val limit = editCreditLimit.toDoubleOrNull() ?: 0.0
                            val initBal = editInitialBalance.toDoubleOrNull() ?: 0.0
                            val updated = original.copy(
                                name = editName.trim(),
                                phone = editPhone.trim(),
                                type = editType,
                                creditLimit = limit,
                                tag = editTag.trim(),
                                initialBalance = initBal
                            )
                            viewModel.updateAccount(updated)
                            editingAccount = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(text = "تعديل الحساب", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingAccount = null }) {
                    Text(text = "إلغاء")
                }
            }
        )
    }
}

@Composable
fun AccountItemCard(
    item: AccountWithBalance,
    defaultCurrency: String,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .threeDTiltEffect()
            .threeDShadow(6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = AppShapes.extraLarge,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AppSpacing.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "خيارات الحساب",
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("كشف كلي PDF", textAlign = TextAlign.Right)
                                Spacer(modifier = Modifier.width(AppSpacing.small))
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            showMenu = false
                            onClick()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("تعديل بيانات الحساب", textAlign = TextAlign.Right)
                                Spacer(modifier = Modifier.width(AppSpacing.small))
                                Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("حذف الحساب نهائياً", color = MaterialTheme.colorScheme.error)
                                Spacer(modifier = Modifier.width(AppSpacing.small))
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                            }
                        },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }

            val balanceColor = if (item.balance >= 0) AppColors.SuccessGreen else AppColors.DangerRed
            val balanceSignText = if (item.balance >= 0) "له/عليه" else "واصل زيادة"
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.padding(start = AppSpacing.small)
            ) {
                val defaultCurrencySymbolField = when (defaultCurrency) {
                    "USD" -> "$"
                    "SAR" -> "ر.س"
                    "YER" -> "ر.ي"
                    else -> defaultCurrency
                }
                Text(
                    text = "${String.format(Locale.US, "%,.2f", Math.abs(item.balance))} $defaultCurrencySymbolField",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = balanceColor
                )
                Text(
                    text = "$balanceSignText | ${item.transactionCount} حركة مبرمة",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.padding(end = AppSpacing.medium)
                ) {
                    Text(
                        text = item.account.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (item.account.phone.isBlank()) "بلا رقم هاتف" else item.account.phone,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(AppShapes.small)
                        .background(
                            if (item.account.type == "مورد") MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                            else if (item.account.type == "صندوق") AppColors.WarningAmber.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                        )
                        .padding(horizontal = AppSpacing.small, vertical = AppSpacing.extraSmall),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = item.account.type,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.account.type == "مورد") MaterialTheme.colorScheme.primary 
                                else if (item.account.type == "صندوق") AppColors.WarningAmber
                                else MaterialTheme.colorScheme.onTertiary
                    )
                }
            }
        }
    }
}

private fun shareToWhatsApp(context: Context, phone: String, message: String) {
    try {
        var cleanPhone = phone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.startsWith("00")) {
            cleanPhone = cleanPhone.substring(2)
        } else if (cleanPhone.startsWith("+")) {
            cleanPhone = cleanPhone.substring(1)
        }
        
        val formattedPhone = if (cleanPhone.length == 9 && cleanPhone.startsWith("7")) {
            "967$cleanPhone"
        } else if (cleanPhone.length == 9 && (cleanPhone.startsWith("1") || cleanPhone.startsWith("2"))) {
            "967$cleanPhone"
        } else {
            cleanPhone
        }
        
        val uri = Uri.parse("https://api.whatsapp.com/send?phone=$formattedPhone&text=${Uri.encode(message)}")
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "مشاركة تفاصيل الحساب عبر"))
    }
}

private fun shareViaSms(context: Context, phone: String, message: String) {
    try {
        val uri = Uri.parse("smsto:$phone")
        val intent = Intent(Intent.ACTION_SENDTO, uri).apply {
            putExtra("sms_body", message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, message)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(shareIntent, "إرسال كرسالة تفصيلية"))
    }
}
