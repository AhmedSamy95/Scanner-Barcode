package com.example.scanpro.ui.scanner

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import com.example.scanpro.theme.PrimaryAccent

/**
 * A beautiful, premium, custom viewfinder canvas overlay.
 * Draws a dark translucent background with a clear scanning center,
 * neon cyan corner brackets, and a sweeping horizontal scanner line.
 */
@Composable
fun ViewfinderOverlay(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanLine")
    val scanLineProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scanLineAnim"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Define bounding scanning box (70% width, 35% height)
        val rectWidth = canvasWidth * 0.7f
        val rectHeight = canvasHeight * 0.35f
        val left = (canvasWidth - rectWidth) / 2
        val top = (canvasHeight - rectHeight) / 2
        val right = left + rectWidth
        val bottom = top + rectHeight

        // 1. Draw translucent dark overlay (surrounding bounds)
        val overlayColor = Color(0xAA080D14)
        drawRect(overlayColor, topLeft = Offset.Zero, size = Size(canvasWidth, top)) // top overlay
        drawRect(overlayColor, topLeft = Offset(0f, bottom), size = Size(canvasWidth, canvasHeight - bottom)) // bottom overlay
        drawRect(overlayColor, topLeft = Offset(0f, top), size = Size(left, rectHeight)) // left overlay
        drawRect(overlayColor, topLeft = Offset(right, top), size = Size(canvasWidth - right, rectHeight)) // right overlay

        // 2. Draw 4 Corner L-Brackets
        val strokeWidth = 8f
        val cornerLen = 50f
        val accent = PrimaryAccent

        // Top Left
        drawLine(accent, Offset(left, top), Offset(left + cornerLen, top), strokeWidth)
        drawLine(accent, Offset(left, top), Offset(left, top + cornerLen), strokeWidth)

        // Top Right
        drawLine(accent, Offset(right, top), Offset(right - cornerLen, top), strokeWidth)
        drawLine(accent, Offset(right, top), Offset(right, top + cornerLen), strokeWidth)

        // Bottom Left
        drawLine(accent, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeWidth)
        drawLine(accent, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeWidth)

        // Bottom Right
        drawLine(accent, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeWidth)
        drawLine(accent, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeWidth)

        // 3. Draw static center guide line (dashed, white)
        val centerY = top + rectHeight / 2f
        drawLine(
            color = Color.White.copy(alpha = 0.55f),
            start = Offset(left + cornerLen + 12f, centerY),
            end = Offset(right - cornerLen - 12f, centerY),
            strokeWidth = 2.5f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 10f), 0f)
        )

        // 4. Draw animated sweeping scanning line with gradient glow
        val lineY = top + (rectHeight * scanLineProgress)
        drawLine(
            brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, accent, accent, Color.Transparent),
                startX = left,
                endX = right
            ),
            start = Offset(left + 8f, lineY),
            end = Offset(right - 8f, lineY),
            strokeWidth = 4f
        )
    }
}
