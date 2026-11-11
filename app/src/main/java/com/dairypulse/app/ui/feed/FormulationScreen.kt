package com.dairypulse.app.ui.feed

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.dairypulse.app.data.Formulation
import com.dairypulse.app.data.FormulationItem
import com.dairypulse.app.domain.formulationCost
import com.dairypulse.app.ui.common.Dropdown
import com.dairypulse.app.ui.common.InfoCard
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.SectionTitle
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.money
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.theme.FarmGreen

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
    val items = lines.mapNotNull { l ->
        val id = l.ingredientId ?: return@mapNotNull null
        val kg = l.kg.toAmount()?.takeIf { it > 0 } ?: return@mapNotNull null
        FormulationItem(0, formulationId, id, kg)
    }
    val cost = formulationCost(items, byId)

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
        TextInput(name, { name = it }, "Ration name (e.g. Lactation High Yield)")
        SectionTitle("Ingredients per batch")
        if (ingredients.isEmpty()) {
            Text("Add ingredients in the Feed tab first.", color = MaterialTheme.colorScheme.error)
        }
        lines.forEach { line ->
            Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Dropdown(
                    "Ingredient", ingredients, line.ingredientId?.let(byId::get), { it.name }, Modifier.weight(1.4f),
                ) { line.ingredientId = it.id }
                Spacer(Modifier.width(8.dp))
                NumberField(line.kg, { line.kg = it }, "kg", Modifier.weight(1f))
                IconButton(onClick = { lines.remove(line) }) { Icon(Icons.Default.Close, "Remove") }
            }
        }
        OutlinedButton(onClick = { lines.add(Line(null, "")) }) { Text("+ Add ingredient") }

        Spacer(Modifier.height(12.dp))
        InfoCard(accent = FarmGreen) {
            Text("Batch: ${num(cost.batchKg)} kg • Cost: ${money(cur, cost.batchCost)}")
            Text("Unit Cost: ${money(cur, cost.costPerKg)} / kg", color = FarmGreen, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(12.dp))
        Button(
            onClick = {
                val f = (existing ?: Formulation(name = "")).copy(name = name.trim())
                app.launchIo { feed().saveFormulation(f, items) }
                onDone()
            },
            enabled = name.isNotBlank() && items.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save Ration") }
        existing?.let { f ->
            OutlinedButton(
                onClick = { app.launchIo { feed().deleteFormulation(f) }; onDone() },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Delete Ration", color = MaterialTheme.colorScheme.error) }
        }
    }
}
