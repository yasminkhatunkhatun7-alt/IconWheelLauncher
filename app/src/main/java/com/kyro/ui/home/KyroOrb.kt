package com.kyro.ui.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.kyro.ui.theme.Amber
import com.kyro.ui.theme.DeepVoid
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun KyroOrb(isListening: Boolean, isThinking: Boolean = false, isSpeaking: Boolean = false, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "OrbTransition")

    val activeState = isListening || isThinking || isSpeaking

    // Breathing pulse for the core
    val pulseTarget = when {
        isSpeaking -> 1.3f
        isListening -> 1.1f
        isThinking -> 1.0f
        else -> 1.0f
    }

    val pulseDuration = when {
        isSpeaking -> 300
        isListening -> 600
        isThinking -> 1000
        else -> 1500
    }

    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = pulseTarget,
        animationSpec = infiniteRepeatable(
            animation = tween(pulseDuration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Pulse"
    )

    // Outer ring rotation
    val rotationDuration = when {
        isThinking -> 1500
        isListening -> 2000
        isSpeaking -> 1800
        else -> 6000
    }

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(rotationDuration, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Canvas(modifier = modifier.size(250.dp)) {
        val center = Offset(size.width / 2, size.height / 2)
        val maxRadius = size.width / 2
        val coreRadius = (maxRadius * 0.45f) * pulse

        // Deep Obsidian Core
        drawCircle(
            color = DeepVoid,
            radius = coreRadius,
            center = center
        )

        // Amber glowing edges
        val edgeColor = if (isSpeaking) Color.White else Amber.copy(alpha = 0.6f)
        drawCircle(
            color = edgeColor,
            radius = coreRadius + 10f,
            center = center,
            style = Stroke(width = 15f)
        )

        // Rotating Wireframe Ring
        rotate(rotation, center) {
            drawArc(
                color = Amber.copy(alpha = 0.4f),
                startAngle = 0f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - maxRadius * 0.8f, center.y - maxRadius * 0.8f),
                size = androidx.compose.ui.geometry.Size(maxRadius * 1.6f, maxRadius * 1.6f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )
            drawArc(
                color = Amber.copy(alpha = 0.4f),
                startAngle = 180f,
                sweepAngle = 100f,
                useCenter = false,
                topLeft = Offset(center.x - maxRadius * 0.8f, center.y - maxRadius * 0.8f),
                size = androidx.compose.ui.geometry.Size(maxRadius * 1.6f, maxRadius * 1.6f),
                style = Stroke(width = 5f, cap = StrokeCap.Round)
            )

            if (isThinking) {
                 drawArc(
                    color = Color.White.copy(alpha = 0.8f),
                    startAngle = 90f,
                    sweepAngle = 45f,
                    useCenter = false,
                    topLeft = Offset(center.x - maxRadius * 0.6f, center.y - maxRadius * 0.6f),
                    size = androidx.compose.ui.geometry.Size(maxRadius * 1.2f, maxRadius * 1.2f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round)
                )
            }
        }

        // Orbiting Particle
        val particleOrbitRadius = maxRadius * 0.95f
        val particleSpeed = if (activeState) 2.5 else 1.5
        val particleX = center.x + particleOrbitRadius * cos(Math.toRadians(rotation.toDouble() * particleSpeed)).toFloat()
        val particleY = center.y + particleOrbitRadius * sin(Math.toRadians(rotation.toDouble() * particleSpeed)).toFloat()
        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(particleX, particleY)
        )
    }
}
