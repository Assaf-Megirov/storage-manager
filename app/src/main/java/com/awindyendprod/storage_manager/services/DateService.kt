package com.awindyendprod.storage_manager.services

import com.awindyendprod.storage_manager.model.AppLanguage
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.DateDisplayFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun Date.toDisplayFormat(settings: Settings): String {
    val locale = when (settings.language) {
        AppLanguage.SYSTEM -> Locale.getDefault()
        AppLanguage.ENGLISH -> Locale("en")
        AppLanguage.HEBREW -> Locale("he")
        AppLanguage.RUSSIAN -> Locale("ru")
    }

    val dateFormat = when (settings.dateDisplayFormat) {
        DateDisplayFormat.NUMERIC -> SimpleDateFormat("dd/MM", locale)
        DateDisplayFormat.DAY_OF_WEEK -> SimpleDateFormat("EEE", locale)
    }

    return dateFormat.format(this)
}
