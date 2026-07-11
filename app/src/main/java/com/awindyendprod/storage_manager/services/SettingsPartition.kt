package com.awindyendprod.storage_manager.services

import com.awindyendprod.storage_manager.model.Settings

object SettingsPartition {

    fun forProfileStorage(settings: Settings, profileId: String): Settings =
        settings.copy(currentProfileId = profileId)

    fun isPristineProfileSettings(settings: Settings): Boolean {
        val defaults = Settings()
        return settings.sectionDateType == defaults.sectionDateType &&
            settings.dateDisplayFormat == defaults.dateDisplayFormat &&
            settings.defaultReturnDateDays == defaults.defaultReturnDateDays &&
            settings.language == defaults.language &&
            settings.fontSize == defaults.fontSize &&
            settings.sectionHeight == defaults.sectionHeight &&
            settings.sectionWidth == defaults.sectionWidth &&
            settings.theme == defaults.theme &&
            settings.fabDragEnabled == defaults.fabDragEnabled &&
            settings.fabPositionMainScreenX == defaults.fabPositionMainScreenX &&
            settings.fabPositionMainScreenY == defaults.fabPositionMainScreenY &&
            settings.fabPositionSectionScreenX == defaults.fabPositionSectionScreenX &&
            settings.fabPositionSectionScreenY == defaults.fabPositionSectionScreenY &&
            !settings.hasSeenLongPressHint &&
            settings.notificationDaysBefore == defaults.notificationDaysBefore &&
            settings.notificationMaxItems == defaults.notificationMaxItems &&
            settings.dailyNotificationsEnabled == defaults.dailyNotificationsEnabled &&
            settings.showProfilesButton == defaults.showProfilesButton
    }

    fun fillDefaultsFromGlobal(stored: Settings, global: Settings): Settings {
        val defaults = Settings()
        return stored.copy(
            language = if (stored.language == defaults.language) global.language else stored.language,
            fontSize = if (stored.fontSize == defaults.fontSize) global.fontSize else stored.fontSize,
            theme = if (stored.theme == defaults.theme) global.theme else stored.theme,
            notificationDaysBefore = if (stored.notificationDaysBefore == defaults.notificationDaysBefore) {
                global.notificationDaysBefore
            } else {
                stored.notificationDaysBefore
            },
            notificationMaxItems = if (stored.notificationMaxItems == defaults.notificationMaxItems) {
                global.notificationMaxItems
            } else {
                stored.notificationMaxItems
            },
            dailyNotificationsEnabled = if (stored.dailyNotificationsEnabled == defaults.dailyNotificationsEnabled) {
                global.dailyNotificationsEnabled
            } else {
                stored.dailyNotificationsEnabled
            },
            showProfilesButton = if (stored.showProfilesButton == defaults.showProfilesButton) {
                global.showProfilesButton
            } else {
                stored.showProfilesButton
            }
        )
    }
}
