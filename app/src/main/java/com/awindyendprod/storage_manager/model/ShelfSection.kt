package com.awindyendprod.storage_manager.model

import java.util.UUID
import java.util.Date

data class ShelfSection(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: Int = 0,
    val items: List<Item> = emptyList()
) {
    fun getLatestEntryDate(): Date? {
        return items.mapNotNull { it.entryDate }
            .maxByOrNull { it.time }
    }

    fun getLatestReturnDate(): Date? {
        return items.mapNotNull { it.returnDate }
            .maxByOrNull { it.time }
    }
}