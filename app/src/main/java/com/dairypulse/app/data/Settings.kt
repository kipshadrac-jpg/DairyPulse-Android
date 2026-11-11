package com.dairypulse.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FarmSettings(
    val farmName: String = "My Dairy Farm",
    val currency: String = "KES",
    val milkPricePerLiter: Double = 50.0,
)

/** Small settings store on SharedPreferences, which Auto Backup also copies. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val settings: StateFlow<FarmSettings> = state.asStateFlow()

    private fun read() = FarmSettings(
        farmName = prefs.getString("farmName", null) ?: FarmSettings().farmName,
        currency = prefs.getString("currency", null) ?: FarmSettings().currency,
        milkPricePerLiter = prefs.getFloat("milkPrice", FarmSettings().milkPricePerLiter.toFloat()).toDouble(),
    )

    fun save(settings: FarmSettings) {
        prefs.edit()
            .putString("farmName", settings.farmName)
            .putString("currency", settings.currency)
            .putFloat("milkPrice", settings.milkPricePerLiter.toFloat())
            .apply()
        state.value = settings
    }
}
