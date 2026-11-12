package com.focus.tomato.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.tomato.data.TaskEntity
import com.focus.tomato.ui.AppViewModel
import com.focus.tomato.ui.components.EmptyState
import com.focus.tomato.ui.components.todayLabel
import com.focus.tomato.ui.theme.CardGradients
import com.focus.tomato.ui.theme.CardGradientsDark

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(viewModel: AppViewModel, onStartFocus: () -> Unit) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val active = tasks.filter { it.completedAt == null }
    val completed = tasks.filter { it.completedAt != null }
    var editing by remember { mutableStateOf<TaskEntity?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<TaskEntity?>(null) }

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = todayLabel(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "任务",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            item {
                SummaryRow(active, completed)
                Spacer(Modifier.height(4.dp))
            }
            item {
                Text(
                    text = "进行中 · ${active.size}",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            if (active.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🌱",
                        title = "今天想完成什么？",
                        subtitle = "添加一个任务，给它估几个番茄"
                    )
                }
            } else {
                items(active, key = { it.id }) { task ->
                    val index = active.indexOf(task)
                    TaskCard(
                        task = task,
                        index = index,
                        onToggle = { viewModel.toggleTask(task) },
                        onStart = {
                            viewModel.startFocus(task.id)
                            onStartFocus()
                        },
                        onEdit = {
                            editing = task
                            showEditor = true
                        },
                        onDelete = { pendingDelete = task }
                    )
                }
            }
            if (completed.isNotEmpty()) {
                item {
                    Text(
                        text = "已完成 · ${completed.size}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
                items(completed, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        index = 0,
                        onToggle = { viewModel.toggleTask(task) },
                        onStart = {},
                        onEdit = {
                            editing = task
                            showEditor = true
                        },
                        onDelete = { pendingDelete = task }
                    )
                }
            }
            item { Spacer(Modifier.height(96.dp)) }
        }

        FloatingActionButton(
            onClick = {
                editing = null
                showEditor = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "添加任务")
        }
    }

    if (showEditor) {
        TaskEditorSheet(
            initial = editing,
            workMinutes = settings.workMinutes,
            onDismiss = { showEditor = false },
            onSave = { title, note, estimate, priority ->
                val current = editing
                if (current == null) {
                    viewModel.addTask(title, note, estimate, priority)
                } else {
                    viewModel.updateTask(
                        current.copy(
                            title = title.trim(),
                            note = note.trim(),
                            estimatedPomodoros = estimate,
                            priority = priority
                        )
                    )
                }
                showEditor = false
            }
        )
    }

    pendingDelete?.let { task ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除任务") },
            text = { Text("确定删除「${task.title}」吗？专注记录会保留。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTask(task)
                    pendingDelete = null
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun SummaryRow(active: List<TaskEntity>, completed: List<TaskEntity>) {
    val estimated = active.sumOf { it.estimatedPomodoros }
    val donePomodoros = active.sumOf { it.completedPomodoros }
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            SummaryCell("进行中", "${active.size}", "个任务")
            SummaryCell("预计", "$estimated", "个番茄")
            SummaryCell("已专注", "$donePomodoros", "个番茄")
            SummaryCell("已完成", "${completed.size}", "个任务")
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskEntity,
    index: Int,
    onToggle: () -> Unit,
    onStart: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var menu by remember { mutableStateOf(false) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val gradient = if (dark) CardGradientsDark[index % CardGradientsDark.size] else CardGradients[index % CardGradients.size]
    val done = task.completedAt != null
    val priorityColor = when (task.priority) {
        3 -> MaterialTheme.colorScheme.primary
        2 -> MaterialTheme.colorScheme.tertiary
        1 -> MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val priorityLabel = when (task.priority) {
        3 -> "高"
        2 -> "中"
        1 -> "低"
        else -> ""
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .background(Brush.linearGradient(gradient), RoundedCornerShape(24.dp))
                .padding(horizontal = 14.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (done) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.65f))
                    .clickable { onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (done) {
                    Icon(Icons.Rounded.Check, contentDescription = "完成", tint = Color.White, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (done) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (priorityLabel.isNotEmpty()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(shape = RoundedCornerShape(50), color = priorityColor.copy(alpha = 0.14f)) {
                            Text(
                                text = priorityLabel,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = priorityColor
                            )
                        }
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(
                    text = "预计 ${task.estimatedPomodoros} 个 · 已完成 ${task.completedPomodoros}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (task.note.isNotBlank()) {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = task.note,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            if (!done) {
                Surface(
                    onClick = onStart,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = "开始专注", tint = Color.White, modifier = Modifier.size(22.dp))
                    }
                }
            }
            Box {
                IconButton(onClick = { menu = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "更多", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("编辑") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskEditorSheet(
    initial: TaskEntity?,
    workMinutes: Int,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, Int) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember(initial?.id) { mutableStateOf(initial?.title ?: "") }
    var note by remember(initial?.id) { mutableStateOf(initial?.note ?: "") }
    var estimate by remember(initial?.id) { mutableIntStateOf(initial?.estimatedPomodoros ?: 1) }
    var priority by remember(initial?.id) { mutableIntStateOf(initial?.priority ?: 0) }

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
            Text(
                text = if (initial == null) "新建任务" else "编辑任务",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(14.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("任务名称") },
                placeholder = { Text("例如：写完产品需求文档") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = note,
                onValueChange = { note = it },
                label = { Text("备注（可选）") },
                placeholder = { Text("补充一点上下文") },
                minLines = 2,
                maxLines = 3,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(16.dp))
            Text("预计番茄", style = MaterialTheme.typography.titleMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { if (estimate > 1) estimate-- }) {
                    Icon(Icons.Rounded.Remove, contentDescription = "减少")
                }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                ) {
                    Text(
                        text = "$estimate",
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { if (estimate < 20) estimate++ }) {
                    Icon(Icons.Rounded.Add, contentDescription = "增加")
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "约 ${estimate * workMinutes} 分钟",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            Text("优先级", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "无", 1 to "低", 2 to "中", 3 to "高").forEach { (value, label) ->
                    FilterChip(
                        selected = priority == value,
                        onClick = { priority = value },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                            selectedLabelColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            Surface(
                onClick = {
                    if (title.isNotBlank()) onSave(title, note, estimate, priority)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(50),
                color = if (title.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "保存任务",
                        style = MaterialTheme.typography.titleMedium,
                        color = if (title.isNotBlank()) Color.White else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
