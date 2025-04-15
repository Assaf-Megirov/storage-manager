package com.awindyendprod.storage_manager.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.awindyendprod.storage_manager.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.awindyendprod.storage_manager.model.AppLanguage
import android.content.res.Resources

class ItemNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val languageStr = inputData.getString("language") ?: AppLanguage.SYSTEM.name
        val language = AppLanguage.valueOf(languageStr)

        val locale = when (language) {
            AppLanguage.SYSTEM -> Resources.getSystem().configuration.locales[0]
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.HEBREW -> Locale("iw")
            AppLanguage.RUSSIAN -> Locale("ru")
        }

        val configuration = context.resources.configuration.apply {
            setLocale(locale)
        }
        val contextWithLocale = context.createConfigurationContext(configuration)
        
        val itemName = inputData.getString("itemName") ?: return Result.failure()
        val clientName = inputData.getString("clientName") ?: ""
        val entryDateMillis = inputData.getLong("entryDate", -1)
        val returnDateMillis = inputData.getLong("returnDate", -1)

        val entryDateStr = if (entryDateMillis != -1L) {
            contextWithLocale.getString(R.string.entry_date_format, 
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(entryDateMillis)))
        } else contextWithLocale.getString(R.string.entry_date_na)

        val returnDateStr = if (returnDateMillis != -1L) {
            contextWithLocale.getString(R.string.return_date_format,
                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(returnDateMillis)))
        } else contextWithLocale.getString(R.string.return_date_na)

        createNotificationChannel(contextWithLocale)
        
        val notification = NotificationCompat.Builder(contextWithLocale, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(contextWithLocale.getString(R.string.item_reminder))
            .setContentText(contextWithLocale.getString(R.string.reminder_for_item, itemName, clientName))
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText(contextWithLocale.getString(R.string.item_reminder_details,
                    itemName,
                    clientName,
                    entryDateStr,
                    returnDateStr)))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = contextWithLocale.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        return Result.success()
    }

    private fun createNotificationChannel(contextWithLocale: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                contextWithLocale.getString(R.string.item_reminders),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = contextWithLocale.getString(R.string.item_reminders_description)
            }
            
            val notificationManager = contextWithLocale.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "item_reminders"
    }
} 