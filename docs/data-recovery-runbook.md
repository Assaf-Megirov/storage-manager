# Recovering data after a "certificate mismatch" Play Store update failure

## Symptom

A user (or you) tries to update Simple Shelf Manager from the Play Store and gets a
generic "Could not install" / "App not installed" error. Retrying does nothing.

## Root cause

Android refuses to install an update over an existing app unless the new APK is
signed with the **same certificate** as what's currently installed. If a build
signed with a different key ever got installed on that device outside of the
Play Store (sideloaded via `adb install`, Android Studio Run, a shared APK,
etc.), every subsequent Play Store update will silently fail forever - the only
fix is to uninstall and reinstall, which wipes the app's local data unless you
recover it first.

This app stores everything in local SharedPreferences with no cloud backend
(as of writing - a Drive-based sync feature is planned/in progress but not yet
released), so **the uninstall step is destructive** unless you extract the data
first. This runbook is that extraction + recovery procedure.

Real users who have only ever installed from the Play Store are not at risk of
this - the Play App Signing certificate is stable across all their updates.
This mainly hits: your own test/dev devices, or a user who was ever handed a
sideloaded APK directly.

## Step 0: Confirm it's actually a signature mismatch

Don't assume - confirm, since the fix path differs for other causes (storage
space, a corrupted Play Store download cache, etc. - try clearing the Play
Store app's cache first as a free, non-destructive check).

Connect the device via USB with USB debugging enabled, then:

```bash
adb devices                 # confirm the device shows up
adb logcat -c                # clear the buffer
# now tap Update/Install in the Play Store on the device
adb logcat -v time | grep -iE "certificate mismatch|INSTALL_FAILED"
```

Look for a line like:

```
I/Finsky: [2] com.awindyendprod.storage_manager is installed but certificate mismatch
```

That confirms it. (If you instead see `INSTALL_FAILED_UPDATE_INCOMPATIBLE` while
you're the one attempting an `adb install`, that's the same root cause from the
installer's side.)

You can also check what's currently installed:

```bash
adb shell dumpsys package com.awindyendprod.storage_manager | grep -E "versionName|versionCode|firstInstallTime|lastUpdateTime|pkgFlags"
```

If `firstInstallTime` equals `lastUpdateTime`, the device has never received a
single Play Store update since that install - a strong signal it was sideloaded
directly and has been silently stuck since that date.

## Step 1: Check whether the installed app is debuggable

This determines which recovery path is available:

```bash
adb shell dumpsys package com.awindyendprod.storage_manager | grep pkgFlags
```

**If `pkgFlags` includes `DEBUGGABLE`** (true for any local `debug` build type,
and for this project's `release` build type too since it currently reuses the
debug signing config) - proceed to Step 2, `run-as` will work without root.

**If it does NOT include `DEBUGGABLE`** (a genuine Play-distributed release) -
`run-as` will refuse with "package not debuggable". In that case:
1. First, just try the uninstall + reinstall anyway and open the app
   immediately - the manifest already declares `android:allowBackup="true"`,
   so Android's Auto Backup to the user's Google account *may* silently
   restore the data on fresh install with no manual steps at all. Verify by
   checking the shelves/items are there before assuming anything is lost.
2. If nothing restored and the device is rooted, a root-level pull of
   `/data/data/com.awindyendprod.storage_manager/shared_prefs/` is still
   possible - same files, same technique from Step 2 onward, just via `su`
   instead of `run-as`.
3. Otherwise, without root there is no supported way to extract
   SharedPreferences from a non-debuggable app via adb. The only recoverable
   data at that point is whatever the user separately exported themselves via
   Settings -> Export Data before the failure occurred.

## Step 2: Pull the raw SharedPreferences files

```bash
mkdir -p /tmp/ssm_recovery/phone_prefs
adb shell run-as com.awindyendprod.storage_manager ls shared_prefs/
# expect: ProfilePrefs.xml, StorageTrackerPrefs.xml, settings.xml (plus an
# android.app.ActivityThread.IDS.xml you can ignore)

for f in ProfilePrefs.xml StorageTrackerPrefs.xml settings.xml; do
  adb shell run-as com.awindyendprod.storage_manager cat "shared_prefs/$f" \
    > "/tmp/ssm_recovery/phone_prefs/$f"
done
```

Sanity check the files aren't empty (`wc -c /tmp/ssm_recovery/phone_prefs/*.xml`)
before continuing.

**Why not just use the app's own Export button first?** Historically (fixed in
commit `f96bd8e`), `ProfileData.shelves` inside the exported JSON was a stale
snapshot, not the live per-profile data - the real shelf/item data lives
separately in `StorageTrackerPrefs.xml` under `shelves_<profileId>` keys. Pulling
the raw files and cross-referencing them (what the script in Step 3 does) is
strictly more reliable than trusting an in-app export from a version that might
predate that fix, and it also catches orphaned profile data an in-app export
can never see (see Step 3).

## Step 3: Reconstruct a clean ExportData JSON

Use `scripts/recover_from_signature_mismatch.py` in this repo:

```bash
python3 scripts/recover_from_signature_mismatch.py \
  /tmp/ssm_recovery/phone_prefs \
  /tmp/ssm_recovery/recovered_export.json
```

It cross-references `ProfilePrefs.xml`'s profile list against every
`shelves_<id>` key in `StorageTrackerPrefs.xml`, and:
- refreshes each known profile's shelf data with the live copy (fixing the
  staleness issue above)
- **recovers any orphaned `shelves_<id>` with no matching profile record** as a
  new profile named `RECOVERED PROFILE N - PLEASE REVIEW`, so nothing is
  silently dropped even if a profile record was lost to some other bug

Read its stderr output - it prints a per-profile shelf/item count summary and a
warning for every orphaned profile it recovers. Sanity-check those numbers
against what the user expects before trusting the file (e.g. "the phone should
have about 40 items across 3 shelves" - does that roughly match?).

If it's been a long time since this script was last used, double check the
`SETTINGS_DEFAULTS` dict at the top of the script still matches the current
`app/src/main/java/com/awindyendprod/storage_manager/model/Settings.kt` -
fields added there since need a corresponding default added to the script.

## Step 4: Get the recovered file onto the device's shared storage

This must land somewhere that survives an app uninstall (i.e. not the app's
private storage):

```bash
adb push /tmp/ssm_recovery/recovered_export.json /sdcard/Download/simple_shelf_manager_recovered_export.json
# also push the raw XMLs as a fallback in case the reconstruction needs redoing
adb push /tmp/ssm_recovery/phone_prefs/ProfilePrefs.xml /sdcard/Download/raw_backup_ProfilePrefs.xml
adb push /tmp/ssm_recovery/phone_prefs/StorageTrackerPrefs.xml /sdcard/Download/raw_backup_StorageTrackerPrefs.xml
adb push /tmp/ssm_recovery/phone_prefs/settings.xml /sdcard/Download/raw_backup_settings.xml
```

Confirm they landed: `adb shell ls -la /sdcard/Download/`

## Step 5: Uninstall, reinstall, import

**This is the irreversible step - do not proceed until Step 4's files are
confirmed present on the device.**

```bash
adb uninstall com.awindyendprod.storage_manager
```

Then, on the device: install "Simple Shelf Manager" fresh from the Play Store
(it should now show as a normal Install, not Update, and will succeed since
there's no conflicting certificate anymore).

Once installed and opened:
1. Go through initial setup if prompted (it'll create a fresh default profile -
   that's fine, it'll be overwritten/supplemented by the import).
2. Settings -> Import Data -> pick `simple_shelf_manager_recovered_export.json`
   from Downloads.
3. Verify: profile count, shelf/item counts per profile match the numbers the
   script printed in Step 3. Open the "RECOVERED PROFILE N" one specifically
   and confirm with the user what's in it - rename it to something meaningful
   or delete it once they've confirmed whether it's real data or leftover test
   junk.

## Step 6: Clean up

Once the user confirms the import looks correct and they've had a chance to
use the app normally for a bit, the `raw_backup_*` files and the recovered
export JSON in `/sdcard/Download/` can be deleted from the device. Keep a copy
off-device until you're confident, though - this is the only safety net until
this is imported and the user has verified it.
