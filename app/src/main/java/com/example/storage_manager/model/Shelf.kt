package com.example.storage_manager.model

import java.util.UUID

data class Shelf(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    var sections: MutableList<ShelfSection> = mutableListOf()
)