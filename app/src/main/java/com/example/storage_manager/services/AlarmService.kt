package com.example.storage_manager.services

import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.storage_manager.model.Item
import com.example.storage_manager.recievers.ReturnAlarmReceiver

class AlarmService(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun setItemReturnAlarm(item: Item) {
        item.alarmDateTime?.let { alarmTime ->
            val intent = Intent(context, ReturnAlarmReceiver::class.java).apply {
                putExtra("ITEM_ID", item.id)
                putExtra("ITEM_NAME", item.name)
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                item.id.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            try{
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime.time,
                    pendingIntent
                )
            }catch (e: SecurityException){
                showPermissionRequiredMessage()
            }

        }
    }

    fun cancelItemReturnAlarm(itemId: String) {
        val intent = Intent(context, ReturnAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            itemId.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let {
            alarmManager.cancel(it)
        }
    }

    private fun showPermissionRequiredMessage() {
        AlertDialog.Builder(context)
            .setTitle("Permission Required")
            .setMessage("To set the alarm, you need to enable the exact alarm permission. Please go to settings and enable it.")
            .setPositiveButton("OK", null)
            .show()
    }
}