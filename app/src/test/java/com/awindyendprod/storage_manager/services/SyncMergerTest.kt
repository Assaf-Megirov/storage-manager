package com.awindyendprod.storage_manager.services

import com.awindyendprod.storage_manager.model.ExportData
import com.awindyendprod.storage_manager.model.Item
import com.awindyendprod.storage_manager.model.Profile
import com.awindyendprod.storage_manager.model.ProfileData
import com.awindyendprod.storage_manager.model.Settings
import com.awindyendprod.storage_manager.model.Shelf
import com.awindyendprod.storage_manager.model.ShelfSection
import com.awindyendprod.storage_manager.model.Tombstone
import com.awindyendprod.storage_manager.model.TombstoneEntityType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class SyncMergerTest {

    private val profileId = "profile-1"
    private val shelfId = "shelf-1"
    private val sectionId = "section-1"

    private fun date(offsetMillis: Long): Date = Date(BASE_TIME + offsetMillis)

    private fun exportData(
        items: List<Item> = emptyList(),
        tombstones: List<Tombstone> = emptyList(),
        currentProfileId: String? = profileId,
        profiles: List<ProfileData>? = null
    ): ExportData {
        val resolvedProfiles = profiles ?: listOf(
            ProfileData(
                profile = Profile(id = profileId, name = "Profile", updatedAt = date(0)),
                shelves = listOf(
                    Shelf(
                        id = shelfId,
                        name = "Shelf",
                        sections = mutableListOf(
                            ShelfSection(id = sectionId, name = "Section", items = items, updatedAt = date(0))
                        ),
                        updatedAt = date(0)
                    )
                ),
                settings = Settings()
            )
        )
        return ExportData(
            globalSettings = Settings(),
            profiles = resolvedProfiles,
            currentProfileId = currentProfileId,
            version = 1,
            tombstones = tombstones
        )
    }

    private fun itemsOf(merged: ExportData): List<Item> =
        merged.profiles.first { it.profile.id == profileId }
            .shelves.first { it.id == shelfId }
            .sections.first { it.id == sectionId }
            .items

    @Test
    fun `disjoint new items on each side are unioned`() {
        val localOnly = Item(id = "item-local", name = "Local item", updatedAt = date(0))
        val remoteOnly = Item(id = "item-remote", name = "Remote item", updatedAt = date(0))

        val local = exportData(items = listOf(localOnly))
        val remote = exportData(items = listOf(remoteOnly))

        val merged = itemsOf(SyncMerger.merge(local, remote))

        assertEquals(setOf("item-local", "item-remote"), merged.map { it.id }.toSet())
    }

    @Test
    fun `newer edit wins over older edit on the same item`() {
        val itemId = "item-1"
        val olderEdit = Item(id = itemId, name = "Old name", updatedAt = date(0))
        val newerEdit = Item(id = itemId, name = "New name", updatedAt = date(1000))

        val local = exportData(items = listOf(olderEdit))
        val remote = exportData(items = listOf(newerEdit))

        val merged = itemsOf(SyncMerger.merge(local, remote))

        assertEquals(1, merged.size)
        assertEquals("New name", merged.first().name)
    }

    @Test
    fun `tombstone newer than both edits deletes the item`() {
        val itemId = "item-1"
        val localItem = Item(id = itemId, name = "Local edit", updatedAt = date(0))
        val remoteItem = Item(id = itemId, name = "Remote edit", updatedAt = date(500))
        val tombstone = Tombstone(id = itemId, entityType = TombstoneEntityType.ITEM, deletedAt = date(1000))

        val local = exportData(items = listOf(localItem), tombstones = listOf(tombstone))
        val remote = exportData(items = listOf(remoteItem))

        val merged = itemsOf(SyncMerger.merge(local, remote))

        assertTrue(merged.isEmpty())
    }

    @Test
    fun `edit strictly newer than a tombstone resurrects the item`() {
        val itemId = "item-1"
        val tombstone = Tombstone(id = itemId, entityType = TombstoneEntityType.ITEM, deletedAt = date(0))
        val editAfterDelete = Item(id = itemId, name = "Resurrected", updatedAt = date(1000))

        val local = exportData(items = emptyList(), tombstones = listOf(tombstone))
        val remote = exportData(items = listOf(editAfterDelete))

        val merged = itemsOf(SyncMerger.merge(local, remote))

        assertEquals(1, merged.size)
        assertEquals("Resurrected", merged.first().name)
    }

    @Test
    fun `whole profile deletion removes it from the merged result`() {
        val otherProfileId = "profile-2"
        val bothProfiles = listOf(
            ProfileData(
                profile = Profile(id = profileId, name = "P1", updatedAt = date(0)),
                shelves = emptyList(),
                settings = Settings()
            ),
            ProfileData(
                profile = Profile(id = otherProfileId, name = "P2", updatedAt = date(0)),
                shelves = emptyList(),
                settings = Settings()
            )
        )
        val local = exportData(
            profiles = bothProfiles,
            tombstones = listOf(
                Tombstone(id = otherProfileId, entityType = TombstoneEntityType.PROFILE, deletedAt = date(1000))
            )
        )
        val remote = exportData(profiles = bothProfiles)

        val merged = SyncMerger.merge(local, remote)

        assertEquals(listOf(profileId), merged.profiles.map { it.profile.id })
    }

    @Test
    fun `null updatedAt is treated as oldest possible`() {
        val itemId = "item-1"
        val noTimestamp = Item(id = itemId, name = "No timestamp", updatedAt = null)
        val withTimestamp = Item(id = itemId, name = "Has timestamp", updatedAt = date(0))

        val local = exportData(items = listOf(noTimestamp))
        val remote = exportData(items = listOf(withTimestamp))

        val merged = itemsOf(SyncMerger.merge(local, remote))

        assertEquals("Has timestamp", merged.first().name)
    }

    @Test
    fun `tombstone merge keeps the newest deletedAt regardless of side`() {
        val itemId = "item-1"
        val tombstoneA = Tombstone(id = itemId, entityType = TombstoneEntityType.ITEM, deletedAt = date(0))
        val tombstoneB = Tombstone(id = itemId, entityType = TombstoneEntityType.ITEM, deletedAt = date(500))

        val local = exportData(tombstones = listOf(tombstoneA))
        val remote = exportData(tombstones = listOf(tombstoneB))

        val merged = SyncMerger.merge(local, remote)

        assertEquals(1, merged.tombstones.count { it.id == itemId })
        assertEquals(date(500), merged.tombstones.first { it.id == itemId }.deletedAt)
    }

    companion object {
        private const val BASE_TIME = 1_700_000_000_000L
    }
}
