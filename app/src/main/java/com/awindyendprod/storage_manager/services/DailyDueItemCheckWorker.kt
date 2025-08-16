package com.awindyendprod.storage_manager.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.AppLanguage
import com.awindyendprod.storage_manager.model.Settings
import android.content.res.Resources
import java.util.*
import java.util.concurrent.TimeUnit

class DailyDueItemCheckWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        try {
            val persistenceService = StorageTrackerPersistenceService(context)
            val shelves = persistenceService.loadData()
            val settings = loadSettings()

            if (!settings.dailyNotificationsEnabled) {
                return Result.success()
            }

            // Calculate target date (today + notificationDaysBefore days)
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_MONTH, settings.notificationDaysBefore)
            val targetDate = calendar.time

            // Count items due on target date
            var dueItemsCount = 0
            val dueItems = mutableListOf<String>()

            shelves.forEach { shelf ->
                shelf.sections.forEach { section ->
                    section.items.forEach { item ->
                        item.returnDate?.let { returnDate ->
                            val itemCalendar = Calendar.getInstance().apply { time = returnDate }
                            val targetCalendar = Calendar.getInstance().apply { time = targetDate }
                            
                            if (itemCalendar.get(Calendar.YEAR) == targetCalendar.get(Calendar.YEAR) &&
                                itemCalendar.get(Calendar.DAY_OF_YEAR) == targetCalendar.get(Calendar.DAY_OF_YEAR)) {
                                dueItemsCount++
                                dueItems.add(item.name)
                            }
                        }
                    }
                }
            }

            // Check if we need to show notification/alert
            if (dueItemsCount >= settings.notificationMaxItems) {
                showNotification(dueItemsCount, settings)
                
                // Store alert data for the app to show dialog when opened
                storeAlertData(dueItemsCount, settings.notificationDaysBefore)
            }

            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }

    private fun loadSettings(): Settings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return Settings(
            notificationDaysBefore = prefs.getInt("notificationDaysBefore", 1),
            notificationMaxItems = prefs.getInt("notificationMaxItems", 10),
            dailyNotificationsEnabled = prefs.getBoolean("dailyNotificationsEnabled", true),
            language = AppLanguage.valueOf(
                prefs.getString("language", AppLanguage.SYSTEM.name) ?: AppLanguage.SYSTEM.name
            )
        )
    }

    private fun showNotification(itemCount: Int, settings: Settings) {
        val locale = when (settings.language) {
            AppLanguage.SYSTEM -> Resources.getSystem().configuration.locales[0]
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.HEBREW -> Locale("iw")
            AppLanguage.RUSSIAN -> Locale("ru")
        }

        val configuration = context.resources.configuration.apply {
            setLocale(locale)
        }
        val contextWithLocale = context.createConfigurationContext(configuration)

        createNotificationChannel(contextWithLocale)

        val dayText = if (settings.notificationDaysBefore == 0) {
            contextWithLocale.getString(R.string.today)
        } else if (settings.notificationDaysBefore == 1) {
            contextWithLocale.getString(R.string.tomorrow)
        } else {
            contextWithLocale.getString(R.string.in_days, settings.notificationDaysBefore)
        }

        val notification = NotificationCompat.Builder(contextWithLocale, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(contextWithLocale.getString(R.string.high_due_items_notification_title))
            .setContentText(contextWithLocale.getString(R.string.high_due_items_notification_text, itemCount, dayText))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(contextWithLocale.getString(R.string.high_due_items_notification_text, itemCount, dayText)))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val notificationManager = contextWithLocale.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel(contextWithLocale: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                contextWithLocale.getString(R.string.due_items_alerts),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = contextWithLocale.getString(R.string.due_items_alerts_description)
            }
            
            val notificationManager = contextWithLocale.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun storeAlertData(itemCount: Int, daysBefore: Int) {
        val prefs = context.getSharedPreferences("daily_alerts", Context.MODE_PRIVATE)
        prefs.edit().apply {
            putInt("pending_alert_count", itemCount)
            putInt("pending_alert_days", daysBefore)
            putLong("alert_timestamp", System.currentTimeMillis())
            apply()
        }
    }

    companion object {
        const val CHANNEL_ID = "due_items_alerts"
        const val NOTIFICATION_ID = 2001
        const val WORK_NAME = "daily_due_item_check"
    }
}
