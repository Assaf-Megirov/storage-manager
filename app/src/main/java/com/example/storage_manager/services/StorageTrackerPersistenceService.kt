package com.example.storage_manager.services

import android.content.Context
import com.example.storage_manager.model.Shelf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class StorageTrackerPersistenceService(context: Context) {
    private val prefs = context.getSharedPreferences("StorageTrackerPrefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveData(shelves: List<Shelf>) {
        val shelvesJson = gson.toJson(shelves)
        prefs.edit().putString("shelves", shelvesJson).apply()
    }

    fun loadData(): List<Shelf> {
        val shelvesJson = prefs.getString("shelves", null)
        return shelvesJson?.let {
            gson.fromJson(it, object : TypeToken<List<Shelf>>() {}.type)
        } ?: emptyList()
    }
}