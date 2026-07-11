package com.awindyendprod.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService
import android.content.Intent
import androidx.core.content.FileProvider
import com.awindyendprod.storage_manager.model.Theme
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.services.ProfileSettingsStore
import com.awindyendprod.storage_manager.services.SettingsPartition
import kotlinx.coroutines.withContext

import java.io.File

enum class DataTransferResult {
    Success,
    Failed
}

class SettingsViewModel(
    context: Context,
    private val persistenceService: StorageTrackerPersistenceService,
    private val storageTrackerViewModel: StorageTrackerViewModel,
    private val profileSettingsStore: ProfileSettingsStore,
) : ViewModel() {

    private val appContext = context.applicationContext

    private val globalPrefs = appContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private var prefs = globalPrefs

    private val _settings = MutableStateFlow(loadSettingsFromPrefs(prefs))
    val settings: StateFlow<Settings> = _settings.asStateFlow()

    private val _recreateActivity = MutableStateFlow(false)
    val recreateActivity: StateFlow<Boolean> = _recreateActivity.asStateFlow()

    private val _dataTransferResult = MutableStateFlow<DataTransferResult?>(null)
    val dataTransferResult: StateFlow<DataTransferResult?> = _dataTransferResult.asStateFlow()

    private var activeProfileId: String? = null

    fun clearDataTransferResult() {
        _dataTransferResult.value = null
    }

    fun switchToProfile(profileId: String) {
        persistActiveProfileSettings()
        activeProfileId = profileId
        prefs = profileSettingsStore.getProfilePrefs(profileId)
        val loaded = loadSettingsFromPrefs(prefs)
        applySettingsInternal(loaded.copy(currentProfileId = profileId), recreateForLanguage = true)
    }

    fun seedProfileSettings(profileId: String, settings: Settings) {
        val profilePrefs = profileSettingsStore.getProfilePrefs(profileId)
        saveSettingsToPrefs(settings, profilePrefs)
        profileSettingsStore.save(profileId, settings)
    }

    fun persistActiveProfileSettings() {
        val profileId = activeProfileId ?: return
        saveSettingsToPrefs(_settings.value, prefs)
        profileSettingsStore.save(profileId, _settings.value)
    }

    private fun applySettingsInternal(settings: Settings, recreateForLanguage: Boolean) {
        val previousLanguage = _settings.value.language
        _settings.value = settings
        saveSettingsToPrefs(settings, prefs)
        saveSettingsToPrefs(settings, globalPrefs)
        if (recreateForLanguage && settings.language != previousLanguage) {
            updateLocale(settings.language)
            _recreateActivity.value = true
        }
    }

    private fun afterSettingChanged() {
        saveSettingsToPrefs(_settings.value, prefs)
        saveSettingsToPrefs(_settings.value, globalPrefs)
        persistActiveProfileSettings()
    }

    private fun loadSettingsFromPrefs(preferences: android.content.SharedPreferences): Settings {
        return Settings(
            sectionDateType = SectionDateType.valueOf(
                preferences.getString("sectionDateType", SectionDateType.ENTRY_DATE.name)!!
            ),
            dateDisplayFormat = DateDisplayFormat.valueOf(
                preferences.getString("dateDisplayFormat", DateDisplayFormat.NUMERIC.name)!!
            ),
            defaultReturnDateDays = preferences.getInt("defaultReturnDateDays", 14),
            language = AppLanguage.valueOf(
                preferences.getString("language", AppLanguage.SYSTEM.name)!!
            ),
            fontSize = FontSize.valueOf(
                preferences.getString("fontSize", FontSize.MEDIUM.name)!!
            ),
            sectionHeight = preferences.getInt("sectionHeight", 210),
            sectionWidth = preferences.getInt("sectionWidth", 300),
            theme = Theme.valueOf(
                preferences.getString("theme", Theme.SYSTEM.name)!!
            ),
            fabDragEnabled = preferences.getBoolean("fabDragEnabled", true),
            fabPositionMainScreenX = preferences.getFloat("fabPositionMainScreenX", Float.MIN_VALUE),
            fabPositionMainScreenY = preferences.getFloat("fabPositionMainScreenY", Float.MIN_VALUE),
            fabPositionSectionScreenX = preferences.getFloat("fabPositionSectionScreenX", Float.MIN_VALUE),
            fabPositionSectionScreenY = preferences.getFloat("fabPositionSectionScreenY", Float.MIN_VALUE),
            hasSeenLongPressHint = preferences.getBoolean("hasSeenLongPressHint", false),
            notificationDaysBefore = preferences.getInt("notificationDaysBefore", 1),
            notificationMaxItems = preferences.getInt("notificationMaxItems", 10),
            dailyNotificationsEnabled = preferences.getBoolean("dailyNotificationsEnabled", true),
            showProfilesButton = preferences.getBoolean("showProfilesButton", true)
        )
    }

    private fun saveSettingsToPrefs(settings: Settings, preferences: android.content.SharedPreferences) {
        preferences.edit().apply {
            putString("sectionDateType", settings.sectionDateType.name)
            putString("dateDisplayFormat", settings.dateDisplayFormat.name)
            putInt("defaultReturnDateDays", settings.defaultReturnDateDays)
            putString("language", settings.language.name)
            putString("fontSize", settings.fontSize.name)
            putInt("sectionHeight", settings.sectionHeight)
            putInt("sectionWidth", settings.sectionWidth)
            putString("theme", settings.theme.name)
            putBoolean("fabDragEnabled", settings.fabDragEnabled)
            putFloat("fabPositionMainScreenX", settings.fabPositionMainScreenX)
            putFloat("fabPositionMainScreenY", settings.fabPositionMainScreenY)
            putFloat("fabPositionSectionScreenX", settings.fabPositionSectionScreenX)
            putFloat("fabPositionSectionScreenY", settings.fabPositionSectionScreenY)
            putBoolean("hasSeenLongPressHint", settings.hasSeenLongPressHint)
            putInt("notificationDaysBefore", settings.notificationDaysBefore)
            putInt("notificationMaxItems", settings.notificationMaxItems)
            putBoolean("dailyNotificationsEnabled", settings.dailyNotificationsEnabled)
            putBoolean("showProfilesButton", settings.showProfilesButton)
            commit()
        }
    }

    fun updateSectionDateType(type: SectionDateType) {
        _settings.value = _settings.value.copy(sectionDateType = type)
        afterSettingChanged()
    }

    fun updateDateDisplayFormat(format: DateDisplayFormat) {
        _settings.value = _settings.value.copy(dateDisplayFormat = format)
        afterSettingChanged()
    }

    fun updateDefaultReturnDateDays(days: Int) {
        _settings.value = _settings.value.copy(defaultReturnDateDays = days)
        afterSettingChanged()
    }

    fun updateLanguage(language: AppLanguage) {
        _settings.value = _settings.value.copy(language = language)
        afterSettingChanged()
        updateLocale(language)
        _recreateActivity.value = true
    }

    fun updateFontSize(fontSize: FontSize) {
        _settings.value = _settings.value.copy(fontSize = fontSize)
        afterSettingChanged()
    }

    fun updateSectionHeight(height: Int) {
        if (height in 100..500) {
            _settings.value = _settings.value.copy(sectionHeight = height)
            afterSettingChanged()
        }
    }

    fun updateSectionWidth(width: Int) {
        if (width in 100..500) {
            _settings.value = _settings.value.copy(sectionWidth = width)
            afterSettingChanged()
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
        afterSettingChanged()
    }

    fun updateFabDragEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(fabDragEnabled = enabled)
        afterSettingChanged()
    }

    fun updateFabPositionMainScreen(x: Float, y: Float) {
        _settings.value = _settings.value.copy(
            fabPositionMainScreenX = x,
            fabPositionMainScreenY = y
        )
        afterSettingChanged()
    }

    fun updateFabPositionSectionScreen(x: Float, y: Float) {
        _settings.value = _settings.value.copy(
            fabPositionSectionScreenX = x,
            fabPositionSectionScreenY = y
        )
        afterSettingChanged()
    }

    fun resetFabPositions() {
        _settings.value = _settings.value.copy(
            fabPositionMainScreenX = Float.MIN_VALUE,
            fabPositionMainScreenY = Float.MIN_VALUE,
            fabPositionSectionScreenX = Float.MIN_VALUE,
            fabPositionSectionScreenY = Float.MIN_VALUE
        )
        afterSettingChanged()
    }

    fun updateHasSeenLongPressHint(hasSeen: Boolean) {
        _settings.value = _settings.value.copy(hasSeenLongPressHint = hasSeen)
        afterSettingChanged()
    }

    fun updateNotificationDaysBefore(days: Int) {
        if (days in 0..7) {
            _settings.value = _settings.value.copy(notificationDaysBefore = days)
            afterSettingChanged()
        }
    }

    fun updateNotificationMaxItems(maxItems: Int) {
        if (maxItems in 1..100) {
            _settings.value = _settings.value.copy(notificationMaxItems = maxItems)
            afterSettingChanged()
        }
    }

    fun updateDailyNotificationsEnabled(enabled: Boolean) {
        _settings.value = _settings.value.copy(dailyNotificationsEnabled = enabled)
        afterSettingChanged()
    }

    fun updateShowProfilesButton(enabled: Boolean) {
        _settings.value = _settings.value.copy(showProfilesButton = enabled)
        afterSettingChanged()
    }

    fun onActivityRecreated() {
        _recreateActivity.value = false
    }

    fun exportData(uri: Uri, profiles: List<ProfileData>, currentProfileId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                persistActiveProfileSettings()
                val withSettings = profileSettingsStore.attachSettingsToProfiles(profiles)
                val withShelves = persistenceService.attachShelvesToProfiles(withSettings)
                persistenceService.exportToFile(uri, settings.value, withShelves, currentProfileId)
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error exporting data", e)
            }
        }
    }

    fun importData(
        uri: Uri,
        onProfilesImported: (List<ProfileData>, String?) -> Boolean
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val importedData = persistenceService.importFromFile(uri)
                withContext(Dispatchers.Main) {
                    val applied = onProfilesImported(importedData.profiles, importedData.currentProfileId)
                    _dataTransferResult.value = if (applied) {
                        DataTransferResult.Success
                    } else {
                        DataTransferResult.Failed
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error importing data", e)
                withContext(Dispatchers.Main) {
                    _dataTransferResult.value = DataTransferResult.Failed
                }
            }
        }
    }

    fun shareData(profiles: List<ProfileData>, currentProfileId: String?) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                persistActiveProfileSettings()
                val withSettings = profileSettingsStore.attachSettingsToProfiles(profiles)
                val withShelves = persistenceService.attachShelvesToProfiles(withSettings)
                val tempFile = File(appContext.cacheDir, "storage_manager_backup.json")
                tempFile.createNewFile()
                persistenceService.exportToFile(tempFile, settings.value, withShelves, currentProfileId)

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
