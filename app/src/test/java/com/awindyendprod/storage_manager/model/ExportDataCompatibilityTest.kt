package com.awindyendprod.storage_manager.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportDataCompatibilityTest {

    private val gson = Gson()

    /** ProfileData as it was before the archive feature. */
    private data class LegacyProfileData(
        val profile: Profile,
        val shelves: List<Shelf>,
        val settings: Settings
    )

    private data class LegacyExportData(
        val globalSettings: Settings,
        val profiles: List<ProfileData>,
        val currentProfileId: String?,
        val version: Int,
        val tombstones: List<Tombstone> = emptyList()
    )

    private fun minimalExportData(mainDeviceId: String? = null) = ExportData(
        globalSettings = Settings(),
        profiles = emptyList(),
        currentProfileId = null,
        version = 1,
        mainDeviceId = mainDeviceId
    )

    @Test
    fun `old JSON without mainDeviceId parses with a null value`() {
        val legacyJson = gson.toJson(
            LegacyExportData(
                globalSettings = Settings(),
                profiles = emptyList(),
                currentProfileId = null,
                version = 1
            )
        )

        val parsed = gson.fromJson(legacyJson, ExportData::class.java)

        assertNull(parsed.mainDeviceId)
    }

    @Test
    fun `new JSON with mainDeviceId still parses on an old app's data class`() {
        val newJson = gson.toJson(minimalExportData(mainDeviceId = "device-123"))

        val parsedByOldApp = gson.fromJson(newJson, LegacyExportData::class.java)

        assertEquals(1, parsedByOldApp.version)
        assertEquals(emptyList<ProfileData>(), parsedByOldApp.profiles)
    }

    @Test
    fun `mainDeviceId round-trips through serialize then deserialize`() {
        val original = minimalExportData(mainDeviceId = "device-abc")

        val roundTripped = gson.fromJson(gson.toJson(original), ExportData::class.java)

        assertEquals("device-abc", roundTripped.mainDeviceId)
    }

    @Test
    fun `profile JSON from before the archive parses with a null archive`() {
        val legacyJson = gson.toJson(
            LegacyProfileData(
                profile = Profile(id = "p1", name = "Profile"),
                shelves = emptyList(),
                settings = Settings()
            )
        )

        val parsed = gson.fromJson(legacyJson, ProfileData::class.java)

        // ProfileData has required params, so Gson instantiates it without applying Kotlin defaults.
        // archivedItems is declared nullable for exactly this reason.
        assertNull(parsed.archivedItems)
    }

    @Test
    fun `settings JSON from before the archive keeps the archive defaults`() {
        val legacyJson = "{\"presetMessage\":\"hi\"}"

        val parsed = gson.fromJson(legacyJson, Settings::class.java)

        // Settings has an all-default constructor, so Gson uses the generated no-arg one and the
        // defaults survive. Adding a required parameter to Settings would silently break this.
        assertEquals(180, parsed.archiveRetentionDays)
        assertEquals(false, parsed.archiveStructuralDeletes)
        assertEquals("hi", parsed.presetMessage)
    }

    @Test
    fun `profile JSON with archive fields still parses on an older app's data class`() {
        val newJson = gson.toJson(
            ProfileData(
                profile = Profile(id = "p1", name = "Profile"),
                shelves = emptyList(),
                settings = Settings(),
                archivedItems = listOf(ArchivedItem(item = Item(name = "Item")))
            )
        )

        val parsedByOldApp = gson.fromJson(newJson, LegacyProfileData::class.java)

        assertEquals("p1", parsedByOldApp.profile.id)
        assertEquals(emptyList<Shelf>(), parsedByOldApp.shelves)
    }
}
