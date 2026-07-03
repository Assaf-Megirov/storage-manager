package com.awindyendprod.storage_manager.model

import com.google.gson.annotations.SerializedName
import java.util.Date

data class Profile(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("createdAt") val createdAt: Date = Date(),
    @SerializedName("isDefault") val isDefault: Boolean = false
)
