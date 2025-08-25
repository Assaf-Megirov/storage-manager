package com.awindyendprod.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.services.ProfilePersistenceService
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService

class ProfileViewModel(
    context: Context,
    private val profilePersistenceService: ProfilePersistenceService,
    private val storageTrackerViewModel: StorageTrackerViewModel
) : ViewModel() {
    
    private val _profiles = MutableStateFlow<List<ProfileData>>(emptyList())
    val profiles: StateFlow<List<ProfileData>> = _profiles.asStateFlow()

    private val _currentProfileId = MutableStateFlow<String?>(null)
    val currentProfileId: StateFlow<String?> = _currentProfileId.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        _profiles.value = profilePersistenceService.loadProfiles()
        _currentProfileId.value = profilePersistenceService.getCurrentProfileId()
    }

    fun createProfile(name: String) {
        val newProfile = profilePersistenceService.createProfile(name)
        val newProfileData = ProfileData(
            profile = newProfile,
            shelves = emptyList(),
            settings = com.awindyendprod.storage_manager.model.Settings(currentProfileId = newProfile.id)
        )
        
        val updatedProfiles = _profiles.value.toMutableList()
        updatedProfiles.add(newProfileData)
        _profiles.value = updatedProfiles
        
        profilePersistenceService.saveProfiles(updatedProfiles)
        
        // Automatically switch to the newly created profile
        switchProfile(newProfile.id)
    }

    fun switchProfile(profileId: String) {
        _currentProfileId.value = profileId
        profilePersistenceService.saveCurrentProfileId(profileId)
        
        // Reload data for the new profile
        storageTrackerViewModel.reloadDataForProfile(profileId)
    }

    fun deleteProfile(profileId: String): Boolean {
        if (_profiles.value.size <= 1) {
            return false // Don't delete the last profile
        }
        
        val success = profilePersistenceService.deleteProfile(profileId)
        if (success) {
            val updatedProfiles = _profiles.value.filter { it.profile.id != profileId }
            _profiles.value = updatedProfiles
            
            // If we deleted the current profile, switch to the first available profile
            if (_currentProfileId.value == profileId) {
                val newCurrentProfileId = updatedProfiles.firstOrNull()?.profile?.id
                if (newCurrentProfileId != null) {
                    switchProfile(newCurrentProfileId)
                }
            }
        }
        return success
    }

    fun updateProfileName(profileId: String, newName: String): Boolean {
        val success = profilePersistenceService.updateProfileName(profileId, newName)
        if (success) {
            loadProfiles() // Reload profiles to get updated names
        }
        return success
    }

    fun getCurrentProfile(): ProfileData? {
        return _profiles.value.find { it.profile.id == _currentProfileId.value }
    }

    class Factory(
        private val context: Context,
        private val profilePersistenceService: ProfilePersistenceService,
        private val storageTrackerViewModel: StorageTrackerViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
                return ProfileViewModel(
                    context.applicationContext,
                    profilePersistenceService,
                    storageTrackerViewModel
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
