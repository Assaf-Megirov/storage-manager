package com.awindyendprod.storage_manager.model

import com.google.gson.annotations.SerializedName
import java.util.Date

enum class TombstoneEntityType {
    PROFILE,
    SHELF,
    SECTION,
    ITEM
}

data class Tombstone(
    @SerializedName("id") val id: String,
    @SerializedName("entityType") val entityType: TombstoneEntityType,
    @SerializedName("profileId") val profileId: String? = null,
    @SerializedName("deletedAt") val deletedAt: Date? = null
)
