package com.dairypulse.app.ui.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Landscape
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.dairypulse.app.data.Photos
import com.dairypulse.app.ui.common.DpCard
import com.dairypulse.app.ui.common.Label
import com.dairypulse.app.ui.common.Muted
import com.dairypulse.app.ui.common.NumberField
import com.dairypulse.app.ui.common.OutlineButton
import com.dairypulse.app.ui.common.PhotoActions
import com.dairypulse.app.ui.common.PrimaryButton
import com.dairypulse.app.ui.common.Serif
import com.dairypulse.app.ui.common.Strong
import com.dairypulse.app.ui.common.TextInput
import com.dairypulse.app.ui.common.num
import com.dairypulse.app.ui.common.rememberApp
import com.dairypulse.app.ui.common.rememberPhoto
import com.dairypulse.app.ui.common.rememberPhotoPicker
import com.dairypulse.app.ui.common.toAmount
import com.dairypulse.app.ui.theme.Dp

private val PanelText = Color(0xFF2E4636)

@Composable
fun SettingsScreen(onDone: () -> Unit) {
    val app = rememberApp()
    val context = LocalContext.current
    val current = app.settings.settings.value
    var farmName by remember { mutableStateOf(current.farmName) }
    var currency by remember { mutableStateOf(current.currency) }
    var price by remember { mutableStateOf(num(current.milkPricePerLiter, 2)) }
    val priceValue = price.toAmount()
    val coverPhoto = app.settings.settings.collectAsState().value.coverPhoto
    // The cover photo is saved as soon as it is picked or removed.
    fun setCover(name: String?) {
        val old = app.settings.settings.value
        app.settings.save(old.copy(coverPhoto = name))
        if (old.coverPhoto != name) Photos.delete(context, old.coverPhoto)
    }
    val coverPicker = rememberPhotoPicker(Photos.COVER_SIDE) { setCover(it) }

    Column(
        Modifier.verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        DpCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Serif("Your farm", 22)
                TextInput(farmName, { farmName = it }, "Farm name · shown on PDF reports")
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextInput(currency, { currency = it.take(4) }, "Currency", Modifier.weight(2f))
                    NumberField(price, { price = it }, "Milk price / litre", Modifier.weight(3f))
                }
                Muted("A new price applies to milk logged from now on.")
                PrimaryButton(
                    "Save",
                    onClick = {
                        app.settings.save(
                            app.settings.settings.value.copy(farmName = farmName.trim(), currency = currency.trim(), milkPricePerLiter = priceValue!!),
                        )
                        onDone()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = farmName.isNotBlank() && currency.isNotBlank() && priceValue != null && priceValue > 0,
                )
            }
        }

        DpCard {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Serif("Farm cover photo", 22)
                Muted("Shown behind your farm name at the top of the Today page.")
                val cover = rememberPhoto(coverPhoto, 1000)
                Box(
                    Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(RoundedCornerShape(18.dp))
                        .background(if (cover == null) Dp.heroGradient else androidx.compose.ui.graphics.SolidColor(Color.Transparent)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (cover != null) {
                        Image(cover, "Farm cover photo", Modifier.matchParentSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Outlined.Landscape, null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(44.dp))
                    }
                }
                PhotoActions(coverPicker, hasPhoto = coverPhoto != null, onRemove = { setCover(null) })
            }
        }

        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Dp.GreenPanel).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Dp.Green), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.CloudUpload, null, tint = Color.White)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Serif("Automatic Google backup", 21)
                    Strong("Records stay on this phone", size = 13, color = Dp.GreenDark)
                }
            }
            Muted(
                "Android copies your farm records to the Google account on this phone about once a day. " +
                    "A new phone with the same account, or a reinstall, gets them back automatically.",
                size = 14, color = PanelText,
            )
            Label("Backs up when the phone is", color = Dp.GreenDark)
            Condition(Icons.Outlined.Bolt, "Charging")
            Condition(Icons.Outlined.Wifi, "On Wi-Fi")
            Condition(Icons.Outlined.Bedtime, "Idle, e.g. overnight")
            Muted("Make sure \"Backup by Google One\" is switched on in your phone's settings.", color = PanelText)
            OutlineButton(
                "Open phone backup settings",
                onClick = {
                    // The backup page has no standard shortcut on every phone; fall back to main settings.
                    listOf(Intent("android.settings.BACKUP_AND_RESET_SETTINGS"), Intent(Settings.ACTION_SETTINGS))
                        .any { runCatching { context.startActivity(it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }.isSuccess }
                },
                modifier = Modifier.fillMaxWidth(),
                color = Dp.GreenDark,
            )
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun Condition(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Dp.GreenPill), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Dp.GreenDark, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Strong(text, weight = FontWeight.SemiBold)
    }
}
