package com.awindyendprod.storage_manager.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.awindyendprod.storage_manager.model.ArchivedItem
import com.awindyendprod.storage_manager.model.Item
import com.awindyendprod.storage_manager.model.Shelf
import com.awindyendprod.storage_manager.model.ShelfSection
import com.awindyendprod.storage_manager.model.Tombstone
import com.awindyendprod.storage_manager.model.TombstoneEntityType
import com.awindyendprod.storage_manager.services.ArchiveStore
import com.awindyendprod.storage_manager.services.StorageTrackerPersistenceService
import com.awindyendprod.storage_manager.services.TombstoneStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import androidx.work.*
import com.awindyendprod.storage_manager.services.ItemNotificationWorker
import java.util.Date
import java.util.concurrent.TimeUnit
import androidx.work.Data.Builder
import com.awindyendprod.storage_manager.model.AppLanguage

/** The archive preferences live in Settings, which this view model has no handle on. */
data class ArchivePreferences(
    val archiveStructuralDeletes: Boolean = false,
    val retentionDays: Int = 180
)

class StorageTrackerViewModel(
    context: Context,
    private val persistenceService: StorageTrackerPersistenceService,
    private val tombstoneStore: TombstoneStore,
    private val archiveStore: ArchiveStore
) : ViewModel() {
    private val applicationContext = context.applicationContext
    private var currentProfileId: String? = null
    private val _shelves = MutableStateFlow<List<Shelf>>(emptyList())
    val shelves: StateFlow<List<Shelf>> = _shelves.asStateFlow()

    private val _archivedItems = MutableStateFlow<List<ArchivedItem>>(emptyList())
    val archivedItems: StateFlow<List<ArchivedItem>> = _archivedItems.asStateFlow()

    var onDataChanged: () -> Unit = {}

    /** Supplied by the factory once the settings view model exists. */
    var archivePreferences: () -> ArchivePreferences = { ArchivePreferences() }

    fun reloadData() {
        if (currentProfileId != null) {
            _shelves.value = persistenceService.loadData(currentProfileId!!)
        } else {
            _shelves.value = persistenceService.loadData() // Legacy fallback
        }
        reloadArchive()
    }

    fun reloadDataForProfile(profileId: String) {
        currentProfileId = profileId
        _shelves.value = persistenceService.loadData(profileId)
        reloadArchive()
    }

    /** Reloads the archive, dropping anything past the retention window on the way. */
    fun reloadArchive() {
        val profileId = currentProfileId
        if (profileId == null) {
            _archivedItems.value = emptyList()
            return
        }
        val kept = archiveStore.pruneExpired(profileId, archivePreferences().retentionDays)
        _archivedItems.value = archiveStore.sorted(kept)
    }

    /** Prepends [entries] to the archive. No-op before a profile exists (pre-migration launches). */
    private fun archive(entries: List<ArchivedItem>) {
        if (entries.isEmpty()) return
        val profileId = currentProfileId ?: return
        // Built from the loaded list rather than re-parsing the stored JSON, which matters when a
        // bulk delete archives many items at once. An item can reach the archive twice if a sync
        // restored it after a delete, so newer entries replace older ones with the same id.
        val replacedIds = entries.map { it.item.id }.toSet()
        val updated = entries + _archivedItems.value.filterNot { it.item.id in replacedIds }
        archiveStore.save(profileId, updated)
        _archivedItems.value = archiveStore.sorted(updated)
    }

    private fun archiveEntriesFor(shelf: Shelf, sections: List<ShelfSection>): List<ArchivedItem> =
        sections.flatMap { section ->
            section.items.map { item -> ArchiveStore.newEntry(item, shelf.name, section.name) }
        }

    /** Removes an archived entry for good. Local only, so no tombstone is needed. */
    fun deleteArchivedItem(itemId: String) {
        val profileId = currentProfileId ?: return
        val remaining = archiveStore.load(profileId).filterNot { it.item.id == itemId }
        archiveStore.save(profileId, remaining)
        _archivedItems.value = archiveStore.sorted(remaining)
    }

    private fun saveData() {
        if (currentProfileId != null) {
            persistenceService.saveData(_shelves.value, currentProfileId!!)
        } else {
            persistenceService.saveData(_shelves.value) // Legacy fallback
        }
        onDataChanged()
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

    fun getItem(shelfId: String, sectionId: String, itemId: String): StateFlow<Item?> = shelves.map { shelvesList ->
        shelvesList
            .find { it.id == shelfId }
            ?.sections
            ?.find { it.id == sectionId }
            ?.items
            ?.find { it.id == itemId }
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    private fun updateShelfNames() {
        _shelves.value = _shelves.value.mapIndexed { index, shelf ->
            shelf.copy(name = (index + 1).toString(), updatedAt = Date())
        }
    }

    fun addShelf() {
        _shelves.value = _shelves.value + Shelf(name = (_shelves.value.size + 1).toString(), updatedAt = Date())
        saveData()
    }

    fun removeShelf(shelfId: String) {
        if (archivePreferences().archiveStructuralDeletes) {
            _shelves.value.firstOrNull { it.id == shelfId }?.let { shelf ->
                archive(archiveEntriesFor(shelf, shelf.sections))
            }
        }
        _shelves.value = _shelves.value.filter { it.id != shelfId }
        updateShelfNames()
        tombstoneStore.append(Tombstone(id = shelfId, entityType = TombstoneEntityType.SHELF, deletedAt = Date()))
        saveData()
    }

    fun addSectionToShelf(shelfId: String, sectionName: String) {
        _shelves.value = _shelves.value.map { shelf ->
            if (shelf.id == shelfId) {
                shelf.copy(sections = mutableListOf<ShelfSection>().also { newList ->
                    newList.addAll(shelf.sections)
                    newList.add(ShelfSection(name = sectionName, updatedAt = Date()))
                })
            } else shelf
        }
        saveData()
    }

    fun removeSection(shelfId: String, sectionId: String) {
        if (archivePreferences().archiveStructuralDeletes) {
            _shelves.value.firstOrNull { it.id == shelfId }?.let { shelf ->
                archive(archiveEntriesFor(shelf, shelf.sections.filter { it.id == sectionId }))
            }
        }
        _shelves.value = _shelves.value.map { shelf ->
            if (shelf.id == shelfId) {
                shelf.copy(sections = mutableListOf<ShelfSection>().also { newSections ->
                    newSections.addAll(shelf.sections.filter { it.id != sectionId })
                })
            } else {
                shelf
            }
        }
        tombstoneStore.append(Tombstone(id = sectionId, entityType = TombstoneEntityType.SECTION, deletedAt = Date()))
        saveData()
    }

    private fun scheduleNotification(item: Item) {
        if (!item.hasAlarm || item.alarmDate == null) return

        val workManager = WorkManager.getInstance(applicationContext)

        val sharedPrefs = applicationContext.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val languageStr = sharedPrefs.getString("language", AppLanguage.SYSTEM.name)
        val language = AppLanguage.valueOf(languageStr ?: AppLanguage.SYSTEM.name)
        
        val notificationData = Builder()
            .putString("itemName", item.name)
            .putString("clientName", item.clientName)
            .putLong("entryDate", item.entryDate?.time ?: -1)
            .putLong("returnDate", item.returnDate?.time ?: -1)
            .putString("language", language.name)
            .build()

        val delay = item.alarmDate.time - System.currentTimeMillis()
        if (delay <= 0) return

        val notificationWork = OneTimeWorkRequestBuilder<ItemNotificationWorker>()
            .setInputData(notificationData)
            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniqueWork(
            item.id,
            ExistingWorkPolicy.REPLACE,
            notificationWork
        )
    }

    fun addItemToSection(shelfId: String, sectionId: String, item: Item) {
        val stampedItem = item.copy(updatedAt = Date())
        _shelves.value = _shelves.value.map { shelf ->
            if (shelf.id == shelfId) {
                shelf.copy(sections = mutableListOf<ShelfSection>().also { newSections ->
                    shelf.sections.forEach { section ->
                        if (section.id == sectionId) {
                            newSections.add(section.copy(
                                items = mutableListOf<Item>().also { newItems ->
                                    newItems.addAll(section.items)
                                    newItems.add(stampedItem)
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
        scheduleNotification(stampedItem)
    }

    fun updateItem(newShelfId: String, newSectionId: String, itemId: String, updatedItem: Item) {
        val stampedItem = updatedItem.copy(updatedAt = Date())

        //remove old item
        val shelvesWithItemRemoved = _shelves.value.map { shelf ->
            shelf.copy(sections = shelf.sections.map { section ->
                section.copy(items = section.items.filter { it.id != itemId })
            }.toMutableList())
        }

        //add new item
        _shelves.value = shelvesWithItemRemoved.map { shelf ->
            if (shelf.id == newShelfId) {
                shelf.copy(sections = shelf.sections.map { section ->
                    if (section.id == newSectionId) {
                        section.copy(items = section.items + stampedItem)
                    } else {
                        section
                    }
                }.toMutableList())
            } else {
                shelf
            }
        }

        saveData()
        scheduleNotification(stampedItem)
    }

    /**
     * Deletes an item and files it in the archive. [noteOverride], when given, replaces the note on
     * the archived copy: the delete dialog lets the user record who actually collected the item.
     */
    fun removeItemFromSection(
        shelfId: String,
        sectionId: String,
        itemId: String,
        noteOverride: String? = null
    ) {
        removeItems(shelfId, sectionId, mapOf(itemId to noteOverride))
    }

    /**
     * Deletes several items in one pass. A loop over [removeItemFromSection] would re-serialise the
     * shelves, the archive and the tombstone list once per item, on the main thread.
     */
    fun removeItemsFromSection(shelfId: String, sectionId: String, itemIds: List<String>) {
        removeItems(shelfId, sectionId, itemIds.associateWith { null })
    }

    private fun removeItems(shelfId: String, sectionId: String, notesByItemId: Map<String, String?>) {
        if (notesByItemId.isEmpty()) return

        val sourceShelf = _shelves.value.firstOrNull { it.id == shelfId }
        val sourceSection = sourceShelf?.sections?.firstOrNull { it.id == sectionId }
        val entries = sourceSection?.items.orEmpty()
            .filter { notesByItemId.containsKey(it.id) }
            .map { item ->
                val archived = notesByItemId[item.id]?.let { item.copy(note = it) } ?: item
                ArchiveStore.newEntry(archived, sourceShelf?.name.orEmpty(), sourceSection?.name.orEmpty())
            }
        archive(entries)

        _shelves.value = _shelves.value.map { shelf ->
            if (shelf.id == shelfId) {
                shelf.copy(sections = mutableListOf<ShelfSection>().also { newSections ->
                    shelf.sections.forEach { section ->
                        if (section.id == sectionId) {
                            newSections.add(section.copy(
                                items = mutableListOf<Item>().also { newItems ->
                                    newItems.addAll(section.items.filterNot { notesByItemId.containsKey(it.id) })
                                }
                            ))
                        } else {
                            newSections.add(section)
                        }
                    }
                })
            } else shelf
        }
        tombstoneStore.appendAll(
            notesByItemId.keys.map {
                Tombstone(id = it, entityType = TombstoneEntityType.ITEM, deletedAt = Date())
            }
        )
        saveData()
    }
}