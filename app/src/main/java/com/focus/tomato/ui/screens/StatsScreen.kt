package com.focus.tomato.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.focus.tomato.ui.AppViewModel
import com.focus.tomato.ui.components.AchievementCard
import com.focus.tomato.ui.components.DistributionBars
import com.focus.tomato.ui.components.EmptyState
import com.focus.tomato.ui.components.FocusHeatmap
import com.focus.tomato.ui.components.SectionTitle
import com.focus.tomato.ui.components.StatCard
import com.focus.tomato.ui.components.WeeklyBars
import com.focus.tomato.ui.components.formatFocusDuration
import com.focus.tomato.ui.components.todayLabel

@Composable
fun StatsScreen(viewModel: AppViewModel) {
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(8.dp))
        Text(
            text = todayLabel(),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "统计",
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "累计 ${formatFocusDuration(stats.totalSeconds)} · ${stats.totalCount} 个番茄",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                label = "今日专注",
                value = formatFocusDuration(stats.todaySeconds),
                unit = "",
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "本周专注",
                value = formatFocusDuration(stats.weekSeconds),
                unit = "",
                modifier = Modifier.weight(1f),
                tint = MaterialTheme.colorScheme.secondary
            )
            StatCard(
                label = "连续天数",
                value = stats.streak.toString(),
                unit = "天",
                modifier = Modifier.weight(1f),
                tint = MaterialTheme.colorScheme.tertiary
            )
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                SectionTitle(title = "最近 7 天", subtitle = "柱高代表专注分钟数")
                Spacer(Modifier.height(16.dp))
                WeeklyBars(stats.last7)
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                SectionTitle(title = "专注热力图", subtitle = "颜色越深，专注越久")
                Spacer(Modifier.height(12.dp))
                FocusHeatmap(stats.heatmap)
            }
        }

        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 1.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(18.dp)) {
                SectionTitle(title = "任务分布", subtitle = "时间花在了哪里")
                Spacer(Modifier.height(14.dp))
                if (stats.distribution.isEmpty()) {
                    EmptyState(
                        emoji = "🧭",
                        title = "还没有专注记录",
                        subtitle = "完成第一个番茄后，这里会显示你的时间去向"
                    )
                } else {
                    DistributionBars(stats.distribution)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            text = "成就",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
        stats.achievements.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { achievement ->
                    AchievementCard(achievement, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(24.dp))
    }
}
