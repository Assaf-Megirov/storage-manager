package com.awindyendprod.storage_manager.model

import java.util.Date

data class Profile(
    val id: String,
    val name: String,
    val createdAt: Date = Date(),
    val isDefault: Boolean = false
)
