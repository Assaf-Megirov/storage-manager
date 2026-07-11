package com.awindyendprod.storage_manager.services

import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

object BackupJsonCompat {

    private val profileDataKeyMap = mapOf(
        "a" to "profile",
        "b" to "shelves",
        "c" to "settings"
    )

    private val profileKeyMap = mapOf(
        "a" to "id",
        "b" to "name",
        "c" to "createdAt",
        "d" to "isDefault"
    )

    fun normalizeExportJson(json: String): String {
        val root = JsonParser.parseString(json)
        if (!root.isJsonObject) return json
        val obj = root.asJsonObject
        if (obj.has("profiles") && obj.get("profiles").isJsonArray) {
            obj.add("profiles", normalizeProfileDataArray(obj.getAsJsonArray("profiles")))
        }
        return obj.toString()
    }

    fun normalizeProfilesJson(json: String): String {
        val element = JsonParser.parseString(json)
        if (!element.isJsonArray) return json
        return normalizeProfileDataArray(element.asJsonArray).toString()
    }

    private fun normalizeProfileDataArray(array: JsonArray): JsonArray {
        val out = JsonArray()
        for (element in array) {
            out.add(normalizeProfileDataElement(element))
        }
        return out
    }

    private fun normalizeProfileDataElement(element: JsonElement): JsonElement {
        if (!element.isJsonObject) return element
        val src = element.asJsonObject
        if (src.has("profile") || !src.has("a")) {
            return element
        }
        val dst = JsonObject()
        for ((key, value) in src.entrySet()) {
            val mapped = profileDataKeyMap[key] ?: key
            dst.add(
                mapped,
                when (mapped) {
                    "profile" -> normalizeProfileElement(value)
                    else -> value
                }
            )
        }
        return dst
    }

    private fun normalizeProfileElement(element: JsonElement): JsonElement {
        if (!element.isJsonObject) return element
        val src = element.asJsonObject
        if (src.has("id") || !src.has("a")) {
            return element
        }
        val dst = JsonObject()
        for ((key, value) in src.entrySet()) {
            dst.add(profileKeyMap[key] ?: key, value)
        }
        return dst
    }
}
