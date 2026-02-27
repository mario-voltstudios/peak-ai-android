package com.peakai.fitness

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.peakai.fitness.notifications.CheckInReceiver
import com.peakai.fitness.security.StrongBoxKeyManager
import com.peakai.fitness.ui.screens.CoachScreen
import com.peakai.fitness.ui.screens.DashboardScreen
import com.peakai.fitness.ui.screens.LogScreen
import com.peakai.fitness.ui.theme.AmberPrimary
import com.peakai.fitness.ui.theme.BackgroundDark
import com.peakai.fitness.ui.theme.OnSurfaceVariant
import com.peakai.fitness.ui.theme.PeakAITheme
import com.peakai.fitness.ui.theme.SurfaceDark
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var strongBoxKeyManager: StrongBoxKeyManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize StrongBox key (runs in background, non-blocking to UI)
        val isStrongBox = strongBoxKeyManager.ensureKeyExists()
        Log.i("MainActivity", "Encryption key backed by StrongBox: $isStrongBox")

        // Schedule periodic workers
        CheckInReceiver.scheduleWorkers(this)

        setContent {
            PeakAITheme {
                PeakAIApp()
            }
        }
    }
}

@Composable
private fun PeakAIApp() {
    val navController = rememberNavController()

    val navItems = listOf(
        NavItem("dashboard", "Today", Icons.Filled.Dashboard),
        NavItem("coach", "Coach", Icons.Filled.ChatBubble),
        NavItem("log", "Log", Icons.Filled.EditNote)
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = BackgroundDark,
        bottomBar = {
            NavigationBar(containerColor = SurfaceDark, tonalElevation = androidx.compose.ui.unit.Dp.Unspecified) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDest = navBackStackEntry?.destination

                navItems.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDest?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AmberPrimary,
                            selectedTextColor = AmberPrimary,
                            indicatorColor = Color(0xFF27272A),
                            unselectedIconColor = OnSurfaceVariant,
                            unselectedTextColor = OnSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = "dashboard",
            modifier = Modifier.padding(innerPadding)
        ) {
            composable("dashboard") { DashboardScreen() }
            composable("coach")     { CoachScreen() }
            composable("log")       { LogScreen() }
        }
    }
}

private data class NavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
