package com.dairypulse.app.ui.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Cow
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.data.FarmTask
import com.dairypulse.app.domain.DRY_OFF_DAYS_BEFORE_CALVING
import com.dairypulse.app.domain.daysUntilCalving
import com.dairypulse.app.domain.isCalvingSoon
import com.dairypulse.app.ui.common.AlertBanner
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.DpCard
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.PageIntro
import com.dairypulse.app.ui.common.RowDivider
import com.dairypulse.app.ui.common.SectionHeader
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.pretty
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.theme.Dp
import com.dairypulse.app.ui.theme.Manrope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Upcoming calvings and dry-offs worked out from the herd, plus the farmer's own tasks. */
@Composable
fun ScheduleScreen(onOpenCow: (Long) -> Unit) {
    val app = rememberApp()
    val today = remember { LocalDate.now() }
    val cows by remember { app.db.cows().all() }.collectAsStateWithLifecycle(emptyList())
    val tasks by remember { app.db.tasks().all() }.collectAsStateWithLifecycle(emptyList())
    var editing by remember { mutableStateOf<FarmTask?>(null) }
    var showDone by remember { mutableStateOf(false) }

    val active = cows.filter { it.status != CowStatus.SOLD }
    val calvings = active.mapNotNull { c -> daysUntilCalving(c, today)?.takeIf { it <= 90 }?.let { c to it } }.sortedBy { it.second }
    val dryOffs = active.filter { it.status == CowStatus.IN_MILK }
        .mapNotNull { c -> daysUntilCalving(c, today)?.let { c to it - DRY_OFF_DAYS_BEFORE_CALVING } }
        .filter { it.second <= 14 }
        .sortedBy { it.second }
    val pending = tasks.filter { !it.done }
    val done = tasks.filter { it.done }
    val cowsById = cows.associateBy { it.id }

    fun plural(n: Int, word: String) = "$n $word${if (n == 1) "" else "s"}"

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            PageIntro(
                "What's coming up", "Schedule",
                "${plural(calvings.size, "calving")}, ${plural(dryOffs.size, "dry-off")} and ${plural(pending.size, "task")} ahead.",
            )

            SectionHeader("Calvings · next 90 days")
            if (calvings.isEmpty()) {
                Muted("No calvings expected. Set expected calving dates in each cow's profile.")
            } else {
                DpCard(padding = 0.dp) {
                    calvings.forEachIndexed { i, (cow, days) ->
                        if (i > 0) Box(Modifier.padding(horizontal = 16.dp)) { RowDivider() }
                        val urgent = isCalvingSoon(cow, today)
                        Row(
                            Modifier.fillMaxWidth().clickable { onOpenCow(cow.id) }.padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            DateBlock(today.plusDays(days), urgent)
                            Column(Modifier.weight(1f).padding(start = 14.dp)) {
                                Strong(cow.displayName)
                                Muted(
                                    when {
                                        days < 0 -> "Overdue by ${-days} days"
                                        days == 0L -> "Due today"
                                        urgent -> "In $days days · get the calving pen ready"
                                        else -> "In $days days"
                                    },
                                    color = if (urgent) Dp.Clay else Dp.Muted,
                                    weight = if (urgent) FontWeight.Bold else FontWeight.Medium,
                                )
                            }
                            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = Dp.Muted)
                        }
                    }
                }
            }

            if (dryOffs.isNotEmpty()) {
                SectionHeader("Dry-off due")
                dryOffs.forEach { (cow, days) ->
                    AlertBanner(
                        "Dry off ${cow.name}: " + when {
                            days < 0 -> "overdue by ${-days} days"
                            days == 0L -> "due today"
                            else -> "in $days days"
                        } + " (≈60 days before calving)",
                        onClick = { onOpenCow(cow.id) },
                    )
                }
            }

            SectionHeader("Tasks") { Label("${pending.size} pending") }
            if (pending.isEmpty()) {
                Muted("No pending tasks. Add vaccinations, deworming, AI, vet visits…")
            } else {
                TaskCard(pending, cowsById, today, { editing = it }) { t, v -> app.launchIo { tasks().upsert(t.copy(done = v)) } }
            }
            if (done.isNotEmpty()) {
                TextButton(onClick = { showDone = !showDone }, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Strong(if (showDone) "Hide completed" else "Show ${done.size} completed", size = 14, color = Dp.Green)
                }
                if (showDone) TaskCard(done, cowsById, today, { editing = it }) { t, v -> app.launchIo { tasks().upsert(t.copy(done = v)) } }
            }
            Spacer(Modifier.height(72.dp))
        }
        ExtendedFloatingActionButton(
            onClick = { editing = FarmTask(title = "", dueDate = today) },
            icon = { Icon(Icons.Outlined.Add, null) },
            text = { Strong("Add task", color = Color.White) },
            containerColor = Dp.Green, contentColor = Color.White,
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editing?.let { TaskDialog(it, active) { editing = null } }
}

private val monthFmt = DateTimeFormatter.ofPattern("MMM", Locale.US)

@Composable
private fun DateBlock(date: LocalDate, urgent: Boolean) {
    Column(
        Modifier.width(52.dp).clip(RoundedCornerShape(14.dp)).background(if (urgent) Dp.ClayTint else Dp.Track).padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Serif("${date.dayOfMonth}", 20, color = if (urgent) Dp.Clay else Dp.Heading)
        androidx.compose.material3.Text(
            date.format(monthFmt).uppercase(),
            style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = if (urgent) Dp.Clay else Dp.Muted),
        )
    }
}

@Composable
private fun TaskCard(
    list: List<FarmTask>,
    cows: Map<Long, Cow>,
    today: LocalDate,
    onEdit: (FarmTask) -> Unit,
    onCheck: (FarmTask, Boolean) -> Unit,
) {
    DpCard(padding = 0.dp) {
        list.forEachIndexed { i, task ->
            if (i > 0) Box(Modifier.padding(horizontal = 16.dp)) { RowDivider() }
            val overdue = !task.done && task.dueDate.isBefore(today)
            val whenText = when (task.dueDate) {
                today -> "Today"
                today.plusDays(1) -> "Tomorrow"
                else -> null
            }
            Row(
                Modifier.fillMaxWidth().clickable { onEdit(task) }.padding(start = 4.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = task.done, onCheckedChange = { onCheck(task, it) },
                    colors = CheckboxDefaults.colors(checkedColor = Dp.Green, uncheckedColor = Color(0xFFB9B09C)),
                )
                Column(Modifier.weight(1f).padding(vertical = 8.dp)) {
                    androidx.compose.material3.Text(
                        task.title,
                        style = TextStyle(
                            fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            color = if (task.done) Dp.Muted else Dp.Ink,
                            textDecoration = if (task.done) TextDecoration.LineThrough else null,
                        ),
                    )
                    Muted(
                        listOfNotNull(
                            if (overdue) "Overdue" else whenText,
                            task.dueDate.pretty(),
                            task.cowId?.let(cows::get)?.name ?: "Whole farm",
                        ).joinToString(" · "),
                        color = if (overdue) Dp.Clay else Dp.Muted,
                        weight = if (overdue) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
private fun TaskDialog(initial: FarmTask, cows: List<Cow>, onDismiss: () -> Unit) {
    val app = rememberApp()
    var title by remember { mutableStateOf(initial.title) }
    var due by remember { mutableStateOf(initial.dueDate) }
    var cowId by remember { mutableStateOf(initial.cowId) }
    var notes by remember { mutableStateOf(initial.notes) }
    val noCow = Cow(id = -1, tag = "", name = "Whole farm", status = CowStatus.IN_MILK)
    FormDialog(
        title = if (initial.id == 0L) "New task" else "Edit task",
        eyebrow = "Schedule",
        onDismiss = onDismiss,
        saveEnabled = title.isNotBlank(),
        onSave = {
            app.launchIo { tasks().upsert(initial.copy(title = title.trim(), dueDate = due, cowId = cowId, notes = notes.trim())) }
            onDismiss()
        },
        onDelete = if (initial.id != 0L) ({ app.launchIo { tasks().delete(initial) }; onDismiss() }) else null,
    ) {
        TextInput(title, { title = it }, "Task (e.g. Deworm calves)")
        DateField("Due date", due, { if (it != null) due = it })
        Dropdown("Animal", listOf(noCow) + cows, cows.firstOrNull { it.id == cowId } ?: noCow, { it.displayName }) {
            cowId = it.id.takeIf { id -> id > 0 }
        }
        TextInput(notes, { notes = it }, "Notes")
    }
}
