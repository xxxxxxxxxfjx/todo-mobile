package com.focus.tomato.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.tomato.audio.NoiseController
import com.focus.tomato.audio.NoisePreset
import com.focus.tomato.audio.NoiseService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoiseSheet(onDismiss: () -> Unit) {
    val state by NoiseController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var minutes by remember { mutableIntStateOf(0) }

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
            Text("白噪音", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                "全部在本机实时合成，不联网、不读取麦克风",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(14.dp))
            NoisePreset.entries.chunked(2).forEach { row ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    row.forEach { preset ->
                        NoisePresetCard(
                            preset = preset,
                            selected = state.preset == preset,
                            playing = state.playing && state.preset == preset,
                            modifier = Modifier.weight(1f),
                            onClick = {
                                if (state.playing && state.preset == preset) {
                                    NoiseService.stop(context)
                                } else {
                                    NoiseController.update { it.copy(preset = preset) }
                                    NoiseService.start(context, preset, minutes, state.volume)
                                }
                            }
                        )
                    }
                    if (row.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
            }
            Spacer(Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "音量",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = state.volume,
                    onValueChange = {
                        NoiseController.setVolume(it)
                        if (state.playing) NoiseService.setVolume(context, it)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                Text(
                    "${(state.volume * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(40.dp)
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "自动停止",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0 to "不停止", 15 to "15 分钟", 30 to "30 分钟", 60 to "60 分钟").forEach { (value, label) ->
                    FilterChip(
                        selected = minutes == value,
                        onClick = {
                            minutes = value
                            if (state.playing) {
                                NoiseService.start(context, state.preset, value, state.volume)
                            }
                        },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Surface(
                onClick = {
                    if (state.playing) {
                        NoiseService.stop(context)
                    } else {
                        NoiseService.start(context, state.preset, minutes, state.volume)
                    }
                },
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (state.playing) "停止播放" else "播放 ${state.preset.label}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun NoisePresetCard(
    preset: NoisePreset,
    selected: Boolean,
    playing: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .background(
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                shape = RoundedCornerShape(18.dp)
            )
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = preset.emoji, fontSize = 22.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                text = preset.label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            if (playing) {
                Spacer(Modifier.weight(1f))
                Text(text = "播放中", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = preset.description,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2
        )
    }
}
