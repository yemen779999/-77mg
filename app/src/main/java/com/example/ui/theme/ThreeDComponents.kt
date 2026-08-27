package com.example.ui.theme

import android.os.Build
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.threeDTiltEffect(
    maxRotationDegrees: Float = 8f
): Modifier = composed {
    val is3DEnabled = Local3DEffectsEnabled.current
    val animScale = LocalAnimationScale.current
    if (!is3DEnabled || animScale <= 0f) return@composed this

    var rotationX by remember { mutableFloatStateOf(0f) }
    var rotationY by remember { mutableFloatStateOf(0f) }
    var isPressed by remember { mutableStateOf(false) }

    val springSpec = spring<Float>(stiffness = (300f / animScale).coerceAtLeast(100f))

    val animatedRotationX by animateFloatAsState(
        targetValue = if (isPressed) rotationX else 0f,
        animationSpec = springSpec,
        label = "rotationX"
    )
    val animatedRotationY by animateFloatAsState(
        targetValue = if (isPressed) rotationY else 0f,
        animationSpec = springSpec,
        label = "rotationY"
    )
    val animatedScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = springSpec,
        label = "scale"
    )

    this
        .graphicsLayer {
            this.rotationX = animatedRotationX
            this.rotationY = animatedRotationY
            this.scaleX = animatedScale
            this.scaleY = animatedScale
            this.cameraDistance = 16f * density
        }
        .pointerInput(Unit) {
            detectTapGestures(
                onPress = { offset ->
                    isPressed = true
                    val width = size.width
                    val height = size.height
                    if (width > 0 && height > 0) {
                        val xFactor = ((offset.x / width) - 0.5f) * 2f
                        val yFactor = ((offset.y / height) - 0.5f) * 2f
                        rotationY = xFactor * maxRotationDegrees
                        rotationX = -yFactor * maxRotationDegrees
                    }
                    tryAwaitRelease()
                    isPressed = false
                }
            )
        }
}

fun Modifier.threeDShadow(
    elevation: Dp = 8.dp
): Modifier = composed {
    val is3DEnabled = Local3DEffectsEnabled.current
    if (!is3DEnabled) return@composed this

    this.graphicsLayer {
        this.shadowElevation = elevation.toPx()
        this.shape = RoundedCornerShape(20.dp)
        this.clip = false
    }
}

/**
 * Reusable GlassCard Composable component that consumes the is3DEffectsEnabled state via Local3DEffectsEnabled
 * and conditionally applies a Surface with semi-transparent background, blur effect, and subtle elevation shadow based on the toggle.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = AppShapes.large,
    elevation: Dp = 6.dp,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    contentColor: Color = contentColorFor(containerColor),
    border: BorderStroke? = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val is3D = Local3DEffectsEnabled.current

    val cardColor = if (is3D) {
        containerColor.copy(alpha = 0.72f)
    } else {
        containerColor
    }

    val cardElevation = if (is3D) elevation else 0.dp

    val glassModifier = if (is3D) {
        modifier
            .threeDShadow(elevation = cardElevation)
            .threeDTiltEffect()
            .then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Modifier.blur(10.dp)
                } else {
                    Modifier
                }
            )
    } else {
        modifier
    }

    val cardBorder = if (is3D) {
        border ?: BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    } else {
        border
    }

    Surface(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = glassModifier,
        shape = shape,
        color = cardColor,
        contentColor = contentColor,
        tonalElevation = if (is3D) elevation / 2 else 0.dp,
        shadowElevation = cardElevation,
        border = cardBorder
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.normal),
            content = content
        )
    }
}
