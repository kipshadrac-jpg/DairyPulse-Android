package com.dairypulse.app

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dairypulse.app.ui.feed.FeedScreen
import com.dairypulse.app.ui.feed.FormulationScreen
import com.dairypulse.app.ui.herd.CowScreen
import com.dairypulse.app.ui.herd.HerdScreen
import com.dairypulse.app.ui.money.MoneyScreen
import com.dairypulse.app.ui.schedule.ScheduleScreen
import com.dairypulse.app.ui.settings.SettingsScreen
import com.dairypulse.app.ui.theme.DairyPulseTheme
import com.dairypulse.app.ui.theme.FarmGreen
import com.dairypulse.app.ui.today.LogMilkingScreen
import com.dairypulse.app.ui.today.TodayScreen

private enum class Tab(val route: String, val label: String, val title: String, val icon: ImageVector) {
    TODAY("today", "Today", "DairyPulse — Today", Icons.Default.Home),
    FEED("feed", "Feed", "Feed Rations & Costs", Icons.Default.Grass),
    HERD("herd", "Herd", "Herd Management", Icons.Default.Pets),
    SCHEDULE("schedule", "Schedule", "Schedule", Icons.Default.CalendarMonth),
    MONEY("money", "Money", "Financial P&L Statement", Icons.Default.Payments),
}

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { DairyPulseTheme { AppRoot() } }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }
    val title = tab?.title ?: when {
        route == "log_milking" -> "Log Milking"
        route == "settings" -> "Settings & Backup"
        route?.startsWith("formulation") == true -> if (entry?.arguments?.getLong("id") == 0L) "New Ration" else "Edit Ration"
        route?.startsWith("cow") == true -> if (entry?.arguments?.getLong("id") == 0L) "Add Animal" else "Animal Profile"
        else -> "DairyPulse"
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    if (tab == null) IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (tab != null) IconButton(onClick = { nav.navigate("settings") }) { Icon(Icons.Default.Settings, "Settings") }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = FarmGreen, titleContentColor = Color.White,
                    navigationIconContentColor = Color.White, actionIconContentColor = Color.White,
                ),
            )
        },
        bottomBar = { if (tab != null) BottomBar(nav, tab) },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(nav, startDestination = Tab.TODAY.route) {
                composable(Tab.TODAY.route) { TodayScreen(onLogMilking = { nav.navigate("log_milking") }) }
                composable(Tab.FEED.route) { FeedScreen(onOpenFormulation = { nav.navigate("formulation/$it") }) }
                composable(Tab.HERD.route) { HerdScreen(onOpenCow = { nav.navigate("cow/$it") }) }
                composable(Tab.SCHEDULE.route) { ScheduleScreen(onOpenCow = { nav.navigate("cow/$it") }) }
                composable(Tab.MONEY.route) { MoneyScreen() }
                composable("log_milking") { LogMilkingScreen(onDone = { nav.popBackStack() }) }
                composable("settings") { SettingsScreen(onDone = { nav.popBackStack() }) }
                composable("formulation/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    FormulationScreen(it.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() })
                }
                composable("cow/{id}", listOf(navArgument("id") { type = NavType.LongType })) {
                    CowScreen(it.arguments?.getLong("id") ?: 0L, onDone = { nav.popBackStack() })
                }
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, current: Tab) {
    NavigationBar {
        Tab.entries.forEach { t ->
            NavigationBarItem(
                selected = t == current,
                onClick = {
                    nav.navigate(t.route) {
                        popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(t.icon, null) },
                label = { Text(t.label) },
            )
        }
    }
}
