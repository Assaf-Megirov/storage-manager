package com.awindyendprod.storage_manager.model

import com.google.gson.annotations.SerializedName

data class ProfileData(
    @SerializedName("profile") val profile: Profile,
    @SerializedName("shelves") val shelves: List<Shelf>,
    @SerializedName("settings") val settings: Settings
)
