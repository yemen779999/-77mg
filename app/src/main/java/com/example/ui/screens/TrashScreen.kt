package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppShapes
import com.example.ui.theme.AppSpacing
import com.example.ui.theme.threeDShadow
import com.example.ui.theme.threeDTiltEffect
import com.example.ui.viewmodel.LedgerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: LedgerViewModel,
    modifier: Modifier = Modifier
) {
    val deletedAccounts by viewModel.allDeletedAccounts.collectAsState(initial = emptyList())
    val deletedTransactions by viewModel.allDeletedTransactions.collectAsState(initial = emptyList())
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = Accounts, 1 = Transactions

    val filteredDeletedAccounts = deletedAccounts.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.phone.contains(searchQuery)
    }

    val filteredDeletedTransactions = deletedTransactions.filter {
        it.details.contains(searchQuery, ignoreCase = true) || it.day.contains(searchQuery)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(AppSpacing.normal)
    ) {
        // Screen Title
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.normal),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (deletedAccounts.isNotEmpty() || deletedTransactions.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearTrash() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.DeleteForever, contentDescription = "تفريغ السلة")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("حذف الكل نهائياً", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            } else {
                Spacer(modifier = Modifier.width(1.dp))
            }

            Text(
                text = "سلة المحذوفات المسترجعة 🗑️",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Right
            )
        }

        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("ابحث في المحذوفات...", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = AppSpacing.small),
            shape = AppShapes.extraLarge,
            singleLine = true,
            trailingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) }
        )

        // Filter Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            modifier = Modifier
                .fillMaxWidth()
                .clip(AppShapes.normal)
                .padding(bottom = AppSpacing.normal)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("الحسابات (${deletedAccounts.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("العمليات والقيود (${deletedTransactions.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            )
        }

        if (selectedTab == 0) {
            if (filteredDeletedAccounts.isEmpty()) {
                EmptyTrashView("لا توجد حسابات محذوفة في السلة")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                    items(filteredDeletedAccounts, key = { it.id }) { acc ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .threeDTiltEffect()
                                .threeDShadow(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = AppShapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.normal),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { viewModel.restoreAccount(acc) }) {
                                        Icon(Icons.Default.Restore, contentDescription = "استعادة", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.removeDeletedAccountPermanently(acc.id) }) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "حذف نهائي", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("نوع الحساب: ${acc.type} | هاتف: ${acc.phone.ifBlank { "غير مسجل" }}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        } else {
            if (filteredDeletedTransactions.isEmpty()) {
                EmptyTrashView("لا توجد عمليات محذوفة في السلة")
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)) {
                    items(filteredDeletedTransactions, key = { it.id }) { tx ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .threeDTiltEffect()
                                .threeDShadow(4.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = AppShapes.large,
                            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(AppSpacing.normal),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    IconButton(onClick = { viewModel.restoreTransaction(tx) }) {
                                        Icon(Icons.Default.Restore, contentDescription = "استعادة", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { viewModel.removeDeletedTransactionPermanently(tx.id) }) {
                                        Icon(Icons.Default.DeleteForever, contentDescription = "حذف نهائي", tint = MaterialTheme.colorScheme.error)
                                    }
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(tx.details, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text("التاريخ: ${tx.date} | المبلغ: ${tx.total} ${tx.currency}", fontSize = 11.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTrashView(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AppSpacing.huge),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(AppSpacing.small)) {
            Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(48.dp))
            Text(message, fontSize = 12.sp, color = Color.Gray, textAlign = TextAlign.Center)
        }
    }
}
