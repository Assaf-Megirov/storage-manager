package com.awindyendprod.storage_manager.services

import android.content.Context
import android.util.Log
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.model.Settings

class ProfileMigrationService(
    private val context: Context,
    private val profilePersistenceService: ProfilePersistenceService,
    private val storageTrackerPersistenceService: StorageTrackerPersistenceService,
    private val profileSettingsStore: ProfileSettingsStore
) {

    fun migrateExistingDataIfNeeded(): String {
        val profiles = profilePersistenceService.loadProfiles()

        if (profiles.isNotEmpty()) {
            val globalSettings = loadExistingSettings()
            val currentProfileId = profilePersistenceService.getCurrentProfileId()
            profileSettingsStore.migrateFromLegacyStorage(profiles, globalSettings, currentProfileId)
            migratePerProfileSettingsInProfileDataIfNeeded()
            return currentProfileId ?: profiles.first().profile.id
        }

        val existingShelves = storageTrackerPersistenceService.loadData()
        val existingSettings = loadExistingSettings()

        val defaultProfile = profilePersistenceService.createDefaultProfile()

        val profileData = ProfileData(
            profile = defaultProfile,
            shelves = existingShelves,
            settings = existingSettings.copy(currentProfileId = defaultProfile.id)
        )

        profilePersistenceService.saveProfiles(listOf(profileData))
        profilePersistenceService.saveCurrentProfileId(defaultProfile.id)

        val storageService = StorageTrackerPersistenceService(context)
        storageService.saveData(existingShelves, defaultProfile.id)

        profileSettingsStore.migrateFromLegacyStorage(
            listOf(profileData),
            existingSettings,
            defaultProfile.id
        )
        migratePerProfileSettingsInProfileDataIfNeeded()

        Log.d("ProfileMigration", "Migrated existing data to default profile: ${defaultProfile.id}")

        return defaultProfile.id
    }

    /** Keeps [ProfileData.settings] aligned with [ProfileSettingsStore] for export. */
    private fun migratePerProfileSettingsInProfileDataIfNeeded() {
        val migrationPrefs = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        if (migrationPrefs.getBoolean("per_profile_settings_full_v2", false)) {
            return
        }

        val profiles = profilePersistenceService.loadProfiles()
        if (profiles.isEmpty()) {
            migrationPrefs.edit()
                .putBoolean("per_profile_settings_v1", true)
                .putBoolean("per_profile_settings_full_v2", true)
                .commit()
            return
        }

        val synced = profileSettingsStore.attachSettingsToProfiles(profiles)
        profilePersistenceService.saveProfiles(synced)
        migrationPrefs.edit()
            .putBoolean("per_profile_settings_v1", true)
            .putBoolean("per_profile_settings_full_v2", true)
            .commit()
        Log.d("ProfileMigration", "Synced profile data settings for ${synced.size} profile(s)")
    }

    private fun loadExistingSettings(): Settings {
        val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

        return Settings(
            sectionDateType = com.awindyendprod.storage_manager.model.SectionDateType.valueOf(
                prefs.getString("sectionDateType", com.awindyendprod.storage_manager.model.SectionDateType.ENTRY_DATE.name)!!
            ),
            dateDisplayFormat = com.awindyendprod.storage_manager.model.DateDisplayFormat.valueOf(
                prefs.getString("dateDisplayFormat", com.awindyendprod.storage_manager.model.DateDisplayFormat.NUMERIC.name)!!
            ),
            defaultReturnDateDays = prefs.getInt("defaultReturnDateDays", 14),
            language = com.awindyendprod.storage_manager.model.AppLanguage.valueOf(
                prefs.getString("language", com.awindyendprod.storage_manager.model.AppLanguage.SYSTEM.name)!!
            ),
            fontSize = com.awindyendprod.storage_manager.model.FontSize.valueOf(
                prefs.getString("fontSize", com.awindyendprod.storage_manager.model.FontSize.MEDIUM.name)!!
            ),
            sectionHeight = prefs.getInt("sectionHeight", 210),
            sectionWidth = prefs.getInt("sectionWidth", 300),
            theme = com.awindyendprod.storage_manager.model.Theme.valueOf(
                prefs.getString("theme", com.awindyendprod.storage_manager.model.Theme.SYSTEM.name)!!
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
}
