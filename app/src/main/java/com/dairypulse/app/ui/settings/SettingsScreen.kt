package com.dairypulse.app.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dairypulse.app.data.FarmSettings
import com.dairypulse.app.ui.common.InfoCard
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.SectionTitle
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.toAmount

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val app = rememberApp()
    val context = LocalContext.current
    val current = app.settings.settings.value
    var farmName by remember { mutableStateOf(current.farmName) }
    var currency by remember { mutableStateOf(current.currency) }
    var price by remember { mutableStateOf(num(current.milkPricePerLiter, 2)) }
    val priceValue = price.toAmount()

    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionTitle("Farm")
        TextInput(farmName, { farmName = it }, "Farm name (shown on PDF reports)")
        TextInput(currency, { currency = it.uppercase().take(4) }, "Currency")
        NumberField(price, { price = it }, "Default milk price per litre", suffix = currency)
        Text(
            "Changing the price only affects milk logged from now on.",
            style = MaterialTheme.typography.bodySmall,
        )
        Button(
            enabled = farmName.isNotBlank() && currency.isNotBlank() && priceValue != null && priceValue > 0,
            onClick = {
                app.settings.save(FarmSettings(farmName.trim(), currency.trim(), priceValue!!))
                onDone()
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Save") }

        SectionTitle("Backup")
        InfoCard {
            Text("Automatic Google backup", fontWeight = FontWeight.Bold)
            Text(
                "All farm records are stored only on this phone. Android backs them up automatically to the " +
                    "Google account on this phone about once a day, when the phone is charging, idle and on Wi-Fi.\n\n" +
                    "When you set up a new phone with the same Google account (or reinstall DairyPulse), your " +
                    "records are restored automatically.\n\n" +
                    "Make sure \"Backup by Google One\" / \"Back up to Google Drive\" is switched on in your phone's settings.",
                style = MaterialTheme.typography.bodySmall,
            )
        }
        OutlinedButton(
            onClick = {
                // The backup page has no standard shortcut on every phone; fall back to main settings.
                val opened = listOf(Intent("android.settings.BACKUP_AND_RESET_SETTINGS"), Intent(Settings.ACTION_SETTINGS))
                    .any { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess }
                if (!opened) return@OutlinedButton
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Open phone backup settings") }
    }
}
