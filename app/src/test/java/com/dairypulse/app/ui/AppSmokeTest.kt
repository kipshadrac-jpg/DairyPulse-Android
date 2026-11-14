package com.dairypulse.app.ui

import android.graphics.Bitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.dairypulse.app.DairyPulseApp
import com.dairypulse.app.MainActivity
import com.dairypulse.app.data.CalfCostType
import com.dairypulse.app.data.CalfExpense
import com.dairypulse.app.data.Categories
import com.dairypulse.app.data.Cow
import com.dairypulse.app.data.FeedRecord
import com.dairypulse.app.data.Formulation
import com.dairypulse.app.data.FormulationItem
import com.dairypulse.app.data.Ingredient
import com.dairypulse.app.data.MilkRecord
import com.dairypulse.app.data.Overhead
import com.dairypulse.app.data.Recurrence
import com.dairypulse.app.data.Session
import com.dairypulse.app.data.CowStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.time.LocalDate

/** Drives the real screens on the JVM: every tab opens, and the main logging flows save data. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w400dp-h860dp-xhdpi")
class AppSmokeTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val app get() = rule.activity.application as DairyPulseApp

    private fun shot(name: String) {
        val dir = System.getProperty("screenshotDir") ?: return
        File(dir).mkdirs()
        rule.waitForIdle()
        val view = rule.activity.window.decorView
        val bmp = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        rule.runOnUiThread { view.draw(android.graphics.Canvas(bmp)) }
        File(dir, "$name.png").outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun tab(name: String) = rule.onNode(hasText(name) and isSelectable()).performClick()

    private fun waitForText(text: String) = rule.waitUntil(5_000) {
        rule.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().isNotEmpty()
    }

    @Test
    fun walkThroughAllScreens() {
        val today = LocalDate.now()
        runBlocking {
            app.db.cows().upsert(Cow(tag = "001", name = "Daisy", status = CowStatus.IN_MILK, lastCalvingDate = today.minusDays(142), expectedCalvingDate = today.plusDays(18)))
            app.db.cows().upsert(Cow(tag = "002", name = "Bossy", status = CowStatus.IN_MILK, lastCalvingDate = today.minusDays(88)))
            app.db.cows().upsert(Cow(tag = "003", name = "Rosa", status = CowStatus.DRY, expectedCalvingDate = today.plusDays(40)))
            app.db.cows().upsert(Cow(tag = "010", name = "Bella", status = CowStatus.CALF, damId = 1, birthDate = today.minusMonths(8)))
            app.db.money().insertCalfExpense(CalfExpense(0, 4, today.minusDays(30), CalfCostType.FEED, 6000.0))
            app.db.money().insertCalfExpense(CalfExpense(0, 4, today.minusDays(20), CalfCostType.UPKEEP, 2400.0))
            app.db.feed().upsertIngredient(Ingredient(name = "Dairy Meal", packKg = 50.0, packPrice = 2500.0))
            val cake = app.db.feed().upsertIngredient(Ingredient(name = "Cotton Seed Cake", packKg = 70.0, packPrice = 4550.0))
            app.db.feed().saveFormulation(Formulation(name = "Lactation High Yield"), listOf(FormulationItem(0, 0, cake, 100.0)))
            app.db.money().upsertOverhead(Overhead(name = "Farm Labour", category = Categories.LABOUR, amount = 30_000.0, recurrence = Recurrence.MONTHLY, startDate = today.minusMonths(2)))
            app.db.milk().insertAll(listOf(MilkRecord(0, today, Session.MORNING, 2, 14.0, 50.0), MilkRecord(0, today, Session.MORNING, null, 300.0, 50.0)))
            app.db.feed().insertRecord(FeedRecord(0, today, Session.MORNING, "Lactation High Yield", 120.0, 65.0))
        }

        // Today
        rule.onNodeWithText("TODAY'S NET PROFIT").assertIsDisplayed()

        // Log a milking session per cow
        rule.onNodeWithText("Log milking").performClick()
        waitForText("Daisy")
        rule.onNodeWithText("Midday").performClick()
        rule.onAllNodes(hasSetTextAction() and hasContentDescription("Litres for", substring = true)).onFirst().performTextInput("12.5")
        // Milk fed to calves comes off what is sold.
        rule.onNode(hasSetTextAction() and hasContentDescription("Milk for calves (subtracted)")).performScrollTo().performTextInput("2.5")
        waitForText("10 L")
        shot("02_log_milking")
        rule.onNodeWithText("Save midday milking").performScrollTo().performClick()
        rule.waitForIdle()
        waitForText("12.5 L")
        waitForText("2.5 L to calves")
        val saved = runBlocking { app.db.milk().forSession(today, Session.MIDDAY) }
        check(saved.single { it.forCalves }.liters == 2.5) { "calf milk not saved: $saved" }
        shot("01_today")
        tab("Feed")
        waitForText("KSh 50.00")
        shot("03_feed")

        // Herd
        tab("Herd")
        waitForText("Calving in 18 days")
        rule.onNodeWithText("Calves & young stock").assertExists()
        shot("04_herd")

        // Cow profile
        rule.onNodeWithText("Bossy").performClick()
        waitForText("Recent milk")
        shot("05_cow")
        rule.activityRule.scenario.onActivity { it.onBackPressedDispatcher.onBackPressed() }

        // Schedule
        tab("Schedule")
        waitForText("Calvings · next 90 days")
        shot("06_schedule")

        // Money
        tab("Money")
        waitForText("litres sold")
        shot("07_money")

        // Settings
        rule.onNode(androidx.compose.ui.test.hasContentDescription("Settings")).performClick()
        waitForText("Automatic Google backup")
        shot("08_settings")
    }
}
