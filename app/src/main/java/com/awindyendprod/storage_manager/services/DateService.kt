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

fun Date.toShortDisplayFormat(settings: Settings): String {
    val locale = when (settings.language) {
        AppLanguage.SYSTEM -> Locale.getDefault()
        AppLanguage.ENGLISH -> Locale("en")
        AppLanguage.HEBREW -> Locale("he")
        AppLanguage.RUSSIAN -> Locale("ru")
    }
    
    return SimpleDateFormat("MMM dd", locale).format(this)
}

fun Date.toLongDisplayFormat(settings: Settings): String {
    val locale = when (settings.language) {
        AppLanguage.SYSTEM -> Locale.getDefault()
        AppLanguage.ENGLISH -> Locale("en")
        AppLanguage.HEBREW -> Locale("he")
        AppLanguage.RUSSIAN -> Locale("ru")
    }
    
    return SimpleDateFormat("MMM dd, yyyy", locale).format(this)
}

fun Date.toDateTimeFormat(settings: Settings): String {
    val locale = when (settings.language) {
        AppLanguage.SYSTEM -> Locale.getDefault()
        AppLanguage.ENGLISH -> Locale("en")
        AppLanguage.HEBREW -> Locale("he")
        AppLanguage.RUSSIAN -> Locale("ru")
    }
    
    return SimpleDateFormat("yyyy-MM-dd HH:mm", locale).format(this)
}

fun Date.toFullDateFormat(settings: Settings): String {
    val locale = when (settings.language) {
        AppLanguage.SYSTEM -> Locale.getDefault()
        AppLanguage.ENGLISH -> Locale("en")
        AppLanguage.HEBREW -> Locale("he")
        AppLanguage.RUSSIAN -> Locale("ru")
    }
    
    return SimpleDateFormat("dd/MM/yyyy", locale).format(this)
}