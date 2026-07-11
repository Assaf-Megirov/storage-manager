package com.awindyendprod.storage_manager.services

import android.content.Context
import com.awindyendprod.storage_manager.model.Tombstone
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class TombstoneStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun append(tombstone: Tombstone) {
        val merged = (loadAll() + tombstone)
            .groupBy { it.entityType to it.id }
            .map { (_, group) -> group.maxBy { it.deletedAt?.time ?: 0L } }
        replaceAll(merged)
    }

    fun loadAll(): List<Tombstone> {
        val json = prefs.getString(KEY_TOMBSTONES, null) ?: return emptyList()
        return gson.fromJson(json, object : TypeToken<List<Tombstone>>() {}.type) ?: emptyList()
    }

    fun replaceAll(tombstones: List<Tombstone>) {
        prefs.edit().putString(KEY_TOMBSTONES, gson.toJson(tombstones)).commit()
    }

    companion object {
        private const val PREFS_NAME = "TombstoneStore"
        private const val KEY_TOMBSTONES = "tombstones"
    }
}
