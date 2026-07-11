package com.awindyendprod.storage_manager.services

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.awindyendprod.storage_manager.model.AppLanguage
import com.awindyendprod.storage_manager.model.DateDisplayFormat
import com.awindyendprod.storage_manager.model.ExportData
import com.awindyendprod.storage_manager.model.SectionDateType
import com.awindyendprod.storage_manager.model.FontSize
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.Theme
import com.google.gson.GsonBuilder
import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Date

sealed class SyncOutcome {
    object Success : SyncOutcome()
    object NothingToDo : SyncOutcome()
    object AuthRequired : SyncOutcome()
    object NetworkUnavailable : SyncOutcome()
    data class Failure(val reason: String) : SyncOutcome()
    object MainClaimRejected : SyncOutcome()
}

class SyncManager(
    private val context: Context,
    private val storageTrackerPersistenceService: StorageTrackerPersistenceService,
    private val profilePersistenceService: ProfilePersistenceService,
    private val profileSettingsStore: ProfileSettingsStore,
    private val tombstoneStore: TombstoneStore,
    private val googleAuthService: GoogleAuthService,
    private val driveSyncService: DriveSyncService,
    private val syncPreferencesStore: SyncPreferencesStore,
) {
    private val gson = GsonBuilder().registerTypeAdapter(Date::class.java, object : TypeAdapter<Date>() {
        override fun write(out: JsonWriter, value: Date?) {
            if (value == null) out.nullValue() else out.value(value.time)
        }
        override fun read(reader: JsonReader): Date? {
            if (reader.peek() == JsonToken.NULL) {
                reader.nextNull()
                return null
            }
            return Date(reader.nextLong())
        }
    }).create()

    suspend fun performSync(confirmWholesaleAdopt: suspend () -> Boolean = { false }): SyncOutcome = withContext(Dispatchers.IO) {
        if (!syncPreferencesStore.isSyncEnabled()) return@withContext SyncOutcome.NothingToDo
        val account = googleAuthService.getSignedInAccount() ?: return@withContext SyncOutcome.NothingToDo
        if (!isNetworkAvailable()) return@withContext SyncOutcome.NetworkUnavailable

        val local = buildLocalExportData()
        writeSafetyBackup(local)

        val token = when (val authResult = googleAuthService.getAccessToken(account)) {
            is AuthResult.Success -> authResult.token
            is AuthResult.RecoverableConsent -> return@withContext SyncOutcome.AuthRequired
            is AuthResult.Failure -> return@withContext SyncOutcome.NetworkUnavailable
        }

        val meta = when (val metaResult = driveSyncService.findSyncFile(token)) {
            is DriveResult.Success -> metaResult.value
            is DriveResult.AuthExpired -> {
                googleAuthService.clearCachedToken(token)
                return@withContext SyncOutcome.AuthRequired
            }
            is DriveResult.NetworkError -> return@withContext SyncOutcome.NetworkUnavailable
            is DriveResult.ServerError -> return@withContext SyncOutcome.NetworkUnavailable
        }

        val remote: ExportData? = if (meta == null) {
            null
        } else {
            when (val downloadResult = driveSyncService.downloadSyncFile(token, meta.id)) {
                is DriveResult.Success -> parseExportDataOrNull(downloadResult.value)
                is DriveResult.AuthExpired -> {
                    googleAuthService.clearCachedToken(token)
                    return@withContext SyncOutcome.AuthRequired
                }
                is DriveResult.NetworkError -> return@withContext SyncOutcome.NetworkUnavailable
                is DriveResult.ServerError -> return@withContext SyncOutcome.NetworkUnavailable
            }
        }

        val thisDeviceId = syncPreferencesStore.getOrCreateDeviceId()
        val wantsMain = syncPreferencesStore.isMarkedAsMainLocally()
        val remoteMainId = remote?.mainDeviceId

        val claimRejected = remoteMainId != null && remoteMainId != thisDeviceId && wantsMain
        if (claimRejected) {
            syncPreferencesStore.setMarkedAsMainLocally(false)
        }
        val resolvedMainDeviceId = when {
            claimRejected -> remoteMainId
            remoteMainId == thisDeviceId && !wantsMain -> null
            remoteMainId == null && wantsMain -> thisDeviceId
            else -> remoteMainId
        }
        syncPreferencesStore.setCachedMainDeviceId(resolvedMainDeviceId)

        val isFirstSyncEver = syncPreferencesStore.getLastSyncedAtMillis() == null
        val shouldOfferWholesaleAdopt = !wantsMain && !claimRejected && isFirstSyncEver &&
            remote != null && remoteMainId != null && remoteMainId != thisDeviceId

        val merged = when {
            shouldOfferWholesaleAdopt -> {
                if (!confirmWholesaleAdopt()) return@withContext SyncOutcome.NothingToDo
                remote!!.copy(mainDeviceId = resolvedMainDeviceId)
            }
            remote == null -> local.copy(mainDeviceId = resolvedMainDeviceId)
            else -> SyncMerger.merge(local, remote).copy(mainDeviceId = resolvedMainDeviceId)
        }.withLocalSettingsPreserved(local)

        persistMergedLocally(merged)

        val mergedJson = gson.toJson(merged)
        when (val uploadResult = driveSyncService.uploadSyncFile(token, meta?.id, mergedJson)) {
            is DriveResult.Success -> syncPreferencesStore.recordSuccessfulSync(System.currentTimeMillis(), uploadResult.value)
            is DriveResult.AuthExpired -> {
                googleAuthService.clearCachedToken(token)
                return@withContext SyncOutcome.AuthRequired
            }
            is DriveResult.NetworkError -> return@withContext SyncOutcome.NetworkUnavailable
            is DriveResult.ServerError -> return@withContext SyncOutcome.NetworkUnavailable
        }

        if (claimRejected) SyncOutcome.MainClaimRejected else SyncOutcome.Success
    }

    suspend fun resetAllData(): Boolean = withContext(Dispatchers.IO) {
        val remoteHandledSafely = deleteRemoteDataIfOwnedByThisDevice()
        wipeAllLocalData()
        remoteHandledSafely
    }

    private suspend fun deleteRemoteDataIfOwnedByThisDevice(): Boolean {
        val account = googleAuthService.getSignedInAccount() ?: return true
        if (!isNetworkAvailable()) return false
        val token = (googleAuthService.getAccessToken(account) as? AuthResult.Success)?.token ?: return false

        val meta = when (val metaResult = driveSyncService.findSyncFile(token)) {
            is DriveResult.Success -> metaResult.value ?: return true
            else -> return false
        }

        val remoteMainId = (driveSyncService.downloadSyncFile(token, meta.id) as? DriveResult.Success)
            ?.let { parseExportDataOrNull(it.value) }
            ?.mainDeviceId
        val thisDeviceId = syncPreferencesStore.getOrCreateDeviceId()
        val ownedByThisDevice = remoteMainId == null || remoteMainId == thisDeviceId
        if (!ownedByThisDevice) return false

        return driveSyncService.deleteSyncFile(token, meta.id) is DriveResult.Success
    }

    private fun wipeAllLocalData() {
        val dataDir = context.filesDir.parentFile
        if (dataDir != null) {
            File(dataDir, "shared_prefs").deleteRecursively()
        }
        context.filesDir.deleteRecursively()
        context.cacheDir.deleteRecursively()
    }

    private fun ExportData.withLocalSettingsPreserved(local: ExportData): ExportData {
        val localSettingsById = local.profiles.associateBy({ it.profile.id }, { it.settings })
        return copy(
            globalSettings = local.globalSettings,
            profiles = profiles.map { profileData ->
                localSettingsById[profileData.profile.id]?.let { profileData.copy(settings = it) } ?: profileData
            }
        )
    }

    private fun buildLocalExportData(): ExportData {
        val profiles = profilePersistenceService.loadProfiles()
        val withSettings = profileSettingsStore.attachSettingsToProfiles(profiles)
        val withShelves = storageTrackerPersistenceService.attachShelvesToProfiles(withSettings)
        return ExportData(
            globalSettings = loadGlobalSettings(),
            profiles = withShelves,
            currentProfileId = profilePersistenceService.getCurrentProfileId(),
            version = 1,
            tombstones = tombstoneStore.loadAll()
        )
    }

    private fun persistMergedLocally(merged: ExportData) {
        profilePersistenceService.saveProfiles(merged.profiles)
        merged.currentProfileId?.let { profilePersistenceService.saveCurrentProfileId(it) }
        merged.profiles.forEach { profileData ->
            storageTrackerPersistenceService.saveData(profileData.shelves, profileData.profile.id)
            profileSettingsStore.save(profileData.profile.id, profileData.settings)
        }
        tombstoneStore.replaceAll(merged.tombstones)
    }

    private fun writeSafetyBackup(local: ExportData) {
        try {
            val backupsDir = File(context.filesDir, "sync_backups").apply { mkdirs() }
            val backupFile = File(backupsDir, "backup_${System.currentTimeMillis()}.json")
            storageTrackerPersistenceService.exportToFile(backupFile, local.globalSettings, local.profiles, local.currentProfileId)
            pruneOldBackups(backupsDir, keep = 20)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write pre-sync safety backup", e)
        }
    }

    private fun pruneOldBackups(dir: File, keep: Int) {
        val files = dir.listFiles()?.sortedByDescending { it.lastModified() } ?: return
        files.drop(keep).forEach { it.delete() }
    }

    private fun parseExportDataOrNull(json: String): ExportData? {
        return try {
            gson.fromJson(json, ExportData::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse remote sync file (${json.length} chars): ${json.take(500)}", e)
            null
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    private fun loadGlobalSettings(): Settings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        return Settings(
            sectionDateType = SectionDateType.valueOf(
                prefs.getString("sectionDateType", SectionDateType.ENTRY_DATE.name)!!
            ),
            dateDisplayFormat = DateDisplayFormat.valueOf(
                prefs.getString("dateDisplayFormat", DateDisplayFormat.NUMERIC.name)!!
            ),
            defaultReturnDateDays = prefs.getInt("defaultReturnDateDays", 14),
            language = AppLanguage.valueOf(
                prefs.getString("language", AppLanguage.SYSTEM.name)!!
            ),
            fontSize = FontSize.valueOf(
                prefs.getString("fontSize", FontSize.MEDIUM.name)!!
            ),
            sectionHeight = prefs.getInt("sectionHeight", 210),
            sectionWidth = prefs.getInt("sectionWidth", 300),
            theme = Theme.valueOf(
                prefs.getString("theme", Theme.SYSTEM.name)!!
            ),
            fabDragEnabled = prefs.getBoolean("fabDragEnabled", true),
            fabPositionMainScreenX = prefs.getFloat("fabPositionMainScreenX", Float.MIN_VALUE),
            fabPositionMainScreenY = prefs.getFloat("fabPositionMainScreenY", Float.MIN_VALUE),
            fabPositionSectionScreenX = prefs.getFloat("fabPositionSectionScreenX", Float.MIN_VALUE),
            fabPositionSectionScreenY = prefs.getFloat("fabPositionSectionScreenY", Float.MIN_VALUE),
            hasSeenLongPressHint = prefs.getBoolean("hasSeenLongPressHint", false),
            notificationDaysBefore = prefs.getInt("notificationDaysBefore", 1),
            notificationMaxItems = prefs.getInt("notificationMaxItems", 10),
            dailyNotificationsEnabled = prefs.getBoolean("dailyNotificationsEnabled", true),
            showProfilesButton = prefs.getBoolean("showProfilesButton", true)
        )
    }

    companion object {
        private const val TAG = "SyncManager"
    }
}
