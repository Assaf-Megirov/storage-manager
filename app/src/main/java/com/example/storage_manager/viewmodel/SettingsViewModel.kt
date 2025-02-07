package com.example.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.storage_manager.model.Settings
import com.example.storage_manager.model.SectionDateType
import com.example.storage_manager.model.DateDisplayFormat
import com.example.storage_manager.model.AppLanguage
import android.content.res.Resources
import java.util.*
import com.example.storage_manager.model.FontSize
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.example.storage_manager.services.StorageTrackerPersistenceService

class SettingsViewModel(
    context: Context,
    private val persistenceService: StorageTrackerPersistenceService,
    private val storageTrackerViewModel: StorageTrackerViewModel
) : ViewModel() {
    // Store application context reference for configuration updates
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    // Add this to track when we need to recreate the activity
    private val _recreateActivity = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity.asStateFlow()

    private fun loadSettings(): Settings {
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
            sectionWidth = prefs.getInt("sectionWidth", 300)
        )
    }

    private fun saveSettings(settings: Settings) {
        prefs.edit().apply {
            putString("sectionDateType", settings.sectionDateType.name)
            putString("dateDisplayFormat", settings.dateDisplayFormat.name)
            putInt("defaultReturnDateDays", settings.defaultReturnDateDays)
            putString("language", settings.language.name)
            putString("fontSize", settings.fontSize.name)
            putInt("sectionHeight", settings.sectionHeight)
            putInt("sectionWidth", settings.sectionWidth)
            apply()
        }
    }

    fun updateSectionDateType(type: SectionDateType) {
        _settings.value = _settings.value.copy(sectionDateType = type)
        saveSettings(_settings.value)
    }

    fun updateDateDisplayFormat(format: DateDisplayFormat) {
        _settings.value = _settings.value.copy(dateDisplayFormat = format)
        saveSettings(_settings.value)
    }

    fun updateDefaultReturnDateDays(days: Int) {
        _settings.value = _settings.value.copy(defaultReturnDateDays = days)
        saveSettings(_settings.value)
    }

    fun updateLanguage(language: AppLanguage) {
        _settings.value = _settings.value.copy(language = language)
        saveSettings(_settings.value)
        updateLocale(language)
        // Trigger activity recreation
        _recreateActivity.value = true
    }

    fun updateFontSize(fontSize: FontSize) {
        _settings.value = _settings.value.copy(fontSize = fontSize)
        saveSettings(_settings.value)
    }

    fun updateSectionHeight(height: Int) {
        if (height in 100..500) {  // Validate range
            _settings.value = _settings.value.copy(sectionHeight = height)
            saveSettings(_settings.value)
        }
    }

    fun updateSectionWidth(width: Int) {
        if (width in 100..500) {  // Changed from 200 to 100
            _settings.value = _settings.value.copy(sectionWidth = width)
            saveSettings(_settings.value)
        }
    }

    private fun updateLocale(language: AppLanguage) {
        val locale = when (language) {
            AppLanguage.SYSTEM -> Resources.getSystem().configuration.locales[0]
            AppLanguage.ENGLISH -> Locale("en")
            AppLanguage.HEBREW -> Locale("iw")
            AppLanguage.RUSSIAN -> Locale("ru")
        }
        
        val config = appContext.resources.configuration
        config.setLocale(locale)
        appContext.createConfigurationContext(config)
        Locale.setDefault(locale)
    }

    // Call this after recreating the activity
    fun onActivityRecreated() {
        _recreateActivity.value = false
    }

    // Factory for creating SettingsViewModel with context
    class Factory(
        private val context: Context,
        private val persistenceService: StorageTrackerPersistenceService,
        private val storageTrackerViewModel: StorageTrackerViewModel
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                return SettingsViewModel(
                    context.applicationContext,
                    persistenceService,
                    storageTrackerViewModel
                ) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }

    fun exportData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                persistenceService.exportToFile(uri, settings.value)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error exporting data", e)
                // Handle error (you might want to add error state handling)
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val importedData = persistenceService.importFromFile(uri)
                // Update settings
                updateSettings(importedData.settings)
                // Update shelves data
                persistenceService.saveData(importedData.shelves)
                // Notify StorageTrackerViewModel to reload
                storageTrackerViewModel.reloadData()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error importing data", e)
                // Handle error
            }
        }
    }

    private fun updateSettings(newSettings: Settings) {
        _settings.value = newSettings
        saveSettings(newSettings)
        // Always recreate after import to refresh all data
        updateLocale(newSettings.language)
        _recreateActivity.value = true
    }
} 