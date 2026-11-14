package com.dairypulse.app.domain

import com.dairypulse.app.data.CalfCostType
import com.dairypulse.app.data.CalfExpense
import com.dairypulse.app.data.FeedRecord
import com.dairypulse.app.data.FormulationItem
import com.dairypulse.app.data.Ingredient
import com.dairypulse.app.data.MilkRecord
import com.dairypulse.app.data.Overhead
import com.dairypulse.app.data.Recurrence
import com.dairypulse.app.data.Session
import com.dairypulse.app.data.Transaction
import com.dairypulse.app.data.TxType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class CalculationsTest {
    private val day = LocalDate.of(2026, 11, 11)

    @Test fun ingredientPricePerKgFromPack() {
        assertEquals(50.0, Ingredient(name = "Dairy Meal", packKg = 50.0, packPrice = 2500.0).pricePerKg, 1e-9)
    }

    @Test fun formulationUnitCost() {
        val ingredients = mapOf(
            1L to Ingredient(1, "Dairy Meal", 50.0, 2500.0),   // 50/kg
            2L to Ingredient(2, "Cotton Seed Cake", 1.0, 65.0), // 65/kg
        )
        val items = listOf(FormulationItem(0, 1, 1, 70.0), FormulationItem(0, 1, 2, 30.0))
        val cost = formulationCost(items, ingredients)
        assertEquals(100.0, cost.batchKg, 1e-9)
        assertEquals(70 * 50.0 + 30 * 65.0, cost.batchCost, 1e-9)
        assertEquals(54.5, cost.costPerKg, 1e-9)
    }

    @Test fun monthlyOverheadSplitsAcrossDaysOfMonth() {
        val o = Overhead(name = "Labour", category = "L", amount = 30_000.0, recurrence = Recurrence.MONTHLY, startDate = day.minusYears(1))
        assertEquals(1000.0, o.costOn(day), 1e-9)              // November: 30 days
        assertEquals(30_000.0 / 31, o.costOn(day.plusMonths(1)), 1e-9) // December: 31 days
        assertEquals(0.0, o.copy(startDate = day.plusDays(1)).costOn(day), 1e-9)
        assertEquals(0.0, o.copy(endDate = day.minusDays(1)).costOn(day), 1e-9)
    }

    @Test fun sessionMatrixComparesWithMorning() {
        val records = listOf(
            MilkRecord(0, day, Session.MORNING, null, 320.0, 50.0),
            MilkRecord(0, day, Session.MIDDAY, null, 210.0, 50.0),
            MilkRecord(0, day, Session.EVENING, null, 240.0, 50.0),
        )
        val rows = sessionMatrix(records)
        assertNull(rows[0].vsMorningPct)
        assertEquals(-34.375, rows[1].vsMorningPct!!, 1e-9)
        assertEquals(-25.0, rows[2].vsMorningPct!!, 1e-9)
        // A session not logged yet shows no variance rather than -100%.
        assertNull(sessionMatrix(records.dropLast(1))[2].vsMorningPct)
    }

    @Test fun milkForCalvesComesOffSales() {
        val milk = listOf(
            MilkRecord(0, day, Session.MORNING, 1, 12.0, 70.0),
            MilkRecord(0, day, Session.MORNING, null, 3.0, 70.0),
            MilkRecord(0, day, Session.MORNING, null, 4.0, 70.0, forCalves = true),
        )
        val pnl = profitAndLoss(day, day, milk, emptyList(), emptyList(), emptyList(), emptyList())
        assertEquals(15.0, pnl.milkProduced, 1e-9)
        assertEquals(4.0, pnl.milkToCalves, 1e-9)
        assertEquals(11.0, pnl.milkLiters, 1e-9)
        assertEquals(11 * 70.0, pnl.milkSales, 1e-9)
        // The session chart shows what was milked, before calves are fed.
        assertEquals(15.0, sessionMatrix(milk)[0].liters, 1e-9)
    }

    @Test fun profitAndLossAddsEverythingUp() {
        val pnl = profitAndLoss(
            from = day, to = day,
            milk = listOf(
                MilkRecord(0, day, Session.MORNING, null, 100.0, 50.0),
                MilkRecord(0, day.minusDays(1), Session.MORNING, null, 999.0, 50.0), // outside range
            ),
            feed = listOf(FeedRecord(0, day, Session.MORNING, "Mix", 40.0, 42.0)),
            overheads = listOf(Overhead(0, "Labour", "Labour & Salaries", 30_000.0, Recurrence.MONTHLY, day.minusDays(10))),
            transactions = listOf(
                Transaction(0, day, TxType.EXPENSE, "Veterinary & Health", 500.0),
                Transaction(0, day, TxType.INCOME, "Manure Sales", 200.0),
            ),
            calfExpenses = listOf(CalfExpense(0, 1, day, CalfCostType.FEED, 100.0)),
        )
        assertEquals(100.0, pnl.milkLiters, 1e-9)
        assertEquals(5000.0, pnl.milkSales, 1e-9)
        assertEquals(5200.0, pnl.totalRevenue, 1e-9)
        assertEquals(1680.0, pnl.feedCost, 1e-9)
        assertEquals(1000.0 + 500.0 + 100.0, pnl.otherCosts, 1e-9)
        assertEquals(5200.0 - 1680.0 - 1600.0, pnl.netProfit, 1e-9)
    }
}
