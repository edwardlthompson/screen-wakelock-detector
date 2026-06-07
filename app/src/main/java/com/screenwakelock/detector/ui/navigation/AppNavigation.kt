package com.screenwakelock.detector.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.screenwakelock.detector.ui.screens.DetailScreen
import com.screenwakelock.detector.ui.screens.HistoryScreen
import com.screenwakelock.detector.ui.screens.HomeScreen
import com.screenwakelock.detector.ui.screens.InsightsScreen
import com.screenwakelock.detector.ui.screens.OnboardingScreen
import com.screenwakelock.detector.ui.screens.PermissionsScreen
import com.screenwakelock.detector.ui.screens.RootScreen
import com.screenwakelock.detector.ui.screens.SettingsScreen
import com.screenwakelock.detector.ui.viewmodel.OnboardingViewModel

object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val HISTORY = "history"
    const val INSIGHTS = "insights"
    const val SETTINGS = "settings"
    const val DETAIL = "detail/{wakeEventId}"
    const val PERMISSIONS = "permissions?highlight={highlight}"
    const val ROOT = "root"

    fun detail(id: Long) = "detail/$id"
    fun permissions(highlight: String? = null) =
        if (highlight != null) "permissions?highlight=$highlight" else "permissions?highlight="
}

@Composable
fun AppNavigation(
    deepLinkWakeId: Long? = null,
    deepLinkHighlight: String? = null,
    deepLinkRoute: String? = null,
    deepLinkQuickFixWakeId: Long? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val navController = rememberNavController()
    val onboardingViewModel: OnboardingViewModel = hiltViewModel()
    val hasCompletedIntro by onboardingViewModel.hasCompletedIntro.collectAsState(initial = false)

    val bottomItems = listOf(
        Triple(Routes.HOME, "Home", Icons.Default.Home),
        Triple(Routes.HISTORY, "History", Icons.Default.History),
        Triple(Routes.INSIGHTS, "Insights", Icons.Default.Insights),
        Triple(Routes.SETTINGS, "Settings", Icons.Default.Settings),
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showBottomBar = currentRoute in bottomItems.map { it.first }

    androidx.compose.runtime.LaunchedEffect(
        hasCompletedIntro,
        deepLinkWakeId,
        deepLinkHighlight,
        deepLinkRoute,
        deepLinkQuickFixWakeId,
    ) {
        if (!hasCompletedIntro) {
            navController.navigate(Routes.ONBOARDING) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        } else {
            when (deepLinkRoute) {
                "root" -> navController.navigate(Routes.ROOT)
                "permissions" -> navController.navigate(Routes.permissions(deepLinkHighlight))
                "insights" -> navController.navigate(Routes.INSIGHTS)
                else -> {
                    deepLinkQuickFixWakeId?.let {
                        navController.navigate(Routes.HOME) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                    deepLinkWakeId?.let { id ->
                        navController.navigate(Routes.detail(id))
                        onDeepLinkConsumed()
                    }
                    deepLinkHighlight?.let {
                        navController.navigate(Routes.permissions(it))
                        onDeepLinkConsumed()
                    }
                }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomItems.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = currentRoute == route,
                            onClick = {
                                navController.navigate(route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(icon, contentDescription = label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (hasCompletedIntro) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.padding(padding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.HOME) {
                HomeScreen(
                    onNavigateHistory = { navController.navigate(Routes.HISTORY) },
                    onNavigateDetail = { navController.navigate(Routes.detail(it)) },
                    deepLinkQuickFixWakeId = deepLinkQuickFixWakeId,
                    onDeepLinkConsumed = onDeepLinkConsumed,
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onNavigateDetail = { navController.navigate(Routes.detail(it)) })
            }
            composable(Routes.INSIGHTS) {
                InsightsScreen()
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onNavigatePermissions = { navController.navigate(Routes.permissions()) },
                    onNavigateRoot = { navController.navigate(Routes.ROOT) },
                    onReplayOnboarding = { navController.navigate(Routes.ONBOARDING) },
                )
            }
            composable(
                route = Routes.DETAIL,
                arguments = listOf(navArgument("wakeEventId") { type = NavType.LongType }),
            ) { entry ->
                val id = entry.arguments?.getLong("wakeEventId") ?: return@composable
                DetailScreen(
                    wakeEventId = id,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                route = Routes.PERMISSIONS,
                arguments = listOf(
                    navArgument("highlight") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = ""
                    },
                ),
            ) { entry ->
                val highlight = entry.arguments?.getString("highlight")?.takeIf { it.isNotEmpty() }
                PermissionsScreen(
                    highlight = highlight,
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Routes.ROOT) {
                RootScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
