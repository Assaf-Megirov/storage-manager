package com.example.storage_manager.recievers

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.storage_manager.R

class ReturnAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val itemId = intent.getStringExtra("ITEM_ID")
        val itemName = intent.getStringExtra("ITEM_NAME")

        // Create and show notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(context, "StorageTrackerChannel")
            .setContentTitle("Item Return Reminder")
            .setContentText("$itemName is due to be returned")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        notificationManager.notify(itemId.hashCode(), notification)
    }
}