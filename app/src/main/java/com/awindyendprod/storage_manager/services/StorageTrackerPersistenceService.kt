package com.awindyendprod.storage_manager.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.awindyendprod.storage_manager.model.ExportData
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.Shelf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class StorageTrackerPersistenceService(private val context: Context) {
    private val prefs = context.getSharedPreferences("StorageTrackerPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveData(shelves: List<Shelf>, profileId: String) {
        val shelvesJson = gson.toJson(shelves)
        prefs.edit().putString("shelves_$profileId", shelvesJson).apply()
    }

    fun loadData(profileId: String): List<Shelf> {
        val shelvesJson = prefs.getString("shelves_$profileId", null)
        return shelvesJson?.let {
            gson.fromJson(it, object : TypeToken<List<Shelf>>() {}.type)
        } ?: emptyList()
    }

    fun attachShelvesToProfiles(profiles: List<ProfileData>): List<ProfileData> =
        profiles.map { profileData -> profileData.copy(shelves = loadData(profileData.profile.id)) }

    fun removeData(profileId: String) {
        prefs.edit().remove("shelves_$profileId").apply()
    }

    // Legacy method for backward compatibility
    fun saveData(shelves: List<Shelf>) {
        val shelvesJson = gson.toJson(shelves)
        prefs.edit().putString("shelves", shelvesJson).apply()
    }

    fun loadData(): List<Shelf> {
        val shelvesJson = prefs.getString("shelves", null)
        return shelvesJson?.let {
            gson.fromJson(it, object : TypeToken<List<Shelf>>() {}.type)
        } ?: emptyList()
    }

    fun exportToFile(file: File, globalSettings: Settings, profiles: List<ProfileData>, currentProfileId: String?) {
        try {
            val exportData = ExportData(
                globalSettings = globalSettings,
                profiles = profiles,
                currentProfileId = currentProfileId,
                version = 1
            )
            val jsonString = gson.toJson(exportData)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e("StorageTrackerPersistenceService", "Error exporting data to file", e)
            throw e
        }
    }

    @Deprecated("Use the version with settings, profiles, and currentProfileId parameters instead")
    fun exportToFile(file: File) {
        val settings = loadSettings()
        val profiles = loadProfiles()
        val currentProfileId = loadCurrentProfileId()
        exportToFile(file, settings, profiles, currentProfileId)
    }

    fun exportToFile(uri: Uri, globalSettings: Settings, profiles: List<ProfileData>, currentProfileId: String?) {
        try {
            val exportData = ExportData(
                globalSettings = globalSettings,
                profiles = profiles,
                currentProfileId = currentProfileId,
                version = 1
            )
            val jsonString = gson.toJson(exportData)
            context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray())
            } ?: throw IllegalStateException("Could not open output stream")
        } catch (e: Exception) {
            Log.e("StorageTrackerPersistenceService", "Error exporting data to URI", e)
            throw e
        }
    }

    fun importFromFile(uri: Uri): ExportData {
        try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val jsonString = inputStream.bufferedReader().use { it.readText() }
                val normalized = BackupJsonCompat.normalizeExportJson(jsonString)
                return gson.fromJson(normalized, ExportData::class.java)
            }
            throw IllegalStateException("Could not open input stream")
        } catch (e: Exception) {
            Log.e("StorageTrackerPersistenceService", "Error importing data", e)
            throw e
        }
    }

    private fun loadSettings(): Settings {
        val settingsJson = prefs.getString("settings", null)
        return settingsJson?.let {
            gson.fromJson(it, Settings::class.java)
        } ?: Settings()
    }

    private fun loadProfiles(): List<ProfileData> {
        val profilesJson = prefs.getString("profiles", null)
        return profilesJson?.let {
            gson.fromJson(it, object : TypeToken<List<ProfileData>>() {}.type)
        } ?: emptyList()
    }

    private fun loadCurrentProfileId(): String? {
        return prefs.getString("currentProfileId", null)
    }
}