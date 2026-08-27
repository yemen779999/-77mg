package com.example.ui.common

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/**
 * Unified Base UI State architecture representing standard UI states
 * across all screens in the application.
 */
sealed interface UiState<out T> {
    data class Loading(
        val message: String = "جاري تحميل البيانات المالية..."
    ) : UiState<Nothing>

    data class Content<out T>(
        val data: T
    ) : UiState<T>

    data class Empty(
        val title: String = "لا توجد بيانات متوفرة حالياً",
        val message: String = "يمكنك إضافة عناصر وسجلات جديدة للبدء مباشرة.",
        val icon: ImageVector = Icons.Default.ReceiptLong,
        val actionLabel: String? = null
    ) : UiState<Nothing>

    data class Error(
        val message: String = "حدث خطأ غير متوقع أثناء معالجة البيانات",
        val errorDetails: String? = null,
        val retryLabel: String = "إعادة المحاولة 🔄"
    ) : UiState<Nothing>
}

/**
 * High-End 3D Animated UI State Container that manages smooth transitions
 * between Loading, Content, Empty, and Error states with depth, lighting and physics.
 */
@Composable
fun <T> ThreeDUiStateLayout(
    state: UiState<T>,
    modifier: Modifier = Modifier,
    onRetry: (() -> Unit)? = null,
    onEmptyAction: (() -> Unit)? = null,
    loadingContent: (@Composable () -> Unit)? = null,
    emptyContent: (@Composable () -> Unit)? = null,
    errorContent: (@Composable (UiState.Error) -> Unit)? = null,
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = state,
        transitionSpec = {
            fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f, animationSpec = tween(300)) togetherWith
            fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 1.05f, animationSpec = tween(200))
        },
        label = "ThreeDUiStateTransition",
        modifier = modifier
    ) { targetState ->
        when (targetState) {
            is UiState.Loading -> {
                if (loadingContent != null) {
                    loadingContent()
                } else {
                    ThreeDLoadingState(message = targetState.message)
                }
            }
            is UiState.Empty -> {
                if (emptyContent != null) {
                    emptyContent()
                } else {
                    ThreeDEmptyState(
                        title = targetState.title,
                        message = targetState.message,
                        icon = targetState.icon,
                        actionLabel = targetState.actionLabel,
                        onAction = onEmptyAction
                    )
                }
            }
            is UiState.Error -> {
                if (errorContent != null) {
                    errorContent(targetState)
                } else {
                    ThreeDErrorState(
                        message = targetState.message,
                        errorDetails = targetState.errorDetails,
                        retryLabel = targetState.retryLabel,
                        onRetry = onRetry
                    )
                }
            }
            is UiState.Content -> {
                content(targetState.data)
            }
        }
    }
}

/**
 * 3D Loading Spinner with dynamic lighting, glowing ring and pulsating depth.
 */
@Composable
fun ThreeDLoadingState(
    message: String,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "3DLoadingLoop")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ringRotation"
    )
    val floatY by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "floatingY"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .graphicsLayer {
                    translationY = floatY
                    cameraDistance = 16f * density
                }
                .shadow(
                    elevation = 16.dp,
                    shape = AppShapes.huge,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                ),
            shape = AppShapes.huge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        MaterialTheme.colorScheme.surfaceVariant
                    )
                )
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.huge, horizontal = AppSpacing.extraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // 3D Outer Glowing Ring
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(88.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .rotate(rotation)
                            .border(
                                width = 4.dp,
                                brush = Brush.sweepGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.primary,
                                        AppColors.PrimaryGlacierBlue
                                    )
                                ),
                                shape = CircleShape
                            )
                    )
                    Icon(
                        imageVector = Icons.Default.AccountBalanceWallet,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Text(
                    text = message,
                    style = AppTypography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = "جاري مزامنة الدفاتر وحساب الأرصدة...",
                    style = AppTypography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * 3D Empty State with elevated illustration, tactile button and spacious layout.
 */
@Composable
fun ThreeDEmptyState(
    title: String,
    message: String,
    icon: ImageVector = Icons.Default.ReceiptLong,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "3DEmptyHover")
    val tilt by infiniteTransition.animateFloat(
        initialValue = -3f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "emptyTilt"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .threeDTiltEffect(maxRotationDegrees = 6f)
                .shadow(
                    elevation = 14.dp,
                    shape = AppShapes.huge,
                    ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                ),
            shape = AppShapes.huge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.huge, horizontal = AppSpacing.extraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // 3D Iconic Emblem
                Box(
                    modifier = Modifier
                        .size(84.dp)
                        .graphicsLayer {
                            rotationZ = tilt
                            cameraDistance = 12f * density
                        }
                        .shadow(8.dp, shape = RoundedCornerShape(24.dp))
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.surfaceVariant
                                )
                            ),
                            shape = RoundedCornerShape(24.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(42.dp)
                    )
                }

                Text(
                    text = title,
                    style = AppTypography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (actionLabel != null && onAction != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                    Button(
                        onClick = onAction,
                        shape = AppShapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        ),
                        contentPadding = PaddingValues(
                            horizontal = AppSpacing.large,
                            vertical = AppSpacing.medium
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimensions.buttonHeight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.small))
                        Text(
                            text = actionLabel,
                            style = AppTypography.labelLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * 3D Error State with alert graphics, diagnostic info and immediate retry button.
 */
@Composable
fun ThreeDErrorState(
    message: String,
    errorDetails: String? = null,
    retryLabel: String = "إعادة المحاولة 🔄",
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(AppSpacing.large),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
                .threeDTiltEffect(maxRotationDegrees = 6f)
                .shadow(
                    elevation = 16.dp,
                    shape = AppShapes.huge,
                    ambientColor = AppColors.DangerRed.copy(alpha = 0.25f),
                    spotColor = AppColors.DangerRed.copy(alpha = 0.4f)
                ),
            shape = AppShapes.huge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = androidx.compose.foundation.BorderStroke(
                width = 1.5.dp,
                color = AppColors.DangerRed.copy(alpha = 0.5f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppSpacing.huge, horizontal = AppSpacing.extraLarge),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.medium)
            ) {
                // 3D Danger Emblem
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .shadow(8.dp, shape = CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    AppColors.DangerRedLight,
                                    Color(0xFFFFCDD2)
                                )
                            ),
                            shape = CircleShape
                        )
                        .border(
                            1.dp,
                            AppColors.DangerRed.copy(alpha = 0.4f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = null,
                        tint = AppColors.DangerRed,
                        modifier = Modifier.size(44.dp)
                    )
                }

                Text(
                    text = "تعذر تحميل البيانات",
                    style = AppTypography.headlineSmall,
                    color = AppColors.DangerRed,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = message,
                    style = AppTypography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                if (!errorDetails.isNullOrBlank()) {
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        shape = AppShapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = errorDetails,
                            style = AppTypography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(AppSpacing.small),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                if (onRetry != null) {
                    Spacer(modifier = Modifier.height(AppSpacing.small))
                    Button(
                        onClick = onRetry,
                        shape = AppShapes.button,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AppColors.DangerRed
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 6.dp,
                            pressedElevation = 2.dp
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AppDimensions.buttonHeight)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(AppSpacing.small))
                        Text(
                            text = retryLabel,
                            style = AppTypography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
