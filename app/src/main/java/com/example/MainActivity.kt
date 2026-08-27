package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.navigation.Screen
import com.example.ui.screens.AccountsScreen
import com.example.ui.screens.AiAdvisorScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.EntriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrashScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.ProvideWindowSizeClass
import com.example.ui.theme.WindowSizeClass
import com.example.ui.theme.rememberWindowSizeClass
import com.example.ui.viewmodel.LedgerViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: LedgerViewModel = viewModel()
            val is3D by viewModel.is3DEffectsEnabled.collectAsState()
            val animLevel by viewModel.animationLevel.collectAsState()

            MyApplicationTheme(
                is3DEffectsEnabled = is3D,
                animationLevel = animLevel
            ) {
                ProvideWindowSizeClass {
                    MainLayout(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppHeader(currentRoute: String?, businessName: String, onLock: () -> Unit) {
    val title = "المحاسب الشامل"
    val subtext = when (currentRoute) {
        Screen.Dashboard.route -> "Pro Ledger • لوحة التحكم"
        Screen.Accounts.route -> "Pro Ledger • إدارة الحسابات"
        Screen.Transactions.route -> "Pro Ledger • العمليات والقيود"
        Screen.Settings.route -> "Pro Ledger • تهيئة الإعدادات"
        Screen.AiAdvisor.route -> "Pro Ledger • المستشار المالي الذكي"
        Screen.Trash.route -> "Pro Ledger • سلة المحذوفات"
        else -> "Pro Ledger"
    }

    val avatarChar = if (businessName.isNotBlank()) businessName.trim().first().toString() else "👤"
    val borderColor = MaterialTheme.colorScheme.outline

    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .drawBehind {
                val strokeWidth = 1.dp.toPx()
                val y = size.height - strokeWidth / 2
                drawLine(
                    color = borderColor,
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = strokeWidth
                )
            }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.Start) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = (-0.5).sp
                    )
                    Text(
                        text = subtext,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    IconButton(
                        onClick = onLock,
                        modifier = Modifier
                            .size(36.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "قفل التطبيق وتأكيد الأمان",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = avatarChar,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MainLayout(viewModel: LedgerViewModel = viewModel()) {
    val navController = rememberNavController()
    val windowSizeClass = rememberWindowSizeClass()

    val isSecurityEnabled by viewModel.isSecurityEnabled.collectAsState()
    val securityPin by viewModel.securityPin.collectAsState()
    
    var isUnlocked by remember { mutableStateOf(false) }
    val businessName by viewModel.businessName.collectAsState()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: Screen.Dashboard.route

    LaunchedEffect(isSecurityEnabled, securityPin) {
        if (!isSecurityEnabled) {
            isUnlocked = true
        }
    }

    val navigateToAccountStatement: (Int) -> Unit = { accountId ->
        viewModel.selectAccount(accountId)
        navController.navigate(Screen.Transactions.route) {
            launchSingleTop = true
        }
    }

    if (!isUnlocked && isSecurityEnabled && securityPin.isNotBlank()) {
        PinLockScreen(
            correctPin = securityPin,
            onUnlockSuccess = { isUnlocked = true }
        )
    } else if (!isUnlocked && (!isSecurityEnabled || securityPin.isBlank())) {
        PinSetupScreen(
            onPinSet = { newPin ->
                viewModel.updateSecuritySettings(true, newPin)
                isUnlocked = true
            },
            onSkip = {
                isUnlocked = true
            }
        )
    } else {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppHeader(
                    currentRoute = currentRoute,
                    businessName = businessName,
                    onLock = { isUnlocked = false }
                )
            },
            bottomBar = {
                if (windowSizeClass == WindowSizeClass.COMPACT) {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentRoute == Screen.Dashboard.route,
                            onClick = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Accounts.route,
                            onClick = { navController.navigate(Screen.Accounts.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Group, contentDescription = "الحسابات") },
                            label = { Text("الحسابات", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Transactions.route,
                            onClick = { navController.navigate(Screen.Transactions.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "العمليات") },
                            label = { Text("العمليات", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.AiAdvisor.route,
                            onClick = { navController.navigate(Screen.AiAdvisor.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Lightbulb, contentDescription = "المستشار") },
                            label = { Text("المستشار", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationBarItem(
                            selected = currentRoute == Screen.Settings.route || currentRoute == Screen.Trash.route,
                            onClick = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                            label = { Text("الإعدادات", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                        )
                    }
                }
            }
        ) { innerPadding ->
            val isCloudFrozen by viewModel.isCloudFrozen.collectAsState()
            val cloudClientId by viewModel.cloudClientId.collectAsState()

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (windowSizeClass != WindowSizeClass.COMPACT) {
                    NavigationRail(
                        modifier = Modifier.fillMaxHeight(),
                        header = {
                            Box(
                                modifier = Modifier
                                    .padding(vertical = 16.dp)
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "📊",
                                    fontSize = 20.sp
                                )
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.weight(1f))
                        NavigationRailItem(
                            selected = currentRoute == Screen.Dashboard.route,
                            onClick = { navController.navigate(Screen.Dashboard.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Dashboard, contentDescription = "الرئيسية") },
                            label = { Text("الرئيسية", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == Screen.Accounts.route,
                            onClick = { navController.navigate(Screen.Accounts.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Group, contentDescription = "الحسابات") },
                            label = { Text("الحسابات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == Screen.Transactions.route,
                            onClick = { navController.navigate(Screen.Transactions.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "العمليات") },
                            label = { Text("العمليات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == Screen.AiAdvisor.route,
                            onClick = { navController.navigate(Screen.AiAdvisor.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Lightbulb, contentDescription = "المستشار") },
                            label = { Text("المستشار", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == Screen.Settings.route,
                            onClick = { navController.navigate(Screen.Settings.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Settings, contentDescription = "الإعدادات") },
                            label = { Text("الإعدادات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        NavigationRailItem(
                            selected = currentRoute == Screen.Trash.route,
                            onClick = { navController.navigate(Screen.Trash.route) { popUpTo(Screen.Dashboard.route) { saveState = true }; launchSingleTop = true; restoreState = true } },
                            icon = { Icon(Icons.Default.Delete, contentDescription = "سلة المحذوفات") },
                            label = { Text("المحذوفات", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    if (isCloudFrozen && currentRoute != Screen.Settings.route) {
                        FrozenBlockedScreen(
                            clientId = cloudClientId,
                            onGoToSettings = { navController.navigate(Screen.Settings.route) }
                        )
                    } else {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Dashboard.route
                        ) {
                            composable(Screen.Dashboard.route) {
                                DashboardScreen(
                                    viewModel = viewModel,
                                    onNavigateToAccount = navigateToAccountStatement
                                )
                            }
                            composable(Screen.Accounts.route) {
                                AccountsScreen(
                                    viewModel = viewModel,
                                    onNavigateToAccount = navigateToAccountStatement
                                )
                            }
                            composable(Screen.Transactions.route) {
                                EntriesScreen(
                                    viewModel = viewModel
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    viewModel = viewModel,
                                    onNavigateToTrash = { navController.navigate(Screen.Trash.route) }
                                )
                            }
                            composable(Screen.AiAdvisor.route) {
                                AiAdvisorScreen(
                                    viewModel = viewModel
                                )
                            }
                            composable(Screen.Trash.route) {
                                TrashScreen(
                                    viewModel = viewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PinLockScreen(
    correctPin: String,
    onUnlockSuccess: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val actualCorrectPin = correctPin.ifBlank { "1234" }

    val onDigitClick: (String) -> Unit = { digit ->
        if (enteredPin.length < 4) {
            showError = false
            enteredPin += digit
            if (enteredPin.length == 4) {
                if (enteredPin == actualCorrectPin) {
                    onUnlockSuccess()
                } else {
                    showError = true
                    enteredPin = ""
                }
            }
        }
    }

    val onBackspace: () -> Unit = {
        if (enteredPin.isNotEmpty()) {
            showError = false
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "المحاسب الشامل • Pro Ledger",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = "أدخل رمز PIN المكون من 4 أرقام لفتح التطبيق",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (0..3).forEach { index ->
                    val isFilled = index < enteredPin.length
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            if (showError) {
                Text(
                    text = "❌ الرمز السري غير صحيح، يرجى المحاولة مرة أخرى",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "back")
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        row.forEach { button ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (button.isNotBlank()) {
                                    if (button == "back") {
                                        IconButton(
                                            onClick = onBackspace,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "مسح الخلف",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                .clickable { onDigitClick(button) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = button,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
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
fun PinSetupScreen(
    onPinSet: (String) -> Unit,
    onSkip: () -> Unit
) {
    var step by remember { mutableStateOf(1) }
    var enteredPin by remember { mutableStateOf("") }
    var confirmedPin by remember { mutableStateOf("") }
    var showError by remember { mutableStateOf(false) }

    val onDigitClick: (String) -> Unit = { digit ->
        showError = false
        if (step == 1) {
            if (enteredPin.length < 4) {
                enteredPin += digit
                if (enteredPin.length == 4) {
                    step = 2
                }
            }
        } else {
            if (confirmedPin.length < 4) {
                confirmedPin += digit
                if (confirmedPin.length == 4) {
                    if (confirmedPin == enteredPin) {
                        onPinSet(confirmedPin)
                    } else {
                        showError = true
                        confirmedPin = ""
                    }
                }
            }
        }
    }

    val onBackspace: () -> Unit = {
        showError = false
        if (step == 1) {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
            }
        } else {
            if (confirmedPin.isNotEmpty()) {
                confirmedPin = confirmedPin.dropLast(1)
            } else {
                step = 1
                enteredPin = enteredPin.dropLast(1)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = "إنشاء قفل الأمان السري 🔒",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = if (step == 1) 
                        "أدخل رمز PIN مكوّن من 4 أرقام لتأمين سجلاتك" 
                    else 
                        "أعد إدخال الرمز السري لتأكيده وتنشيط الحماية فوراً",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val currentPinLength = if (step == 1) enteredPin.length else confirmedPin.length
                (0..3).forEach { index ->
                    val isFilled = index < currentPinLength
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (isFilled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f)
                            )
                    )
                }
            }

            if (showError) {
                Text(
                    text = "❌ الرموز غير متطابقة! يرجى إعادة التأكيد مجدداً",
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 280.dp)
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "back")
                )

                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        row.forEach { button ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1.2f),
                                contentAlignment = Alignment.Center
                            ) {
                                if (button.isNotBlank()) {
                                    if (button == "back") {
                                        IconButton(
                                            onClick = onBackspace,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "تراجع",
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                .clickable { onDigitClick(button) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = button,
                                                fontSize = 20.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = MaterialTheme.colorScheme.onBackground
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            TextButton(
                onClick = onSkip,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("تخطي للرئيسية بدون قفل حالياً 🔓", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FrozenBlockedScreen(clientId: String, onGoToSettings: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.error)
        ) {
            Column(
                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(64.dp)
                )
                
                Text(
                    text = "عذراً، هذا الحساب مجمد! 🛑",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "لقد تم تجميد أو تعليق ترخيص هذا الجهاز (المعرّف: $clientId) بواسطة المالك العام للنظام في لوحة التحكم الإدارية للويندوز.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Text(
                    text = "الرجاء التواصل مع المطور/المشرف لإعادة تفعيل حسابك ومتابعة عملياتك المالية اليومية:\nanas774928318@gmail.com",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Button(
                    onClick = onGoToSettings,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("انتقل للإعدادات لإعادة مزامنة التفعيل 💳", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
