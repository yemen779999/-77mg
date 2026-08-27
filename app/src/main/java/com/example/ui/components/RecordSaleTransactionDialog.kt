package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Account
import com.example.data.model.InventoryItem
import com.example.ui.viewmodel.LedgerViewModel
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordSaleTransactionDialog(
    viewModel: LedgerViewModel,
    onDismiss: () -> Unit,
    onSaleRecorded: (() -> Unit)? = null
) {
    val accounts by viewModel.allAccounts.collectAsState()
    val stockItems by viewModel.inventoryItems.collectAsState()
    val defaultCurrency by viewModel.defaultCurrency.collectAsState()

    var selectedAccount by remember { mutableStateOf<Account?>(null) }
    var customerSearchQuery by remember { mutableStateOf("") }
    var showCustomerPicker by remember { mutableStateOf(false) }

    var itemNameInput by remember { mutableStateOf("") }
    var selectedStockItem by remember { mutableStateOf<InventoryItem?>(null) }
    var showStockPicker by remember { mutableStateOf(false) }

    var priceInput by remember { mutableStateOf("") }
    var quantityInput by remember { mutableStateOf("1") }
    var detailsInput by remember { mutableStateOf("") }

    val parsedPrice = priceInput.toDoubleOrNull() ?: 0.0
    val parsedQuantity = quantityInput.toDoubleOrNull() ?: 1.0
    val totalAmount = parsedPrice * parsedQuantity

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.AddBusiness,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Text(
                            text = "تسجيل عملية مبيعات جديدة 🛍️",
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "إغلاق")
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // --- 1. Customer Selection Input ---
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "1. اختيار العميل المشتري 👤",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (selectedAccount == null) {
                            OutlinedCard(
                                onClick = { showCustomerPicker = true },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.PersonSearch,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.outline
                                        )
                                        Text(
                                            text = "انقر لاختيار العميل من القائمة...",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        Icons.Default.ArrowDropDown,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = selectedAccount?.name ?: "",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        if (!selectedAccount?.phone.isNullOrBlank()) {
                                            Text(
                                                text = "📱 ${selectedAccount?.phone}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                    TextButton(onClick = { showCustomerPicker = true }) {
                                        Text("تغيير", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // --- 2. Item Name Input & Inventory Selection ---
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "2. اسم السلعة / المادة المباعة 📦",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )

                        OutlinedTextField(
                            value = itemNameInput,
                            onValueChange = {
                                itemNameInput = it
                                selectedStockItem = null
                            },
                            label = { Text("اسم المادة أو السلعة") },
                            placeholder = { Text("مثال: بلاط سيراميك، أكياس أسمنت...") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.Inventory2, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(onClick = { showStockPicker = !showStockPicker }) {
                                    Icon(Icons.Default.Search, contentDescription = "اختر من المخزن")
                                }
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quick Stock Items Selector Chips
                        if (stockItems.isNotEmpty()) {
                            Text(
                                text = "أو اختر من المخزن والمستودع:",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                stockItems.take(3).forEach { item ->
                                    FilterChip(
                                        selected = selectedStockItem?.id == item.id,
                                        onClick = {
                                            selectedStockItem = item
                                            itemNameInput = item.name
                                            priceInput = item.salePrice.toString()
                                            detailsInput = "بيع سلعة ${item.name}"
                                        },
                                        label = { Text(item.name, fontSize = 11.sp) },
                                        leadingIcon = if (selectedStockItem?.id == item.id) {
                                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(12.dp)) }
                                        } else null
                                    )
                                }
                            }
                        }
                    }

                    // --- 3. Price & Quantity Inputs ---
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Price Input
                        OutlinedTextField(
                            value = priceInput,
                            onValueChange = { priceInput = it },
                            label = { Text("سعر البيع") },
                            suffix = { Text(defaultCurrency, fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            leadingIcon = {
                                Icon(Icons.Default.AttachMoney, contentDescription = null)
                            },
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Quantity Input with Step Controls
                        Column(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = quantityInput,
                                onValueChange = { quantityInput = it },
                                label = { Text("الكمية") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                trailingIcon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                val curr = quantityInput.toDoubleOrNull() ?: 1.0
                                                quantityInput = (curr + 1).toInt().toString()
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = "زيادة", modifier = Modifier.size(16.dp))
                                        }
                                        IconButton(
                                            onClick = {
                                                val curr = quantityInput.toDoubleOrNull() ?: 1.0
                                                if (curr > 1) {
                                                    quantityInput = (curr - 1).toInt().toString()
                                                }
                                            },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(Icons.Default.Remove, contentDescription = "إنقاص", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                },
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // Details Input
                    OutlinedTextField(
                        value = detailsInput,
                        onValueChange = { detailsInput = it },
                        label = { Text("ملاحظات / تفاصيل أخرى") },
                        placeholder = { Text("مثال: فاتورة بيع آجل") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = {
                            Icon(Icons.Default.Notes, contentDescription = null)
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    // --- Live Total Summary Card ---
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        )
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
                                    text = "إجمالي المبلغ المستحق:",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "${NumberFormat.getNumberInstance(Locale.US).format(totalAmount)} $defaultCurrency",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Text(
                                text = "الكمية: $parsedQuantity",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val acc = selectedAccount ?: return@Button
                        val details = when {
                            detailsInput.isNotBlank() -> detailsInput
                            itemNameInput.isNotBlank() -> "بيع $itemNameInput"
                            else -> "مبيعات بضائع"
                        }

                        viewModel.addTransaction(
                            accountId = acc.id,
                            details = details,
                            quantity = parsedQuantity,
                            unitPrice = parsedPrice,
                            addition = 0.0,
                            isPayment = false,
                            currency = defaultCurrency
                        )

                        // Update inventory stock if linked to a stock item
                        selectedStockItem?.let { item ->
                            viewModel.updateInventoryItem(
                                item.copy(stockQuantity = (item.stockQuantity - parsedQuantity).coerceAtLeast(0.0))
                            )
                        }

                        onSaleRecorded?.invoke()
                        onDismiss()
                    },
                    enabled = selectedAccount != null && (itemNameInput.isNotBlank() || selectedStockItem != null) && parsedPrice > 0,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("حفظ وقيد المبيعات", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("إلغاء")
                }
            }
        )
    }

    // --- Customer Selection Dialog Sheet ---
    if (showCustomerPicker) {
        AlertDialog(
            onDismissRequest = { showCustomerPicker = false },
            title = {
                Text(
                    text = "اختر العميل المشتري 👤",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = customerSearchQuery,
                        onValueChange = { customerSearchQuery = it },
                        label = { Text("بحث باسم العميل أو الهاتف") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    val filteredAccounts = accounts.filter {
                        (it.type == "مشتري" || it.type == "عميل" || it.type.isBlank()) &&
                        (it.name.contains(customerSearchQuery, ignoreCase = true) || it.phone.contains(customerSearchQuery))
                    }

                    if (filteredAccounts.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("لا يوجد عملاء مطابقون للبحث", fontSize = 12.sp, color = Color.Gray)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(filteredAccounts) { acc ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedAccount = acc
                                            showCustomerPicker = false
                                        },
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (selectedAccount?.id == acc.id)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(acc.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        if (acc.phone.isNotBlank()) {
                                            Text(acc.phone, fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCustomerPicker = false }) {
                    Text("إغلاق")
                }
            }
        )
    }

    // --- Stock Items Picker Dialog Sheet ---
    if (showStockPicker) {
        AlertDialog(
            onDismissRequest = { showStockPicker = false },
            title = {
                Text(
                    text = "اختر من المخزن والمستودع 📦",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                if (stockItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("لا توجد أصناف في المخزن حالياً", fontSize = 12.sp, color = Color.Gray)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(stockItems) { item ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedStockItem = item
                                        itemNameInput = item.name
                                        priceInput = item.salePrice.toString()
                                        detailsInput = "بيع سلعة ${item.name}"
                                        showStockPicker = false
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(item.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                        Text("الكمية المتاحة: ${item.stockQuantity} ${item.unit}", fontSize = 11.sp, color = MaterialTheme.colorScheme.outline)
                                    }
                                    Text("${item.salePrice} $defaultCurrency", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showStockPicker = false }) {
                    Text("إغلاق")
                }
            }
        )
    }
}
