package com.awindyendprod.storage_manager.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExportDataCompatibilityTest {

    private val gson = Gson()

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
}
