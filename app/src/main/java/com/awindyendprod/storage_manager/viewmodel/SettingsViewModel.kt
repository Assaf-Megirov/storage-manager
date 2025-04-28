package com.awindyendprod.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.SectionDateType
import com.awindyendprod.storage_manager.model.DateDisplayFormat
import com.awindyendprod.storage_manager.model.AppLanguage
import android.content.res.Resources
import java.util.*
import com.awindyendprod.storage_manager.model.FontSize
import android.net.Uri
import android.util.Log
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService
import android.content.Intent
import androidx.core.content.FileProvider
import com.awindyendprod.storage_manager.model.Theme
import java.io.File

class SettingsViewModel(
    context: Context,
    private val persistenceService: StorageTrackerPersistenceService,
    private val storageTrackerViewModel: StorageTrackerViewModel
) : ViewModel() {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    
    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<Settings> = _settings.asStateFlow()

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
            sectionWidth = prefs.getInt("sectionWidth", 300),
            theme = Theme.valueOf(
                prefs.getString("theme", Theme.SYSTEM.name)!!
            )
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
            putString("theme", settings.theme.name)
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
        _recreateActivity.value = true
    }

    fun updateFontSize(fontSize: FontSize) {
        _settings.value = _settings.value.copy(fontSize = fontSize)
        saveSettings(_settings.value)
    }

    fun updateSectionHeight(height: Int) {
        if (height in 100..500) {
            _settings.value = _settings.value.copy(sectionHeight = height)
            saveSettings(_settings.value)
        }
    }

    fun updateSectionWidth(width: Int) {
        if (width in 100..500) {
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

    fun updateTheme(theme: Theme) {
        _settings.value = _settings.value.copy(theme = theme)
        saveSettings(_settings.value)
    }

    fun onActivityRecreated() {
        _recreateActivity.value = false
    }

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
                //TODO: Handle error
            }
        }
    }

    fun importData(uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val importedData = persistenceService.importFromFile(uri)
                updateSettings(importedData.settings)
                persistenceService.saveData(importedData.shelves)
                storageTrackerViewModel.reloadData()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error importing data", e)
                //TODO: Handle error
            }
        }
    }

    private fun updateSettings(newSettings: Settings) {
        _settings.value = newSettings
        saveSettings(newSettings)
        updateLocale(newSettings.language)
        _recreateActivity.value = true
    }

    fun shareData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val tempFile = File(appContext.cacheDir, "storage_manager_backup.json")
                tempFile.createNewFile()
                persistenceService.exportToFile(tempFile, settings.value)

                val contentUri = FileProvider.getUriForFile(
                    appContext,
                    "${appContext.packageName}.fileprovider",
                    tempFile
                )

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_STREAM, contentUri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooserIntent = Intent.createChooser(shareIntent, null)
                chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                appContext.startActivity(chooserIntent)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error sharing data", e)
            }
        }
    }
} 