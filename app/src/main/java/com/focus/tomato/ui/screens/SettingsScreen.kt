package com.focus.tomato.ui.screens

import android.app.AlarmManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.tomato.BuildConfig
import com.focus.tomato.data.AppSettings
import com.focus.tomato.data.DarkMode
import com.focus.tomato.ui.AppViewModel
import com.focus.tomato.ui.components.SectionTitle
import kotlin.math.roundToInt

private enum class DurationTarget(val title: String, val range: IntRange, val step: Int) {
    WORK("工作时长", 5..120, 5),
    SHORT("短休息", 1..30, 1),
    LONG("长休息", 5..60, 5),
    INTERVAL("长休间隔", 1..10, 1)
}

@Composable
fun SettingsScreen(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumeTick by remember { mutableIntStateOf(0) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationsEnabled = remember(resumeTick) {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
    val exactAlarmEnabled = remember(resumeTick) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(AlarmManager::class.java)?.canScheduleExactAlarms() == true
        } else true
    }
    val batteryIgnored = remember(resumeTick) {
        context.getSystemService(PowerManager::class.java)
            ?.isIgnoringBatteryOptimizations(context.packageName) == true
    }

    var durationTarget by remember { mutableStateOf<DurationTarget?>(null) }
    var showClearRecords by remember { mutableStateOf(false) }
    var showClearAll by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "把节奏调成适合你的样子",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))

        SettingsGroup(title = "计时") {
            SettingValueRow(
                title = "工作时长",
                value = "${settings.workMinutes} 分钟",
                onClick = { durationTarget = DurationTarget.WORK }
            )
            Divider()
            SettingValueRow(
                title = "短休息",
                value = "${settings.shortBreakMinutes} 分钟",
                onClick = { durationTarget = DurationTarget.SHORT }
            )
            Divider()
            SettingValueRow(
                title = "长休息",
                value = "${settings.longBreakMinutes} 分钟",
                onClick = { durationTarget = DurationTarget.LONG }
            )
            Divider()
            SettingValueRow(
                title = "长休间隔",
                value = "每 ${settings.longBreakInterval} 个番茄",
                onClick = { durationTarget = DurationTarget.INTERVAL }
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup(title = "行为") {
            SettingSwitchRow(
                title = "自动开始下一段",
                subtitle = "工作结束自动进入休息，休息结束自动开始工作",
                checked = settings.autoStartNext,
                onCheckedChange = { checked -> viewModel.updateSettings { it.copy(autoStartNext = checked) } }
            )
            Divider()
            SettingSwitchRow(
                title = "结束声音",
                subtitle = "专注或休息结束时播放提示音",
                checked = settings.soundEnabled,
                onCheckedChange = { checked -> viewModel.updateSettings { it.copy(soundEnabled = checked) } }
            )
            Divider()
            SettingSwitchRow(
                title = "震动",
                subtitle = "结束时轻轻震动提醒",
                checked = settings.vibrationEnabled,
                onCheckedChange = { checked -> viewModel.updateSettings { it.copy(vibrationEnabled = checked) } }
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup(title = "外观") {
            Column(Modifier.padding(16.dp)) {
                Text("深色模式", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "跟随系统，或固定为浅色 / 深色",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DarkMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.darkMode == mode,
                            onClick = { viewModel.updateSettings { it.copy(darkMode = mode) } },
                            label = {
                                Text(
                                    when (mode) {
                                        DarkMode.SYSTEM -> "跟随系统"
                                        DarkMode.LIGHT -> "浅色"
                                        DarkMode.DARK -> "深色"
                                    }
                                )
                            }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup(title = "权限与稳定性") {
            PermissionRow(
                title = "通知权限",
                subtitle = if (notificationsEnabled) "已开启，结束提醒可以正常送达" else "未开启，结束提醒可能看不到",
                granted = notificationsEnabled,
                onClick = {
                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    runCatching { context.startActivity(intent) }
                }
            )
            Divider()
            PermissionRow(
                title = "精确闹钟",
                subtitle = if (exactAlarmEnabled) "已授权，后台到点提醒更准" else "未授权，将降级为普通提醒",
                granted = exactAlarmEnabled,
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                            .setData(Uri.parse("package:${context.packageName}"))
                        runCatching { context.startActivity(intent) }
                    }
                }
            )
            Divider()
            PermissionRow(
                title = "电池优化",
                subtitle = if (batteryIgnored) "已忽略电池优化，计时更稳定" else "建议允许后台运行，避免被系统杀掉",
                granted = batteryIgnored,
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                        .setData(Uri.parse("package:${context.packageName}"))
                    runCatching { context.startActivity(intent) }
                }
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup(title = "数据") {
            SettingValueRow(
                title = "清空专注记录",
                value = "",
                destructive = true,
                onClick = { showClearRecords = true }
            )
            Divider()
            SettingValueRow(
                title = "清空所有数据",
                value = "",
                destructive = true,
                onClick = { showClearAll = true }
            )
        }

        Spacer(Modifier.height(16.dp))
        SettingsGroup(title = "关于") {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "🍅", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("番茄专注", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "版本 ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    "本地优先 · 无广告 · 不需要联网。任务、专注记录与设置只保存在这台设备上。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (BuildConfig.DEBUG) {
                    Spacer(Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(4.dp))
                    SettingValueRow(
                        title = "开发者测试：60 秒专注",
                        value = "运行",
                        onClick = { viewModel.startDebugSession(60) }
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }

    durationTarget?.let { target ->
        DurationDialog(
            target = target,
            current = when (target) {
                DurationTarget.WORK -> settings.workMinutes
                DurationTarget.SHORT -> settings.shortBreakMinutes
                DurationTarget.LONG -> settings.longBreakMinutes
                DurationTarget.INTERVAL -> settings.longBreakInterval
            },
            onDismiss = { durationTarget = null },
            onConfirm = { value ->
                viewModel.updateSettings {
                    when (target) {
                        DurationTarget.WORK -> it.copy(workMinutes = value)
                        DurationTarget.SHORT -> it.copy(shortBreakMinutes = value)
                        DurationTarget.LONG -> it.copy(longBreakMinutes = value)
                        DurationTarget.INTERVAL -> it.copy(longBreakInterval = value)
                    }
                }
                durationTarget = null
            }
        )
    }

    if (showClearRecords) {
        ConfirmDialog(
            title = "清空专注记录",
            text = "会删除所有番茄记录与统计，任务本身保留。此操作不可撤销。",
            onConfirm = {
                viewModel.clearFocusRecords()
                showClearRecords = false
            },
            onDismiss = { showClearRecords = false }
        )
    }
    if (showClearAll) {
        ConfirmDialog(
            title = "清空所有数据",
            text = "会删除所有任务、专注记录和计时状态，恢复到刚安装的状态。此操作不可撤销。",
            onConfirm = {
                viewModel.clearAllData()
                showClearAll = false
            },
            onDismiss = { showClearAll = false }
        )
    }
}

@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column {
        SectionTitle(title = title)
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column { content() }
        }
    }
}

@Composable
private fun SettingValueRow(
    title: String,
    value: String,
    destructive: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (destructive) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (value.isNotEmpty()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(4.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionRow(
    title: String,
    subtitle: String,
    granted: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = if (granted) "已开启" else "去开启",
            style = MaterialTheme.typography.labelLarge,
            color = if (granted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(2.dp))
        Icon(
            Icons.Rounded.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant
    )
}

@Composable
private fun DurationDialog(
    target: DurationTarget,
    current: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var value by remember(target, current) { mutableIntStateOf(current) }
    val steps = ((target.range.last - target.range.first) / target.step).coerceAtLeast(1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(target.title) },
        text = {
            Column {
                Text(
                    text = if (target == DurationTarget.INTERVAL) "$value 个番茄" else "$value 分钟",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Slider(
                    value = ((value - target.range.first) / target.step.toFloat()).coerceIn(0f, steps.toFloat()),
                    onValueChange = { raw ->
                        val index = raw.roundToInt()
                        value = (target.range.first + index * target.step).coerceIn(target.range.first, target.range.last)
                    },
                    valueRange = 0f..steps.toFloat(),
                    steps = (steps - 1).coerceAtLeast(0)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(value) }) { Text("保存") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

@Composable
private fun ConfirmDialog(
    title: String,
    text: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("确定", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
