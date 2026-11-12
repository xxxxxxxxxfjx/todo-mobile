package com.focus.tomato.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.focus.tomato.data.TimerMode
import com.focus.tomato.data.TimerStatus
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TimerRing(
    progress: Float,
    mode: TimerMode,
    status: TimerStatus,
    timeText: String,
    taskTitle: String?,
    modifier: Modifier = Modifier
) {
    val accent = when (mode) {
        TimerMode.FOCUS -> MaterialTheme.colorScheme.primary
        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
    }
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(500, easing = LinearOutSlowInEasing),
        label = "timerRing"
    )
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
    val tickColor = MaterialTheme.colorScheme.onSurface
    val pausedAlpha = if (status == TimerStatus.PAUSED) 0.45f else 1f

    Box(modifier = modifier.size(260.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 16.dp.toPx()
            val inset = stroke / 2 + 8.dp.toPx()
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            val radius = arcSize.width / 2
            val center = Offset(size.width / 2, size.height / 2)

            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            if (animated > 0.002f) {
                drawArc(
                    brush = Brush.linearGradient(
                        colors = listOf(accent.copy(alpha = pausedAlpha), accent.copy(alpha = 0.55f * pausedAlpha)),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, size.height)
                    ),
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                val angle = Math.toRadians((-90f + 360f * animated).toDouble())
                val dot = Offset(
                    x = center.x + radius * cos(angle).toFloat(),
                    y = center.y + radius * sin(angle).toFloat()
                )
                drawCircle(color = accent.copy(alpha = 0.18f * pausedAlpha), radius = 12.dp.toPx(), center = dot)
                drawCircle(color = accent.copy(alpha = pausedAlpha), radius = 5.dp.toPx(), center = dot)
            }

            // Fine tick marks around the inner edge.
            val tickRadius = radius - 26.dp.toPx()
            for (i in 0 until 60) {
                val angle = Math.toRadians((i * 6 - 90).toDouble())
                val long = i % 5 == 0
                val inner = tickRadius - if (long) 7.dp.toPx() else 4.dp.toPx()
                drawLine(
                    color = tickColor.copy(alpha = if (long) 0.14f else 0.07f),
                    start = Offset(
                        center.x + inner * cos(angle).toFloat(),
                        center.y + inner * sin(angle).toFloat()
                    ),
                    end = Offset(
                        center.x + tickRadius * cos(angle).toFloat(),
                        center.y + tickRadius * sin(angle).toFloat()
                    ),
                    strokeWidth = if (long) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 34.dp)) {
            Surface(
                shape = RoundedCornerShape(50),
                color = accent.copy(alpha = 0.12f)
            ) {
                Text(
                    text = when (status) {
                        TimerStatus.IDLE -> "准备开始"
                        TimerStatus.RUNNING -> when (mode) {
                            TimerMode.FOCUS -> "专注中"
                            else -> "休息中"
                        }
                        TimerStatus.PAUSED -> "已暂停"
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = accent
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = timeText,
                fontSize = 50.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = pausedAlpha),
                letterSpacing = (-1.5).sp,
                style = MaterialTheme.typography.displayLarge.copy(fontFeatureSettings = "tnum")
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = taskTitle ?: when (mode) {
                    TimerMode.FOCUS -> "把注意力留给此刻"
                    TimerMode.SHORT_BREAK -> "站起来，伸个懒腰"
                    TimerMode.LONG_BREAK -> "走远一点，彻底放松"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
    }
}
