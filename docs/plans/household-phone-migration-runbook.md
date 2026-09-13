# Household legacy-phone migration runbook

**Status:** Craig's Galaxy S25+ procedure is proven; repeat independently for the second household phone.  
**Legacy package:** `com.example.favoriteplaces`  
**Replacement package:** `com.personal.favoriteplaces`

This is the authoritative repeatable procedure for moving saved places out of the non-debuggable legacy app and into the side-by-side replacement. It records what worked for Craig's 26-place migration and the safeguards required when repeating it for his wife's phone.

## New-phone transfer failure: recovery order

If a replacement phone receives a missing, empty, or unusable Places installation, do not uninstall either phone's app, clear storage, factory-reset either device, or repeatedly open an unexpectedly empty copy while recovery sources are still being identified.

Use this order:

1. Look for an explicit `places-app-backup` JSON created from **Settings → Data & backup → Export**. Validate its format, version, declared count, record count, and record digest before importing it. This is the preferred recovery source because it is independent of Android's opaque transfer timing.
2. If the old phone still opens the replacement app, create a new export there and preserve the old phone until the new phone's post-import export matches it.
3. If the old phone has only the legacy app, use the guarded record-level capture in this runbook; do not attempt to pull or replace its private SQLite database.
4. If neither explicit source exists, inspect the old device's Google backup and any Samsung Smart Switch computer/external-storage backup before considering a reset. Android says Google backup can include app data but not every app restores all data, and setup transfer does not automatically copy apps that were not installed from Google Play. Auto Backup may restore data when the matching APK is installed if the correct ancestral backup was selected during setup, but this is less observable than the explicit Places export. References: [Android backup and restore](https://support.google.com/android/answer/2819582), [Android device-to-device copy limits](https://support.google.com/android/answer/13761358), [Auto Backup restore schedule](https://developer.android.com/identity/data/autobackup), and [Samsung backup/restore options](https://www.samsung.com/us/support/answer/ANS10002780/).

The current replacement package explicitly includes its Room database and relevant preferences in Android cloud/device-transfer rules, but the signed APK is distributed outside Google Play. Keep making explicit versioned exports because they can be inspected and imported without resetting a phone.

### 2026-09-13 recovery snapshot

The Mac's synced Google Drive `places` directory contains two intact version-4 exports belonging to Craig: an older 26-record file and a newer 34-record file. Both have the required envelope, matching declared/actual counts, and matching canonical record digests. The 34-record file contains all 26 older titles plus eight additional titles. It is suitable as an owner-approved starter collection for the wife's replacement app, but it is not a recovery of her prior local-only records. No phone or database was changed during this inspection.

## Non-negotiable safety rules

1. Never uninstall, clear, reset, or overwrite the legacy app during capture or verification.
2. Install the replacement under its separate package ID. Both apps must remain installed together until that phone's owner signs off.
3. Never attempt a destructive Room migration or copy a database over either app's live database.
4. Keep screenshots, accessibility XML, reviewed JSON, phone numbers, addresses, notes, device identifiers, and exports outside this Git repository.
5. Give each phone its own private migration directory and artifacts. Never reuse or merge Craig's files with the second phone's files.
6. Do not invent inaccessible values. Preserve visible legacy values exactly; label re-resolved Google metadata and generated migration IDs honestly.
7. Treat import as unproven until an export from the replacement matches the reviewed source and the owner visually checks the result.

## Why we could not simply pull the old SQLite database

The legacy APK is a release/non-debuggable app, its original signing environment is gone, and Android correctly blocks `run-as` and direct access to the app's private data directory. An APK alone does not grant access to `/data/data/com.example.favoriteplaces`. Re-signing or installing another build cannot inherit that private storage because Android app identity includes the signing certificate.

We therefore used an owner-visible migration:

- ADB captured the rendered Edit screen for each place as accessibility XML and a screenshot.
- The visible title, address, notes, rating, Favorite heart, and selected color were reviewed and transcribed.
- Hidden place IDs, coordinates, city, and Google types were re-resolved from the owner-approved listing/address where possible.
- Deterministic positive import IDs were generated because the old internal Room IDs were inaccessible.
- A guarded offline converter produced a versioned backup accepted by the replacement app.
- The replacement imported the file transactionally, exported it again, and the source/export record digests were compared.

This is a record-level recovery, not a raw database extraction. The preservation boundary must remain explicit in every evidence manifest.

## What passed on Craig's phone

- ADB was authorized after Developer options and USB debugging were enabled and the phone's Allow debugging prompt was accepted.
- The legacy and replacement packages coexisted on the Galaxy S25+.
- Twenty-six distinct places were captured from the legacy UI with per-record XML/PNG evidence stored outside Git.
- All visible names, addresses, notes, ratings, Favorite states, and exact legacy colors were reviewed.
- Google metadata was resolved separately. Three businesses had moved; those records deliberately retained their saved-address coordinates and were not silently attached to the new location.
- Backup v2 carried the reviewed legacy values and editable default types; v3 added reviewed phone numbers; v4 added the separate Google primary type used for reservation eligibility.
- The replacement imported all 26 places in one additive transaction, then exported 26 records with an identical parsed-record digest.
- The old app remained installed.

Craig's exact private artifacts and personal data remain under `/Users/macc/Documents/Places-Migration/Craig-S25-2026-08-22`, outside Git. Do not use that folder as the second phone's working directory.

## Private directory layout for the second phone

Create a new directory outside the repository, using the device owner and date. A recommended layout is:

```text
/Users/macc/Documents/Places-Migration/<owner>-<device>-YYYY-MM-DD/
├── device-identity.txt
├── legacy-capture/
│   └── records/
│       ├── 0001/
│       │   ├── edit.xml
│       │   ├── edit.png
│       │   └── capture-metadata.json
│       └── ...
├── reviewed-capture-v2.json
├── places-backup-v2.json
├── reviewed-phone-metadata.json
├── places-backup-v3-with-phones.json
├── reviewed-google-metadata.json
├── places-backup-v4-with-google-details.json
├── replacement-post-import-export.json
├── verification-notes.md
└── migration-evidence-manifest.json
```

Set restrictive permissions on the directory because it contains household personal data:

```sh
chmod 700 "/Users/macc/Documents/Places-Migration/<owner>-<device>-YYYY-MM-DD"
```

## Stage 1 — connect and identify the exact phone

On the phone:

1. Open **Settings → About phone → Software information**.
2. Tap **Build number** seven times to enable Developer options.
3. Open **Settings → Developer options** and enable **USB debugging**.
4. Connect with a data-capable USB cable, unlock the phone, choose data transfer if prompted, and accept **Allow USB debugging** for this Mac.
5. Auto Blocker may need to be disabled temporarily on Samsung devices if it prevents USB debugging. A T-Mobile carrier lock does not normally block ADB.

On the Mac, use the SDK's explicit ADB path so shell PATH differences do not matter:

```sh
ADB=/Users/macc/Library/Android/sdk/platform-tools/adb
"$ADB" devices -l
```

There must be exactly one intended phone in the `device` state. `unauthorized` means the phone still needs the Allow debugging confirmation. Record the exact serial locally; do not commit it.

Record package/version facts before changing anything:

```sh
"$ADB" -s PHONE_SERIAL shell dumpsys package com.example.favoriteplaces
"$ADB" -s PHONE_SERIAL shell dumpsys package com.personal.favoriteplaces
```

Retain, at minimum, package name, version code/name, first-install time, last-update time, and signing-certificate evidence where available. Confirm the legacy app still opens and displays the expected saved-place count.

## Stage 2 — capture every legacy record

Create the private root first. Then, for each legacy place:

1. Open that place in the legacy app.
2. Open its **Edit** screen. The helper intentionally refuses to capture the legacy home/list screen.
3. Assign the next stable import/evidence number: `1`, `2`, `3`, and so on. Never reuse a number.
4. Run:

```sh
python3 tools/legacy_migration/capture_evidence.py \
  --adb /Users/macc/Library/Android/sdk/platform-tools/adb \
  --serial PHONE_SERIAL \
  --record-id 1 \
  --output-root "/Users/macc/Documents/Places-Migration/<owner>-<device>-YYYY-MM-DD/legacy-capture"
```

5. Confirm the new record folder contains `edit.xml`, `edit.png`, and `capture-metadata.json`.
6. Return to the legacy list and continue until every unique place is captured.

The helper guards against the wrong foreground package, a non-Edit screen, malformed XML/PNG, repository-local output, and overwriting an existing record folder. Do not bypass those guards.

### Completeness reconciliation

Before transcription, reconcile the capture set against the legacy app:

- Count unique saved places in the legacy list and unique evidence folders.
- Capture additional list screenshots/XML when needed to prove grouping, rating, heart, or color state.
- Maintain a private index mapping evidence ID to the visible place name.
- Check for duplicate names at different addresses and for a place appearing in more than one visual group.
- Do not proceed if the counts or identities cannot be reconciled.

## Stage 3 — build and owner-review the capture JSON

Start from `tools/legacy_migration/fixtures/complete-capture-v2.json`; never edit the fixture itself. For each record, transcribe and review:

- `title`, `address`, and notes/content exactly as visible, including blank notes and Unicode.
- Rating as null or the visible 1–5 value.
- Favorite as the independent heart state, not a color interpretation.
- Exact selected ARGB color.
- Editable `placeType`, using the reviewed Google-derived default when appropriate.
- City, coordinates, and Google Place ID from the reviewed saved address/listing where available.
- Evidence paths relative to the private capture JSON's directory.
- Field provenance using only `legacy-ui`, `resolved-from-address`, or `migration-generated`.

Use deterministic positive import IDs. They are migration IDs, not claims about inaccessible legacy Room row IDs.

The phone's owner must compare every draft record with the old app before `reviewedBy` is filled. Special attention is required for:

- Moved, renamed, or closed businesses.
- Multiple branches with the same name.
- Addresses Google reformats or relocates.
- Empty notes versus missing capture.
- Favorite heart versus saved-place color.
- Ratings of zero/unrated versus one star.

For a moved business, preserve what the owner actually saved unless the owner explicitly chooses the current listing. Record that decision; never silently move a place during metadata resolution.

## Stage 4 — validate and create backup v2

Run the migration tool tests before converting household data:

```sh
python3 -m unittest discover -s tools/legacy_migration -p 'test_*.py'
```

Create the v2 backup using new, non-existing output paths:

```sh
python3 tools/legacy_migration/build_backup.py \
  "/private/path/reviewed-capture-v2.json" \
  "/private/path/places-backup-v2.json"
```

Record both converter-reported values:

- Capture SHA-256 proves which reviewed source JSON was converted.
- Record SHA-256 proves the canonical persisted records and is the value used for source/export comparison.

The converter refuses malformed fields, invalid timestamps, duplicates, unsafe/missing evidence files, invalid ratings/colors/coordinates, incomplete provenance, and overwrite.

## Stage 5 — reviewed phone and Google-category enrichment

The current replacement supports nullable phone numbers in backup v3 and a separate nullable Google primary type in backup v4. Keep the user's editable `placeType` independent from Google's provider category.

Phone/category research must be matched against both the reviewed legacy name and address. Retain retrieval time, source, raw-response hash, and a match status in the private metadata. Never match on name alone.

Create v3:

```sh
python3 tools/legacy_migration/enrich_backup_v3.py \
  "/private/path/places-backup-v2.json" \
  "/private/path/reviewed-phone-metadata.json" \
  "/private/path/places-backup-v3-with-phones.json"
```

Create v4:

```sh
python3 tools/legacy_migration/enrich_backup_v4.py \
  "/private/path/places-backup-v3-with-phones.json" \
  "/private/path/reviewed-google-metadata.json" \
  "/private/path/places-backup-v4-with-google-details.json"
```

Both enrichment tools require an exact one-to-one set of import IDs and refuse to overwrite output. The v4 tool also refuses phone changes and unexpected Google Place ID changes. It can preserve a null backup Place ID for a reviewed moved-business record even though the private metadata retains the researched current listing.

Important limitation: the current v3 offline enrichment helper expects a reviewed nonblank phone for every source record. If Google does not publish a phone for one of the second phone's places, do not invent one. Stop and update/test the helper to accept a reviewed null before producing that phone's v3/v4 artifact. The app itself correctly supports nullable phones and can lazily fetch missing action metadata later.

## Stage 6 — install the replacement side by side

Use the exact approved signed APK and record its hash before installation:

```sh
places_release_apk="app/build/outputs/apk/release/Places-1.0.0.apk"
shasum -a 256 "$places_release_apk"
```

Replace the example version in `places_release_apk` with the explicitly approved release version; release builds are named automatically and the embedded version/signature must still be verified.

Install without uninstalling or clearing either package:

```sh
"$ADB" -s PHONE_SERIAL install -r "$places_release_apk"
```

Verify afterward:

- Legacy `com.example.favoriteplaces` still opens with its original data.
- Replacement `com.personal.favoriteplaces` opens separately.
- The replacement signer/version matches the approved artifact.
- If the replacement already contains data, export it before import. Import is additive and conflict-safe, but an unexpected preexisting dataset must be understood before continuing.

Do not use `pm clear`, uninstall, `adb restore`, a raw database push, or any destructive filesystem/database command.

## Stage 7 — import through the replacement app

1. Copy the reviewed v4 JSON to the phone or make it available through Google Drive.
2. Open replacement **Settings → Data & backup → Import backup**.
3. Select the exact v4 artifact in Android's document picker.
4. Read the confirmation count. It must match the reviewed source count.
5. Confirm import once.

The app performs an additive transaction. It does not clear or overwrite records. It may fill only missing Google metadata; any incompatible conflict aborts the entire import without partial changes.

Immediately record:

- The confirmation count and completion result.
- The replacement's displayed place count.
- A screenshot showing both apps remain installed.
- Any error message exactly, without retrying blindly.

## Stage 8 — export and prove the import

From replacement **Settings → Data & backup → Export backup**, export a fresh post-import file to the private migration directory.

Verification has two layers.

### Mechanical verification

- Parsed source and post-import record counts match.
- Canonical record digests match, or every intentional enrichment-only difference is explained.
- There are no missing or duplicate import identities.
- Title, address, notes, rating, Favorite, color, editable type, coordinates, phone, and Google category are compared field by field.
- The installed package/version/signer and APK SHA-256 are recorded in the evidence manifest.

Never compare only file SHA-256: app export formatting can differ while parsed records remain identical. Compare the canonical record digest and parsed fields.

### Owner-visible verification

On the replacement, the owner checks every place or a documented complete review sequence:

- Card/list name, address, notes preview, rating, Favorite, and color accent.
- Details name, editable type, full notes, address, rating, heart, and color.
- City/type/color/Favorite filters.
- Map position, especially moved or ambiguous businesses.
- Call disclosure and system dialer where a phone exists.
- Restaurant reservation handoff where a Google restaurant category exists.
- Edit and Save on a test record, then export again to prove persistence.

The old app remains the source of truth until this passes.

## Stage 9 — sign-off and retention

Record owner sign-off with date, source count, imported/exported count, source/export record digests, approved APK hash, and unresolved exceptions.

After sign-off:

1. Retain at least two independent encrypted/restricted copies of the final v4 backup and evidence manifest.
2. Keep the private raw screenshots/XML only as long as needed for recovery/audit, then decide together whether to archive or securely remove them.
3. Do not remove the legacy app merely because import completed. Remove it only after owner review, backup recovery proof, and an agreed retention period.
4. Never remove the second household legacy app based on Craig's sign-off; each device requires its own evidence and approval.

## Failure and rollback rules

| Situation | Required response |
|---|---|
| ADB says `unauthorized` | Unlock phone and accept the debugging prompt; do not change app data. |
| Capture helper refuses the screen | Open that record's legacy Edit screen and retry with the same unused ID. Do not weaken the guard. |
| Duplicate/missing record | Reconcile against the legacy list before conversion. |
| Google match is ambiguous or business moved | Keep the legacy saved address/coordinates or obtain an explicit owner decision; document it. |
| Converter/enricher rejects input | Fix the reviewed source or tool with tests; never hand-edit the final backup around validation. |
| Import count is unexpected | Cancel import and inspect the selected file/preexisting replacement data. |
| Import reports a conflict | Export replacement state, preserve both apps, and compare the conflicting record. Do not clear and retry. |
| Post-import digest differs | Keep both apps installed and perform a field-level diff before any further write. |
| Replacement build fails or crashes | Return to the still-installed legacy app; no legacy data should have changed. |
| Phone disconnects | Stop at the current checkpoint. Captured evidence and generated files are non-overwriting and can be resumed. |

## Second-phone execution checklist

- [ ] Create a new private outside-Git directory for the second owner/device/date.
- [ ] Record ADB serial privately and legacy package/version/install facts.
- [ ] Confirm the replacement package state before import.
- [ ] Count the legacy list and capture every distinct Edit screen with sequential IDs.
- [ ] Reconcile evidence count and identity against the legacy list.
- [ ] Transcribe visible values and provenance into a separate reviewed v2 capture.
- [ ] Owner reviews every record, with explicit moved/ambiguous-business decisions.
- [ ] Run all offline migration tests.
- [ ] Generate v2 and record capture/record digests.
- [ ] Resolve and review phone/category metadata without name-only matching.
- [ ] Generate v3/v4 without overwriting or inventing missing values.
- [ ] Record the exact signed replacement APK hash and signer/version.
- [ ] Install side by side without uninstall/clear; prove both apps and legacy data remain.
- [ ] Import the exact reviewed v4 artifact and record the confirmation/result counts.
- [ ] Export replacement data and compare parsed fields plus canonical record digest.
- [ ] Owner reviews every place, important filters/maps, notes, ratings, hearts, colors, Call, and reservation behavior.
- [ ] Make and verify independent recovery copies.
- [ ] Record second-owner sign-off before considering legacy-app removal.

## Related implementation references

- [`tools/legacy_migration/README.md`](../../tools/legacy_migration/README.md) — converter and evidence-helper details.
- [`phase-0-evidence-report.md`](phase-0-evidence-report.md) — Craig's engineering and first-household evidence summary.
- [`rolling-implementation-plan.md`](rolling-implementation-plan.md) — authoritative implementation history and remaining household rollout gate.
- [`release-signing-runbook.md`](release-signing-runbook.md) — permanent replacement signing and update continuity.
