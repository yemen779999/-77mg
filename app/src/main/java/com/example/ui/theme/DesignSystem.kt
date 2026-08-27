package com.example.ui.theme

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// 1. WINDOW SIZE CLASSIFICATION
// ==========================================
enum class WindowSizeClass {
    COMPACT, // Phones (< 600dp width)
    MEDIUM,  // Large phones / foldable / small tablets (600dp - 840dp width)
    EXPANDED // Large tablets / desktop screens (> 840dp width)
}

val LocalWindowSizeClass = staticCompositionLocalOf { WindowSizeClass.COMPACT }

// ==========================================
// 1.B ANIMATION & 3D COMPOSITION LOCALS
// ==========================================
enum class AnimationLevel(val label: String, val scale: Float) {
    FULL("كاملة (Full)", 1.0f),
    REDUCED("مخفضة (Reduced)", 0.5f),
    OFF("معطلة (Off)", 0.0f)
}

val Local3DEffectsEnabled = staticCompositionLocalOf { true }
val LocalAnimationLevel = staticCompositionLocalOf { AnimationLevel.FULL }
val LocalAnimationScale = staticCompositionLocalOf { 1.0f }

@Composable
fun rememberWindowSizeClass(): WindowSizeClass {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp
    return when {
        screenWidth < 600 -> WindowSizeClass.COMPACT
        screenWidth < 840 -> WindowSizeClass.MEDIUM
        else -> WindowSizeClass.EXPANDED
    }
}

@Composable
fun ProvideWindowSizeClass(
    windowSizeClass: WindowSizeClass = rememberWindowSizeClass(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        content()
    }
}

// ==========================================
// 2. SPACING DESIGN TOKENS (8dp Grid System)
// ==========================================
object AppSpacing {
    val none: Dp = 0.dp
    val micro: Dp = 2.dp
    val extraSmall: Dp = 4.dp
    val small: Dp = 8.dp
    val medium: Dp = 12.dp
    val normal: Dp = 16.dp
    val default: Dp = 16.dp
    val large: Dp = 20.dp
    val extraLarge: Dp = 24.dp
    val huge: Dp = 32.dp
    val massive: Dp = 48.dp
    val gigantic: Dp = 64.dp

    // Semantic Component Spacing
    val screenPadding: Dp = 16.dp
    val screenPaddingHorizontal: Dp = 16.dp
    val screenPaddingVertical: Dp = 16.dp
    val cardPadding: Dp = 16.dp
    val cardPaddingDense: Dp = 12.dp
    val dialogPadding: Dp = 24.dp
    val listItemGap: Dp = 8.dp
    val sectionGap: Dp = 20.dp
    val iconTextGap: Dp = 8.dp
    val bottomBarHeight: Dp = 64.dp
    val headerHeight: Dp = 56.dp
}

// ==========================================
// 3. SHAPES DESIGN TOKENS
// ==========================================
object AppShapes {
    val none = RoundedCornerShape(0.dp)
    val extraSmall = RoundedCornerShape(4.dp)
    val small = RoundedCornerShape(8.dp)
    val medium = RoundedCornerShape(12.dp)
    val normal = RoundedCornerShape(16.dp)
    val large = RoundedCornerShape(20.dp)
    val extraLarge = RoundedCornerShape(24.dp)
    val huge = RoundedCornerShape(32.dp)
    val full = CircleShape

    // Semantic Component Shapes
    val card = RoundedCornerShape(16.dp)
    val cardDense = RoundedCornerShape(12.dp)
    val button = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(8.dp)
    val badge = RoundedCornerShape(6.dp)
    val dialog = RoundedCornerShape(24.dp)
    val bottomSheet = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    val searchBar = RoundedCornerShape(14.dp)
    val textField = RoundedCornerShape(12.dp)
}

// ==========================================
// 4. DIMENSIONS & ELEVATION DESIGN TOKENS
// ==========================================
object AppDimensions {
    val minTouchTarget: Dp = 48.dp
    val buttonHeight: Dp = 48.dp
    val buttonHeightSmall: Dp = 36.dp
    val buttonHeightLarge: Dp = 56.dp
    val cardElevation: Dp = 2.dp
    val cardElevationHovered: Dp = 6.dp
    val iconSizeSmall: Dp = 16.dp
    val iconSizeMedium: Dp = 20.dp
    val iconSizeNormal: Dp = 24.dp
    val iconSizeLarge: Dp = 32.dp
    val iconSizeExtraLarge: Dp = 48.dp
    val avatarSmall: Dp = 32.dp
    val avatarMedium: Dp = 40.dp
    val avatarLarge: Dp = 56.dp
    val strokeThin: Dp = 1.dp
    val strokeMedium: Dp = 2.dp
    val badgeHeight: Dp = 24.dp
}

object AppElevation {
    val level0: Dp = 0.dp
    val level1: Dp = 1.dp
    val level2: Dp = 3.dp
    val level3: Dp = 6.dp
    val level4: Dp = 8.dp
    val level5: Dp = 12.dp
}

// ==========================================
// 5. COLORS DESIGN TOKENS (Comprehensive Palette)
// ==========================================
object AppColors {
    // Brand Core
    val PrimaryRoyalNavy = Color(0xFF1E3A8A)
    val PrimaryGlacierBlue = Color(0xFF38BDF8)
    val SecondarySlate = Color(0xFF475569)
    val TertiaryAmberGold = Color(0xFFD97706)
    val TertiarySunbeamGold = Color(0xFFFBBF24)

    // Financial & Accounting Status (Light Theme)
    val SuccessGreen = Color(0xFF10B981)        // الإيرادات / السداد / رصيد إيجابي
    val SuccessGreenLight = Color(0xFFD1FAE5)
    val SuccessGreenDark = Color(0xFF047857)

    val DangerRed = Color(0xFFEF4444)           // المصروفات / مديونية / مستحق
    val DangerRedLight = Color(0xFFFEE2E2)
    val DangerRedDark = Color(0xFFB91C1C)

    val WarningAmber = Color(0xFFF59E0B)        // تنبيه / مستحق قريباً / معلق
    val WarningAmberLight = Color(0xFFFEF3C7)
    val WarningAmberDark = Color(0xFFB45309)

    val InfoBlue = Color(0xFF3B82F6)            // معلومات / حسابات / قيود
    val InfoBlueLight = Color(0xFFDBEAFE)
    val InfoBlueDark = Color(0xFF1D4ED8)

    val PurpleIndigo = Color(0xFF6366F1)
    val PurpleIndigoLight = Color(0xFFEEF2FF)

    // Dark Mode Variants
    val DarkSuccessGreen = Color(0xFF34D399)
    val DarkDangerRed = Color(0xFFF87171)
    val DarkWarningAmber = Color(0xFFFBBF24)
    val DarkInfoBlue = Color(0xFF60A5FA)
    val DarkPurpleIndigo = Color(0xFF818CF8)

    // Financial Semantic Aliases
    val Debit = DangerRed
    val Credit = SuccessGreen
    val Balanced = InfoBlue
    val Pending = WarningAmber
    val CashBox = TertiaryAmberGold

    // Neutral Surfaces & Backgrounds (Light Theme)
    val BackgroundLight = Color(0xFFF1F5F9)
    val SurfaceLight = Color(0xFFFFFFFF)
    val SurfaceLightVariant = Color(0xFFF8FAFC)
    val CardBorderLight = Color(0xFFE2E8F0)
    val TextPrimaryLight = Color(0xFF0F172A)
    val TextSecondaryLight = Color(0xFF64748B)
    val TextMutedLight = Color(0xFF94A3B8)

    // Neutral Surfaces & Backgrounds (Dark Theme)
    val BackgroundDark = Color(0xFF0F172A)
    val SurfaceDark = Color(0xFF1E293B)
    val SurfaceDarkVariant = Color(0xFF334155)
    val CardBorderDark = Color(0xFF334155)
    val TextPrimaryDark = Color(0xFFF8FAFC)
    val TextSecondaryDark = Color(0xFF94A3B8)
    val TextMutedDark = Color(0xFF64748B)

    // Currency Badge Tints
    val CurrencyYer = Color(0xFF10B981) // ريال يمني (أخضر زمردي)
    val CurrencySar = Color(0xFF0284C7) // ريال سعودي (أزرق سماوي)
    val CurrencyUsd = Color(0xFF059669) // دولار أمريكي (أخضر مالي)

    // Gradients
    val PrimaryGradientLight = Brush.horizontalGradient(
        colors = listOf(Color(0xFF1E3A8A), Color(0xFF2563EB))
    )
    val PrimaryGradientDark = Brush.horizontalGradient(
        colors = listOf(Color(0xFF0F172A), Color(0xFF1E3A8A))
    )
    val SuccessGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFF059669), Color(0xFF10B981))
    )
    val DangerGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFDC2626), Color(0xFFEF4444))
    )
    val GoldGradient = Brush.horizontalGradient(
        colors = listOf(Color(0xFFD97706), Color(0xFFF59E0B))
    )
}

// ==========================================
// 6. TYPOGRAPHY DESIGN TOKENS (RTL / Arabic Optimized)
// ==========================================
object AppTypography {
    private val arabicFontFamily = FontFamily.Default

    // Display Styles (for Large Figures, Hero Headers)
    val displayLarge = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val displayMedium = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val displaySmall = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    // Headline Styles
    val headlineLarge = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 30.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val headlineMedium = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val headlineSmall = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    // Title Styles (Card Headers, Dialog Titles)
    val titleLarge = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val titleMedium = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val titleSmall = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    // Body Styles (Main Text, Descriptions, Entries)
    val bodyLarge = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val bodyMedium = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val bodySmall = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    // Label & Badge Styles (Tags, Statuses, Buttons)
    val labelLarge = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Rtl
    )

    val labelMedium = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Rtl
    )

    val labelSmall = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Center,
        textDirection = TextDirection.Rtl
    )

    // Specialized Financial & Numeric Typography
    val financialBalance = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl
    )

    val financialTotal = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp,
        textDirection = TextDirection.Rtl
    )

    val tableHeader = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    val tableCell = TextStyle(
        fontFamily = arabicFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp,
        textAlign = TextAlign.Start,
        textDirection = TextDirection.Rtl
    )

    // Complete Material 3 Typography Scale Mapping
    val materialTypography = Typography(
        displayLarge = displayLarge,
        displayMedium = displayMedium,
        displaySmall = displaySmall,
        headlineLarge = headlineLarge,
        headlineMedium = headlineMedium,
        headlineSmall = headlineSmall,
        titleLarge = titleLarge,
        titleMedium = titleMedium,
        titleSmall = titleSmall,
        bodyLarge = bodyLarge,
        bodyMedium = bodyMedium,
        bodySmall = bodySmall,
        labelLarge = labelLarge,
        labelMedium = labelMedium,
        labelSmall = labelSmall
    )
}

// ==========================================
// 7. ARABIC / RTL COMPOSITION HELPERS
// ==========================================

/**
 * Ensures strict RTL direction layout for all nested Composables,
 * providing standard Arabic typography and layout standards.
 */
@Composable
fun AppRtlContainer(
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        content()
    }
}

/**
 * Returns dynamic balance color based on financial amount and current theme
 */
@Composable
fun getBalanceColor(balance: Double): Color {
    val isDark = isSystemInDarkTheme()
    return when {
        balance > 0 -> if (isDark) AppColors.DarkSuccessGreen else AppColors.SuccessGreen
        balance < 0 -> if (isDark) AppColors.DarkDangerRed else AppColors.DangerRed
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
}

// ==========================================
// 8. ADAPTIVE UI WRAPPERS & GRID HELPERS
// ==========================================

/**
 * Wraps screen content with adaptive width constraints to ensure optimal layout
 * on phones, foldables, tablets, and wide screens.
 */
@Composable
fun AdaptiveContentContainer(
    modifier: Modifier = Modifier,
    maxWidthCompact: Dp = Dp.Unspecified,
    maxWidthMedium: Dp = 720.dp,
    maxWidthExpanded: Dp = 1080.dp,
    content: @Composable androidx.compose.foundation.layout.BoxScope.() -> Unit
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val targetMaxWidth = when (windowSizeClass) {
        WindowSizeClass.COMPACT -> maxWidthCompact
        WindowSizeClass.MEDIUM -> maxWidthMedium
        WindowSizeClass.EXPANDED -> maxWidthExpanded
    }

    androidx.compose.foundation.layout.Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = androidx.compose.ui.Alignment.TopCenter
    ) {
        androidx.compose.foundation.layout.Box(
            modifier = if (targetMaxWidth != Dp.Unspecified) {
                Modifier
                    .widthIn(max = targetMaxWidth)
                    .fillMaxWidth()
            } else {
                Modifier.fillMaxWidth()
            },
            content = content
        )
    }
}

/**
 * Returns optimal grid column count based on current WindowSizeClass
 */
@Composable
fun rememberAdaptiveGridColumns(): Int {
    val windowSizeClass = LocalWindowSizeClass.current
    return when (windowSizeClass) {
        WindowSizeClass.COMPACT -> 1
        WindowSizeClass.MEDIUM -> 2
        WindowSizeClass.EXPANDED -> 3
    }
}

/**
 * Helper to produce haptic vibration feedback for tactile responses on key button clicks and actions.
 */
@Composable
fun rememberHapticFeedback(): () -> Unit {
    val haptic = LocalHapticFeedback.current
    val view = LocalView.current
    return remember(haptic, view) {
        {
            try {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } catch (_: Exception) {}
            try {
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            } catch (_: Exception) {}
        }
    }
}


