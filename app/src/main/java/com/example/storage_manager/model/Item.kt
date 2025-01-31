package com.example.storage_manager.model

import java.util.Date
import java.util.UUID

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val clientName: String = "",
    val entryDate: Date? = null,
    val returnDate: Date? = null,
    val hasAlarm: Boolean = false,
    val alarmDate: Date? = null,
    val note: String = ""
)