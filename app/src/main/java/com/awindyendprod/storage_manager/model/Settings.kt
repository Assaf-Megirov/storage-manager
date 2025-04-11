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
    SYSTEM,  // Use system language
    ENGLISH,
    HEBREW,
    RUSSIAN
}

enum class FontSize {
    SMALL,
    MEDIUM,
    LARGE
}

data class Settings(
    val sectionDateType: SectionDateType = SectionDateType.ENTRY_DATE,
    val dateDisplayFormat: DateDisplayFormat = DateDisplayFormat.NUMERIC,
    val defaultReturnDateDays: Int = 14,
    val language: AppLanguage = AppLanguage.SYSTEM,
    val fontSize: FontSize = FontSize.MEDIUM,
    val sectionHeight: Int = 210, // Default height in dp
    val sectionWidth: Int = 300   // Default width in dp
) 