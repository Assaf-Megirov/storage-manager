package com.awindyendprod.storage_manager.services

import android.content.Context
import android.util.Log
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.model.Settings
import java.util.Date

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
            migrateSyncFieldsIfNeeded()
            migrateDefaultProfileIdIfNeeded()
            return profilePersistenceService.getCurrentProfileId()
                ?: profilePersistenceService.loadProfiles().first().profile.id
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
        migrateSyncFieldsIfNeeded()

        Log.d("ProfileMigration", "Migrated existing data to default profile: ${defaultProfile.id}")

        return defaultProfile.id
    }

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

    private fun migrateSyncFieldsIfNeeded() {
        val migrationPrefs = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        if (migrationPrefs.getBoolean("sync_fields_backfill_v1", false)) {
            return
        }

        val profiles = profilePersistenceService.loadProfiles()
        val backfilledProfiles = profiles.map { profileData ->
            val profile = if (profileData.profile.updatedAt == null) {
                profileData.profile.copy(updatedAt = profileData.profile.createdAt)
            } else {
                profileData.profile
            }

            val shelves = storageTrackerPersistenceService.loadData(profileData.profile.id).map { shelf ->
                val stampedShelf = if (shelf.updatedAt == null) shelf.copy(updatedAt = Date()) else shelf
                stampedShelf.copy(sections = stampedShelf.sections.map { section ->
                    val stampedSection = if (section.updatedAt == null) section.copy(updatedAt = Date()) else section
                    stampedSection.copy(items = stampedSection.items.map { item ->
                        if (item.updatedAt == null) item.copy(updatedAt = Date()) else item
                    })
                }.toMutableList())
            }
            storageTrackerPersistenceService.saveData(shelves, profileData.profile.id)

            profileData.copy(profile = profile, shelves = shelves)
        }
        profilePersistenceService.saveProfiles(backfilledProfiles)

        migrationPrefs.edit().putBoolean("sync_fields_backfill_v1", true).commit()
        Log.d("ProfileMigration", "Backfilled sync fields for ${backfilledProfiles.size} profile(s)")
    }

    private fun migrateDefaultProfileIdIfNeeded() {
        val migrationPrefs = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
        if (migrationPrefs.getBoolean("default_profile_id_migration_v1", false)) {
            return
        }

        val profiles = profilePersistenceService.loadProfiles()
        val defaultProfiles = profiles.filter { it.profile.isDefault }
        val alreadyCanonical = defaultProfiles.size == 1 &&
            defaultProfiles[0].profile.id == ProfilePersistenceService.DEFAULT_PROFILE_ID

        if (defaultProfiles.isEmpty() || alreadyCanonical) {
            migrationPrefs.edit().putBoolean("default_profile_id_migration_v1", true).commit()
            return
        }

        val survivor = defaultProfiles.maxBy { it.profile.updatedAt.orEpoch().time }
        val oldIds = defaultProfiles.map { it.profile.id }.toSet()

        val mergedShelves = oldIds.flatMap { storageTrackerPersistenceService.loadData(it) }.toMutableList()
        val canonicalSettings = profileSettingsStore.load(survivor.profile.id) ?: survivor.settings
        val canonicalProfile = survivor.profile.copy(id = ProfilePersistenceService.DEFAULT_PROFILE_ID)

        val newProfiles = profiles.filterNot { it.profile.isDefault } +
            ProfileData(profile = canonicalProfile, shelves = mergedShelves, settings = canonicalSettings)
        profilePersistenceService.saveProfiles(newProfiles)
        storageTrackerPersistenceService.saveData(mergedShelves, ProfilePersistenceService.DEFAULT_PROFILE_ID)
        profileSettingsStore.save(ProfilePersistenceService.DEFAULT_PROFILE_ID, canonicalSettings)

        oldIds.filter { it != ProfilePersistenceService.DEFAULT_PROFILE_ID }.forEach { oldId ->
            storageTrackerPersistenceService.removeData(oldId)
            profileSettingsStore.remove(oldId)
        }

        val currentProfileId = profilePersistenceService.getCurrentProfileId()
        if (currentProfileId in oldIds) {
            profilePersistenceService.saveCurrentProfileId(ProfilePersistenceService.DEFAULT_PROFILE_ID)
        }

        migrationPrefs.edit().putBoolean("default_profile_id_migration_v1", true).commit()
        Log.d(
            "ProfileMigration",
            "Consolidated ${oldIds.size} default profile id(s) into ${ProfilePersistenceService.DEFAULT_PROFILE_ID}"
        )
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
