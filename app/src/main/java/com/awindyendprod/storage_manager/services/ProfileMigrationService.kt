package com.awindyendprod.storage_manager.services

import android.content.Context
import android.util.Log
import com.awindyendprod.storage_manager.model.Profile
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.model.Settings

class ProfileMigrationService(
    private val context: Context,
    private val profilePersistenceService: ProfilePersistenceService,
    private val storageTrackerPersistenceService: StorageTrackerPersistenceService
) {
    
    fun migrateExistingDataIfNeeded(): String {
        val profiles = profilePersistenceService.loadProfiles()
        
        // If profiles already exist, return current profile ID
        if (profiles.isNotEmpty()) {
            val currentProfileId = profilePersistenceService.getCurrentProfileId()
            return currentProfileId ?: profiles.first().profile.id
        }
        
        // Check if there's existing data to migrate
        val existingShelves = storageTrackerPersistenceService.loadData()
        val existingSettings = loadExistingSettings()
        
        // Create default profile
        val defaultProfile = profilePersistenceService.createDefaultProfile()
        
        // Create profile data with existing data
        val profileData = ProfileData(
            profile = defaultProfile,
            shelves = existingShelves,
            settings = existingSettings.copy(currentProfileId = defaultProfile.id)
        )
        
        // Save the default profile
        profilePersistenceService.saveProfiles(listOf(profileData))
        profilePersistenceService.saveCurrentProfileId(defaultProfile.id)
        
        // Also save the shelves data for the profile
        val storageService = StorageTrackerPersistenceService(context)
        storageService.saveData(existingShelves, defaultProfile.id)
        
        Log.d("ProfileMigration", "Migrated existing data to default profile: ${defaultProfile.id}")
        
        return defaultProfile.id
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
            dailyNotificationsEnabled = prefs.getBoolean("dailyNotificationsEnabled", true)
        )
    }
}
