package com.awindyendprod.storage_manager.services

import android.content.Context
import android.net.Uri
import android.util.Log
import com.awindyendprod.storage_manager.model.ExportData
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.Shelf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.File

class StorageTrackerPersistenceService(private val context: Context) {
    private val prefs = context.getSharedPreferences("StorageTrackerPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

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

    fun exportToFile(file: File, settings: Settings) {
        try {
            val exportData = ExportData(
                settings = settings,
                shelves = loadData(),
                version = 1
            )
            val jsonString = gson.toJson(exportData)
            file.writeText(jsonString)
        } catch (e: Exception) {
            Log.e("StorageTrackerPersistenceService", "Error exporting data to file", e)
            throw e
        }
    }

    @Deprecated("Use the version with settings parameter instead")
    fun exportToFile(file: File) {
        exportToFile(file, loadSettings())
    }

    fun exportToFile(uri: Uri, settings: Settings) {
        try {
            val exportData = ExportData(
                settings = settings,
                shelves = loadData(),
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
                return gson.fromJson(jsonString, ExportData::class.java)
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
}