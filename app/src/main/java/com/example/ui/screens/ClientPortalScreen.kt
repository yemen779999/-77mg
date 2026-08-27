package com.example.ui.screens

import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.LedgerViewModel
import com.example.data.model.Account
import com.example.data.model.Transaction
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClientPortalScreen(
    viewModel: LedgerViewModel,
    onNavigateToRoleSelection: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accounts by viewModel.allAccounts.collectAsState()
    val transactions by viewModel.allTransactions.collectAsState()
    val clientAccountId by viewModel.clientAccountId.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val businessPhone by viewModel.businessPhone.collectAsState()
    val businessAddress by viewModel.businessAddress.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    var showLinkAccountDialog by remember { mutableStateOf(false) }
    var showLiveCameraScanner by remember { mutableStateOf(false) }
    var selectedAccountToLink by remember { mutableStateOf<Account?>(null) }
    var clientPinInput by remember { mutableStateOf("") }
    
    // Find linked account
    val linkedAccount = accounts.find { it.id == clientAccountId }

    // Filter transactions for linked account
    val clientTransactions = transactions.filter { it.accountId == clientAccountId }
    
    // Compute totals
    val totalInvoices = clientTransactions.filter { !it.isPayment }.sumOf { it.total }
    val totalPayments = clientTransactions.filter { it.isPayment }.sumOf { it.total }
    val outstandingBalance = totalInvoices - totalPayments

    var isSendingConfirmation by remember { mutableStateOf(false) }
    var confirmationSuccess by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Portal Header Banner
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                ),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onNavigateToRoleSelection) {
                        Icon(
                            imageVector = Icons.Default.SwitchAccount,
                            contentDescription = "تغيير الدور",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "بوابة العميل والمورد الرقمية 📱🔐",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right
                        )
                        Text(
                            text = "منشأة: $businessName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Right
                        )
                    }
                }
            }

            if (linkedAccount == null) {
                // Empty state or linking required
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.QrCodeScanner,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        modifier = Modifier.size(80.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "الرجاء ربط وتأكيد الحساب المالي الخاص بك",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "اختر حسابك من القائمة للحصول على الكشوفات الفورية وتأكيد الأرصدة مباشرة بدون اتصال بالإنترنت.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showLinkAccountDialog = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Link, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("ربط حسابي الحالي الآن 🔗")
                    }
                }
            } else {
                // Client Account Details Screen
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Linked Customer Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    TextButton(
                                        onClick = {
                                            viewModel.updateClientAccountId(-1)
                                            Toast.makeText(context, "تم إلغاء ربط الحساب بنجاح.", Toast.LENGTH_SHORT).show()
                                        }
                                    ) {
                                        Text("إلغاء الربط 💔", color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                    }

                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = linkedAccount.name,
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Black,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "نوع الحساب: ${linkedAccount.type} | هاتف: ${linkedAccount.phone.ifBlank { "غير متوفر" }}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Balance cards
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("الفواتير المستلمة", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text(
                                        text = "${String.format(Locale.US, "%,.2f", totalInvoices)} $defaultCurrency",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }

                            Card(
                                modifier = Modifier.weight(1f),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Text("المدفوعات الواصلة", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                                    Text(
                                        text = "${String.format(Locale.US, "%,.2f", totalPayments)} $defaultCurrency",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF16A34A)
                                    )
                                }
                            }
                        }
                    }

                    // Outstanding Balance Hero Card
                    item {
                        val cardBg = if (outstandingBalance >= 0) {
                            MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
                        } else {
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                        }
                        val balanceColor = if (outstandingBalance >= 0) {
                            MaterialTheme.colorScheme.error
                        } else {
                            Color(0xFF16A34A)
                        }
                        val balanceLabel = if (linkedAccount.type == "مورد") {
                            if (outstandingBalance >= 0) "مستحقات للمورد (دائن)" else "دفعات مقدمة لنا"
                        } else {
                            if (outstandingBalance >= 0) "إجمالي المتبقي (عليك دين آجل)" else "رصيد دائن لك زيادة"
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardBg),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, balanceColor.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = balanceLabel,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${String.format(Locale.US, "%,.2f", Math.abs(outstandingBalance))} $defaultCurrency",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = balanceColor
                                )
                            }
                        }
                    }

                    // Action Button: Send Balance Confirmation
                    item {
                        Button(
                            onClick = {
                                isSendingConfirmation = true
                                confirmationSuccess = false
                                scope.launch {
                                    kotlinx.coroutines.delay(1800) // Simulate cloud submission
                                    isSendingConfirmation = false
                                    confirmationSuccess = true
                                    Toast.makeText(context, "تم إرسال تأكيد مطابقة الرصيد للمسؤول بنجاح! 📝⚡", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            enabled = !isSendingConfirmation,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (confirmationSuccess) Color(0xFF16A34A) else MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            if (isSendingConfirmation) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text("جاري معالجة وتوقيع التأكيد...", fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = if (confirmationSuccess) Icons.Default.CheckCircle else Icons.Default.AssignmentTurnedIn,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (confirmationSuccess) "تم تأكيد مطابقة الرصيد الحالي ✓" else "إرسال طلب تأكيد مطابقة الرصيد 📝⚡",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }

                    // Transactions Ledger title
                    item {
                        Text(
                            text = "كشف الحساب وحركات الدفتر التفصيلية",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Ledger list items
                    if (clientTransactions.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Box(modifier = Modifier.padding(16.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "لا توجد عمليات مقيدة حالياً في كشف حسابك.",
                                        fontSize = 12.sp,
                                        color = Color.Gray,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    } else {
                        items(clientTransactions.sortedByDescending { it.timestamp }) { tx ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    // Left: Total cost and type
                                    Column(horizontalAlignment = Alignment.Start) {
                                        Text(
                                            text = "${String.format(Locale.US, "%,.2f", tx.total)} $defaultCurrency",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.isPayment) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                        )
                                        Text(
                                            text = if (tx.isPayment) "تسديد / واصل 💵" else "فاتورة دين 🧾",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (tx.isPayment) Color(0xFF16A34A) else MaterialTheme.colorScheme.error
                                        )
                                    }

                                    // Right: date and details
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = tx.details,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            textAlign = TextAlign.Right
                                        )
                                        Text(
                                            text = "التاريخ: ${tx.date} | ${tx.day}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Right
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog for linking account
        if (showLinkAccountDialog) {
            AlertDialog(
                onDismissRequest = { showLinkAccountDialog = false },
                title = {
                    Text(
                        text = "ربط وتأكيد هوية الحساب 🔒",
                        textAlign = TextAlign.Right,
                        modifier = Modifier.fillMaxWidth(),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                showLiveCameraScanner = true
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.QrCodeScanner, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("مسح كود QR لبطاقة الحساب 📸💳", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }

                        Text(
                            text = "أو البحث اليدوي عن الحساب من القائمة:",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.secondary,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Right,
                            modifier = Modifier.fillMaxWidth()
                        )

                        var searchQuery by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            label = { Text("ابحث باسم الحساب أو الهاتف") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        val filteredAccounts = accounts.filter {
                            it.name.contains(searchQuery, ignoreCase = true) || 
                            it.phone.contains(searchQuery, ignoreCase = true)
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                .padding(4.dp)
                        ) {
                            items(filteredAccounts) { acc ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedAccountToLink = acc }
                                        .background(
                                            if (selectedAccountToLink?.id == acc.id) {
                                                MaterialTheme.colorScheme.primaryContainer
                                            } else {
                                                Color.Transparent
                                            }
                                        )
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = acc.type,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                    Text(
                                        text = acc.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Right
                                    )
                                }
                            }
                        }

                        if (selectedAccountToLink != null) {
                            Text(
                                text = "الحساب المختار: ${selectedAccountToLink?.name}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                textAlign = TextAlign.Right,
                                modifier = Modifier.fillMaxWidth()
                            )

                            OutlinedTextField(
                                value = clientPinInput,
                                onValueChange = { clientPinInput = it.take(4) },
                                label = { Text("رمز الأمان الموحد PIN للربط") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val acc = selectedAccountToLink
                            if (acc != null) {
                                viewModel.updateClientAccountId(acc.id)
                                showLinkAccountDialog = false
                                Toast.makeText(context, "تم ربط حسابك المالي (${acc.name}) بنجاح تام! 🤝⚡", Toast.LENGTH_LONG).show()
                            }
                        },
                        enabled = selectedAccountToLink != null && clientPinInput.length >= 4
                    ) {
                        Text("تأكيد وربط")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showLinkAccountDialog = false }) {
                        Text("إلغاء")
                    }
                }
            )
        }

        if (showLiveCameraScanner) {
            CameraQrScannerDialog(
                onDismiss = { showLiveCameraScanner = false },
                onQrScanned = { scannedData ->
                    val potentialId = scannedData.replace("ACC_", "").trim().toIntOrNull()
                    val matchedAcc = accounts.find { 
                        it.id == potentialId || 
                        it.phone == scannedData.trim() || 
                        it.name.contains(scannedData.trim(), ignoreCase = true) 
                    }
                    if (matchedAcc != null) {
                        selectedAccountToLink = matchedAcc
                        clientPinInput = "1234" // Auto-fill standard secure pin
                        showLiveCameraScanner = false
                        Toast.makeText(context, "تم العثور على حسابك بنجاح: ${matchedAcc.name} 🎉🤝", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(context, "عذراً، لم نجد حساباً يطابق الكود: $scannedData", Toast.LENGTH_LONG).show()
                    }
                },
                title = "مسح كود QR الحساب المالي 📸💳"
            )
        }
    }
}
