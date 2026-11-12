package com.dairypulse.app.ui.feed

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Formulation
import com.dairypulse.app.data.FormulationItem
import com.dairypulse.app.domain.formulationCost
import com.dairypulse.app.ui.common.BarColors
import com.dairypulse.app.ui.common.CompactNumber
import com.dairypulse.app.ui.common.DangerTextButton
import com.dairypulse.app.ui.common.DpCard
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.HeroCard
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.PrimaryButton
import com.dairypulse.app.ui.common.RowDivider
import com.dairypulse.app.ui.common.SectionHeader
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.SplitBar
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rate
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.theme.Dp
import com.dairypulse.app.ui.theme.Manrope

private class Line(ingredientId: Long?, kg: String) {
    var ingredientId by mutableStateOf(ingredientId)
    var kg by mutableStateOf(kg)
}

/** Recipe builder: pick ingredients and kg per batch; the cost per kg updates live. */
@Composable
fun FormulationScreen(formulationId: Long, onDone: () -> Unit) {
    val app = rememberApp()
    val cur = app.settings.settings.value.currency
    val ingredients by remember { app.db.feed().ingredients() }.collectAsStateWithLifecycle(emptyList())
    var existing by remember { mutableStateOf<Formulation?>(null) }
    var name by remember { mutableStateOf("") }
    val lines = remember { mutableStateListOf<Line>() }

    LaunchedEffect(formulationId) {
        if (formulationId != 0L) {
            existing = app.db.feed().formulation(formulationId)
            name = existing?.name ?: ""
            lines.clear()
            app.db.feed().itemsFor(formulationId).forEach { lines.add(Line(it.ingredientId, num(it.kg, 2))) }
        }
        if (lines.isEmpty()) lines.add(Line(null, ""))
    }

    val byId = ingredients.associateBy { it.id }
    val colorOf = ingredients.mapIndexed { i, ing -> ing.id to BarColors[i % BarColors.size] }.toMap()
    val items = lines.mapNotNull { l ->
        val id = l.ingredientId ?: return@mapNotNull null
        val kg = l.kg.toAmount()?.takeIf { it > 0 } ?: return@mapNotNull null
        FormulationItem(0, formulationId, id, kg)
    }
    val cost = formulationCost(items, byId)

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        TextInput(name, { name = it }, "Ration name (e.g. Lactation High Yield)")

        HeroCard {
            Text("UNIT COST", style = TextStyle(fontFamily = Manrope, fontWeight = FontWeight.Bold, fontSize = 11.sp, letterSpacing = 0.16.em, color = Color.White.copy(alpha = 0.85f)))
            Serif("${rate(cur, cost.costPerKg)} / kg", 34, Modifier.padding(top = 6.dp, bottom = 4.dp), color = Color.White)
            Text(
                "Batch ${num(cost.batchKg)} kg · costs ${money(cur, cost.batchCost)}",
                style = TextStyle(fontFamily = Manrope, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = Color.White.copy(alpha = 0.9f)),
            )
            if (items.isNotEmpty()) {
                SplitBar(items.map { it.kg.toFloat() to (colorOf[it.ingredientId] ?: Color.White) }, Modifier.padding(top = 16.dp))
            }
        }

        SectionHeader("Ingredients per batch")
        if (ingredients.isEmpty()) Muted("Add ingredients in the Feed tab first.", color = Dp.Clay)
        DpCard(padding = 0.dp) {
            lines.forEachIndexed { i, line ->
                if (i > 0) Box(Modifier.padding(horizontal = 16.dp)) { RowDivider() }
                Row(Modifier.padding(start = 16.dp, end = 4.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(line.ingredientId?.let { colorOf[it] } ?: Dp.InputBorder))
                    Spacer(Modifier.width(10.dp))
                    Box(Modifier.weight(1f)) {
                        Dropdown("Ingredient ${i + 1}", ingredients, line.ingredientId?.let(byId::get), { it.name }) { line.ingredientId = it.id }
                    }
                    Spacer(Modifier.width(8.dp))
                    CompactNumber(line.kg, { line.kg = it }, "Kilograms of ingredient ${i + 1}", "kg", Modifier.width(100.dp).padding(top = 22.dp))
                    IconButton(onClick = { lines.remove(line) }, modifier = Modifier.padding(top = 22.dp)) {
                        Icon(Icons.Outlined.Close, "Remove ingredient ${i + 1}", tint = Dp.Muted, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
        Surface(
            onClick = { lines.add(Line(null, "")) },
            shape = RoundedCornerShape(16.dp), color = Color.Transparent,
            border = BorderStroke(1.5.dp, Color(0xFFB9B09C)),
            modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        ) {
            Row(horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Add, null, tint = Dp.GreenDark)
                Strong("  Add ingredient", color = Dp.GreenDark, weight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(4.dp))
        PrimaryButton(
            "Save ration",
            onClick = {
                val f = (existing ?: Formulation(name = "")).copy(name = name.trim())
                app.launchIo { feed().saveFormulation(f, items) }
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = name.isNotBlank() && items.isNotEmpty(),
        )
        existing?.let { f ->
            DangerTextButton("Delete ration", { app.launchIo { feed().deleteFormulation(f) }; onDone() }, Modifier.align(Alignment.CenterHorizontally))
        }
    }
}
