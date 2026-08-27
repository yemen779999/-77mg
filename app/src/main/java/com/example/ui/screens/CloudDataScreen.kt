package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.widget.Toast
import com.example.ui.viewmodel.LedgerViewModel
import com.example.data.model.MaterialItem
import com.example.data.model.Invoice
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CloudDataScreen(viewModel: LedgerViewModel) {
    var showGeminiLiveDialog by remember { mutableStateOf(false) }
    val materials by viewModel.cloudMaterials.collectAsState()
    val invoices by viewModel.cloudInvoices.collectAsState()
    val lastCloudSync by viewModel.lastCloudSync.collectAsState()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsState()
    val cloudClientId by viewModel.cloudClientId.collectAsState()
    val businessName by viewModel.businessName.collectAsState()
    val isCloudFrozen by viewModel.isCloudFrozen.collectAsState()
    val context = LocalContext.current

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showGeminiLiveDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(imageVector = Icons.Default.Mic, contentDescription = "Gemini Live Assistant")
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                // Header Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (!isCloudFrozen && cloudClientId.isNotBlank()) Color(0xFF22C55E) else Color(0xFFF59E0B))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                if (!isCloudFrozen && cloudClientId.isNotBlank()) "مزامنة سحابية نشطة" else "وضع المزامنة معطل / مجمد",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Text(
                        text = "المزامنة والبيانات السحابية",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            // Sync Profile Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountCircle,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = businessName.ifBlank { "منشأة المحاسب الشامل" },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "معرف العميل السحابي: $cloudClientId",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        HorizontalDivider()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "آخر مزامنة: ${lastCloudSync.ifBlank { "لم تتم المزامنة بعد" }}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Button(
                                onClick = {
                                    viewModel.syncWithCloud { success: Boolean, msg: String ->
                                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                    }
                                },
                                enabled = !isCloudSyncing,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                if (isCloudSyncing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("جاري المزامنة...", fontSize = 11.sp)
                                } else {
                                    Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("مزامنة الآن 🔄", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "المواد المسجلة بالسحابة (Inventory Items)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            }

            if (materials.isEmpty()) {
                item {
                    Text("لا توجد مواد مسجلة", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            } else {
                items(materials) { material ->
                    MaterialItemCard(material)
                }
            }

            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "الفواتير",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Right
                )
            }

            if (invoices.isEmpty()) {
                item {
                    Text("لا توجد فواتير مسجلة", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                }
            } else {
                items(invoices) { invoice ->
                    InvoiceCard(invoice)
                }
            }
        }
    }

    if (showGeminiLiveDialog) {
        GeminiLiveChatDialog(
            viewModel = viewModel,
            onDismiss = { showGeminiLiveDialog = false }
        )
    }
}

@Composable
fun MaterialItemCard(item: MaterialItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(text = item.name, fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الإجمالي: ${item.total}")
                Text("السعر: ${item.unitPrice} | العدد: ${item.count}")
            }
        }
    }
}

@Composable
fun InvoiceCard(invoice: Invoice) {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val dateStr = sdf.format(Date(invoice.date))
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
            Text(text = "فاتورة: ${invoice.invoiceId}", fontWeight = FontWeight.Bold, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Text(text = "العميل: ${invoice.clientName}", textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الإجمالي: ${invoice.total}")
                Text("السعر: ${invoice.unitPrice} | العدد: ${invoice.count}")
            }
            Text(text = "التاريخ: $dateStr", style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Right, modifier = Modifier.fillMaxWidth())
        }
    }
}
