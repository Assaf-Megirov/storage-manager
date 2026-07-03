package com.awindyendprod.storage_manager.services

import android.content.Context
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.model.Settings
import com.google.gson.Gson

/**
 * Canonical per-profile settings storage (separate from the in-memory [ProfileData] list).
 * Uses [commit] so a profile switch always reads what was just saved.
 */
class ProfileSettingsStore(private val context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun save(profileId: String, settings: Settings): Boolean {
        val profilePrefs = getProfilePrefs(profileId)
        saveToPrefs(settings, profilePrefs)
        val toStore = SettingsPartition.forProfileStorage(settings, profileId)
        return prefs.edit()
            .putString(settingsKey(profileId), gson.toJson(toStore))
            .commit()
    }

    private fun saveToPrefs(settings: Settings, preferences: android.content.SharedPreferences) {
        preferences.edit().apply {
            putString("sectionDateType", settings.sectionDateType.name)
            putString("dateDisplayFormat", settings.dateDisplayFormat.name)
            putInt("defaultReturnDateDays", settings.defaultReturnDateDays)
            putString("language", settings.language.name)
            putString("fontSize", settings.fontSize.name)
            putInt("sectionHeight", settings.sectionHeight)
            putInt("sectionWidth", settings.sectionWidth)
            putString("theme", settings.theme.name)
            putBoolean("fabDragEnabled", settings.fabDragEnabled)
            putFloat("fabPositionMainScreenX", settings.fabPositionMainScreenX)
            putFloat("fabPositionMainScreenY", settings.fabPositionMainScreenY)
            putFloat("fabPositionSectionScreenX", settings.fabPositionSectionScreenX)
            putFloat("fabPositionSectionScreenY", settings.fabPositionSectionScreenY)
            putBoolean("hasSeenLongPressHint", settings.hasSeenLongPressHint)
            putInt("notificationDaysBefore", settings.notificationDaysBefore)
            putInt("notificationMaxItems", settings.notificationMaxItems)
            putBoolean("dailyNotificationsEnabled", settings.dailyNotificationsEnabled)
            putBoolean("showProfilesButton", settings.showProfilesButton)
            commit()
        }
    }

    fun load(profileId: String): Settings? {
        val json = prefs.getString(settingsKey(profileId), null) ?: return null
        return gson.fromJson(json, Settings::class.java)
    }

    fun remove(profileId: String) {
        prefs.edit().remove(settingsKey(profileId)).commit()
    }

    /**
     * Seeds per-profile keys from [ProfileData.settings] / global prefs when missing.
     * Does not overwrite keys that already exist (safe for upgrades).
     */
    fun migrateFromLegacyStorage(
        profiles: List<ProfileData>,
        globalSettings: Settings,
        currentProfileId: String?
    ) {
        if (prefs.getInt(KEY_STORE_MIGRATION_VERSION, 0) >= STORE_MIGRATION_VERSION) {
            return
        }

        profiles.forEach { profileData ->
            val id = profileData.profile.id
            val settings = when {
                id == currentProfileId -> globalSettings
                SettingsPartition.isPristineProfileSettings(profileData.settings) ->
                    Settings().copy(currentProfileId = id)
                else -> profileData.settings
            }
            save(id, settings)
        }

        prefs.edit().putInt(KEY_STORE_MIGRATION_VERSION, STORE_MIGRATION_VERSION).commit()
    }

    /** Keep [ProfileData.settings] in sync for export. */
    fun attachSettingsToProfiles(profiles: List<ProfileData>): List<ProfileData> =
        profiles.map { profileData ->
            val stored = load(profileData.profile.id) ?: profileData.settings
            profileData.copy(
                settings = SettingsPartition.forProfileStorage(stored, profileData.profile.id)
            )
        }

    fun getProfilePrefs(profileId: String) =
        context.getSharedPreferences(settingsKey(profileId), Context.MODE_PRIVATE)

    private fun settingsKey(profileId: String) = "settings_$profileId"

    companion object {
        private const val PREFS_NAME = "ProfileSettingsStore"
        private const val KEY_STORE_MIGRATION_VERSION = "store_migration_version"
        private const val STORE_MIGRATION_VERSION = 4
    }
}
