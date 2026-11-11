package com.dairypulse.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.domain.profitAndLoss
import com.dairypulse.app.domain.sessionMatrix
import com.dairypulse.app.ui.common.InfoCard
import com.dairypulse.app.ui.common.SectionTitle
import com.dairypulse.app.ui.common.compact
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.pct
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.theme.FarmGreenLight
import com.dairypulse.app.ui.theme.LossRed
import java.time.LocalDate

@Composable
fun TodayScreen(onLogMilking: () -> Unit) {
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

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        // Executive summary card
        Column(
            Modifier.fillMaxWidth()
                .background(if (pnl.netProfit < 0) LossRed else FarmGreenLight, RoundedCornerShape(12.dp))
                .padding(16.dp),
        ) {
            Text("TODAY'S NET PROFIT", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
            Text(
                money(cur, pnl.netProfit),
                color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold,
            )
            HorizontalDivider(Modifier.padding(vertical = 8.dp), color = Color.White.copy(alpha = 0.3f))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Sales: ${compact(pnl.totalRevenue)}", color = Color.White, fontSize = 13.sp)
                Text("Feed: ${compact(pnl.feedCost)}", color = Color.White, fontSize = 13.sp)
                Text("Other: ${compact(pnl.otherCosts)}", color = Color.White, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = onLogMilking, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Text(" Log Milking")
        }
        FilledTonalButton(onClick = { showFeedDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Add, null); Text(" Log Feeding Round")
        }

        SectionTitle("Today's Session Yields")
        InfoCard {
            Row(Modifier.fillMaxWidth()) {
                Text("Session", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("Milk (L)", Modifier.weight(1f), fontWeight = FontWeight.Bold)
                Text("vs Morn", Modifier.weight(1f), fontWeight = FontWeight.Bold, textAlign = TextAlign.End)
            }
            sessionMatrix(milk).forEach { row ->
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(row.session.label, Modifier.weight(1f))
                    Text("${num(row.liters)} L", Modifier.weight(1f))
                    val v = row.vsMorningPct
                    Text(
                        when {
                            row.session.ordinal == 0 -> "Base"
                            v == null -> "–"
                            else -> pct(v)
                        },
                        Modifier.weight(1f),
                        textAlign = TextAlign.End,
                        color = if (v != null && v < 0) LossRed else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("${num(pnl.milkLiters)} L", "Milk Today", Modifier.weight(1f))
            MetricTile("${num(feed.sumOf { it.kg })} Kg", "Feed Used (${compact(pnl.feedCost)})", Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile("${milkedIds.size} / $inMilk", "Head Milked", Modifier.weight(1f))
            MetricTile("$dry", "Dry Cows", Modifier.weight(1f))
        }

        if (feed.isNotEmpty()) {
            SectionTitle("Feeding Rounds Today")
            feed.forEach { r ->
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${r.session.label}: ${r.description}", fontWeight = FontWeight.SemiBold)
                            Text("${num(r.kg)} kg • ${money(cur, r.cost)}", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { app.launchIo { feed().deleteRecord(r) } }) {
                            Icon(Icons.Default.Delete, "Delete")
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    if (showFeedDialog) LogFeedingDialog(onDismiss = { showFeedDialog = false })
}

@Composable
private fun MetricTile(value: String, label: String, modifier: Modifier = Modifier) {
    InfoCard(modifier) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            Text(label, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center)
        }
    }
}
