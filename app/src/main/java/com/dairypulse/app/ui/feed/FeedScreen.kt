package com.dairypulse.app.ui.feed

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Categories
import com.dairypulse.app.data.Ingredient
import com.dairypulse.app.data.Overhead
import com.dairypulse.app.data.Recurrence
import com.dairypulse.app.domain.dailyEquivalent
import com.dairypulse.app.domain.formulationCost
import com.dairypulse.app.ui.common.ChipRow
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.InfoCard
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.theme.FarmGreen
import java.time.LocalDate

@Composable
fun FeedScreen(onOpenFormulation: (Long) -> Unit) {
    val app = rememberApp()
    val cur = app.settings.settings.collectAsState().value.currency
    val ingredients by remember { app.db.feed().ingredients() }.collectAsStateWithLifecycle(emptyList())
    val formulations by remember { app.db.feed().formulations() }.collectAsStateWithLifecycle(emptyList())
    val items by remember { app.db.feed().allItems() }.collectAsStateWithLifecycle(emptyList())
    val overheads by remember { app.db.money().overheads() }.collectAsStateWithLifecycle(emptyList())

    var editIngredient by remember { mutableStateOf<Ingredient?>(null) }
    var editOverhead by remember { mutableStateOf<Overhead?>(null) }
    val byId = ingredients.associateBy { it.id }
    val today = LocalDate.now()

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Header("Ingredients Library") { editIngredient = Ingredient(name = "", packKg = 0.0, packPrice = 0.0) }
        if (ingredients.isEmpty()) Hint("Add what you buy, e.g. Dairy Meal: 50 kg bag for 2,500.")
        ingredients.forEach { ing ->
            InfoCard(onClick = { editIngredient = ing }) {
                Text(ing.name, fontWeight = FontWeight.Bold)
                Text(
                    "${money(cur, ing.pricePerKg)} / kg (${num(ing.packKg)}kg @ ${num(ing.packPrice, 2)})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Header("Active Formulations") { onOpenFormulation(0) }
        if (formulations.isEmpty()) Hint("Build a ration recipe from your ingredients to get its exact cost per kg.")
        formulations.forEachIndexed { i, f ->
            val cost = formulationCost(items.filter { it.formulationId == f.id }, byId)
            InfoCard(accent = if (i == 0) FarmGreen else null, onClick = { onOpenFormulation(f.id) }) {
                Text(f.name, fontWeight = FontWeight.Bold)
                Text(
                    "Batch: ${num(cost.batchKg)} kg • Cost: ${money(cur, cost.batchCost)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Text(
                    "Unit Cost: ${money(cur, cost.costPerKg)} / kg",
                    color = FarmGreen, fontWeight = FontWeight.Bold,
                )
            }
        }

        Header("Other Overhead Expenses") {
            editOverhead = Overhead(name = "", category = Categories.LABOUR, amount = 0.0, recurrence = Recurrence.MONTHLY, startDate = today.withDayOfMonth(1))
        }
        if (overheads.isEmpty()) Hint("Recurring costs like labour or electricity. Monthly amounts are split across each day.")
        overheads.forEach { o ->
            val ended = o.endDate?.isBefore(today) == true
            InfoCard(onClick = { editOverhead = o }) {
                Text(
                    "${o.name} (${o.recurrence.label})" + if (ended) " – ended" else "",
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "${money(cur, o.amount)} / ${if (o.recurrence == Recurrence.MONTHLY) "mo" else "day"} • " +
                        "${o.category} • ≈ ${money(cur, o.dailyEquivalent(today))}/day",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    editIngredient?.let { IngredientDialog(it) { editIngredient = null } }
    editOverhead?.let { OverheadDialog(it, cur) { editOverhead = null } }
}

@Composable
private fun Header(title: String, onAdd: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        TextButton(onClick = onAdd) { Icon(Icons.Default.Add, null); Text("Add") }
    }
}

@Composable
private fun Hint(text: String) {
    Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun IngredientDialog(initial: Ingredient, onDismiss: () -> Unit) {
    val app = rememberApp()
    var name by remember { mutableStateOf(initial.name) }
    var packKg by remember { mutableStateOf(if (initial.packKg > 0) num(initial.packKg, 2) else "") }
    var packPrice by remember { mutableStateOf(if (initial.packPrice > 0) num(initial.packPrice, 2) else "") }
    val kg = packKg.toAmount()
    val price = packPrice.toAmount()
    FormDialog(
        title = if (initial.id == 0L) "New Ingredient" else "Edit Ingredient",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank() && kg != null && kg > 0 && price != null,
        onSave = {
            app.launchIo { feed().upsertIngredient(initial.copy(name = name.trim(), packKg = kg!!, packPrice = price!!)) }
            onDismiss()
        },
        onDelete = if (initial.id != 0L) ({ app.launchIo { feed().deleteIngredient(initial) }; onDismiss() }) else null,
    ) {
        TextInput(name, { name = it }, "Name (e.g. Dairy Meal)")
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumberField(packKg, { packKg = it }, "Pack size", Modifier.weight(1f), suffix = "kg")
            NumberField(packPrice, { packPrice = it }, "Pack price", Modifier.weight(1f))
        }
        if (kg != null && kg > 0 && price != null) Text("= ${num(price / kg, 2)} per kg")
        if (initial.id != 0L) {
            Text(
                "Deleting an ingredient also removes it from any ration recipe.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun OverheadDialog(initial: Overhead, cur: String, onDismiss: () -> Unit) {
    val app = rememberApp()
    var name by remember { mutableStateOf(initial.name) }
    var category by remember { mutableStateOf(initial.category) }
    var amount by remember { mutableStateOf(if (initial.amount > 0) num(initial.amount, 2) else "") }
    var recurrence by remember { mutableStateOf(initial.recurrence) }
    var start by remember { mutableStateOf(initial.startDate) }
    var end by remember { mutableStateOf(initial.endDate) }
    val value = amount.toAmount()
    FormDialog(
        title = if (initial.id == 0L) "New Overhead" else "Edit Overhead",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank() && value != null && value > 0,
        onSave = {
            app.launchIo {
                money().upsertOverhead(initial.copy(name = name.trim(), category = category, amount = value!!, recurrence = recurrence, startDate = start, endDate = end))
            }
            onDismiss()
        },
        onDelete = if (initial.id != 0L) ({ app.launchIo { money().deleteOverhead(initial) }; onDismiss() }) else null,
    ) {
        TextInput(name, { name = it }, "Name (e.g. Farm Labour)")
        Dropdown("Category", Categories.expense, category, { it }) { category = it }
        ChipRow(Recurrence.entries, recurrence, { it.label }) { recurrence = it }
        NumberField(amount, { amount = it }, "Amount per ${if (recurrence == Recurrence.MONTHLY) "month" else "day"}", suffix = cur)
        DateField("Starts", start, { if (it != null) start = it })
        DateField("Ends (leave empty if ongoing)", end, { end = it }, allowClear = true)
        if (initial.id != 0L) {
            Text(
                "To stop a cost, set an end date instead of deleting it, so past profit figures stay correct.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
