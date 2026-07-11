package com.awindyendprod.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.Shelf
import com.awindyendprod.storage_manager.model.Tombstone
import com.awindyendprod.storage_manager.model.TombstoneEntityType
import com.awindyendprod.storage_manager.services.ProfilePersistenceService
import com.awindyendprod.storage_manager.services.ProfileSettingsStore
import com.awindyendprod.storage_manager.services.SettingsPartition
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService
import com.awindyendprod.storage_manager.services.TombstoneStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Date

class ProfileViewModel(
    context: Context,
    private val profilePersistenceService: ProfilePersistenceService,
    private val profileSettingsStore: ProfileSettingsStore,
    private val storageTrackerPersistenceService: StorageTrackerPersistenceService,
    private val tombstoneStore: TombstoneStore,
    private val storageTrackerViewModel: StorageTrackerViewModel,
    private val settingsViewModel: SettingsViewModel,
) : ViewModel() {

    private val _profiles = MutableStateFlow<List<ProfileData>>(emptyList())
    val profiles: StateFlow<List<ProfileData>> = _profiles.asStateFlow()

    private val _currentProfileId = MutableStateFlow<String?>(null)
    val currentProfileId: StateFlow<String?> = _currentProfileId.asStateFlow()

    var onDataChanged: () -> Unit = {}

    fun initializeAfterMigration() {
        loadProfiles()
        val profileId = _currentProfileId.value ?: return
        settingsViewModel.switchToProfile(profileId)
        storageTrackerViewModel.reloadDataForProfile(profileId)
    }

    fun loadProfiles() {
        val loaded = profilePersistenceService.loadProfiles()
        _profiles.value = profileSettingsStore.attachSettingsToProfiles(loaded)
        _currentProfileId.value = profilePersistenceService.getCurrentProfileId()
    }

    fun reloadAfterSync() {
        loadProfiles()
        _currentProfileId.value?.let { storageTrackerViewModel.reloadDataForProfile(it) }
    }

    fun createProfile(name: String) {
        val previousId = _currentProfileId.value
        if (previousId != null) {
            settingsViewModel.persistActiveProfileSettings()
            syncProfileSettingsToProfileData(previousId, settingsViewModel.settings.value)
        }

        val newProfile = profilePersistenceService.createProfile(name)
        val newSettings = SettingsPartition.forProfileStorage(
            settingsViewModel.settings.value,
            newProfile.id
        )
        settingsViewModel.seedProfileSettings(newProfile.id, newSettings)

        val newProfileData = ProfileData(
            profile = newProfile,
            shelves = emptyList(),
            settings = newSettings
        )

        val updatedProfiles = _profiles.value.toMutableList()
        updatedProfiles.add(newProfileData)
        syncProfilesToDisk(updatedProfiles)

        switchProfile(newProfile.id)
    }

    fun switchProfile(profileId: String) {
        if (_currentProfileId.value == profileId) {
            return
        }

        val leavingId = _currentProfileId.value
        if (leavingId != null) {
            settingsViewModel.persistActiveProfileSettings()
            syncProfileSettingsToProfileData(leavingId, settingsViewModel.settings.value)
        }

        _currentProfileId.value = profileId
        profilePersistenceService.saveCurrentProfileId(profileId)
        settingsViewModel.switchToProfile(profileId)
        storageTrackerViewModel.reloadDataForProfile(profileId)

        profileSettingsStore.load(profileId)?.let { stored ->
            refreshProfileSettingsInList(profileId, stored)
        }
    }

    fun deleteProfile(profileId: String): Boolean {
        if (_profiles.value.size <= 1) {
            return false
        }

        val success = profilePersistenceService.deleteProfile(profileId)
        if (success) {
            profileSettingsStore.remove(profileId)
            tombstoneStore.append(
                Tombstone(id = profileId, entityType = TombstoneEntityType.PROFILE, profileId = profileId, deletedAt = Date())
            )
            val updatedProfiles = _profiles.value.filter { it.profile.id != profileId }
            _profiles.value = updatedProfiles

            if (_currentProfileId.value == profileId) {
                val newCurrentProfileId = updatedProfiles.firstOrNull()?.profile?.id
                if (newCurrentProfileId != null) {
                    switchProfile(newCurrentProfileId)
                }
            }
            onDataChanged()
        }
        return success
    }

    fun updateProfileName(profileId: String, newName: String): Boolean {
        val success = profilePersistenceService.updateProfileName(profileId, newName)
        if (success) {
            loadProfiles()
            onDataChanged()
        }
        return success
    }

    fun getCurrentProfile(): ProfileData? {
        return _profiles.value.find { it.profile.id == _currentProfileId.value }
    }

    fun importProfiles(profiles: List<ProfileData>, currentProfileId: String?): Boolean {
        if (profiles.isEmpty()) {
            return false
        }

        profiles.forEach { profileData ->
            profileSettingsStore.save(profileData.profile.id, profileData.settings)
        }

        val withSettings = profileSettingsStore.attachSettingsToProfiles(profiles)
        profilePersistenceService.saveProfiles(withSettings)
        withSettings.forEach { profileData ->
            storageTrackerPersistenceService.saveData(profileData.shelves, profileData.profile.id)
        }
        _profiles.value = withSettings

        val targetId = currentProfileId?.takeIf { id ->
            withSettings.any { it.profile.id == id }
        } ?: withSettings.first().profile.id

        _currentProfileId.value = targetId
        profilePersistenceService.saveCurrentProfileId(targetId)
        settingsViewModel.switchToProfile(targetId)
        storageTrackerViewModel.reloadDataForProfile(targetId)
        onDataChanged()
        return true
    }

    private fun syncProfileSettingsToProfileData(profileId: String, mergedSettings: Settings) {
        val storedSettings = SettingsPartition.forProfileStorage(mergedSettings, profileId)
        refreshProfileSettingsInList(profileId, storedSettings)
        val index = _profiles.value.indexOfFirst { it.profile.id == profileId }
        if (index == -1) return
        val updated = _profiles.value.toMutableList()
        updated[index] = updated[index].copy(settings = storedSettings)
        profilePersistenceService.saveProfiles(updated)
        _profiles.value = updated
        onDataChanged()
    }

    private fun refreshProfileSettingsInList(profileId: String, storedSettings: Settings) {
        val index = _profiles.value.indexOfFirst { it.profile.id == profileId }
        if (index == -1) return
        val updated = _profiles.value.toMutableList()
        updated[index] = updated[index].copy(settings = storedSettings)
        _profiles.value = updated
    }

    private fun syncProfilesToDisk(profiles: List<ProfileData>) {
        _profiles.value = profiles
        profilePersistenceService.saveProfiles(profiles)
        onDataChanged()
    }
}
