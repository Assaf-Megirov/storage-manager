package com.example.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.storage_manager.model.Item
import com.example.storage_manager.model.Shelf
import com.example.storage_manager.model.ShelfSection
import com.example.storage_manager.services.StorageTrackerPersistenceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class StorageTrackerViewModel(context: Context) : ViewModel() {
    private val persistenceService = StorageTrackerPersistenceService(context)
    private val _shelves = MutableStateFlow<List<Shelf>>(persistenceService.loadData())
    val shelves: StateFlow<List<Shelf>> = _shelves.asStateFlow()

    private fun saveData() {
        persistenceService.saveData(_shelves.value)
    }

    fun getSectionById(shelfId: String, sectionId: String): StateFlow<ShelfSection?> = shelves.map { shelvesList ->
        shelvesList
            .find { it.id == shelfId }
            ?.sections
            ?.find { it.id == sectionId }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    fun addShelf(name: String) {
        _shelves.value = _shelves.value + Shelf(name = name)
        saveData()
    }

    fun removeShelf(shelfId: String) {
        _shelves.value = _shelves.value.filter { it.id != shelfId }
        saveData()
    }

    fun addSectionToShelf(shelfId: String, sectionName: String) {
        _shelves.value = _shelves.value.map { shelf ->
            if (shelf.id == shelfId) {
                shelf.copy(sections = mutableListOf<ShelfSection>().also { newList ->
                    newList.addAll(shelf.sections)
                    newList.add(ShelfSection(name = sectionName))
                })
            } else shelf
        }
        saveData()
    }

    fun removeSection(shelfId: String, sectionId: String) {
        _shelves.value = _shelves.value.map { shelf ->
            if (shelf.id == shelfId) {
                shelf.copy(sections = mutableListOf<ShelfSection>().also { newSections ->
                    newSections.addAll(shelf.sections.filter { it.id != sectionId })
                })
            } else {
                shelf
            }
        }
        saveData()
    }

    fun addItemToSection(shelfId: String, sectionId: String, item: Item) {
        _shelves.value = _shelves.value.map { shelf ->
            if (shelf.id == shelfId) {
                shelf.copy(sections = mutableListOf<ShelfSection>().also { newSections ->
                    shelf.sections.forEach { section ->
                        if (section.id == sectionId) {
                            newSections.add(section.copy(
                                items = mutableListOf<Item>().also { newItems ->
                                    newItems.addAll(section.items)
                                    newItems.add(item)
                                }
                            ))
                        } else {
                            newSections.add(section)
                        }
                    }
                })
            } else shelf
        }
        saveData()
    }

    fun removeItemFromSection(shelfId: String, sectionId: String, itemId: String) {
        _shelves.value = _shelves.value.map { shelf ->
            if (shelf.id == shelfId) {
                shelf.copy(sections = mutableListOf<ShelfSection>().also { newSections ->
                    shelf.sections.forEach { section ->
                        if (section.id == sectionId) {
                            newSections.add(section.copy(
                                items = mutableListOf<Item>().also { newItems ->
                                    newItems.addAll(section.items.filter { it.id != itemId })
                                }
                            ))
                        } else {
                            newSections.add(section)
                        }
                    }
                })
            } else shelf
        }
        saveData()
    }
}