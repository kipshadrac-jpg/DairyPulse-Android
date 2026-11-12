package com.dairypulse.app.ui.money

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Categories
import com.dairypulse.app.data.Transaction
import com.dairypulse.app.data.TxType
import com.dairypulse.app.domain.ProfitAndLoss
import com.dairypulse.app.domain.profitAndLoss
import com.dairypulse.app.ui.common.DateField
import com.dairypulse.app.ui.common.DpCard
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.FormDialog
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.OutlineButton
import com.dairypulse.app.ui.common.PageIntro
import com.dairypulse.app.ui.common.RowDivider
import com.dairypulse.app.ui.common.SectionHeader
import com.dairypulse.app.ui.common.Segmented
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.SoftButton
import com.dairypulse.app.ui.common.StatTile
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.TileRow
import com.dairypulse.app.ui.common.compact
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.pretty
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.common.whole
import com.dairypulse.app.ui.theme.Dp
import java.time.LocalDate
import kotlin.math.abs

enum class PnlPeriod(val label: String, val days: Long) { TODAY("Today", 1), WEEK("7 days", 7), MONTH("30 days", 30) }

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
    val feedKg = feed.filter { !it.date.isBefore(from) }.sumOf { it.kg }
    val feedShare = if (pnl.totalExpenses > 0) pnl.feedCost / pnl.totalExpenses * 100 else 0.0
    fun big(v: Double) = if (abs(v) >= 100_000) compact(v) else whole(v)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageIntro("Profit & loss", "Money", "Milk sales against feed, labour, machinery and transport costs.")
        Segmented(PnlPeriod.entries, period, { it.label }) { period = it }

        PnlCard(pnl, cur, period, feedKg)

        TileRow {
            StatTile("Revenue", big(pnl.totalRevenue), Dp.GreenValue, "$cur · all income", Modifier.weight(1f), valueSize = 26)
            StatTile("Total costs", big(pnl.totalExpenses), Dp.Clay, "$cur · feed + overheads", Modifier.weight(1f), valueSize = 26)
        }
        TileRow {
            StatTile("Profit / litre", num(pnl.profitPerLiter, 2), Dp.Slate, "$cur · ${num(pnl.milkLiters)} L", Modifier.weight(1f), valueSize = 26)
            StatTile("Feed share", "${num(feedShare, 0)}%", Dp.Ochre, "of all costs", Modifier.weight(1f), valueSize = 26)
        }

        SoftButton(
            "Export ${period.label} PDF report",
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
            icon = Icons.Outlined.Description,
        )

        SectionHeader("Other income & costs")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlineButton("− Expense", { editTx = Transaction(date = to, type = TxType.EXPENSE, category = Categories.VET, amount = 0.0) }, Modifier.weight(1f), color = Dp.Clay)
            OutlineButton("+ Income", { editTx = Transaction(date = to, type = TxType.INCOME, category = Categories.income.first(), amount = 0.0) }, Modifier.weight(1f), color = Dp.GreenValue)
        }
        val shownTx = txs.filter { !it.date.isBefore(from) }
        if (shownTx.isEmpty()) {
            Muted("One-off costs (vet, repairs, transport) and extra income (animal or manure sales). Milk sales come from the milking log.")
        } else {
            DpCard(padding = 0.dp) {
                shownTx.forEachIndexed { i, t ->
                    if (i > 0) Box(Modifier.padding(horizontal = 18.dp)) { RowDivider() }
                    Row(
                        Modifier.fillMaxWidth().clickable { editTx = t }.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Strong(t.category)
                            Muted(t.date.pretty() + if (t.note.isNotBlank()) " · ${t.note}" else "")
                        }
                        Strong(
                            (if (t.type == TxType.EXPENSE) "−" else "+") + whole(t.amount),
                            color = if (t.type == TxType.EXPENSE) Dp.Clay else Dp.GreenValue, weight = FontWeight.ExtraBold,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }

    editTx?.let { TransactionDialog(it, cur) { editTx = null } }
}

@Composable
private fun PnlCard(pnl: ProfitAndLoss, cur: String, period: PnlPeriod, feedKg: Double) {
    val loss = pnl.netProfit < 0
    DpCard(padding = 0.dp, radius = 26.dp) {
        Row(
            Modifier.fillMaxWidth().background(Dp.CreamDeep).padding(22.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            Column(Modifier.weight(1f)) {
                Label(if (period == PnlPeriod.TODAY) "Net · today" else "Net · last ${period.days} days")
                Serif(money(cur, pnl.netProfit), 34, Modifier.padding(top = 8.dp), color = if (loss) Dp.Clay else Dp.GreenValue)
            }
            if (pnl.totalRevenue > 0) {
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(if (loss) Dp.ClayTint else Dp.GreenPill).padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        if (loss) Icons.AutoMirrored.Outlined.TrendingDown else Icons.AutoMirrored.Outlined.TrendingUp, null,
                        tint = if (loss) Dp.Clay else Dp.GreenDark, modifier = Modifier.size(16.dp),
                    )
                    Strong(" ${num(pnl.marginPct)}%", size = 13, color = if (loss) Dp.Clay else Dp.GreenDark, weight = FontWeight.ExtraBold)
                }
            }
        }
        PnlLineRow("Milk sales", "${num(pnl.milkLiters)} litres sold", money(cur, pnl.milkSales), Dp.GreenValue)
        pnl.otherIncome.forEach { PnlLineRow(it.label, "other income", money(cur, it.amount), Dp.GreenValue) }
        PnlLineRow("Feed cost", "${num(feedKg)} kg fed", money(cur, -pnl.feedCost), Dp.Clay)
        pnl.expenses.forEach { PnlLineRow(it.label, null, money(cur, -it.amount), Dp.Clay) }
    }
}

@Composable
private fun PnlLineRow(title: String, caption: String?, value: String, color: androidx.compose.ui.graphics.Color) {
    RowDivider()
    Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Strong(title, size = 16)
            if (caption != null) Muted(caption)
        }
        Strong(value, color = color, weight = FontWeight.ExtraBold)
    }
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
        title = if (initial.id == 0L) (if (type == TxType.EXPENSE) "New expense" else "New income") else "Edit entry",
        eyebrow = "Money",
        onDismiss = onDismiss,
        saveEnabled = value != null && value > 0,
        onSave = {
            app.launchIo { money().upsertTransaction(initial.copy(type = type, category = category, amount = value!!, date = date, note = note.trim())) }
            onDismiss()
        },
        onDelete = if (initial.id != 0L) ({ app.launchIo { money().deleteTransaction(initial) }; onDismiss() }) else null,
    ) {
        Segmented(TxType.entries, type, { if (it == TxType.EXPENSE) "Expense" else "Income" }) {
            type = it
            category = (if (it == TxType.EXPENSE) Categories.expense else Categories.income).first()
        }
        Dropdown("Category", categories, category, { it }) { category = it }
        NumberField(amount, { amount = it }, "Amount", suffix = cur)
        DateField("Date", date, { if (it != null) date = it })
        TextInput(note, { note = it }, "Note")
    }
}
