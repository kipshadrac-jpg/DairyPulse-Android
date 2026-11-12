package com.dairypulse.app.ui.feed

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Categories
import com.dairypulse.app.data.Ingredient
import com.dairypulse.app.data.Overhead
import com.dairypulse.app.data.Recurrence
import com.dairypulse.app.domain.dailyEquivalent
import com.dairypulse.app.domain.formulationCost
import com.dairypulse.app.ui.common.AddPill
import com.dairypulse.app.ui.common.BarColors
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.DpCard
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.LegendDot
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.PageIntro
import com.dairypulse.app.ui.common.RowDivider
import com.dairypulse.app.ui.common.SectionHeader
import com.dairypulse.app.ui.common.Segmented
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.SplitBar
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rate
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.common.whole
import com.dairypulse.app.ui.theme.Dp
import java.time.LocalDate

@OptIn(ExperimentalLayoutApi::class)
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
    val colorOf = ingredients.mapIndexed { i, ing -> ing.id to BarColors[i % BarColors.size] }.toMap()
    val today = LocalDate.now()

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageIntro("Rations & costs", "Feed", "What your feed really costs, down to the kilo.")

        SectionHeader("Ingredients") { AddPill("Add", { editIngredient = Ingredient(name = "", packKg = 0.0, packPrice = 0.0) }, Icons.Outlined.Add) }
        if (ingredients.isEmpty()) {
            Muted("Add what you buy, e.g. Dairy Meal: 50 kg bag for 2,500.")
        } else {
            DpCard(padding = 0.dp) {
                ingredients.forEachIndexed { i, ing ->
                    if (i > 0) Box(Modifier.padding(horizontal = 18.dp)) { RowDivider() }
                    Row(
                        Modifier.fillMaxWidth().clickable { editIngredient = ing }.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Strong(ing.name)
                            Muted("${num(ing.packKg)} kg pack @ ${money(cur, ing.packPrice)}")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Strong(rate(cur, ing.pricePerKg), color = Dp.Green, weight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                            Muted("per kg", size = 12)
                        }
                    }
                }
            }
        }

        SectionHeader("Rations") { AddPill("New", { onOpenFormulation(0) }, Icons.Outlined.Add) }
        if (formulations.isEmpty()) Muted("Build a ration recipe from your ingredients to get its exact cost per kg.")
        formulations.forEach { f ->
            val mine = items.filter { it.formulationId == f.id }
            val cost = formulationCost(mine, byId)
            DpCard(onClick = { onOpenFormulation(f.id) }) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Serif(f.name, 20)
                        Muted("Batch ${num(cost.batchKg)} kg · ${money(cur, cost.batchCost)}", Modifier.padding(top = 2.dp))
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Serif(String.format(java.util.Locale.US, "%,.2f", cost.costPerKg), 24, color = Dp.Green)
                        Muted("$cur per kg", size = 12)
                    }
                }
                if (mine.isNotEmpty()) {
                    SplitBar(mine.map { it.kg.toFloat() to (colorOf[it.ingredientId] ?: Dp.Green) }, Modifier.padding(top = 14.dp, bottom = 10.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        mine.forEach { item ->
                            byId[item.ingredientId]?.let { LegendDot("${it.name} ${num(item.kg)} kg", colorOf[it.id] ?: Dp.Green) }
                        }
                    }
                }
            }
        }

        SectionHeader("Overheads") {
            AddPill("Add", {
                editOverhead = Overhead(name = "", category = Categories.LABOUR, amount = 0.0, recurrence = Recurrence.MONTHLY, startDate = today.withDayOfMonth(1))
            }, Icons.Outlined.Add)
        }
        if (overheads.isEmpty()) {
            Muted("Recurring costs like labour or electricity.")
        } else {
            DpCard(padding = 0.dp) {
                overheads.forEachIndexed { i, o ->
                    val ended = o.endDate?.isBefore(today) == true
                    if (i > 0) Box(Modifier.padding(horizontal = 18.dp)) { RowDivider() }
                    Row(
                        Modifier.fillMaxWidth().clickable { editOverhead = o }.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Strong(o.name + if (ended) " (ended)" else "", color = if (ended) Dp.Muted else Dp.Ink)
                            Muted("${o.category} · ${o.recurrence.label.lowercase()}")
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Strong(money(cur, o.amount), weight = androidx.compose.ui.text.font.FontWeight.ExtraBold)
                            Muted(if (o.recurrence == Recurrence.MONTHLY) "≈ ${whole(o.dailyEquivalent(today))} / day" else "per day", size = 12)
                        }
                    }
                }
            }
        }
        Muted("Monthly costs are split across the days of each month, so every day carries its fair share.")
        Spacer(Modifier.height(4.dp))
    }

    editIngredient?.let { IngredientDialog(it, cur) { editIngredient = null } }
    editOverhead?.let { OverheadDialog(it, cur) { editOverhead = null } }
}

@Composable
private fun IngredientDialog(initial: Ingredient, cur: String, onDismiss: () -> Unit) {
    val app = rememberApp()
    var name by remember { mutableStateOf(initial.name) }
    var packKg by remember { mutableStateOf(if (initial.packKg > 0) num(initial.packKg, 2) else "") }
    var packPrice by remember { mutableStateOf(if (initial.packPrice > 0) num(initial.packPrice, 2) else "") }
    val kg = packKg.toAmount()
    val price = packPrice.toAmount()
    FormDialog(
        title = if (initial.id == 0L) "New ingredient" else "Edit ingredient",
        eyebrow = "Feed library",
        onDismiss = onDismiss,
        saveEnabled = name.isNotBlank() && kg != null && kg > 0 && price != null,
        onSave = {
            app.launchIo { feed().upsertIngredient(initial.copy(name = name.trim(), packKg = kg!!, packPrice = price!!)) }
            onDismiss()
        },
        onDelete = if (initial.id != 0L) ({ app.launchIo { feed().deleteIngredient(initial) }; onDismiss() }) else null,
    ) {
        TextInput(name, { name = it }, "Name (e.g. Dairy Meal)")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            NumberField(packKg, { packKg = it }, "Pack size", Modifier.weight(1f), suffix = "kg")
            NumberField(packPrice, { packPrice = it }, "Pack price", Modifier.weight(1f), suffix = cur)
        }
        if (kg != null && kg > 0 && price != null) {
            DpCard(background = Dp.GreenPanel, border = Dp.GreenPanel, radius = 18.dp, padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Label("Works out to", color = Dp.GreenDark)
                        Muted("used in every ration that has it", color = Dp.GreenDark, size = 12)
                    }
                    Serif("${rate(cur, price / kg)} /kg", 22, color = Dp.GreenValue)
                }
            }
        }
        if (initial.id != 0L) Muted("Deleting an ingredient also removes it from any ration recipe.", size = 12)
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
        title = if (initial.id == 0L) "New overhead" else "Edit overhead",
        eyebrow = "Running costs",
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
        Segmented(Recurrence.entries, recurrence, { it.label }) { recurrence = it }
        NumberField(amount, { amount = it }, "Amount per ${if (recurrence == Recurrence.MONTHLY) "month" else "day"}", suffix = cur)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            DateField("Starts", start, { if (it != null) start = it }, Modifier.weight(1f))
            DateField("Ends", end, { end = it }, Modifier.weight(1f), allowClear = true, placeholder = "Ongoing")
        }
        if (initial.id != 0L) Muted("To stop a cost, set an end date instead of deleting it, so past profit figures stay correct.", size = 12)
    }
}
