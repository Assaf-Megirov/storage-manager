package com.awindyendprod.storage_manager.model

data class ProfileData(
    val profile: Profile,
    val shelves: List<Shelf>,
    val settings: Settings
)
