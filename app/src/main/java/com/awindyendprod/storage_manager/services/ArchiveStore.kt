package com.awindyendprod.storage_manager.services

import android.content.Context
import android.util.Log
import com.awindyendprod.storage_manager.model.ArchivedItem
import com.awindyendprod.storage_manager.model.ProfileData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Date
import java.util.concurrent.TimeUnit

/**
 * Per-profile storage for deleted items, kept alongside the shelves in their own preferences file so
 * that the archive can grow and be pruned without rewriting the shelf data.
 */
class ArchiveStore(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun load(profileId: String): List<ArchivedItem> {
        val json = prefs.getString(key(profileId), null) ?: return emptyList()
        return runCatching {
            gson.fromJson<List<ArchivedItem>>(json, object : TypeToken<List<ArchivedItem>>() {}.type)
                ?: emptyList()
        }.getOrElse {
            Log.e(TAG, "Failed to read archive for $profileId", it)
            emptyList()
        }
    }

    /** Entries are unique by item id: the screen keys its list on it, and duplicates would crash it. */
    fun save(profileId: String, items: List<ArchivedItem>) {
        val unique = items.distinctBy { it.item.id }
        prefs.edit().putString(key(profileId), gson.toJson(unique)).apply()
    }

    fun remove(profileId: String) {
        prefs.edit().remove(key(profileId)).apply()
    }

    /** Used for export and import files only; sync deliberately leaves the archive behind. */
    fun attachArchiveToProfiles(profiles: List<ProfileData>): List<ProfileData> =
        profiles.map { profileData ->
            profileData.copy(archivedItems = load(profileData.profile.id))
        }

    /**
     * Drops entries archived more than [retentionDays] ago and returns what is left. A value of zero
     * or less keeps everything. The archive never leaves the device, so this needs no coordination.
     */
    fun pruneExpired(profileId: String, retentionDays: Int): List<ArchivedItem> {
        val stored = load(profileId)
        if (retentionDays <= 0) return stored

        val cutoff = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(retentionDays.toLong())
        val kept = stored.filter { it.archivedAt.time >= cutoff }
        if (kept.size != stored.size) {
            save(profileId, kept)
            Log.d(TAG, "Pruned ${stored.size - kept.size} archived item(s) from $profileId")
        }
        return kept
    }

    /** Newest first, which is how the archive is always shown. */
    fun sorted(items: List<ArchivedItem>): List<ArchivedItem> =
        items.sortedByDescending { it.archivedAt.time }

    companion object {
        private const val TAG = "ArchiveStore"
        private const val PREFS_NAME = "ArchiveStore"

        private fun key(profileId: String) = "archive_$profileId"

        /** Matching on client or item name, the two things the user would search by. */
        fun matches(archived: ArchivedItem, query: String): Boolean {
            val trimmed = query.trim()
            if (trimmed.isEmpty()) return true
            return archived.item.name.contains(trimmed, ignoreCase = true) ||
                archived.item.clientName.contains(trimmed, ignoreCase = true)
        }

        fun newEntry(
            item: com.awindyendprod.storage_manager.model.Item,
            shelfName: String,
            sectionName: String
        ) = ArchivedItem(
            item = item,
            shelfName = shelfName,
            sectionName = sectionName,
            archivedAt = Date()
        )
    }
}
