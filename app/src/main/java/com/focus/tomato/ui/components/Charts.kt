package com.focus.tomato.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.focus.tomato.ui.Achievement
import com.focus.tomato.ui.DayPoint
import kotlin.math.max

@Composable
fun WeeklyBars(points: List<DayPoint>, modifier: Modifier = Modifier) {
    val maxSeconds = max(1L, points.maxOfOrNull { it.seconds } ?: 1L)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        points.forEach { point ->
            val fraction = (point.seconds.toFloat() / maxSeconds.toFloat()).coerceIn(0f, 1f)
            val animated by animateFloatAsState(
                targetValue = fraction,
                animationSpec = tween(600),
                label = "bar"
            )
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (point.seconds > 0) "${point.seconds / 60}" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((12 + 88 * animated).dp)
                            .background(
                                color = if (point.seconds > 0) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant,
                                shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 3.dp, bottomEnd = 3.dp)
                            )
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = weekdayLabel(point.date),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FocusHeatmap(points: List<DayPoint>, modifier: Modifier = Modifier) {
    val colors = listOf(
        MaterialTheme.colorScheme.surfaceVariant,
        MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.42f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.68f),
        MaterialTheme.colorScheme.primary
    )
    val cell = 13.dp
    val gap = 4.dp
    val width = cell * 12f + gap * 11f
    val height = cell * 7f + gap * 6f
    Column(modifier = modifier) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Spacer(Modifier.width(18.dp))
            Text(
                text = "过去 12 周",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(8.dp))
        Row {
            Column(
                modifier = Modifier.height(height),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf("一", "", "三", "", "五", "", "日").forEach {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.width(16.dp)
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Canvas(Modifier.size(width, height)) {
                val cellPx = cell.toPx()
                val gapPx = gap.toPx()
                val chunks = points.chunked(7)
                chunks.take(12).forEachIndexed { col, week ->
                    week.forEachIndexed { row, point ->
                        val level = when {
                            point.seconds <= 0 -> 0
                            point.seconds < 15 * 60 -> 1
                            point.seconds < 30 * 60 -> 2
                            point.seconds < 60 * 60 -> 3
                            else -> 4
                        }
                        val x = col * (cellPx + gapPx)
                        val y = row * (cellPx + gapPx)
                        drawRoundRect(
                            color = colors[level],
                            topLeft = Offset(x, y),
                            size = Size(cellPx, cellPx),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "少",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(6.dp))
            colors.forEach { c ->
                Box(
                    Modifier
                        .padding(horizontal = 1.dp)
                        .size(10.dp)
                        .background(c, RoundedCornerShape(3.dp))
                )
            }
            Spacer(Modifier.width(6.dp))
            Text(
                text = "多",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun DistributionBars(items: List<Pair<String, Long>>, modifier: Modifier = Modifier) {
    val total = max(1L, items.sumOf { it.second })
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f),
        MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        items.forEachIndexed { index, (label, seconds) ->
            val fraction = (seconds.toFloat() / total.toFloat()).coerceIn(0f, 1f)
            Column {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = formatFocusDuration(seconds),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(9.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(fraction)
                            .height(9.dp)
                            .background(colors[index % colors.size], RoundedCornerShape(50))
                    )
                }
            }
        }
    }
}

@Composable
fun AchievementCard(item: Achievement, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(
                color = if (item.unlocked) MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = item.emoji, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold,
            color = if (item.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = item.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}
