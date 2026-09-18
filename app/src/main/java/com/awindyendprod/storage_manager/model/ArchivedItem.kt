package com.awindyendprod.storage_manager.model

import com.google.gson.annotations.SerializedName
import java.util.Date

/**
 * An item that was deleted from a shelf, kept as a record of what was handed back and to whom.
 *
 * The shelf and section are stored as the names they had at the time of deletion: the archive has to
 * outlive them, and a name is what the user would recognise anyway.
 *
 * Every parameter has a default so that Kotlin generates a no-arg constructor. Gson uses it when
 * reading older JSON, which is what makes a field added later default instead of coming back null.
 */
data class ArchivedItem(
    @SerializedName("item") val item: Item = Item(name = ""),
    @SerializedName("shelfName") val shelfName: String = "",
    @SerializedName("sectionName") val sectionName: String = "",
    @SerializedName("archivedAt") val archivedAt: Date = Date()
)
