package com.dairypulse.app.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Grass
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.R
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.data.Session
import com.dairypulse.app.domain.profitAndLoss
import com.dairypulse.app.domain.sessionMatrix
import com.dairypulse.app.ui.common.Badge
import com.dairypulse.app.ui.common.DpCard
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.PrimaryButton
import com.dairypulse.app.ui.common.RowDivider
import com.dairypulse.app.ui.common.SectionHeader
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.SoftButton
import com.dairypulse.app.ui.common.StatTile
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.TileRow
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.pct
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.rememberPhoto
import com.dairypulse.app.ui.common.whole
import com.dairypulse.app.ui.theme.Dp
import com.dairypulse.app.ui.theme.Manrope
import com.dairypulse.app.ui.theme.serif
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(onLogMilking: () -> Unit, onSettings: () -> Unit) {
    val app = rememberApp()
    val today = remember { LocalDate.now() }
    val settings by app.settings.settings.collectAsState()
    val milk by remember { app.db.milk().between(today, today) }.collectAsStateWithLifecycle(emptyList())
    val feed by remember { app.db.feed().recordsBetween(today, today) }.collectAsStateWithLifecycle(emptyList())
    val overheads by remember { app.db.money().overheads() }.collectAsStateWithLifecycle(emptyList())
    val txs by remember { app.db.money().transactionsBetween(today, today) }.collectAsStateWithLifecycle(emptyList())
    val calfCosts by remember { app.db.money().calfExpenses() }.collectAsStateWithLifecycle(emptyList())
    val cows by remember { app.db.cows().all() }.collectAsStateWithLifecycle(emptyList())
    var showFeedDialog by remember { mutableStateOf(false) }

    val pnl = profitAndLoss(today, today, milk, feed, overheads, txs, calfCosts)
    val cur = settings.currency
    val inMilk = cows.count { it.status == CowStatus.IN_MILK }
    val dry = cows.count { it.status == CowStatus.DRY }
    val milkedIds = milk.mapNotNull { it.cowId }.toSet()
    val sessionsLogged = milk.map { it.session }.distinct().size
    val toCalves = milk.filter { it.forCalves }.sumOf { it.liters }

    Column(Modifier.verticalScroll(rememberScrollState())) {
        FarmHero(
            farmName = settings.farmName,
            coverPhoto = settings.coverPhoto,
            loss = pnl.netProfit < 0,
            onSettings = onSettings,
        ) {
            Label("Today's net profit", color = Color.White.copy(alpha = 0.85f))
            Text(
                money(cur, pnl.netProfit), Modifier.padding(top = 6.dp),
                style = serif(46, Color.White), maxLines = 1,
            )
            Row(Modifier.padding(top = 6.dp, bottom = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (pnl.netProfit >= 0) Icons.AutoMirrored.Outlined.TrendingUp else Icons.AutoMirrored.Outlined.TrendingDown,
                    null, tint = Color.White.copy(alpha = 0.92f), modifier = Modifier.size(18.dp),
                )
                Text(
                    "  " + when {
                        pnl.totalRevenue == 0.0 && pnl.totalExpenses == 0.0 -> "Log milking to see today's profit"
                        pnl.netProfit >= 0 -> "Running above break-even today"
                        else -> "Below break-even so far today"
                    },
                    style = TextStyle(fontFamily = Manrope, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White.copy(alpha = 0.92f)),
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GlassFigure("Milk sales", whole(pnl.totalRevenue), Modifier.weight(1f))
                GlassFigure("Feed", whole(pnl.feedCost), Modifier.weight(1f))
                GlassFigure("Other", whole(pnl.otherCosts), Modifier.weight(1f))
            }
        }

        Column(
            Modifier.padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PrimaryButton("Log milking", onLogMilking, Modifier.weight(1f), icon = Icons.Outlined.LocalDrink)
                SoftButton("Log feeding", { showFeedDialog = true }, Modifier.weight(1f), icon = Icons.Outlined.Grass)
            }

            TileRow {
                StatTile(
                    "Milk today", "${num(pnl.milkProduced)} L", Dp.Green,
                    if (toCalves > 0) "${num(toCalves)} L to calves · $sessionsLogged of 3 logged" else "$sessionsLogged of 3 sessions logged",
                    Modifier.weight(1f),
                )
                StatTile("Feed used", "${num(feed.sumOf { it.kg })} kg", Dp.Ochre, money(cur, pnl.feedCost), Modifier.weight(1f))
            }
            TileRow {
                StatTile("Head milked", "${milkedIds.size}", Dp.Slate, "of $inMilk in milk", Modifier.weight(1f))
                StatTile("Dry cows", "$dry", Dp.Neutral, "resting before calving", Modifier.weight(1f))
            }

            SectionHeader("Today's sessions") { Label("vs morning") }
            val rows = sessionMatrix(milk)
            val max = rows.maxOf { it.liters }.takeIf { it > 0 } ?: 1.0
            DpCard {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row ->
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(12.dp)).background(Dp.Track),
                                contentAlignment = Alignment.BottomCenter,
                            ) {
                                if (row.liters > 0) {
                                    Box(
                                        Modifier.fillMaxWidth().fillMaxHeight((row.liters / max).toFloat().coerceAtLeast(0.06f))
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(if (row.session == Session.MORNING) Dp.Green else Dp.GreenSoft),
                                    )
                                }
                            }
                            Label(row.session.label)
                            Serif("${num(row.liters)} L", 22)
                            val v = row.vsMorningPct
                            when {
                                row.session == Session.MORNING -> Badge("Base", Dp.NeutralTint, Dp.Neutral)
                                v == null -> Badge("–", Dp.NeutralTint, Dp.Neutral)
                                v < 0 -> Badge(pct(v), Dp.ClayTint, Dp.Clay)
                                else -> Badge(pct(v), Dp.GreenPill, Dp.GreenDark)
                            }
                        }
                    }
                }
            }

            if (feed.isNotEmpty()) {
                SectionHeader("Feeding rounds")
                DpCard(padding = 0.dp) {
                    feed.forEachIndexed { i, r ->
                        if (i > 0) Box(Modifier.padding(horizontal = 18.dp)) { RowDivider() }
                        Row(Modifier.padding(start = 18.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Dp.OchreTint), contentAlignment = Alignment.Center) {
                                Icon(Icons.Outlined.Grass, null, tint = Dp.Ochre, modifier = Modifier.size(20.dp))
                            }
                            Column(Modifier.weight(1f).padding(start = 12.dp)) {
                                Strong(r.description)
                                Muted("${r.session.label} · ${num(r.kg)} kg")
                            }
                            Strong(whole(-r.cost), color = Dp.Clay)
                            IconButton(onClick = { app.launchIo { feed().deleteRecord(r) } }) {
                                Icon(Icons.Outlined.Delete, "Delete feeding round", tint = Dp.Muted, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
        }
    }

    if (showFeedDialog) LogFeedingDialog(onDismiss = { showFeedDialog = false })
}

/**
 * Full-width header that runs to the top edge of the screen (the status bar is hidden on Today):
 * the farm cover photo behind a green wash, the farm name with the settings button, the greeting,
 * date and a live clock, then the day's figures in [content].
 */
@Composable
private fun FarmHero(
    farmName: String,
    coverPhoto: String?,
    loss: Boolean,
    onSettings: () -> Unit,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(bottomStart = 34.dp, bottomEnd = 34.dp)
    val cover = rememberPhoto(coverPhoto, maxPx = 1600)
    val now by produceState(LocalTime.now()) {
        while (true) {
            delay((60 - value.second).coerceIn(1, 60) * 1000L)
            value = LocalTime.now()
        }
    }
    val greeting = when (now.hour) { in 0..11 -> "Good morning"; in 12..16 -> "Good afternoon"; else -> "Good evening" }

    Box(Modifier.fillMaxWidth().clip(shape).background(if (loss) Dp.lossGradient else Dp.heroGradient)) {
        if (cover != null) {
            Image(cover, null, Modifier.matchParentSize(), contentScale = ContentScale.Crop)
            // Wash over the photo so white text stays readable on any picture.
            val wash = if (loss) Color(0xFF6E2A1D) else Color(0xFF0E3A23)
            Box(
                Modifier.matchParentSize().background(
                    Brush.verticalGradient(listOf(wash.copy(alpha = 0.62f), wash.copy(alpha = 0.38f), wash.copy(alpha = 0.88f))),
                ),
            )
        }
        Column(
            Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
                .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 28.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painterResource(R.drawable.dairydesk_logo), "DairyDesk",
                    Modifier.size(54.dp).clip(CircleShape).border(2.dp, Color.White, CircleShape),
                    contentScale = ContentScale.Crop,
                )
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(
                        farmName, maxLines = 2, style = serif(30, Color.White),
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        "Your dairy, all in one place.", maxLines = 1,
                        style = TextStyle(fontFamily = Manrope, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.88f)),
                    )
                }
                Surface(
                    onClick = onSettings, shape = CircleShape, color = Color.White.copy(alpha = 0.18f),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.35f)),
                    modifier = Modifier.size(46.dp).semantics { contentDescription = "Settings" },
                ) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Settings, null, tint = Color.White, modifier = Modifier.size(22.dp)) }
                }
            }

            // Greeting and date, with the time beside them.
            Row(Modifier.padding(top = 30.dp, bottom = 22.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Label(greeting, color = Color.White.copy(alpha = 0.85f))
                    Text(
                        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE d MMMM", Locale.US)),
                        Modifier.padding(top = 4.dp),
                        style = TextStyle(fontFamily = Manrope, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White),
                    )
                }
                Text(
                    now.format(DateTimeFormatter.ofPattern("HH:mm")),
                    Modifier.clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.16f))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    style = serif(28, Color.White),
                )
            }
            content()
        }
    }
}

@Composable
private fun GlassFigure(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.17f))
            .padding(horizontal = 14.dp, vertical = 14.dp),
    ) {
        Text(label, maxLines = 1, style = TextStyle(fontFamily = Manrope, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.88f)))
        Text(value, maxLines = 1, style = TextStyle(fontFamily = Manrope, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Color.White), modifier = Modifier.padding(top = 6.dp))
    }
}
