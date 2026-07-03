package com.awindyendprod.storage_manager.model

import com.google.gson.annotations.SerializedName

enum class SectionDateType {
    ENTRY_DATE,
    RETURN_DATE
}

enum class DateDisplayFormat {
    NUMERIC,
    DAY_OF_WEEK
}

enum class AppLanguage {
    SYSTEM,
    ENGLISH,
    HEBREW,
    RUSSIAN
}

enum class FontSize {
    SMALL,
    MEDIUM,
    LARGE
}

enum class Theme{
    SYSTEM,
    LIGHT,
    DARK
}

data class Settings(
    @SerializedName("sectionDateType") val sectionDateType: SectionDateType = SectionDateType.ENTRY_DATE,
    @SerializedName("dateDisplayFormat") val dateDisplayFormat: DateDisplayFormat = DateDisplayFormat.NUMERIC,
    @SerializedName("defaultReturnDateDays") val defaultReturnDateDays: Int = 14,
    @SerializedName("language") val language: AppLanguage = AppLanguage.SYSTEM,
    @SerializedName("fontSize") val fontSize: FontSize = FontSize.MEDIUM,
    @SerializedName("sectionHeight") val sectionHeight: Int = 210,
    @SerializedName("sectionWidth") val sectionWidth: Int = 300,
    @SerializedName("theme") val theme: Theme = Theme.SYSTEM,
    @SerializedName("fabDragEnabled") val fabDragEnabled: Boolean = true,
    @SerializedName("fabPositionMainScreenX") val fabPositionMainScreenX: Float = Float.MIN_VALUE,
    @SerializedName("fabPositionMainScreenY") val fabPositionMainScreenY: Float = Float.MIN_VALUE,
    @SerializedName("fabPositionSectionScreenX") val fabPositionSectionScreenX: Float = Float.MIN_VALUE,
    @SerializedName("fabPositionSectionScreenY") val fabPositionSectionScreenY: Float = Float.MIN_VALUE,
    @SerializedName("hasSeenLongPressHint") val hasSeenLongPressHint: Boolean = false,
    @SerializedName("notificationDaysBefore") val notificationDaysBefore: Int = 1,
    @SerializedName("notificationMaxItems") val notificationMaxItems: Int = 10,
    @SerializedName("dailyNotificationsEnabled") val dailyNotificationsEnabled: Boolean = true,
    @SerializedName("currentProfileId") val currentProfileId: String? = null,
    @SerializedName("showProfilesButton") val showProfilesButton: Boolean = true
) 