package com.dairypulse.app.ui.schedule

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Cow
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.data.FarmTask
import com.dairypulse.app.domain.DRY_OFF_DAYS_BEFORE_CALVING
import com.dairypulse.app.domain.daysUntilCalving
import com.dairypulse.app.domain.isCalvingSoon
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.InfoCard
import com.dairypulse.app.ui.common.SectionTitle
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.pretty
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.theme.LossRed
import java.time.LocalDate

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

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
            SectionTitle("Calvings (next 90 days)")
            if (calvings.isEmpty()) Text("No calvings expected. Set expected calving dates in each cow's profile.", style = MaterialTheme.typography.bodySmall)
            calvings.forEach { (cow, days) -> CowEvent(cow, days, "calving", isCalvingSoon(cow, today)) { onOpenCow(cow.id) } }

            if (dryOffs.isNotEmpty()) {
                SectionTitle("Dry-off due (≈60 days before calving)")
                dryOffs.forEach { (cow, days) -> CowEvent(cow, days, "dry-off", days <= 0) { onOpenCow(cow.id) } }
            }

            SectionTitle("Tasks")
            if (pending.isEmpty()) Text("No pending tasks. Add vaccinations, deworming, AI, vet visits…", style = MaterialTheme.typography.bodySmall)
            pending.forEach { t -> TaskRow(t, cowsById, today, { editing = t }) { app.launchIo { tasks().upsert(t.copy(done = it)) } } }

            if (done.isNotEmpty()) {
                TextButton(onClick = { showDone = !showDone }) { Text(if (showDone) "Hide completed (${done.size})" else "Show completed (${done.size})") }
                if (showDone) done.forEach { t -> TaskRow(t, cowsById, today, { editing = t }) { app.launchIo { tasks().upsert(t.copy(done = it)) } } }
            }
            Spacer(Modifier.padding(40.dp))
        }
        ExtendedFloatingActionButton(
            onClick = { editing = FarmTask(title = "", dueDate = today) },
            icon = { Icon(Icons.Default.Add, null) },
            text = { Text("Add Task") },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        )
    }

    editing?.let { TaskDialog(it, active) { editing = null } }
}

@Composable
private fun CowEvent(cow: Cow, days: Long, what: String, urgent: Boolean, onClick: () -> Unit) {
    InfoCard(accent = if (urgent) LossRed else null, onClick = onClick) {
        Text(cow.displayName, fontWeight = FontWeight.Bold)
        Text(
            when {
                days < 0 -> "${what.replaceFirstChar { it.uppercase() }} overdue by ${-days} days"
                days == 0L -> "${what.replaceFirstChar { it.uppercase() }} due today"
                else -> "${what.replaceFirstChar { it.uppercase() }} in $days days"
            },
            color = if (urgent) LossRed else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun TaskRow(task: FarmTask, cows: Map<Long, Cow>, today: LocalDate, onEdit: () -> Unit, onCheck: (Boolean) -> Unit) {
    val overdue = !task.done && task.dueDate.isBefore(today)
    InfoCard(onClick = onEdit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.done, onCheckedChange = onCheck)
            Column(Modifier.weight(1f)) {
                Text(
                    task.title, fontWeight = FontWeight.SemiBold,
                    textDecoration = if (task.done) TextDecoration.LineThrough else null,
                )
                Text(
                    (if (overdue) "Overdue • " else "") + task.dueDate.pretty() +
                        (task.cowId?.let(cows::get)?.let { " • ${it.displayName}" } ?: ""),
                    color = if (overdue) LossRed else MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                )
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
        title = if (initial.id == 0L) "New Task" else "Edit Task",
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
