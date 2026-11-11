package com.dairypulse.app.ui.herd

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Cow
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.domain.calfCost
import com.dairypulse.app.domain.daysInMilk
import com.dairypulse.app.domain.daysUntilCalving
import com.dairypulse.app.domain.isCalvingSoon
import com.dairypulse.app.ui.common.ChipRow
import com.dairypulse.app.ui.common.InfoCard
import com.dairypulse.app.ui.common.SectionTitle
import com.dairypulse.app.ui.common.compact
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.theme.LossRed
import java.time.LocalDate

private enum class HerdFilter { ALL, IN_MILK, DRY, SOLD }

@Composable
fun HerdScreen(onOpenCow: (Long) -> Unit) {
    val app = rememberApp()
    val cur = app.settings.settings.collectAsState().value.currency
    val today = remember { LocalDate.now() }
    val cows by remember { app.db.cows().all() }.collectAsStateWithLifecycle(emptyList())
    val milkToday by remember { app.db.milk().between(today, today) }.collectAsStateWithLifecycle(emptyList())
    val calfExpenses by remember { app.db.money().calfExpenses() }.collectAsStateWithLifecycle(emptyList())
    var filter by rememberSaveable { mutableStateOf(HerdFilter.ALL) }

    val adults = cows.filter { it.status == CowStatus.IN_MILK || it.status == CowStatus.DRY }
    val young = cows.filter { it.status.isYoungStock }
    val shown = when (filter) {
        HerdFilter.ALL -> adults
        HerdFilter.IN_MILK -> adults.filter { it.status == CowStatus.IN_MILK }
        HerdFilter.DRY -> adults.filter { it.status == CowStatus.DRY }
        HerdFilter.SOLD -> cows.filter { it.status == CowStatus.SOLD }
    }.sortedWith(compareBy<Cow> { !isCalvingSoon(it, today) }.thenBy { it.name.lowercase() })
    val byId = cows.associateBy { it.id }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                ChipRow(HerdFilter.entries, filter, {
                    when (it) {
                        HerdFilter.ALL -> "All (${adults.size})"
                        HerdFilter.IN_MILK -> "In Milk (${adults.count { c -> c.status == CowStatus.IN_MILK }})"
                        HerdFilter.DRY -> "Dry (${adults.count { c -> c.status == CowStatus.DRY }})"
                        HerdFilter.SOLD -> "Sold (${cows.count { c -> c.status == CowStatus.SOLD }})"
                    }
                }) { filter = it }
            }
            if (cows.isEmpty()) {
                Text(
                    "No animals yet. Tap \"Add Animal\" to register your cows, heifers and calves.",
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            shown.forEach { cow ->
                val records = milkToday.filter { it.cowId == cow.id }
                CowCard(cow, today, records.sumOf { it.liters }, records.map { it.session }.distinct().size) { onOpenCow(cow.id) }
            }

            if (filter != HerdFilter.SOLD && young.isNotEmpty()) {
                SectionTitle("Calves & Rearing Expenses")
                young.forEach { calf ->
                    val cost = calfCost(calf.id, calfExpenses)
                    InfoCard(onClick = { onOpenCow(calf.id) }) {
                        Text(
                            "${calf.status.label}: ${calf.displayName}" + (calf.damId?.let(byId::get)?.let { " (Dam: ${it.name})" } ?: ""),
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            (if (calf.isFemale) "Heifer" else "Bull") + (calf.birthDate?.let { " • Born: $it" } ?: ""),
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Text(
                            "Total Cost: ${money(cur, cost.total)} (Feed: ${compact(cost.feed)}, Upkeep: ${compact(cost.upkeep)})",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .background(Color(0xFFF0F4C3), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.padding(40.dp))
        }
        ExtendedFloatingActionButton(
            onClick = { onOpenCow(0) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add Animal") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }
}

@Composable
private fun CowCard(cow: Cow, today: LocalDate, litersToday: Double, sessions: Int, onClick: () -> Unit) {
    InfoCard(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).background(Color(0xFFDDDDDD), CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text(cow.name.take(1).uppercase(), fontWeight = FontWeight.Bold, color = Color.DarkGray) }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(cow.displayName, fontWeight = FontWeight.Bold)
                val dim = daysInMilk(cow, today)
                Text(
                    "Status: ${cow.status.label}" + if (cow.status == CowStatus.IN_MILK && dim != null) " • DIM: $dim days" else "",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (cow.status == CowStatus.IN_MILK) {
                    Text("Today: ${num(litersToday)} L ($sessions sessions)", style = MaterialTheme.typography.bodySmall)
                }
                daysUntilCalving(cow, today)?.let { days ->
                    val soon = isCalvingSoon(cow, today)
                    Text(
                        if (days < 0) "Calving overdue by ${-days} days (Action req.)"
                        else "Due in: $days days" + if (soon) " (Action req.)" else "",
                        color = if (soon) LossRed else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (soon) FontWeight.Bold else FontWeight.Normal,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
