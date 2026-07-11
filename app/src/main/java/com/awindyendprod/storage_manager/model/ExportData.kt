package com.awindyendprod.storage_manager.model

data class ExportData(
    val globalSettings: Settings, // App-wide settings (language, theme, etc.)
    val profiles: List<ProfileData>,
    val currentProfileId: String?,
    val version: Int,
    val tombstones: List<Tombstone> = emptyList(),
    val mainDeviceId: String? = null
)