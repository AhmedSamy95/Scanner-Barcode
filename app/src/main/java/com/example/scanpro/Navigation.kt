package com.example.scanpro

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.scanpro.ui.detail.DetailScreen
import com.example.scanpro.ui.detail.DetailViewModel
import com.example.scanpro.ui.generator.GeneratorScreen
import com.example.scanpro.ui.generator.GeneratorViewModel
import com.example.scanpro.ui.history.HistoryScreen
import com.example.scanpro.ui.history.HistoryViewModel
import com.example.scanpro.ui.scanner.ScannerScreen
import com.example.scanpro.ui.scanner.ScannerViewModel
import com.example.scanpro.ui.settings.SettingsScreen
import com.example.scanpro.ui.settings.SettingsViewModel

sealed class Screen(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Scanner : Screen("scanner", "Scanner", Icons.Default.CameraAlt)
    object Generator : Screen("generator", "Generator", Icons.Default.QrCode)
    object History : Screen("history", "History", Icons.Default.History)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}

/**
 * Main application navigation component.
 * Integrates bottom navigation bar and coordinate destinations.
 */
@Composable
fun MainNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var generatorInitialValue by remember { mutableStateOf<String?>(null) }

    val bottomNavItems = listOf(
        Screen.Scanner,
        Screen.Generator,
        Screen.History,
        Screen.Settings
    )

    Scaffold(
        bottomBar = {
            // Show bottom navigation bar only on primary screens
            val showBottomBar = bottomNavItems.any { currentRoute == it.route }
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface,
                    tonalElevation = 8.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.title) },
                            label = { Text(screen.title) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        // Pop up to the start destination of the graph to
                                        // avoid building up a large stack of destinations
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        // Avoid multiple copies of the same destination when
                                        // reselecting the same item
                                        launchSingleTop = true
                                        // Restore state when reselecting a previously selected item
                                        restoreState = true
                                    }
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Scanner.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            composable(Screen.Scanner.route) {
                val viewModel: ScannerViewModel = hiltViewModel()
                ScannerScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { barcodeId ->
                        navController.navigate("detail/$barcodeId")
                    },
                    onNavigateToGenerator = { rawValue ->
                        generatorInitialValue = rawValue
                        navController.navigate(Screen.Generator.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = false
                            }
                            launchSingleTop = true
                            restoreState = false
                        }
                    }
                )
            }

            composable(Screen.Generator.route) {
                val viewModel: GeneratorViewModel = hiltViewModel()
                GeneratorScreen(
                    viewModel = viewModel,
                    initialValue = generatorInitialValue,
                    onInitialValueConsumed = { generatorInitialValue = null }
                )
            }

            composable(Screen.History.route) {
                val viewModel: HistoryViewModel = hiltViewModel()
                HistoryScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = { barcodeId ->
                        navController.navigate("detail/$barcodeId")
                    }
                )
            }

            composable(Screen.Settings.route) {
                val viewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen(viewModel = viewModel)
            }

            composable(
                route = "detail/{barcodeId}",
                arguments = listOf(
                    navArgument("barcodeId") { type = NavType.LongType }
                )
            ) {
                val viewModel: DetailViewModel = hiltViewModel()
                DetailScreen(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
