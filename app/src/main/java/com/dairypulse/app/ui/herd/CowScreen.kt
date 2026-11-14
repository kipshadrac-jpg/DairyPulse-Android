package com.dairypulse.app.ui.herd

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.CalfCostType
import com.dairypulse.app.data.CalfExpense
import com.dairypulse.app.data.Cow
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.data.Photos
import com.dairypulse.app.domain.calfCost
import com.dairypulse.app.domain.daysInMilk
import com.dairypulse.app.domain.daysUntilCalving
import com.dairypulse.app.domain.isCalvingSoon
import com.dairypulse.app.ui.common.AddPill
import com.dairypulse.app.ui.common.CowAvatar
import com.dairypulse.app.ui.common.CowPhoto
import com.dairypulse.app.ui.common.PhotoActions
import com.dairypulse.app.ui.common.DangerTextButton
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.DpCard
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.PrimaryButton
import com.dairypulse.app.ui.common.RowDivider
import com.dairypulse.app.ui.common.SectionHeader
import com.dairypulse.app.ui.common.Segmented
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.SoftButton
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.pretty
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.rememberPhotoPicker
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.theme.Dp
import java.time.LocalDate

/** Average cattle gestation used to estimate the calving date from a service / AI date. */
private const val GESTATION_DAYS = 283L

@Composable
fun CowScreen(cowId: Long, onDone: () -> Unit) {
    val app = rememberApp()
    val context = LocalContext.current
    val cur = app.settings.settings.value.currency
    val today = remember { LocalDate.now() }
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
    var photo by remember { mutableStateOf<String?>(null) }
    val picker = rememberPhotoPicker(Photos.COW_SIDE) { picked ->
        // Drop an earlier pick from this visit that was never saved.
        if (photo != original?.photo) Photos.delete(context, photo)
        photo = picked
    }
    var addExpense by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(cowId) {
        if (cowId != 0L) {
            app.db.cows().get(cowId)?.let { c ->
                original = c; name = c.name; tag = c.tag; status = c.status; female = c.isFemale
                birth = c.birthDate; lastCalving = c.lastCalvingDate; expectedCalving = c.expectedCalvingDate
                damId = c.damId; notes = c.notes; photo = c.photo
            }
            loaded = true
        }
    }
    if (!loaded) return

    val dams = cows.filter { it.id != cowId && it.isFemale && !it.status.isYoungStock }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        original?.let { cow -> Summary(cow.copy(photo = photo), today, history.filter { it.date == today }.sumOf { it.liters }) }

        SectionHeader("Photo")
        CowPhoto(
            photo, name.ifBlank { "?" }, Modifier.fillMaxWidth().aspectRatio(1f), RoundedCornerShape(22.dp),
            maxPx = Photos.COW_SIDE, initialSize = 96,
        )
        PhotoActions(picker, hasPhoto = photo != null, onRemove = {
            if (photo != original?.photo) Photos.delete(context, photo)
            photo = null
        })

        SectionHeader("Details")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TextInput(name, { name = it }, "Name", Modifier.weight(1.5f))
            TextInput(tag, { tag = it }, "Tag no.", Modifier.weight(1f))
        }
        Dropdown("Status", CowStatus.entries, status, { it.label }) { status = it }
        Segmented(listOf(true, false), female, { if (it) "Female" else "Male" }) { female = it }
        DateField("Date of birth", birth, { birth = it }, allowClear = true)

        if (!status.isYoungStock) {
            SectionHeader("Breeding")
            DateField("Last calving (for days in milk)", lastCalving, { lastCalving = it }, allowClear = true)
            DateField("Expected calving", expectedCalving, { expectedCalving = it }, allowClear = true)
            DateField(
                "…or set from service / AI date (+283 days)", null,
                { served -> if (served != null) expectedCalving = served.plusDays(GESTATION_DAYS) },
            )
            SoftButton("Record calving today", {
                lastCalving = LocalDate.now(); expectedCalving = null; status = CowStatus.IN_MILK
            }, Modifier.fillMaxWidth())
        }
        if (status.isYoungStock || damId != null) {
            Dropdown("Dam (mother)", dams, dams.firstOrNull { it.id == damId }, { it.displayName }) { damId = it.id }
        }
        TextInput(notes, { notes = it }, "Notes", minLines = 2)

        PrimaryButton(
            "Save",
            onClick = {
                val cow = (original ?: Cow(tag = "", name = "", status = status)).copy(
                    name = name.trim(), tag = tag.trim(), status = status, isFemale = female, birthDate = birth,
                    lastCalvingDate = lastCalving, expectedCalvingDate = expectedCalving, damId = damId, notes = notes.trim(),
                    photo = photo,
                )
                val replaced = original?.photo?.takeIf { it != photo }
                app.launchIo { cows().upsert(cow); Photos.delete(context.applicationContext, replaced) }
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank(),
        )

        val cow = original ?: return@Column

        if (status.isYoungStock || allCalfExpenses.any { it.calfId == cow.id }) {
            val mine = allCalfExpenses.filter { it.calfId == cow.id }
            val cost = calfCost(cow.id, allCalfExpenses)
            SectionHeader("Rearing costs") { AddPill("Add", { addExpense = true }, Icons.Outlined.Add) }
            DpCard(padding = 0.dp) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.Bottom) {
                    Column(Modifier.weight(1f)) { Label("Total to date"); Serif(money(cur, cost.total), 24, Modifier.padding(top = 4.dp)) }
                    Muted("Feed ${money(cur, cost.feed)}\nUpkeep ${money(cur, cost.upkeep)}", size = 12)
                }
                mine.forEach { e ->
                    Box(Modifier.padding(horizontal = 18.dp)) { RowDivider() }
                    Row(Modifier.padding(start = 18.dp, end = 4.dp, top = 6.dp, bottom = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Strong("${e.type.label} · ${money(cur, e.amount)}")
                            Muted(e.date.pretty() + if (e.note.isNotBlank()) " · ${e.note}" else "")
                        }
                        IconButton(onClick = { app.launchIo { money().deleteCalfExpense(e) } }) {
                            Icon(Icons.Outlined.Delete, "Delete cost", tint = Dp.Muted, modifier = Modifier.padding(0.dp))
                        }
                    }
                }
            }
        }

        if (history.isNotEmpty()) {
            SectionHeader("Recent milk")
            DpCard(padding = 0.dp) {
                history.forEachIndexed { i, r ->
                    if (i > 0) Box(Modifier.padding(horizontal = 18.dp)) { RowDivider() }
                    Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Muted("${r.date.pretty()} · ${r.session.label}", Modifier.weight(1f), size = 15, color = Dp.Ink)
                        Strong("${num(r.liters)} L")
                    }
                }
            }
        }

        DangerTextButton("Delete animal", { confirmDelete = true }, Modifier.align(Alignment.CenterHorizontally))
        Muted(
            "Sold or died? Set the status to \"Sold / Gone\" instead, to keep the history.",
            Modifier.fillMaxWidth(), size = 12,
        )
        Spacer(Modifier.height(4.dp))

        if (addExpense) CalfExpenseDialog(cow.id, cur) { addExpense = false }
        if (confirmDelete) {
            AlertDialog(
                onDismissRequest = { confirmDelete = false },
                containerColor = Dp.NavBg,
                title = { Serif("Delete ${cow.name}?", 22) },
                text = { Muted("This removes the animal and its rearing costs. Milk already logged stays in the totals.", size = 15) },
                confirmButton = {
                    TextButton(onClick = {
                        val photos = listOfNotNull(cow.photo, photo).distinct()
                        app.launchIo { cows().delete(cow); photos.forEach { Photos.delete(context.applicationContext, it) } }
                        confirmDelete = false
                        onDone()
                    }) {
                        Text("Delete", color = Dp.Clay)
                    }
                },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
            )
        }
    }
}

@Composable
private fun Summary(cow: Cow, today: LocalDate, litersToday: Double) {
    val milking = cow.status == CowStatus.IN_MILK
    DpCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CowAvatar(cow.photo, cow.name, 60.dp, muted = !milking)
            Column(Modifier.padding(start = 14.dp)) {
                Serif(cow.name, 26)
                Muted(listOfNotNull(cow.tag.takeIf { it.isNotBlank() }?.let { "#$it" }, if (cow.isFemale) "Female" else "Male", cow.status.label).joinToString(" · "), size = 14)
            }
        }
        val dim = daysInMilk(cow, today)
        val due = daysUntilCalving(cow, today)
        val stats = buildList {
            if (milking && dim != null) add(Triple("In milk", "$dim d", false))
            if (milking) add(Triple("Today", "${num(litersToday)} L", false))
            if (due != null) add(Triple("Calving", if (due < 0) "${-due} d late" else "$due d", isCalvingSoon(cow, today)))
        }
        if (stats.isNotEmpty()) {
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                stats.forEach { (label, value, alert) ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                            .background(if (alert) Dp.ClayTint else Color.White)
                            .border(1.dp, if (alert) Dp.ClayBorder else Dp.Divider, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                    ) {
                        Label(label, size = 10, color = if (alert) Dp.ClayDark else Dp.Muted)
                        Serif(value, 20, Modifier.padding(top = 4.dp), color = if (alert) Dp.Clay else Dp.Green)
                    }
                }
            }
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
        title = "Add rearing cost",
        eyebrow = "Young stock",
        onDismiss = onDismiss,
        saveEnabled = value != null && value > 0,
        onSave = {
            app.launchIo { money().insertCalfExpense(CalfExpense(0, calfId, date, type, value!!, note.trim())) }
            onDismiss()
        },
    ) {
        Segmented(CalfCostType.entries, type, { it.label }) { type = it }
        NumberField(amount, { amount = it }, "Amount", suffix = cur)
        DateField("Date", date, { if (it != null) date = it })
        TextInput(note, { note = it }, "Note (e.g. milk replacer, vaccination)")
    }
}
