package com.dairypulse.app.ui.herd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.CalfCostType
import com.dairypulse.app.data.CalfExpense
import com.dairypulse.app.data.Cow
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.domain.calfCost
import com.dairypulse.app.ui.common.ChipRow
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.InfoCard
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.SectionTitle
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.pretty
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import java.time.LocalDate

/** Average cattle gestation used to estimate the calving date from a service / AI date. */
private const val GESTATION_DAYS = 283L

@Composable
fun CowScreen(cowId: Long, onDone: () -> Unit) {
    val app = rememberApp()
    val cur = app.settings.settings.value.currency
    val cows by remember { app.db.cows().all() }.collectAsStateWithLifecycle(emptyList())
    val history by remember(cowId) { app.db.milk().forCow(cowId) }.collectAsStateWithLifecycle(emptyList())
    val allCalfExpenses by remember { app.db.money().calfExpenses() }.collectAsStateWithLifecycle(emptyList())

    var loaded by remember { mutableStateOf(cowId == 0L) }
    var original by remember { mutableStateOf<Cow?>(null) }
    var name by remember { mutableStateOf("") }
    var tag by remember { mutableStateOf("") }
    var status by remember { mutableStateOf(CowStatus.IN_MILK) }
    var female by remember { mutableStateOf(true) }
    var birth by remember { mutableStateOf<LocalDate?>(null) }
    var lastCalving by remember { mutableStateOf<LocalDate?>(null) }
    var expectedCalving by remember { mutableStateOf<LocalDate?>(null) }
    var damId by remember { mutableStateOf<Long?>(null) }
    var notes by remember { mutableStateOf("") }
    var addExpense by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(cowId) {
        if (cowId != 0L) {
            app.db.cows().get(cowId)?.let { c ->
                original = c; name = c.name; tag = c.tag; status = c.status; female = c.isFemale
                birth = c.birthDate; lastCalving = c.lastCalvingDate; expectedCalving = c.expectedCalvingDate
                damId = c.damId; notes = c.notes
            }
            loaded = true
        }
    }
    if (!loaded) return

    val dams = cows.filter { it.id != cowId && it.isFemale && !it.status.isYoungStock }

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextInput(name, { name = it }, "Name", Modifier.weight(1.5f))
            TextInput(tag, { tag = it }, "Tag no.", Modifier.weight(1f))
        }
        Dropdown("Status", CowStatus.entries, status, { it.label }) { status = it }
        ChipRow(listOf(true, false), female, { if (it) "Female" else "Male" }) { female = it }
        DateField("Date of birth", birth, { birth = it }, allowClear = true)

        if (!status.isYoungStock) {
            DateField("Last calving date (for days in milk)", lastCalving, { lastCalving = it }, allowClear = true)
            DateField("Expected calving date", expectedCalving, { expectedCalving = it }, allowClear = true)
            DateField(
                "…or set from service / AI date",
                null,
                { served -> if (served != null) expectedCalving = served.plusDays(GESTATION_DAYS) },
            )
            OutlinedButton(onClick = {
                lastCalving = LocalDate.now(); expectedCalving = null; status = CowStatus.IN_MILK
            }) { Text("Record calving today") }
        }
        if (status.isYoungStock || damId != null) {
            Dropdown("Dam (mother)", dams, dams.firstOrNull { it.id == damId }, { it.displayName }) { damId = it.id }
        }
        OutlinedTextField(notes, { notes = it }, label = { Text("Notes") }, modifier = Modifier.fillMaxWidth(), minLines = 2)

        Button(
            enabled = name.isNotBlank(),
            onClick = {
                val cow = (original ?: Cow(tag = "", name = "", status = status)).copy(
                    name = name.trim(), tag = tag.trim(), status = status, isFemale = female, birthDate = birth,
                    lastCalvingDate = lastCalving, expectedCalvingDate = expectedCalving, damId = damId, notes = notes.trim(),
                )
                app.launchIo { cows().upsert(cow) }
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }

        val cow = original ?: return@Column

        if (status.isYoungStock || allCalfExpenses.any { it.calfId == cow.id }) {
            val mine = allCalfExpenses.filter { it.calfId == cow.id }
            val cost = calfCost(cow.id, allCalfExpenses)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Rearing Costs: ${money(cur, cost.total)}", Modifier.weight(1f))
                TextButton(onClick = { addExpense = true }) { Text("+ Add") }
            }
            mine.forEach { e ->
                InfoCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${e.type.label}: ${money(cur, e.amount)}", fontWeight = FontWeight.SemiBold)
                            Text(e.date.pretty() + if (e.note.isNotBlank()) " • ${e.note}" else "", style = MaterialTheme.typography.bodySmall)
                        }
                        IconButton(onClick = { app.launchIo { money().deleteCalfExpense(e) } }) { Icon(Icons.Default.Delete, "Delete") }
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            SectionTitle("Recent Milk Records")
            history.forEach { r ->
                Row(Modifier.fillMaxWidth()) {
                    Text("${r.date.pretty()} • ${r.session.label}", Modifier.weight(1f))
                    Text("${num(r.liters)} L")
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = { confirmDelete = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Delete Animal", color = MaterialTheme.colorScheme.error)
        }
        Text(
            "Tip: if the animal was sold or died, set its status to \"Sold / Gone\" instead, to keep its history.",
            style = MaterialTheme.typography.bodySmall,
        )

        if (addExpense) CalfExpenseDialog(cow.id, cur) { addExpense = false }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                title = { Text("Delete ${cow.name}?") },
                text = { Text("This removes the animal and its rearing costs. Milk already logged stays in the totals.") },
                confirmButton = {
                    TextButton(onClick = { app.launchIo { cows().delete(cow) }; confirmDelete = false; onDone() }) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun CalfExpenseDialog(calfId: Long, cur: String, onDismiss: () -> Unit) {
    val app = rememberApp()
    var type by remember { mutableStateOf(CalfCostType.FEED) }
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf(LocalDate.now()) }
    var note by remember { mutableStateOf("") }
    val value = amount.toAmount()
    FormDialog(
        title = "Add Rearing Cost",
        onDismiss = onDismiss,
        saveEnabled = value != null && value > 0,
        onSave = {
            app.launchIo { money().insertCalfExpense(CalfExpense(0, calfId, date, type, value!!, note.trim())) }
            onDismiss()
        },
    ) {
        ChipRow(CalfCostType.entries, type, { it.label }) { type = it }
        NumberField(amount, { amount = it }, "Amount", suffix = cur)
        DateField("Date", date, { if (it != null) date = it })
        TextInput(note, { note = it }, "Note (e.g. milk replacer, vaccination)")
    }
}
