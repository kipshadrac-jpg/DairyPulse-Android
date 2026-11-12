package com.dairypulse.app.ui.today

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.FeedRecord
import com.dairypulse.app.data.Session
import com.dairypulse.app.domain.formulationCost
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.Segmented
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.rate
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.theme.Dp
import java.time.LocalDate
import java.time.LocalTime

private data class FeedOption(val name: String, val kind: String, val costPerKg: Double)

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
        FeedOption(f.name, "Ration", formulationCost(items.filter { it.formulationId == f.id }, byId).costPerKg)
    } + ingredients.map { FeedOption(it.name, "Ingredient", it.pricePerKg) }

    var date by remember { mutableStateOf(LocalDate.now()) }
    var session by remember { mutableStateOf(currentSession()) }
    var option by remember { mutableStateOf<FeedOption?>(null) }
    var kg by remember { mutableStateOf("") }
    val kgValue = kg.toAmount()

    FormDialog(
        title = "Log feeding",
        onDismiss = onDismiss,
        saveLabel = "Save round",
        saveEnabled = option != null && kgValue != null && kgValue > 0,
        onSave = {
            val o = option ?: return@FormDialog
            app.launchIo { feed().insertRecord(FeedRecord(0, date, session, o.name, kgValue ?: 0.0, o.costPerKg)) }
            onDismiss()
        },
    ) {
        DateField("Date", date, { if (it != null) date = it })
        Segmented(Session.entries, session, { it.label }) { session = it }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Strong("Ration or ingredient", size = 13, color = Dp.Neutral)
            if (options.isEmpty()) Muted("Add ingredients or rations in the Feed tab first.", color = Dp.Clay)
            options.forEach { o ->
                val on = o == option
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (on) Dp.GreenPanel else androidx.compose.ui.graphics.Color.White,
                    border = BorderStroke(1.5.dp, if (on) Dp.Green else Dp.InputBorder),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                        .selectable(selected = on, role = Role.RadioButton) { option = o },
                ) {
                    Row(Modifier.padding(end = 14.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = on, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Dp.Green), modifier = Modifier.padding(12.dp))
                        Column(Modifier.weight(1f)) { Strong(o.name); Muted(o.kind, size = 12) }
                        Strong("${rate(cur, o.costPerKg)}/kg", color = if (on) Dp.Green else Dp.Neutral, size = 14)
                    }
                }
            }
        }

        NumberField(kg, { kg = it }, "Quantity fed", suffix = "kg")
        option?.let { o ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Dp.OchreTint).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Label("Cost of this round", Modifier.weight(1f), color = Dp.Ochre)
                Serif(money(cur, o.costPerKg * (kgValue ?: 0.0)), 22, color = Dp.Ochre)
            }
        }
    }
}
