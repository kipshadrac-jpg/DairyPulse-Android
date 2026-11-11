package com.dairypulse.app.ui.today

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.data.MilkRecord
import com.dairypulse.app.data.Session
import com.dairypulse.app.ui.common.ChipRow
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.SectionTitle
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import java.time.LocalDate

/**
 * Log one milking session. Litres can be entered per cow (feeds the Herd yields and
 * "Head Milked") and/or as one bulk figure. Re-opening the same date + session edits it.
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
    val perCow = remember { mutableStateMapOf<Long, String>() }
    var editing by remember { mutableStateOf(false) }

    // Prefill when a session was already logged.
    LaunchedEffect(date, session) {
        val existing = app.db.milk().forSession(date, session)
        perCow.clear()
        bulk = ""
        existing.forEach { r ->
            if (r.cowId == null) bulk = num(r.liters, 2) else perCow[r.cowId] = num(r.liters, 2)
        }
        existing.firstOrNull()?.let { price = num(it.pricePerLiter, 2) }
        editing = existing.isNotEmpty()
    }

    val total = perCow.values.sumOf { it.toAmount() ?: 0.0 } + (bulk.toAmount() ?: 0.0)
    val priceValue = price.toAmount() ?: 0.0

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp)) {
        item {
            DateField("Date", date, { if (it != null) date = it })
            Spacer(Modifier.height(8.dp))
            ChipRow(Session.entries, session, { it.label }) { session = it }
            if (editing) {
                Text(
                    "This session was already logged; saving will replace it.",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(8.dp))
            NumberField(price, { price = it }, "Milk price per litre", suffix = settings.currency)
            SectionTitle(if (inMilk.isEmpty()) "Herd total" else "Litres per cow (${inMilk.size} in milk)")
            if (inMilk.isEmpty()) {
                Text(
                    "Tip: add cows in the Herd tab to track yield per cow.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        items(inMilk, key = { it.id }) { cow ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(cow.displayName, Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(8.dp))
                NumberField(perCow[cow.id] ?: "", { perCow[cow.id] = it }, "Litres", Modifier.weight(1f))
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            NumberField(bulk, { bulk = it }, if (inMilk.isEmpty()) "Total litres" else "Extra / untagged litres (optional)", suffix = "L")
            Spacer(Modifier.height(12.dp))
            Column {
                Text("Session total: ${num(total)} L", fontWeight = FontWeight.Bold)
                Text("Value: ${money(settings.currency, total * priceValue)}")
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    val records = buildList {
                        perCow.forEach { (id, v) ->
                            val l = v.toAmount()
                            if (l != null && l > 0) add(MilkRecord(0, date, session, id, l, priceValue))
                        }
                        bulk.toAmount()?.takeIf { it > 0 }?.let { add(MilkRecord(0, date, session, null, it, priceValue)) }
                    }
                    app.launchIo { milk().replaceSession(date, session, records) }
                    onDone()
                },
                enabled = priceValue > 0 && (total > 0 || editing),
                modifier = Modifier.fillMaxWidth(),
            ) { Text(if (editing) "Update ${session.label} Milking" else "Save ${session.label} Milking") }
        }
    }
}
