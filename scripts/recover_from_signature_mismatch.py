#!/usr/bin/env python3
"""
Reconstructs a Simple Shelf Manager ExportData JSON directly from a device's raw
SharedPreferences XML files, bypassing the app's own Export feature entirely.

Why this exists: ProfilePrefs.xml's "profiles" key stores a snapshot of each
profile's `shelves` field that can be stale or (pre-f96bd8e) was never refreshed
at all before export. The real, live shelf data lives separately, keyed by
profile ID, in StorageTrackerPrefs.xml. This script cross-references the two so
recovered data always reflects what's actually on disk - including "orphaned"
shelves_<id> entries whose profile record no longer exists in the profiles list
(these are invisible in the app's UI and would be silently dropped by a normal
export/reinstall cycle).

See docs/data-recovery-runbook.md for the full procedure this script is one
step of (pulling the files via `adb shell run-as`, pushing the result back to
the device, uninstall/reinstall, import).

Usage:
    python3 recover_from_signature_mismatch.py <prefs_dir> <output_json>

<prefs_dir> must contain ProfilePrefs.xml, StorageTrackerPrefs.xml, settings.xml
pulled from the device (see runbook for the adb commands).
"""
import xml.etree.ElementTree as ET
import json
import sys

# Must match model/Settings.kt's declared defaults for the app version you are
# recovering into. Re-check this dict against Settings.kt if it's been a while
# since this script was last touched - new fields need an entry here or the
# import will still work (nullable-safe fields default fine) but non-nullable
# fields without a default would need adding.
SETTINGS_DEFAULTS = {
    "sectionDateType": "ENTRY_DATE",
    "dateDisplayFormat": "NUMERIC",
    "defaultReturnDateDays": 14,
    "language": "SYSTEM",
    "fontSize": "MEDIUM",
    "sectionHeight": 210,
    "sectionWidth": 300,
    "theme": "SYSTEM",
    "fabDragEnabled": True,
    "fabPositionMainScreenX": 1.4e-45,
    "fabPositionMainScreenY": 1.4e-45,
    "fabPositionSectionScreenX": 1.4e-45,
    "fabPositionSectionScreenY": 1.4e-45,
    "hasSeenLongPressHint": False,
    "notificationDaysBefore": 1,
    "notificationMaxItems": 10,
    "dailyNotificationsEnabled": True,
    "currentProfileId": None,
    "showProfilesButton": True,
}


def load_map(path):
    tree = ET.parse(path)
    root = tree.getroot()
    out = {}
    for child in root:
        name = child.get("name")
        if child.tag == "string":
            out[name] = child.text
        elif child.tag == "boolean":
            out[name] = child.get("value") == "true"
        elif child.tag == "int":
            out[name] = int(child.get("value"))
        elif child.tag == "float":
            out[name] = float(child.get("value"))
    return out


def main():
    if len(sys.argv) != 3:
        print(f"Usage: {sys.argv[0]} <prefs_dir> <output_json>", file=sys.stderr)
        sys.exit(1)

    prefs_dir, out_path = sys.argv[1], sys.argv[2]

    profile_prefs = load_map(f"{prefs_dir}/ProfilePrefs.xml")
    storage_prefs = load_map(f"{prefs_dir}/StorageTrackerPrefs.xml")
    settings_prefs = load_map(f"{prefs_dir}/settings.xml")

    profiles = json.loads(profile_prefs["profiles"]) if profile_prefs.get("profiles") else []
    current_profile_id = profile_prefs.get("currentProfileId")

    shelves_by_profile = {}
    for key, val in storage_prefs.items():
        if key.startswith("shelves_"):
            pid = key[len("shelves_"):]
            shelves_by_profile[pid] = json.loads(val)

    known_ids = {p["profile"]["id"] for p in profiles}

    # Replace each profile's stale/empty shelves snapshot with the live data.
    for p in profiles:
        pid = p["profile"]["id"]
        p["shelves"] = shelves_by_profile.get(pid, p.get("shelves", []))

    # Any shelves_<id> with no matching profile record is orphaned real data -
    # recover it as its own profile rather than silently dropping it. Flag it
    # clearly so a human reviews/renames/deletes it after import.
    orphaned_ids = [pid for pid in shelves_by_profile if pid not in known_ids]
    template_settings = dict(profiles[0]["settings"]) if profiles else dict(SETTINGS_DEFAULTS)
    for i, pid in enumerate(orphaned_ids):
        recovered_settings = dict(template_settings)
        recovered_settings["currentProfileId"] = pid
        profiles.append({
            "profile": {
                "id": pid,
                "name": f"RECOVERED PROFILE {i + 1} - PLEASE REVIEW",
                "createdAt": "Jan 1, 2026 00:00:00",
                "isDefault": False,
            },
            "settings": recovered_settings,
            "shelves": shelves_by_profile[pid],
        })
        print(
            f"WARNING: orphaned shelves data for profile id {pid} "
            f"(no matching profile record) recovered as '{profiles[-1]['profile']['name']}'",
            file=sys.stderr,
        )

    global_settings = {
        field: settings_prefs.get(field, default)
        for field, default in SETTINGS_DEFAULTS.items()
    }
    global_settings["currentProfileId"] = None  # global settings never carries this

    export_data = {
        "globalSettings": global_settings,
        "profiles": profiles,
        "currentProfileId": current_profile_id,
        "version": 1,
    }

    with open(out_path, "w", encoding="utf-8") as f:
        json.dump(export_data, f, ensure_ascii=False)

    print(f"Wrote {out_path}", file=sys.stderr)
    for p in profiles:
        item_count = sum(len(s["items"]) for sh in p["shelves"] for s in sh["sections"])
        print(
            f"  profile {p['profile']['name']!r} ({p['profile']['id']}): "
            f"{len(p['shelves'])} shelves, {item_count} items",
            file=sys.stderr,
        )


if __name__ == "__main__":
    main()
