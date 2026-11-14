package com.dairypulse.app.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.dairypulse.app.DairyPulseApp
import com.dairypulse.app.MainActivity
import com.dairypulse.app.R
import com.dairypulse.app.data.CowStatus
import com.dairypulse.app.domain.daysUntilCalving
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.util.concurrent.TimeUnit

/** Once a day, on the phone itself: notify about tasks due and cows close to calving. */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as DairyPulseApp
        val today = LocalDate.now()
        val tasks = app.db.tasks().dueBy(today)
        val calving = app.db.cows().all().first()
            .filter { it.status != CowStatus.SOLD }
            .filter { c -> daysUntilCalving(c, today)?.let { it in 0..7 } ?: false }

        val lines = buildList {
            tasks.take(4).forEach { add("• ${it.title}") }
            calving.take(4).forEach { add("• ${it.displayName} due to calve in ${daysUntilCalving(it, today)} days") }
        }
        if (lines.isNotEmpty()) notify(lines, tasks.size + calving.size)
        return Result.success()
    }

    private fun notify(lines: List<String>, count: Int) {
        val ctx = applicationContext
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= 33
        ) return
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("DairyDesk: $count item(s) need attention")
            .setContentText(lines.first())
            .setStyle(NotificationCompat.BigTextStyle().bigText(lines.joinToString("\n")))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(ctx).notify(1, n)
    }

    companion object {
        private const val CHANNEL = "reminders"

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL, "Farm reminders", NotificationManager.IMPORTANCE_DEFAULT)
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork("daily-reminders", ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
