package com.dairypulse.app

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.EditCalendar
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.Pets
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.feed.FeedScreen
import com.dairypulse.app.ui.feed.FormulationScreen
import com.dairypulse.app.ui.herd.CowScreen
import com.dairypulse.app.ui.herd.HerdScreen
import com.dairypulse.app.ui.money.MoneyScreen
import com.dairypulse.app.ui.schedule.ScheduleScreen
import com.dairypulse.app.ui.settings.SettingsScreen
import com.dairypulse.app.ui.theme.DairyPulseTheme
import com.dairypulse.app.ui.theme.Dp
import com.dairypulse.app.ui.theme.Manrope
import com.dairypulse.app.ui.today.LogMilkingScreen
import com.dairypulse.app.ui.today.TodayScreen

private enum class Tab(val route: String, val label: String, val title: String, val icon: ImageVector) {
    TODAY("today", "Today", "Today", Icons.Outlined.WbSunny),
    FEED("feed", "Feed", "Feed", Icons.Outlined.Grass),
    HERD("herd", "Herd", "Herd", Icons.Outlined.Pets),
    SCHEDULE("schedule", "Schedule", "Schedule", Icons.Outlined.EditCalendar),
    MONEY("money", "Money", "Money", Icons.Outlined.AccountBalanceWallet),
}

class MainActivity : ComponentActivity() {
    private val askNotifications = registerForActivityResult(ActivityResultContracts.RequestPermission()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Dp.Cream.toArgb(), Dp.Cream.toArgb()),
            navigationBarStyle = SystemBarStyle.light(Dp.NavBg.toArgb(), Dp.NavBg.toArgb()),
        )
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent { DairyPulseTheme { AppRoot() } }
    }
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val tab = Tab.entries.firstOrNull { it.route == route }
    val newItem = entry?.arguments?.getLong("id") == 0L
    val (eyebrow, title) = when {
        route == "log_milking" -> "Today" to "Log milking"
        route == "settings" -> "DairyDesk" to "Settings & backup"
        route?.startsWith("formulation") == true -> "Feed" to if (newItem) "New ration" else "Edit ration"
        route?.startsWith("cow") == true -> "Herd" to if (newItem) "Add animal" else "Animal profile"
        else -> "" to "DairyDesk"
    }

    // The Today page runs its farm header to the top edge, so the status bar (clock, signal,
    // battery) is hidden there; a swipe down still shows it for a moment. Other pages keep it.
    val onToday = tab == Tab.TODAY
    val view = LocalView.current
    LaunchedEffect(onToday) {
        val window = view.context.findActivity()?.window ?: return@LaunchedEffect
        WindowCompat.getInsetsController(window, view).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            if (onToday) hide(WindowInsetsCompat.Type.statusBars()) else show(WindowInsetsCompat.Type.statusBars())
        }
    }

    Scaffold(
        containerColor = Dp.Cream,
        // Top insets are handled by each top bar (or the Today header); the content keeps clear
        // of the navigation bar and the keyboard.
        contentWindowInsets = WindowInsets.navigationBars.union(WindowInsets.ime),
        topBar = {
            when {
                onToday -> Unit
                tab != null -> TabTopBar(onSettings = { nav.navigate("settings") })
                else -> DetailTopBar(eyebrow, title, onBack = { nav.popBackStack() })
            }
        },
        bottomBar = { if (tab != null) BottomBar(nav, tab) },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(
                nav, startDestination = Tab.TODAY.route,
                enterTransition = { fadeIn(tween(160)) }, exitTransition = { fadeOut(tween(120)) },
            ) {
                composable(Tab.TODAY.route) { TodayScreen(onLogMilking = { nav.navigate("log_milking") }, onSettings = { nav.navigate("settings") }) }
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
private fun CircleIconButton(icon: ImageVector, description: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick, shape = CircleShape, color = Dp.Card, border = BorderStroke(1.dp, Dp.Border),
        modifier = Modifier.size(44.dp).semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = Dp.GreenDark, modifier = Modifier.size(20.dp)) }
    }
}

@Composable
private fun TopBarFrame(content: @Composable RowScope.() -> Unit) {
    Column(Modifier.background(Dp.Cream).statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically, content = content,
        )
        HorizontalDivider(color = Color(0xFFEAE2D2))
    }
}

@Composable
private fun TabTopBar(onSettings: () -> Unit) = TopBarFrame {
    Image(painterResource(R.drawable.dairydesk_logo), null, Modifier.size(42.dp))
    Spacer(Modifier.width(12.dp))
    Serif("DairyDesk", 22, Modifier.weight(1f))
    CircleIconButton(Icons.Outlined.Settings, "Settings", onSettings)
}

@Composable
private fun DetailTopBar(eyebrow: String, title: String, onBack: () -> Unit) = TopBarFrame {
    CircleIconButton(Icons.AutoMirrored.Outlined.ArrowBack, "Back", onBack)
    Spacer(Modifier.width(12.dp))
    Column {
        if (eyebrow.isNotEmpty()) Label(eyebrow)
        Serif(title, 22)
    }
}

/** Compact bottom navigation: small icons with a light pill on the current tab. */
@Composable
private fun BottomBar(nav: NavHostController, current: Tab) {
    Column(Modifier.background(Dp.NavBg)) {
        HorizontalDivider(color = Dp.Border)
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 4.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceAround,
        ) {
            Tab.entries.forEach { t ->
                val on = t == current
                Column(
                    Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                        .selectable(selected = on, role = Role.Tab) {
                            nav.navigate(t.route) {
                                popUpTo(nav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Box(
                        Modifier.size(width = 48.dp, height = 26.dp).clip(RoundedCornerShape(13.dp))
                            .background(if (on) Dp.GreenPill else Color.Transparent),
                        contentAlignment = Alignment.Center,
                    ) { Icon(t.icon, null, tint = if (on) Dp.GreenDark else Dp.Muted, modifier = Modifier.size(20.dp)) }
                    Text(
                        t.label,
                        style = TextStyle(fontFamily = Manrope, fontSize = 11.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.SemiBold, color = if (on) Dp.GreenDark else Dp.Muted),
                    )
                }
            }
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
