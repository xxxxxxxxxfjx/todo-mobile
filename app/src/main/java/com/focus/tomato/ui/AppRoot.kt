package com.focus.tomato.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.focus.tomato.data.DarkMode
import com.focus.tomato.ui.components.AppBackground
import com.focus.tomato.ui.screens.SettingsScreen
import com.focus.tomato.ui.screens.StatsScreen
import com.focus.tomato.ui.screens.TasksScreen
import com.focus.tomato.ui.screens.TimerScreen
import com.focus.tomato.ui.theme.TomatoTheme

private data class Destination(val route: String, val label: String, val icon: ImageVector)

private val destinations = listOf(
    Destination("timer", "专注", Icons.Rounded.Timer),
    Destination("tasks", "任务", Icons.Rounded.CheckCircle),
    Destination("stats", "统计", Icons.Rounded.Insights),
    Destination("settings", "设置", Icons.Rounded.Tune)
)

@Composable
fun AppRoot(viewModel: AppViewModel) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val darkTheme = when (settings.darkMode) {
        DarkMode.SYSTEM -> isSystemInDarkTheme()
        DarkMode.LIGHT -> false
        DarkMode.DARK -> true
    }
    TomatoTheme(darkTheme = darkTheme) {
        val navController = rememberNavController()
        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = backStackEntry?.destination?.route ?: "timer"
        AppBackground {
            Scaffold(
                containerColor = Color.Transparent,
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.97f),
                        tonalElevation = 0.dp
                    ) {
                        destinations.forEach { destination ->
                            NavigationBarItem(
                                selected = currentRoute == destination.route,
                                onClick = {
                                    if (currentRoute != destination.route) {
                                        navController.navigate(destination.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        destination.icon,
                                        contentDescription = destination.label,
                                        modifier = Modifier
                                    )
                                },
                                label = { Text(destination.label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.primary,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                NavHost(
                    navController = navController,
                    startDestination = "timer",
                    modifier = Modifier.padding(padding)
                ) {
                    composable("timer") {
                        TimerScreen(
                            viewModel = viewModel,
                            onOpenSettings = { navController.navigate("settings") },
                            onOpenTasks = { navController.navigate("tasks") }
                        )
                    }
                    composable("tasks") {
                        TasksScreen(
                            viewModel = viewModel,
                            onStartFocus = { navController.navigate("timer") }
                        )
                    }
                    composable("stats") {
                        StatsScreen(viewModel)
                    }
                    composable("settings") {
                        SettingsScreen(viewModel)
                    }
                }
            }
        }
    }
}
