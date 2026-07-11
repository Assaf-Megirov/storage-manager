package com.awindyendprod.storage_manager.services

import android.content.Context
import java.util.UUID

class SyncPreferencesStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun isSyncEnabled(): Boolean = prefs.getBoolean(KEY_SYNC_ENABLED, false)

    fun setSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_SYNC_ENABLED, enabled).commit()
    }

    fun getLastSyncedAtMillis(): Long? {
        val value = prefs.getLong(KEY_LAST_SYNCED_AT, -1L)
        return if (value == -1L) null else value
    }

    fun getDriveFileId(): String? = prefs.getString(KEY_DRIVE_FILE_ID, null)

    fun recordSuccessfulSync(atMillis: Long, driveFileId: String) {
        prefs.edit()
            .putLong(KEY_LAST_SYNCED_AT, atMillis)
            .putString(KEY_DRIVE_FILE_ID, driveFileId)
            .commit()
    }

    fun getOrCreateDeviceId(): String {
        prefs.getString(KEY_DEVICE_ID, null)?.let { return it }
        val newId = UUID.randomUUID().toString()
        prefs.edit().putString(KEY_DEVICE_ID, newId).commit()
        return newId
    }

    fun isMarkedAsMainLocally(): Boolean = prefs.getBoolean(KEY_MARKED_AS_MAIN, false)

    fun setMarkedAsMainLocally(marked: Boolean) {
        prefs.edit().putBoolean(KEY_MARKED_AS_MAIN, marked).commit()
    }

    fun getCachedMainDeviceId(): String? = prefs.getString(KEY_CACHED_MAIN_DEVICE_ID, null)

    fun setCachedMainDeviceId(mainDeviceId: String?) {
        prefs.edit().putString(KEY_CACHED_MAIN_DEVICE_ID, mainDeviceId).commit()
    }

    companion object {
        private const val PREFS_NAME = "SyncPreferencesStore"
        private const val KEY_SYNC_ENABLED = "sync_enabled"
        private const val KEY_LAST_SYNCED_AT = "last_synced_at_millis"
        private const val KEY_DRIVE_FILE_ID = "drive_file_id"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_MARKED_AS_MAIN = "marked_as_main_locally"
        private const val KEY_CACHED_MAIN_DEVICE_ID = "cached_main_device_id"
    }
}
