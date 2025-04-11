package com.awindyendprod.storage_manager.model

data class ExportData (
    val settings: Settings,
    val shelves: List<Shelf>,
    val version: Int
)