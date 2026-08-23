# Legacy data migration tool

This directory converts a fully reviewed legacy capture into the exact version-1 or version-2 JSON backup accepted by the replacement app. Version 2 additionally preserves the Google-derived, user-editable default `placeType`; the guarded enrichment helpers advance a reviewed v2 backup to v3 with phone numbers and then v4 with a separate Google primary type. It does not read private app storage or modify either app; the separate capture helper only records the currently visible legacy Edit screen.

The complete, phone-by-phone operational procedure is in [`docs/plans/household-phone-migration-runbook.md`](../../docs/plans/household-phone-migration-runbook.md). Use that runbook for the second household phone rather than copying Craig's private files or reconstructing the process from command history.

## Safety workflow

1. Keep the legacy app installed and do not clear its storage.
2. For each visible saved place, retain both an Edit-screen accessibility XML dump and screenshot PNG under a device-specific folder outside Git. Screenshots and captures contain personal data and must never be committed.
3. Transcribe the visible title, address, notes, rating, independent Favorite heart, and exact selected color into a copy of `fixtures/complete-capture.json`.
4. Assign a deterministic positive migration ID. The old Room row ID is private and may be unavailable after deletions; do not claim a generated ID is the hidden legacy ID.
5. Resolve city, place ID when available, latitude, longitude, and the default place type from the reviewed address using the replacement's Places flow. Do not invent missing values or claim re-resolved metadata is a byte-for-byte copy of inaccessible legacy metadata.
6. Record provenance for every persisted field using exactly one of `legacy-ui`, `resolved-from-address`, or `migration-generated`. Set the top-level `reviewedBy` after the device owner compares every draft record with the old screen.
7. Run the validator/converter. It refuses non-object input, empty captures, timestamps without a timezone, incomplete coordinates/provenance, out-of-range ratings/colors, blank identity fields, duplicate IDs, unsafe/missing evidence files, and extra or missing fields. It also refuses to overwrite an existing migration artifact.
8. Retain the reviewed capture, its XML/PNG evidence, and the converter-reported capture and record digests together outside Git.
9. Import the resulting file into the side-by-side replacement. Compare record count and digest, then check every place before considering removal of the legacy app.

With the old app open on one place's Edit screen, capture its evidence bundle using the exact ADB serial. The command refuses a non-legacy foreground screen, invalid XML/PNG output, output inside this repository, and an existing record folder. It writes private-permission files and hashes without parsing or printing the personal contents:

```sh
python3 tools/legacy_migration/capture_evidence.py \
  --adb /Users/macc/Library/Android/sdk/platform-tools/adb \
  --serial PHONE_SERIAL_FROM_ADB_DEVICES \
  --record-id 1 \
  --output-root /path/outside-the-repo/craig-legacy-capture
```

Then reference `records/0001/edit.xml` and `records/0001/edit.png` from that capture JSON record. A version-2 reviewed capture must also include nullable `placeType` and its provenance for every record.

Phone and Google-category enrichment now have explicit Room migrations and backup formats. Keep the researched metadata in separately checksummed private sidecars with retrieval source/time and match-review status, then use the strict v3/v4 enrichment helpers. Never hide metadata in notes, match a business on name alone, or silently change Room during household migration. The current v3 helper requires one reviewed nonblank phone per source ID; if a listing has no published phone, stop rather than inventing a value and update/test the helper's nullable-input contract first.

```sh
python3 tools/legacy_migration/build_backup.py \
  /path/outside-the-repo/craig-reviewed-capture.json \
  /path/outside-the-repo/craig-places-backup-v2.json

python3 tools/legacy_migration/enrich_backup_v3.py \
  /path/outside-the-repo/craig-places-backup-v2.json \
  /path/outside-the-repo/craig-reviewed-phone-metadata.json \
  /path/outside-the-repo/craig-places-backup-v3.json

python3 tools/legacy_migration/enrich_backup_v4.py \
  /path/outside-the-repo/craig-places-backup-v3.json \
  /path/outside-the-repo/craig-reviewed-google-metadata.json \
  /path/outside-the-repo/craig-places-backup-v4.json

python3 -m unittest discover -s tools/legacy_migration -p 'test_*.py'
```

The converter duplicates the Kotlin backup canonicalization only to create an offline migration artifact. Its golden digest is checked independently by the Python test and by `LegacyMigrationGoldenTest` in the Android project.

## Preservation boundary

The user-visible legacy values—title, address, notes, rating, Favorite heart, and exact color—must be transcribed from and reviewed against the old app. Hidden place ID/coordinates and the old internal Room row ID cannot be read from a non-debuggable package without its lost signer. The capture therefore preserves their provenance explicitly: address-derived metadata is re-resolved and IDs are migration-generated where necessary. The old app remains installed until the owner verifies the imported record and its map position.
