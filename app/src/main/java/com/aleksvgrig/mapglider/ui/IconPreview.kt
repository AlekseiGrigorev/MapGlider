package com.aleksvgrig.mapglider.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AppIcon512() {
    Canvas(modifier = Modifier.size(512.dp)) {
        val width = size.width
        val height = size.height
        
        // Background
        drawRect(color = Color(0xFF2196F3))
        
        // Waves
        val waveColor = Color(0xFF4FC3F7)
        val strokeWidth = width * (1f / 108f)
        
        // M20,40 Q54,30 88,40
        drawPath(
            path = Path().apply {
                moveTo(width * (20f / 108f), height * (40f / 108f))
                quadraticTo(
                    width * (54f / 108f), height * (30f / 108f),
                    width * (88f / 108f), height * (40f / 108f),
                )
            },
            color = waveColor,
            style = Stroke(width = strokeWidth)
        )
        
        // M20,60 Q54,50 88,60
        drawPath(
            path = Path().apply {
                moveTo(width * (20f / 108f), height * (60f / 108f))
                quadraticTo(
                    width * (54f / 108f), height * (50f / 108f),
                    width * (88f / 108f), height * (60f / 108f),
                )
            },
            color = waveColor,
            style = Stroke(width = strokeWidth)
        )
        
        // M20,80 Q54,70 88,80
        drawPath(
            path = Path().apply {
                moveTo(width * (20f / 108f), height * (80f / 108f))
                quadraticTo(
                    width * (54f / 108f), height * (70f / 108f),
                    width * (88f / 108f), height * (80f / 108f),
                )
            },
            color = waveColor,
            style = Stroke(width = strokeWidth)
        )
        
        // Foreground Glider: M54,25 L85,75 L54,65 L23,75 Z
        drawPath(
            path = Path().apply {
                moveTo(width * (54f / 108f), height * (25f / 108f))
                lineTo(width * (85f / 108f), height * (75f / 108f))
                lineTo(width * (54f / 108f), height * (65f / 108f))
                lineTo(width * (23f / 108f), height * (75f / 108f))
                close()
            },
            color = Color.White
        )
    }
}

@Preview(showBackground = true, widthDp = 512, heightDp = 512)
@Composable
fun AppIconPreview() {
    AppIcon512()
}
