package com.awindyendprod.storage_manager.services

import android.content.Context
import android.util.Log
import com.awindyendprod.storage_manager.R
import com.awindyendprod.storage_manager.model.Profile
import com.awindyendprod.storage_manager.model.ProfileData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.UUID

class ProfilePersistenceService(private val context: Context) {
    private val prefs = context.getSharedPreferences("ProfilePrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveProfiles(profiles: List<ProfileData>) {
        val profilesJson = gson.toJson(profiles)
        prefs.edit().putString("profiles", profilesJson).apply()
    }

    fun loadProfiles(): List<ProfileData> {
        val profilesJson = prefs.getString("profiles", null)
        return profilesJson?.let {
            gson.fromJson(it, object : TypeToken<List<ProfileData>>() {}.type)
        } ?: emptyList()
    }

    fun saveCurrentProfileId(profileId: String) {
        prefs.edit().putString("currentProfileId", profileId).apply()
    }

    fun getCurrentProfileId(): String? {
        return prefs.getString("currentProfileId", null)
    }

    fun createDefaultProfile(): Profile {
        return Profile(
            id = UUID.randomUUID().toString(),
            name = context.getString(R.string.default_profile_name),
            isDefault = true
        )
    }

    fun createProfile(name: String): Profile {
        return Profile(
            id = UUID.randomUUID().toString(),
            name = name
        )
    }

    fun deleteProfile(profileId: String): Boolean {
        val profiles = loadProfiles().toMutableList()
        val removed = profiles.removeAll { it.profile.id == profileId }
        if (removed) {
            saveProfiles(profiles)
        }
        return removed
    }

    fun updateProfileName(profileId: String, newName: String): Boolean {
        val profiles = loadProfiles().toMutableList()
        val profileIndex = profiles.indexOfFirst { it.profile.id == profileId }
        if (profileIndex != -1) {
            val profileData = profiles[profileIndex]
            val updatedProfile = profileData.profile.copy(name = newName)
            profiles[profileIndex] = profileData.copy(profile = updatedProfile)
            saveProfiles(profiles)
            return true
        }
        return false
    }
}
