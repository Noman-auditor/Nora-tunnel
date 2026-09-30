package com.norabuilder.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import com.norabuilder.ui.screens.BuildConsoleScreen
import com.norabuilder.ui.screens.DashboardScreen
import com.norabuilder.ui.screens.FileExplorerScreen
import com.norabuilder.ui.screens.ProjectImportScreen
import com.norabuilder.ui.screens.SettingsScreen

@Composable
fun NoraNavGraph(navController: NavHostController) {
    val items = listOf(
        "Home" to Icons.Default.Home,
        "Projects" to Icons.Default.Add,
        "Builds" to Icons.Default.Build,
        "Files" to Icons.Default.Info,
        "Settings" to Icons.Default.Settings
    )
    val backStack by navController.currentBackStackEntryAsState()
    val current = backStack?.destination?.route
    Scaffold(bottomBar = {
        NavigationBar {
            items.forEach { (label, icon) ->
                NavigationBarItem(
                    selected = current == label,
                    onClick = {
                        navController.navigate(label) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    label = { Text(label) },
                    icon = { Icon(icon, contentDescription = label) }
                )
            }
        }
    }) { padding ->
        NavHost(navController, startDestination = "Home", modifier = Modifier.padding(padding)) {
            composable("Home") { DashboardScreen() }
            composable("Projects") { ProjectImportScreen() }
            composable("Builds") { BuildConsoleScreen() }
            composable("Files") { FileExplorerScreen() }
            composable("Settings") { SettingsScreen() }
        }
    }
}
