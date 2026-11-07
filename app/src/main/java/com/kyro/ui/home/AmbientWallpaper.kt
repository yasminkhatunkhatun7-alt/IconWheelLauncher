package com.kyro.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import com.kyro.ui.theme.Amber
import com.kyro.ui.theme.DeepVoid

@Composable
fun AmbientWallpaper() {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -200f,
        targetValue = 200f,
        animationSpec = infiniteRepeatable(
            animation = tween(15000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ambientOffset"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        drawRect(color = DeepVoid)

        val brush = Brush.radialGradient(
            colors = listOf(Amber.copy(alpha = 0.05f), DeepVoid),
            center = Offset(w / 2, (h / 2) + offsetY),
            radius = w
        )

        drawRect(brush = brush)

        val brush2 = Brush.radialGradient(
            colors = listOf(Amber.copy(alpha = 0.03f), Color.Transparent),
            center = Offset(w * 0.8f, h * 0.3f - offsetY),
            radius = w * 0.8f
        )
        drawRect(brush = brush2)
    }
}
