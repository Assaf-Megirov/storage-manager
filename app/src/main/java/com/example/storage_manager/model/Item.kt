package com.example.storage_manager.model

import java.util.Date
import java.util.UUID

data class Item(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val clientName: String,
    val entryDate: Date,
    val returnDate: Date,
    val hasAlarm: Boolean,
    val alarmDateTime: Date? = null,
    val note: String
)