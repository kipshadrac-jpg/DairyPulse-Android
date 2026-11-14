package com.dairypulse.app.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.data.MilkRecord
import com.dairypulse.app.data.Session
import com.dairypulse.app.ui.common.CowAvatar
import com.dairypulse.app.ui.common.CompactNumber
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.PrimaryButton
import com.dairypulse.app.ui.common.RowDivider
import com.dairypulse.app.ui.common.SectionHeader
import com.dairypulse.app.ui.common.Segmented
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.theme.Dp
import java.time.LocalDate

/**
 * Log one milking session. Litres can be entered per cow (feeds the Herd yields and
 * "Head milked") and/or as one bulk figure. Milk fed to calves is entered separately and comes
 * off the litres and value sold. Re-opening the same date + session edits it.
 */
@Composable
fun LogMilkingScreen(onDone: () -> Unit) {
    val app = rememberApp()
    val settings = app.settings.settings.value
    val cows by remember { app.db.cows().all() }.collectAsStateWithLifecycle(emptyList())
    val inMilk = cows.filter { it.status == CowStatus.IN_MILK }

    var date by remember { mutableStateOf(LocalDate.now()) }
    var session by remember { mutableStateOf(currentSession()) }
    var price by remember { mutableStateOf(num(settings.milkPricePerLiter, 2)) }
    var bulk by remember { mutableStateOf("") }
    var calves by remember { mutableStateOf("") }
    val perCow = remember { mutableStateMapOf<Long, String>() }
    var editing by remember { mutableStateOf(false) }

    // Prefill when a session was already logged.
    LaunchedEffect(date, session) {
        val existing = app.db.milk().forSession(date, session)
        perCow.clear()
        bulk = ""
        calves = ""
        existing.forEach { r ->
            when {
                r.forCalves -> calves = num(r.liters, 2)
                r.cowId == null -> bulk = num(r.liters, 2)
                else -> perCow[r.cowId] = num(r.liters, 2)
            }
        }
        existing.firstOrNull()?.let { price = num(it.pricePerLiter, 2) }
        editing = existing.isNotEmpty()
    }

    val total = perCow.values.sumOf { it.toAmount() ?: 0.0 } + (bulk.toAmount() ?: 0.0)
    val toCalves = calves.toAmount() ?: 0.0
    val sold = total - toCalves
    val tooMuchForCalves = toCalves > total
    val priceValue = price.toAmount() ?: 0.0

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    DateField("Date", date, { if (it != null) date = it }, Modifier.weight(1f))
                    NumberField(price, { price = it }, "Price per litre", Modifier.weight(1f), suffix = settings.currency)
                }
                Segmented(Session.entries, session, { it.label }) { session = it }
                if (editing) Muted("This session was already logged; saving will replace it.", color = Dp.Green, weight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                SectionHeader(if (inMilk.isEmpty()) "Herd total" else "Litres per cow") {
                    if (inMilk.isNotEmpty()) Label("${inMilk.size} in milk")
                }
                if (inMilk.isEmpty()) Muted("Tip: add cows in the Herd tab to track yield per cow.")
                Spacer(Modifier.height(2.dp))
            }
        }
        itemsIndexed(inMilk, key = { _, c -> c.id }) { i, cow ->
            val first = i == 0
            val last = i == inMilk.lastIndex
            val shape = RoundedCornerShape(
                topStart = if (first) 22.dp else 0.dp, topEnd = if (first) 22.dp else 0.dp,
                bottomStart = if (last) 22.dp else 0.dp, bottomEnd = if (last) 22.dp else 0.dp,
            )
            Column(Modifier.fillMaxWidth().clip(shape).background(Dp.Card)) {
                if (!first) Box(Modifier.padding(horizontal = 16.dp)) { RowDivider() }
                Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    CowAvatar(cow.photo, cow.name, 40.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Strong(cow.name)
                        if (cow.tag.isNotBlank()) Muted("#${cow.tag}")
                    }
                    CompactNumber(perCow[cow.id] ?: "", { perCow[cow.id] = it }, "Litres for ${cow.displayName}", "L", Modifier.width(120.dp))
                }
            }
        }
        item {
            Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NumberField(bulk, { bulk = it }, if (inMilk.isEmpty()) "Total litres" else "Extra or untagged litres (optional)", suffix = "L")
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    NumberField(calves, { calves = it }, "Milk for calves (subtracted)", suffix = "L")
                    if (tooMuchForCalves) {
                        Muted("More than was milked this session.", color = Dp.Clay, weight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    } else {
                        Muted("Fed to calves, not sold: taken off the litres and value sold.")
                    }
                }
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Dp.CreamDeep)
                        .border(1.dp, Dp.Border, RoundedCornerShape(22.dp)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Column(Modifier.weight(1f)) {
                            Label(if (toCalves > 0) "Milked" else "Session total")
                            Serif("${num(total)} L", 30, Modifier.padding(top = 4.dp), color = Dp.GreenValue)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Label("Value")
                            Strong(money(settings.currency, sold.coerceAtLeast(0.0) * priceValue), size = 18, modifier = Modifier.padding(top = 6.dp))
                        }
                    }
                    if (toCalves > 0) {
                        RowDivider()
                        SummaryLine("To calves", "− ${num(toCalves)} L", Dp.Clay)
                        SummaryLine("Sold", "${num(sold.coerceAtLeast(0.0))} L", Dp.GreenValue)
                    }
                    PrimaryButton(
                        if (editing) "Update ${session.label.lowercase()} milking" else "Save ${session.label.lowercase()} milking",
                        onClick = {
                            val records = buildList {
                                perCow.forEach { (id, v) ->
                                    val l = v.toAmount()
                                    if (l != null && l > 0) add(MilkRecord(0, date, session, id, l, priceValue))
                                }
                                bulk.toAmount()?.takeIf { it > 0 }?.let { add(MilkRecord(0, date, session, null, it, priceValue)) }
                                toCalves.takeIf { it > 0 }?.let { add(MilkRecord(0, date, session, null, it, priceValue, forCalves = true)) }
                            }
                            app.launchIo { milk().replaceSession(date, session, records) }
                            onDone()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        icon = Icons.Outlined.Check,
                        enabled = priceValue > 0 && (total > 0 || editing) && !tooMuchForCalves,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryLine(label: String, value: String, color: androidx.compose.ui.graphics.Color) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Muted(label, Modifier.weight(1f), size = 15, color = Dp.Neutral, weight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        Strong(value, size = 16, color = color)
    }
}
