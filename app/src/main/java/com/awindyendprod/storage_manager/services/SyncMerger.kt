package com.awindyendprod.storage_manager.services

import com.awindyendprod.storage_manager.model.ExportData
import com.awindyendprod.storage_manager.model.Item
import com.awindyendprod.storage_manager.model.Shelf
import com.awindyendprod.storage_manager.model.ShelfSection
import com.awindyendprod.storage_manager.model.Tombstone
import com.awindyendprod.storage_manager.model.TombstoneEntityType
import java.util.Date

object SyncMerger {

    fun merge(local: ExportData, remote: ExportData): ExportData {
        val mergedTombstones = mergeTombstones(local.tombstones, remote.tombstones)
        val tombstoneIndex = mergedTombstones.associateBy { it.entityType to it.id }

        val localProfilesById = local.profiles.associateBy { it.profile.id }
        val remoteProfilesById = remote.profiles.associateBy { it.profile.id }

        val mergedProfiles = mergeIdMatchedList(localProfilesById.keys, remoteProfilesById.keys) { id ->
            val l = localProfilesById[id]
            val r = remoteProfilesById[id]
            resolveEntity(
                tombstone = tombstoneIndex[TombstoneEntityType.PROFILE to id],
                local = l,
                remote = r,
                updatedAtOf = { it.profile.updatedAt }
            ) { winnerBase ->
                winnerBase.copy(
                    shelves = mergeShelves(l?.shelves.orEmpty(), r?.shelves.orEmpty(), tombstoneIndex)
                )
            }
        }

        val mergedCurrentProfileId =
            local.currentProfileId?.takeIf { id -> mergedProfiles.any { it.profile.id == id } }
                ?: remote.currentProfileId?.takeIf { id -> mergedProfiles.any { it.profile.id == id } }
                ?: mergedProfiles.firstOrNull()?.profile?.id

        return local.copy(
            profiles = mergedProfiles,
            currentProfileId = mergedCurrentProfileId,
            tombstones = mergedTombstones
        )
    }

    private fun mergeTombstones(local: List<Tombstone>, remote: List<Tombstone>): List<Tombstone> =
        (local + remote)
            .groupBy { it.entityType to it.id }
            .map { (_, group) -> group.maxBy { it.deletedAt.orEpoch().time } }

    private fun mergeShelves(
        local: List<Shelf>,
        remote: List<Shelf>,
        tombstones: Map<Pair<TombstoneEntityType, String>, Tombstone>
    ): MutableList<Shelf> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        return mergeIdMatchedList(localById.keys, remoteById.keys) { id ->
            val l = localById[id]
            val r = remoteById[id]
            resolveEntity(
                tombstone = tombstones[TombstoneEntityType.SHELF to id],
                local = l,
                remote = r,
                updatedAtOf = { it.updatedAt }
            ) { winnerBase ->
                winnerBase.copy(
                    sections = mergeSections(l?.sections.orEmpty(), r?.sections.orEmpty(), tombstones).toMutableList()
                )
            }
        }.toMutableList()
    }

    private fun mergeSections(
        local: List<ShelfSection>,
        remote: List<ShelfSection>,
        tombstones: Map<Pair<TombstoneEntityType, String>, Tombstone>
    ): List<ShelfSection> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        return mergeIdMatchedList(localById.keys, remoteById.keys) { id ->
            val l = localById[id]
            val r = remoteById[id]
            resolveEntity(
                tombstone = tombstones[TombstoneEntityType.SECTION to id],
                local = l,
                remote = r,
                updatedAtOf = { it.updatedAt }
            ) { winnerBase ->
                winnerBase.copy(items = mergeItems(l?.items.orEmpty(), r?.items.orEmpty(), tombstones))
            }
        }
    }

    private fun mergeItems(
        local: List<Item>,
        remote: List<Item>,
        tombstones: Map<Pair<TombstoneEntityType, String>, Tombstone>
    ): List<Item> {
        val localById = local.associateBy { it.id }
        val remoteById = remote.associateBy { it.id }
        return mergeIdMatchedList(localById.keys, remoteById.keys) { id ->
            val l = localById[id]
            val r = remoteById[id]
            resolveEntity(
                tombstone = tombstones[TombstoneEntityType.ITEM to id],
                local = l,
                remote = r,
                updatedAtOf = { it.updatedAt }
            ) { winnerBase -> winnerBase }
        }
    }

    private fun <T> mergeIdMatchedList(
        localIds: Set<String>,
        remoteIds: Set<String>,
        resolve: (String) -> T?
    ): List<T> = (localIds + remoteIds).mapNotNull(resolve)

    private fun <T> resolveEntity(
        tombstone: Tombstone?,
        local: T?,
        remote: T?,
        updatedAtOf: (T) -> Date?,
        mergeChildren: (winnerBase: T) -> T
    ): T? {
        if (tombstone != null) {
            val localUpdatedAt = local?.let(updatedAtOf).orEpoch()
            val remoteUpdatedAt = remote?.let(updatedAtOf).orEpoch()
            val latestKnownEdit = maxOf(localUpdatedAt, remoteUpdatedAt)
            if (tombstone.deletedAt.orEpoch() >= latestKnownEdit) {
                return null
            }
        }
        if (local == null && remote == null) return null
        if (local == null) return remote
        if (remote == null) return local
        val winnerBase = if (updatedAtOf(local).orEpoch() >= updatedAtOf(remote).orEpoch()) local else remote
        return mergeChildren(winnerBase)
    }
}
