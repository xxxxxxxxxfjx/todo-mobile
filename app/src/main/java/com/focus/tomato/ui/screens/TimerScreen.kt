package com.focus.tomato.ui.screens

import android.os.Build
import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.tomato.data.TimerMode
import com.focus.tomato.data.TimerStatus
import com.focus.tomato.data.TimerStore
import com.focus.tomato.ui.AppViewModel
import com.focus.tomato.ui.components.TimerRing
import com.focus.tomato.ui.components.formatClock
import com.focus.tomato.ui.components.formatFocusDuration
import com.focus.tomato.ui.components.todayLabel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

private val quotes = listOf(
    "把注意力交给此刻，时间会给你答案。",
    "不必追赶，专注本身就是速度。",
    "先完成，再完美。",
    "一小段专注，也是一整天里最亮的部分。",
    "你只需要开始，剩下的交给时间。",
    "慢一点，但不要停。",
    "今天的一小步，是明天的底气。"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    viewModel: AppViewModel,
    onOpenSettings: () -> Unit,
    onOpenTasks: () -> Unit
) {
    val state by TimerStore.state.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val selectedTask by viewModel.selectedTask.collectAsStateWithLifecycle()
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current
    var showTaskPicker by remember { mutableStateOf(false) }
    var showNoise by remember { mutableStateOf(false) }
    var tick by remember { mutableStateOf(0L) }

    LaunchedEffect(state.status) {
        while (isActive) {
            tick = SystemClock.elapsedRealtime()
            delay(500)
        }
    }

    val remaining = remember(state, tick) {
        if (state.status == TimerStatus.RUNNING) TimerStore.computeRemaining(state) else state.remainingMs
    }
    val plannedMs = (state.plannedSeconds * 1000L).coerceAtLeast(1L)
    val progress = (remaining.toFloat() / plannedMs.toFloat()).coerceIn(0f, 1f)
    val accent = when (state.mode) {
        TimerMode.FOCUS -> MaterialTheme.colorScheme.primary
        TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
    }
    val notificationsEnabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
    val quote = remember { quotes[(java.time.LocalDate.now().dayOfYear) % quotes.size] }
    val taskTitle = selectedTask?.title ?: state.taskId?.let { id -> tasks.firstOrNull { it.id == id }?.title }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = todayLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "专注当下",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { showNoise = true }) {
                    Icon(
                        Icons.Rounded.GraphicEq,
                        contentDescription = "白噪音",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "设置",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = quote,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        if (Build.VERSION.SDK_INT >= 33 && !notificationsEnabled) {
            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val intent = android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                        runCatching { context.startActivity(intent) }
                    }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "🔔", fontSize = 16.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "开启通知，结束提醒才不会错过",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        ModeTabs(
            selected = state.mode,
            enabled = state.status == TimerStatus.IDLE,
            onSelect = viewModel::switchMode
        )
        Spacer(Modifier.height(4.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimerRing(
                progress = progress,
                mode = state.mode,
                status = state.status,
                timeText = formatClock(remaining),
                taskTitle = taskTitle
            )
        }

        Spacer(Modifier.height(2.dp))
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showTaskPicker = true },
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(accent.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = "🍅", fontSize = 18.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "当前任务",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = taskTitle ?: "选择任务，让专注有落点",
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (taskTitle != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            CircleAction(
                icon = Icons.Rounded.Refresh,
                label = "重置",
                enabled = state.status != TimerStatus.IDLE,
                onClick = { viewModel.reset() }
            )
            Spacer(Modifier.width(20.dp))
            Box(
                modifier = Modifier
                    .height(64.dp)
                    .width(196.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(accent, accent.copy(alpha = 0.82f))))
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        when (state.status) {
                            TimerStatus.IDLE -> viewModel.startCurrentMode()
                            TimerStatus.RUNNING -> viewModel.pause()
                            TimerStatus.PAUSED -> viewModel.resume()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (state.status) {
                            TimerStatus.RUNNING -> Icons.Rounded.Pause
                            else -> Icons.Rounded.PlayArrow
                        },
                        contentDescription = null,
                        tint = Color.White
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (state.status) {
                            TimerStatus.IDLE -> if (state.mode == TimerMode.FOCUS) "开始专注" else "开始休息"
                            TimerStatus.RUNNING -> "暂停"
                            TimerStatus.PAUSED -> "继续"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.width(20.dp))
            CircleAction(
                icon = Icons.Rounded.SkipNext,
                label = "跳过",
                enabled = state.status != TimerStatus.IDLE,
                onClick = { viewModel.skip() }
            )
        }
        if (state.status != TimerStatus.IDLE) {
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text(
                    text = "+5 分钟",
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable { viewModel.extend() }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = accent
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        TodaySummaryCard(
            todaySeconds = stats.todaySeconds,
            todayCount = stats.todayCount,
            streak = stats.streak,
            goal = 8,
            accent = accent
        )
        Spacer(Modifier.height(20.dp))
    }

    if (showTaskPicker) {
        TaskPickerSheet(
            viewModel = viewModel,
            onDismiss = { showTaskPicker = false },
            onOpenTasks = {
                showTaskPicker = false
                onOpenTasks()
            }
        )
    }

    if (showNoise) {
        NoiseSheet(onDismiss = { showNoise = false })
    }
}

@Composable
private fun ModeTabs(
    selected: TimerMode,
    enabled: Boolean,
    onSelect: (TimerMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(4.dp)
    ) {
        TimerMode.entries.forEach { mode ->
            val isSelected = mode == selected
            val color = when (mode) {
                TimerMode.FOCUS -> MaterialTheme.colorScheme.primary
                TimerMode.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
                TimerMode.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent)
                    .clickable(enabled = enabled && !isSelected) { onSelect(mode) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = mode.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                )
            }
        }
    }
}

@Composable
private fun CircleAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            enabled = enabled,
            shape = RoundedCornerShape(50),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.size(48.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    icon,
                    contentDescription = label,
                    tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.4f)
        )
    }
}

@Composable
private fun TodaySummaryCard(
    todaySeconds: Long,
    todayCount: Int,
    streak: Int,
    goal: Int,
    accent: Color
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "今日进度",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = if (streak > 0) "🔥 连续 $streak 天" else "从今天开始",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = formatFocusDuration(todaySeconds),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "专注 · $todayCount/$goal 个番茄",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(50))
            ) {
                Box(
                    Modifier
                        .fillMaxWidth((todayCount.toFloat() / goal.toFloat()).coerceIn(0f, 1f))
                        .height(8.dp)
                        .background(accent, RoundedCornerShape(50))
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskPickerSheet(
    viewModel: AppViewModel,
    onDismiss: () -> Unit,
    onOpenTasks: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedTaskId.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val active = tasks.filter { it.completedAt == null }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
        ) {
            Text("选择当前任务", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                "完成一个番茄后，会自动累计到任务上",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable {
                        viewModel.selectTask(null)
                        onDismiss()
                    }
                    .padding(vertical = 12.dp, horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🚫", fontSize = 18.sp)
                Spacer(Modifier.width(10.dp))
                Text("不关联任务", style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.weight(1f))
                if (selectedId == null) {
                    Text("已选择", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            if (active.isEmpty()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 26.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("还没有任务", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "先创建一个任务，再回来开始专注",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        onClick = onOpenTasks,
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("去创建任务", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            } else {
                active.forEach { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                viewModel.selectTask(task.id)
                                onDismiss()
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(50))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                "预计 ${task.estimatedPomodoros} 个番茄 · 已完成 ${task.completedPomodoros}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (selectedId == task.id) {
                            Text("已选择", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
