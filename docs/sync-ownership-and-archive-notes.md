# Sync ownership and the archive: decisions and deferred work

Written 2026-09-18, covering work done on 2026-09-17/18: the per-profile preset
message (commit `80fab13`), the archive/trash feature (commit `fc393d6`, merged
as `7ef759e`), and the main-device takeover work that lives on the
`main-device-takeover` branch.

The point of this document is to record two things that are easy to lose: **why
the archive deliberately does not sync**, and **what was deliberately deferred**
around main-device ownership, so that a future you does not rediscover either
the hard way.

---

## Part 1: Main device ownership

### What the "main device" role actually controls

Less than the name suggests. Grepping every consumer of `mainDeviceId` /
`MainDeviceStatus`, it gates exactly two behaviours:

1. **The first-sync wholesale adopt offer.** A device that is *not* main, on its
   first sync ever, with a remote file that names some main device, is offered
   "replace this device's data with the synced data"
   (`shouldOfferWholesaleAdopt` in `SyncManager`).
2. **Deleting the remote file on a clean slate.**
   `deleteRemoteDataIfOwnedByThisDevice` only deletes `sync_data.json` when
   `remoteMainId == null || remoteMainId == thisDeviceId`.

The merge itself never consults it. `SyncMerger.merge(local, remote)` is
symmetric: union by id, tombstones for deletions, last-write-wins on
`updatedAt`. So a wrong or stale main claim **cannot lose or corrupt data**; it
locks a role.

### How a claim becomes orphaned

`SyncPreferencesStore.getOrCreateDeviceId()` stores a random UUID in
SharedPreferences. That identity dies with the app's data, so **uninstall,
"Clear data", or losing the phone all produce the same outcome**: the id in the
remote file's `mainDeviceId` belongs to a device that can never exist again.

Once that happens, no device can claim the role, because
`claimRejected = remoteMainId != null && remoteMainId != thisDeviceId && wantsMain`
fires for everyone. The only in-app escape was to unmark main *on the main
device* - which is exactly the device you no longer have.

This is not hypothetical. On 2026-09-18 both test devices had
`cached_main_device_id = eaf88ce6...`, which matched neither the phone
(`f9ae1c7f...`) nor the emulator (`b249866c...`). An earlier install claimed
main and was gone.

### Symptoms of being stuck

- Settings permanently shows "Another device is marked as main", and toggling it
  is rejected.
- Clean slate wipes the device but leaves the Drive copy, so the next sync pulls
  everything back.

### Manual escape hatch (no app changes needed)

The sync file lives in Drive's hidden `appDataFolder` as `sync_data.json`, so it
is not visible in the normal Drive UI. A stuck user can clear it via
**Drive -> Settings -> Manage apps -> (this app) -> Delete hidden app data**.
That drops `mainDeviceId` along with the remote copy; each device still holds
its own data locally and re-uploads on the next sync. Worth knowing before
talking anyone through code changes.

### What was built (branch `main-device-takeover`)

**Takeover.** Confirming in Settings sets a persisted
`main_takeover_pending` flag; claim resolution then resolves to this device
instead of rejecting it, and the flag clears only once a sync has actually
written the claim - so an offline or failed sync retries rather than silently
dropping the intent. The displaced device, if it still exists, sees its claim
rejected on its next sync and steps down through the pre-existing path.

The confirmation dialog requires typing a localised word (`DELETE` / `מחק` /
`УДАЛИТЬ`), mirroring the existing clean-slate dialog. The instruction to type it
is deliberately placed mid-paragraph inside the warning, and the text field has
no label, so skimming to the buttons does not reveal what to type.

**Backup exclusion.** `SyncPreferencesStore.xml` is now excluded in
`backup_rules.xml` and in both blocks of `data_extraction_rules.xml`. With
`allowBackup="true"` and the empty default rule files, Android Auto Backup was
copying `device_id` and `marked_as_main_locally` to Google's servers; restoring
that onto a new phone gave two physical devices one sync identity, both
believing they were main. A restored device now generates a fresh identity,
which is the correct outcome because it genuinely is a different device.

### DEFERRED: stable device id

**The idea.** Derive the device id from `Settings.Secure.ANDROID_ID` instead of a
random UUID. It survives uninstall and reinstall, changing only on factory reset
or under a different app-signing key. A reinstalled device would then reclaim its
own main role automatically, with no dialog at all.

**Why it was deferred.** It only reduces how *often* the takeover is needed; the
takeover already rescues every case, including a genuinely lost phone, which the
stable id cannot. It is also the riskiest of the three changes.

**The migration trap, if you ever pick this up.** Apply it to *new installs only*
and keep any UUID already stored. Switching every install at once re-identifies
every device simultaneously and orphans every existing claim - the exact bug,
inflicted on the whole user base in one release.

Other caveats: `ANDROID_ID` is scoped per app-signing-key, per user, per device,
so debug and release builds differ; it changes on factory reset.

**Do not add a staleness fallback** ("if the main device has not synced in N
days, let another device prune/claim"). That reintroduces devices acting on each
other's behalf, which is the class of behaviour deliberately removed below.

---

## Part 2: Why the archive does not sync

### The decision

Archived items live only on the device that deleted them.
`SyncMerger` sets `archivedItems = null` on every merged profile,
`buildLocalExportData` strips it from the payload, and `persistMergedLocally`
never writes it. Export and import files *do* carry it
(`ArchiveStore.attachArchiveToProfiles` runs on both export paths), so a manual
backup is a complete record.

### The measurements behind it

A realistic entry (UUID id, client name, a note like "taken by David, brother of
the client", four dates, shelf and section names):

| | per entry | 30 deletions/day for a year (10,950 entries) |
|---|---|---|
| On disk | 443 B | 4.6 MiB |
| In the sync payload (millis dates) | 363 B | 3.8 MiB |
| Trimmed of unused fields | 285 B | 3.0 MiB |

The disk cost is irrelevant. The payload cost is not: the Drive file is a single
JSON document rewritten and re-downloaded **in full on every sync**. For
comparison, a few hundred active items is on the order of 100 KB, so a year of
archive at one-year retention would be roughly 40x everything else in the file.

Sync triggers, for sizing: 3 s debounce after any data change
(`DATA_CHANGE_SYNC_DEBOUNCE_MS`), every 5 min while foregrounded
(`PERIODIC_SYNC_INTERVAL_MS`), on foreground if more than 60 s since the last
sync (`FOREGROUND_DEBOUNCE_MS`), on background, and manually. The debounce is per
*burst*, not per item, so a rapid series of deletions produces one sync.

### What this costs you

Deletions made on device A never appear in device B's archive, and a reinstall
restores an empty archive unless an export is imported. This is the accepted
trade, not an oversight. If the archive screen ever grows a subtitle, saying it
is local to this device would be honest.

### A design that was built and then removed

An earlier version synced the archive and, because retention is a per-device
setting, needed a **prune watermark**: a per-profile `archivePrunedBefore` date
that only the main device advanced, carried in `ProfileData` and merged by max,
so one device's pruning decision propagated instead of being undone by the union
merge. It also needed an `ARCHIVED_ITEM` tombstone type so manual purges stuck.

All of it went when the archive became device-local - roughly 30 lines of
distributed-systems machinery that existed solely because a deletion rule was
parameterised per device. If the archive is ever made to sync again, that
problem comes back with it, and the watermark is the cheapest known answer:
per-item tombstones for rule-based deletions grow without bound, and tombstones
here are never garbage collected.

### Rules worth not breaking

- **`ProfileData.archivedItems == null` means "not included", never "empty".**
  `importProfiles` must not call `.orEmpty()` and save: an older export, or any
  pre-sync safety backup (which is built from the stripped payload), would then
  wipe the archive of every profile in the file.
- **`ArchiveStore.save` enforces uniqueness by item id.** The archive list is
  keyed on it, and duplicates crash the screen.
- **Settings switch before data loads.** Every path that changes the active
  profile calls `settingsViewModel.switchToProfile` before
  `reloadDataForProfile`, because the archive prune reads the active profile's
  retention. `reloadAfterSync` was missing this and could truncate one profile's
  archive using another profile's window.

---

## Known pre-existing issues, not addressed

- `SettingsPartition.fillDefaultsFromGlobal` has no callers. Both features added
  branches to it that never run. Either wire it in or delete it.
- `createProfile` seeds a new profile from the active profile's settings, so a
  new profile inherits the customer-facing preset message - which contradicts
  the hint text saying it is saved for that profile only.
- Shelf names are positions: `updateShelfNames()` rewrites every shelf's name to
  its 1-based index, so an archived entry's `shelfName` is a snapshot that stops
  matching once shelves are added or removed.
- `gradlew` has CRLF line endings and will not run directly on Linux
  (`sed -i 's/\r$//' gradlew` fixes it).
