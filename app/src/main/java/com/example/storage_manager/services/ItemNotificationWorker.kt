package com.example.storage_manager.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.storage_manager.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ItemNotificationWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        val itemName = inputData.getString("itemName") ?: return Result.failure()
        val clientName = inputData.getString("clientName") ?: ""
        val entryDateMillis = inputData.getLong("entryDate", -1)
        val returnDateMillis = inputData.getLong("returnDate", -1)

        // Format dates
        val entryDateStr = if (entryDateMillis != -1L) {
            "Entry Date: ${SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(entryDateMillis))}"
        } else "Entry Date: N/A"

        val returnDateStr = if (returnDateMillis != -1L) {
            "Return Date: ${SimpleDateFormat("MM/dd/yyyy", Locale.getDefault()).format(Date(returnDateMillis))}"
        } else "Return Date: N/A"

        createNotificationChannel()
        
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Item Reminder")
            .setContentText("Reminder for item: $itemName (Client: $clientName)")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Item: $itemName\nClient: $clientName\n$entryDateStr\n$returnDateStr"))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)

        return Result.success()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Item Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for item reminders"
            }
            
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "item_reminders"
    }
} 