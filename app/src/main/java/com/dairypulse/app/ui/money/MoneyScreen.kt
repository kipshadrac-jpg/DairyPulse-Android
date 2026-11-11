package com.dairypulse.app.ui.money

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Categories
import com.dairypulse.app.data.Transaction
import com.dairypulse.app.data.TxType
import com.dairypulse.app.domain.ProfitAndLoss
import com.dairypulse.app.domain.profitAndLoss
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
import com.dairypulse.app.ui.theme.FarmGreen
import com.dairypulse.app.ui.theme.FarmGreenLight
import com.dairypulse.app.ui.theme.LossRed
import java.time.LocalDate

enum class PnlPeriod(val label: String, val days: Long) { TODAY("Today", 1), WEEK("7 Days", 7), MONTH("30 Days", 30) }

@Composable
fun MoneyScreen() {
    val app = rememberApp()
    val context = LocalContext.current
    val settings by app.settings.settings.collectAsState()
    val cur = settings.currency
    var period by rememberSaveable { mutableStateOf(PnlPeriod.MONTH) }
    val to = remember { LocalDate.now() }
    val from = to.minusDays(period.days - 1)

    // Load the longest window once and slice it per period in memory.
    val windowStart = to.minusDays(PnlPeriod.MONTH.days - 1)
    val milk by remember { app.db.milk().between(windowStart, to) }.collectAsStateWithLifecycle(emptyList())
    val feed by remember { app.db.feed().recordsBetween(windowStart, to) }.collectAsStateWithLifecycle(emptyList())
    val overheads by remember { app.db.money().overheads() }.collectAsStateWithLifecycle(emptyList())
    val txs by remember { app.db.money().transactionsBetween(windowStart, to) }.collectAsStateWithLifecycle(emptyList())
    val calf by remember { app.db.money().calfExpenses() }.collectAsStateWithLifecycle(emptyList())
    var editTx by remember { mutableStateOf<Transaction?>(null) }

    val pnl = profitAndLoss(from, to, milk, feed, overheads, txs, calf)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center) {
            ChipRow(PnlPeriod.entries, period, { it.label }) { period = it }
        }
        Spacer(Modifier.height(8.dp))
        Column(
            Modifier.fillMaxWidth().background(if (pnl.netProfit < 0) LossRed else FarmGreenLight, RoundedCornerShape(12.dp)).padding(16.dp),
        ) {
            Text(
                (if (period == PnlPeriod.TODAY) "TODAY'S" else "${period.days}-DAY") + " NET PROFIT",
                color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp,
            )
            Text(money(cur, pnl.netProfit), color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(
                "Margin: ${num(pnl.marginPct)}% • Profit/L: ${money(cur, pnl.profitPerLiter)}",
                color = Color.White, fontSize = 13.sp,
            )
        }

        SectionTitle("P&L Breakdown (${period.label})")
        InfoCard { PnlTable(pnl, cur) }

        Spacer(Modifier.height(8.dp))
        FilledTonalButton(
            onClick = {
                runCatching { PdfExporter.export(context, settings, pnl, period.label) }
                    .onSuccess { uri ->
                        val share = Intent(Intent.ACTION_SEND).apply {
                            type = "application/pdf"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, "${settings.farmName} P&L – ${period.label}")
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        }
                        context.startActivity(Intent.createChooser(share, "Share or save the PDF report"))
                    }
                    .onFailure { Toast.makeText(context, "Could not create PDF: ${it.message}", Toast.LENGTH_LONG).show() }
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Icon(Icons.Default.PictureAsPdf, null); Text("  Export ${period.label} PDF Report") }

        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Other Income & Expenses", Modifier.weight(1f))
        }
        Row {
            OutlinedButton(onClick = { editTx = Transaction(date = to, type = TxType.EXPENSE, category = Categories.VET, amount = 0.0) }, Modifier.weight(1f)) { Text("+ Expense") }
            Spacer(Modifier.padding(4.dp))
            OutlinedButton(onClick = { editTx = Transaction(date = to, type = TxType.INCOME, category = Categories.income.first(), amount = 0.0) }, Modifier.weight(1f)) { Text("+ Income") }
        }
        val shownTx = txs.filter { !it.date.isBefore(from) }
        if (shownTx.isEmpty()) {
            Text(
                "One-off costs (vet, repairs, transport) and extra income (animal or manure sales). Milk sales come from the milking log.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        shownTx.forEach { t ->
            InfoCard(onClick = { editTx = t }) {
                Row {
                    Column(Modifier.weight(1f)) {
                        Text(t.category, fontWeight = FontWeight.SemiBold)
                        Text(t.date.pretty() + if (t.note.isNotBlank()) " • ${t.note}" else "", style = MaterialTheme.typography.bodySmall)
                    }
                    Text(
                        (if (t.type == TxType.EXPENSE) "- " else "+ ") + money(cur, t.amount),
                        color = if (t.type == TxType.EXPENSE) LossRed else FarmGreen,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }

    editTx?.let { TransactionDialog(it, cur) { editTx = null } }
}

@Composable
private fun PnlTable(pnl: ProfitAndLoss, cur: String) {
    @Composable
    fun line(label: String, amount: Double, negative: Boolean = false, bold: Boolean = false) {
        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text(label, Modifier.weight(1f), fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)
            Text(
                (if (negative) "- " else "") + money(cur, amount),
                color = when {
                    negative -> LossRed
                    bold && amount < 0 -> LossRed
                    bold -> FarmGreen
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (bold || !negative) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
    line("Gross Milk Sales (${num(pnl.milkLiters)} L)", pnl.milkSales)
    pnl.otherIncome.forEach { line(it.label, it.amount) }
    HorizontalDivider()
    line("- Feed Expenses", pnl.feedCost, negative = true)
    pnl.expenses.forEach { line("- ${it.label}", it.amount, negative = true) }
    HorizontalDivider()
    line("Net Farm Profit", pnl.netProfit, bold = true)
}

@Composable
private fun TransactionDialog(initial: Transaction, cur: String, onDismiss: () -> Unit) {
    val app = rememberApp()
    var type by remember { mutableStateOf(initial.type) }
    var category by remember { mutableStateOf(initial.category) }
    var amount by remember { mutableStateOf(if (initial.amount > 0) num(initial.amount, 2) else "") }
    var date by remember { mutableStateOf(initial.date) }
    var note by remember { mutableStateOf(initial.note) }
    val categories = if (type == TxType.EXPENSE) Categories.expense else Categories.income
    val value = amount.toAmount()
    FormDialog(
        title = if (initial.id == 0L) "New ${if (type == TxType.EXPENSE) "Expense" else "Income"}" else "Edit Entry",
        onDismiss = onDismiss,
        saveEnabled = value != null && value > 0,
        onSave = {
            app.launchIo { money().upsertTransaction(initial.copy(type = type, category = category, amount = value!!, date = date, note = note.trim())) }
            onDismiss()
        },
        onDelete = if (initial.id != 0L) ({ app.launchIo { money().deleteTransaction(initial) }; onDismiss() }) else null,
    ) {
        ChipRow(TxType.entries, type, { if (it == TxType.EXPENSE) "Expense" else "Income" }) {
            type = it
            category = (if (it == TxType.EXPENSE) Categories.expense else Categories.income).first()
        }
        Dropdown("Category", categories, category, { it }) { category = it }
        NumberField(amount, { amount = it }, "Amount", suffix = cur)
        DateField("Date", date, { if (it != null) date = it })
        TextInput(note, { note = it }, "Note")
    }
}
