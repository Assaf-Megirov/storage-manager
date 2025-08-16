package com.awindyendprod.storage_manager.model

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
    val sectionDateType: SectionDateType = SectionDateType.ENTRY_DATE,
    val dateDisplayFormat: DateDisplayFormat = DateDisplayFormat.NUMERIC,
    val defaultReturnDateDays: Int = 14,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val sectionHeight: Int = 210,
    val sectionWidth: Int = 300,
    val theme: Theme = Theme.SYSTEM,
    val fabDragEnabled: Boolean = true,
    val fabPositionMainScreenX: Float = Float.MIN_VALUE, // Use MIN_VALUE to indicate default position
    val fabPositionMainScreenY: Float = Float.MIN_VALUE,
    val fabPositionSectionScreenX: Float = Float.MIN_VALUE,
    val fabPositionSectionScreenY: Float = Float.MIN_VALUE,
    val hasSeenLongPressHint: Boolean = false,
    val notificationDaysBefore: Int = 1,
    val notificationMaxItems: Int = 10,
    val dailyNotificationsEnabled: Boolean = true
) 