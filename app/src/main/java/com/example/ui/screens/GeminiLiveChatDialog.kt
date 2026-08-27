package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.BuildConfig
import com.example.ui.viewmodel.LedgerViewModel
import com.example.utils.GeminiLiveService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiLiveChatDialog(
    viewModel: LedgerViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var hasRecordPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasRecordPermission = isGranted
    }

    val apiKey = BuildConfig.GEMINI_API_KEY // Assuming secrets-gradle-plugin is configured
    val liveService = remember { GeminiLiveService(apiKey) }
    val connectionState by liveService.connectionState.collectAsState()

    DisposableEffect(Unit) {
        onDispose {
            liveService.disconnect()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "المساعد الصوتي الفوري (Gemini Live)",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "تحدث مباشرة مع النظام للمزامنة والاستعلام الصوتي السريع",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                val statusText = when (connectionState) {
                    GeminiLiveService.ConnectionState.Disconnected -> "مستعد للاتصال"
                    GeminiLiveService.ConnectionState.Connecting -> "جاري الاتصال..."
                    GeminiLiveService.ConnectionState.Connected -> "متصل الآن - تحدث..."
                    GeminiLiveService.ConnectionState.Error -> "حدث خطأ في الاتصال"
                }

                Text(
                    text = statusText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (connectionState == GeminiLiveService.ConnectionState.Connected) 
                        MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (connectionState == GeminiLiveService.ConnectionState.Disconnected || connectionState == GeminiLiveService.ConnectionState.Error) {
                    Button(
                        onClick = {
                            if (hasRecordPermission) {
                                liveService.connect()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        },
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.Mic, contentDescription = "Start", modifier = Modifier.size(40.dp))
                    }
                } else {
                    val infiniteTransition = rememberInfiniteTransition()
                    val scale by infiniteTransition.animateFloat(
                        initialValue = 1f,
                        targetValue = 1.2f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse"
                    )
                    
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(80.dp * scale)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                        )
                        Button(
                            onClick = {
                                liveService.disconnect()
                            },
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = "Stop", modifier = Modifier.size(40.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("إغلاق")
            }
        }
    )
}
