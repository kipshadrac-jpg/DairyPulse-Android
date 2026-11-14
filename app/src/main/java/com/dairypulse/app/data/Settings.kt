package com.dairypulse.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class FarmSettings(
    val farmName: String = "My Dairy Farm",
    val currency: String = "KSh",
    val milkPricePerLiter: Double = 50.0,
    /** File name of the farm cover photo shown on the Today page, if one was added. */
    val coverPhoto: String? = null,
)

/** Small settings store on SharedPreferences, which Auto Backup also copies. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    init {
        // One-time switch of the old default label "KES" to "KSh".
        if (!prefs.getBoolean("currencyMigrated", false)) {
            val edit = prefs.edit().putBoolean("currencyMigrated", true)
            if (prefs.getString("currency", null) == "KES") edit.putString("currency", "KSh")
            edit.apply()
        }
    }

    private val state = MutableStateFlow(read())
    val settings: StateFlow<FarmSettings> = state.asStateFlow()

    private fun read() = FarmSettings(
        farmName = prefs.getString("farmName", null) ?: FarmSettings().farmName,
        currency = prefs.getString("currency", null) ?: FarmSettings().currency,
        milkPricePerLiter = prefs.getFloat("milkPrice", FarmSettings().milkPricePerLiter.toFloat()).toDouble(),
        coverPhoto = prefs.getString("coverPhoto", null),
    )

    fun save(settings: FarmSettings) {
        prefs.edit()
            .putString("farmName", settings.farmName)
            .putString("currency", settings.currency)
            .putFloat("milkPrice", settings.milkPricePerLiter.toFloat())
            .putString("coverPhoto", settings.coverPhoto)
            .apply()
        state.value = settings
    }
}
