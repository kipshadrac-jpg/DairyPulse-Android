package com.dairypulse.app.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.dairypulse.app.DairyPulseApp
import com.dairypulse.app.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** Form dialogs, run with Robolectric's default screen. */
@RunWith(RobolectricTestRunner::class)
class DialogFormTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private val app get() = rule.activity.application as DairyPulseApp

    /** Type into the Add Ingredient dialog and check the row is saved. */
    @Test
    fun addIngredientThroughDialog() {
        rule.onNodeWithText("Feed").performClick()
        rule.onAllNodesWithText("Add").onFirst().performClick()
        rule.onNode(hasSetTextAction() and hasText("Name (e.g. Dairy Meal)")).performTextInput("Dairy Meal")
        rule.onNode(hasSetTextAction() and hasText("Pack size")).performTextInput("50")
        rule.onNode(hasSetTextAction() and hasText("Pack price")).performTextInput("2500")
        rule.onNodeWithText("= 50 per kg").assertExists()
        rule.onNodeWithText("Save").performClick()
        rule.waitUntil(5_000) {
            runBlocking { app.db.feed().ingredients().first() }.any { it.name == "Dairy Meal" && it.pricePerKg == 50.0 }
        }
    }

}
