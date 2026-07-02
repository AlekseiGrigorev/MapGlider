package com.aleksvgrig.mapglider.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun Joystick(
    modifier: Modifier = Modifier,
    size: Dp = 150.dp,
    dotSize: Dp = 50.dp,
    onJoystickUpdate: (Offset) -> Unit
) {
    var fingerOffset by remember { mutableStateOf(Offset.Zero) }
    val radius = with(androidx.compose.ui.platform.LocalDensity.current) { (size / 2).toPx() }

    Box(
        modifier = modifier
            .size(size)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { /* Handle start */ },
                    onDragEnd = {
                        fingerOffset = Offset.Zero
                        onJoystickUpdate(Offset.Zero)
                    },
                    onDragCancel = {
                        fingerOffset = Offset.Zero
                        onJoystickUpdate(Offset.Zero)
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        val newOffset = fingerOffset + dragAmount
                        val distance = sqrt(newOffset.x.pow(2) + newOffset.y.pow(2))
                        
                        fingerOffset = if (distance <= radius) {
                            newOffset
                        } else {
                            val angle = atan2(newOffset.y, newOffset.x)
                            Offset(
                                cos(angle) * radius,
                                sin(angle) * radius
                            )
                        }
                        
                        // Normalize the offset to -1.0 to 1.0 range
                        onJoystickUpdate(Offset(fingerOffset.x / radius, fingerOffset.y / radius))
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Base of joystick
        val baseColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = baseColor,
                radius = radius
            )
            drawCircle(
                color = outlineColor,
                radius = radius,
                style = Stroke(width = 2.dp.toPx())
            )
        }

        // Joystick knob
        Surface(
            modifier = Modifier
                .size(dotSize)
                .offset {
                    IntOffset(
                        fingerOffset.x.toInt(),
                        fingerOffset.y.toInt()
                    )
                },
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            tonalElevation = 8.dp,
            shadowElevation = 4.dp
        ) {
            Box(modifier = Modifier.fillMaxSize())
        }
    }
}
