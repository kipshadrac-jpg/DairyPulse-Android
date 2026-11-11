package com.dairypulse.app.ui.today

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.FeedRecord
import com.dairypulse.app.data.Session
import com.dairypulse.app.domain.formulationCost
import com.dairypulse.app.ui.common.ChipRow
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import java.time.LocalDate
import java.time.LocalTime

private data class FeedOption(val label: String, val costPerKg: Double)

fun currentSession(): Session = when (LocalTime.now().hour) {
    in 0..10 -> Session.MORNING
    in 11..15 -> Session.MIDDAY
    else -> Session.EVENING
}

@Composable
fun LogFeedingDialog(onDismiss: () -> Unit) {
    val app = rememberApp()
    val cur = app.settings.settings.collectAsState().value.currency
    val ingredients by remember { app.db.feed().ingredients() }.collectAsStateWithLifecycle(emptyList())
    val formulations by remember { app.db.feed().formulations() }.collectAsStateWithLifecycle(emptyList())
    val items by remember { app.db.feed().allItems() }.collectAsStateWithLifecycle(emptyList())

    val byId = ingredients.associateBy { it.id }
    val options = formulations.map { f ->
        FeedOption("${f.name} (ration)", formulationCost(items.filter { it.formulationId == f.id }, byId).costPerKg)
    } + ingredients.map { FeedOption(it.name, it.pricePerKg) }

    var date by remember { mutableStateOf(LocalDate.now()) }
    var session by remember { mutableStateOf(currentSession()) }
    var option by remember { mutableStateOf<FeedOption?>(null) }
    var kg by remember { mutableStateOf("") }
    val kgValue = kg.toAmount()

    FormDialog(
        title = "Log Feeding Round",
        onDismiss = onDismiss,
        saveEnabled = option != null && kgValue != null && kgValue > 0,
        onSave = {
            val o = option ?: return@FormDialog
            app.launchIo { feed().insertRecord(FeedRecord(0, date, session, o.label.removeSuffix(" (ration)"), kgValue ?: 0.0, o.costPerKg)) }
            onDismiss()
        },
    ) {
        if (options.isEmpty()) {
            Text("Add ingredients or rations in the Feed tab first.", color = MaterialTheme.colorScheme.error)
        }
        DateField("Date", date, { if (it != null) date = it })
        ChipRow(Session.entries, session, { it.label }) { session = it }
        Dropdown("Ration / ingredient", options, option, { "${it.label} • ${money(cur, it.costPerKg)}/kg" }) { option = it }
        NumberField(kg, { kg = it }, "Quantity fed", suffix = "kg")
        option?.let { o -> kgValue?.let { Text("Cost: ${money(cur, o.costPerKg * it)}") } }
    }
}
