package com.nickdegs.sosyalpanel.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nickdegs.sosyalpanel.data.Platform
import kotlin.math.cos
import kotlin.math.sin

// iOS .glassEffect karşılığı — yarı saydam katman + ince ışıklı kenar (mercek taklidi).
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Int = 20,
    tint: Color = Color.Transparent,
    content: @Composable ColumnScope.() -> Unit
) {
    val dark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(cornerRadius.dp)
    val base = if (dark) Color.White.copy(alpha = 0.06f) else Color.White.copy(alpha = 0.55f)
    val edge = Brush.linearGradient(
        colors = listOf(
            Color.White.copy(alpha = if (dark) 0.30f else 0.70f),
            Color.White.copy(alpha = 0.04f),
            Color.White.copy(alpha = if (dark) 0.12f else 0.25f)
        ),
        start = Offset(0f, 0f), end = Offset(220f, 220f)
    )
    Column(
        modifier
            .clip(shape)
            .background(base, shape)
            .then(if (tint != Color.Transparent) Modifier.background(tint.copy(alpha = 0.10f), shape) else Modifier)
            .border(0.8.dp, edge, shape)
            .padding(16.dp),
        content = content
    )
}

// iOS PlatformBadge karşılığı — marka renginde camsı rozet + baş harf.
@Composable
fun PlatformBadge(platform: Platform, size: Int = 36) {
    val shape = RoundedCornerShape((size * 0.28f).dp)
    Box(
        Modifier
            .size(size.dp)
            .clip(shape)
            .background(platform.brandColor.copy(alpha = 0.18f), shape)
            .border(0.8.dp, platform.brandColor.copy(alpha = 0.45f), shape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            platform.displayName.take(1),
            color = platform.brandColor,
            fontSize = (size * 0.42f).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

// iOS AnimatedMeshBackground karşılığı — yavaşça hareket eden çok renkli gradyan.
@Composable
fun AnimatedMeshBackground(modifier: Modifier = Modifier) {
    val dark = isSystemInDarkTheme()
    val transition = rememberInfiniteTransition(label = "mesh")
    val t by transition.animateFloat(
        initialValue = 0f, targetValue = (2 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(18000, easing = LinearEasing), RepeatMode.Restart),
        label = "t"
    )
    val colors = if (dark)
        listOf(Color(0xFF2D1170), Color(0xFF5B21B6), Color(0xFF1D4ED8), Color(0xFF1E1B6E))
    else
        listOf(Color(0xFFC4B5FD), Color(0xFFDDD6FE), Color(0xFFC7D2FE), Color(0xFFBFDBFE))

    Canvas(modifier.fillMaxSize()) {
        val w = size.width; val h = size.height
        drawRect(if (dark) Color(0xFF08040F) else Color(0xFFF1EEF9))
        val pts = listOf(
            Offset(w * (0.2f + 0.1f * sin(t)), h * (0.15f + 0.08f * cos(t))),
            Offset(w * (0.8f + 0.08f * cos(t * 0.9f)), h * (0.25f + 0.10f * sin(t * 1.1f))),
            Offset(w * (0.25f + 0.10f * cos(t * 1.2f)), h * (0.8f + 0.06f * sin(t))),
            Offset(w * (0.75f + 0.09f * sin(t * 0.8f)), h * (0.85f + 0.07f * cos(t)))
        )
        pts.forEachIndexed { i, p ->
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(colors[i].copy(alpha = if (dark) 0.55f else 0.45f), Color.Transparent),
                    center = p, radius = w * 0.7f
                ),
                radius = w * 0.7f, center = p
            )
        }
    }
}
