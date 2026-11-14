package com.dairypulse.app

import android.app.Application
import androidx.work.Configuration
import com.dairypulse.app.data.AppDatabase
import com.dairypulse.app.data.Photos
import com.dairypulse.app.data.SettingsStore
import com.dairypulse.app.work.ReminderWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DairyPulseApp : Application(), Configuration.Provider {
    lateinit var db: AppDatabase
        private set
    lateinit var settings: SettingsStore
        private set

    /** Writes run here, so leaving a screen right after "Save" never cancels them. */
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun launchIo(block: suspend AppDatabase.() -> Unit) {
        appScope.launch { db.block() }
    }

    // WorkManager starts on demand (its auto-initializer is removed in the manifest).
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        db = AppDatabase.build(this)
        settings = SettingsStore(this)
        ReminderWorker.createChannel(this)
        ReminderWorker.schedule(this)
        // Tidy photos left behind by forms closed without saving, or by deleted animals.
        appScope.launch {
            val inUse = db.cows().all().first().mapNotNull { it.photo } + listOfNotNull(settings.settings.value.coverPhoto)
            Photos.deleteUnused(this@DairyPulseApp, inUse.toSet())
        }
    }
}
