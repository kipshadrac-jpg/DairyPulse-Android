package com.dairypulse.app.domain

import com.dairypulse.app.data.CalfCostType
import com.dairypulse.app.data.CalfExpense
import com.dairypulse.app.data.Cow
import com.dairypulse.app.data.FeedRecord
import com.dairypulse.app.data.FormulationItem
import com.dairypulse.app.data.Ingredient
import com.dairypulse.app.data.MilkRecord
import com.dairypulse.app.data.Overhead
import com.dairypulse.app.data.Recurrence
import com.dairypulse.app.data.Session
import com.dairypulse.app.data.Transaction
import com.dairypulse.app.data.TxType
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** Days before the expected calving date when a cow gets the red "due" warning. */
const val CALVING_WARNING_DAYS = 21L

/** Typical dry-off point: about 60 days before calving. */
const val DRY_OFF_DAYS_BEFORE_CALVING = 60L

data class FormulationCost(val batchKg: Double, val batchCost: Double) {
    val costPerKg get() = if (batchKg > 0) batchCost / batchKg else 0.0
}

fun formulationCost(items: List<FormulationItem>, ingredients: Map<Long, Ingredient>): FormulationCost {
    var kg = 0.0
    var cost = 0.0
    for (item in items) {
        val ingredient = ingredients[item.ingredientId] ?: continue
        kg += item.kg
        cost += item.kg * ingredient.pricePerKg
    }
    return FormulationCost(kg, cost)
}

/** The share of an overhead that falls on one day. Monthly costs are split over that month's days. */
fun Overhead.costOn(day: LocalDate): Double {
    if (day.isBefore(startDate)) return 0.0
    if (endDate != null && day.isAfter(endDate)) return 0.0
    return when (recurrence) {
        Recurrence.DAILY -> amount
        Recurrence.MONTHLY -> amount / day.lengthOfMonth()
    }
}

fun Overhead.dailyEquivalent(day: LocalDate = LocalDate.now()): Double = when (recurrence) {
    Recurrence.DAILY -> amount
    Recurrence.MONTHLY -> amount / day.lengthOfMonth()
}

fun daysInMilk(cow: Cow, today: LocalDate): Long? =
    cow.lastCalvingDate?.let { ChronoUnit.DAYS.between(it, today) }

fun daysUntilCalving(cow: Cow, today: LocalDate): Long? =
    cow.expectedCalvingDate?.let { ChronoUnit.DAYS.between(today, it) }

fun isCalvingSoon(cow: Cow, today: LocalDate): Boolean =
    daysUntilCalving(cow, today)?.let { it <= CALVING_WARNING_DAYS } ?: false

data class SessionRow(val session: Session, val liters: Double, val vsMorningPct: Double?)

/** Litres milked per session (before any milk is fed to calves), each compared with the morning baseline. */
fun sessionMatrix(records: List<MilkRecord>): List<SessionRow> {
    val milked = records.filter { !it.forCalves }
    val totals = Session.entries.associateWith { s -> milked.filter { it.session == s }.sumOf { it.liters } }
    val morning = totals.getValue(Session.MORNING)
    return Session.entries.map { s ->
        val liters = totals.getValue(s)
        val pct = when {
            s == Session.MORNING -> null
            morning <= 0.0 || liters <= 0.0 -> null // not logged yet
            else -> (liters - morning) / morning * 100.0
        }
        SessionRow(s, liters, pct)
    }
}

data class PnlLine(val label: String, val amount: Double)

data class ProfitAndLoss(
    val from: LocalDate,
    val to: LocalDate,
    /** Litres sold: milked minus what was fed to calves. */
    val milkLiters: Double,
    val milkSales: Double,
    val otherIncome: List<PnlLine>,
    val feedCost: Double,
    val expenses: List<PnlLine>,
    val milkToCalves: Double = 0.0,
) {
    val milkProduced get() = milkLiters + milkToCalves
    val totalRevenue get() = milkSales + otherIncome.sumOf { it.amount }
    val totalExpenses get() = feedCost + expenses.sumOf { it.amount }
    val netProfit get() = totalRevenue - totalExpenses
    val marginPct get() = if (totalRevenue > 0) netProfit / totalRevenue * 100.0 else 0.0
    val profitPerLiter get() = if (milkLiters > 0) netProfit / milkLiters else 0.0
    val otherCosts get() = expenses.sumOf { it.amount }
}

/**
 * Builds the profit & loss for the inclusive date range. All inputs may contain records
 * outside the range; they are filtered here.
 */
fun profitAndLoss(
    from: LocalDate,
    to: LocalDate,
    milk: List<MilkRecord>,
    feed: List<FeedRecord>,
    overheads: List<Overhead>,
    transactions: List<Transaction>,
    calfExpenses: List<CalfExpense>,
): ProfitAndLoss {
    fun LocalDate.inRange() = !isBefore(from) && !isAfter(to)

    val milkIn = milk.filter { it.date.inRange() }
    val txIn = transactions.filter { it.date.inRange() }

    val expenseByCategory = linkedMapOf<String, Double>()
    var day = from
    while (!day.isAfter(to)) {
        for (o in overheads) {
            val c = o.costOn(day)
            if (c != 0.0) expenseByCategory.merge(o.category, c, Double::plus)
        }
        day = day.plusDays(1)
    }
    txIn.filter { it.type == TxType.EXPENSE }
        .forEach { expenseByCategory.merge(it.category, it.amount, Double::plus) }
    val calfCost = calfExpenses.filter { it.date.inRange() }.sumOf { it.amount }
    if (calfCost != 0.0) expenseByCategory.merge("Calf Rearing", calfCost, Double::plus)

    val incomeByCategory = linkedMapOf<String, Double>()
    txIn.filter { it.type == TxType.INCOME }
        .forEach { incomeByCategory.merge(it.category, it.amount, Double::plus) }

    // Milk fed to calves is logged as its own record and comes off the litres and value sold.
    fun MilkRecord.signed() = if (forCalves) -liters else liters
    return ProfitAndLoss(
        from = from,
        to = to,
        milkLiters = milkIn.sumOf { it.signed() },
        milkSales = milkIn.sumOf { it.signed() * it.pricePerLiter },
        milkToCalves = milkIn.filter { it.forCalves }.sumOf { it.liters },
        otherIncome = incomeByCategory.map { PnlLine(it.key, it.value) },
        feedCost = feed.filter { it.date.inRange() }.sumOf { it.cost },
        expenses = expenseByCategory.map { PnlLine(it.key, it.value) }.sortedByDescending { it.amount },
    )
}

data class CalfCost(val feed: Double, val upkeep: Double) {
    val total get() = feed + upkeep
}

fun calfCost(calfId: Long, expenses: List<CalfExpense>): CalfCost {
    val mine = expenses.filter { it.calfId == calfId }
    return CalfCost(
        feed = mine.filter { it.type == CalfCostType.FEED }.sumOf { it.amount },
        upkeep = mine.filter { it.type == CalfCostType.UPKEEP }.sumOf { it.amount },
    )
}
