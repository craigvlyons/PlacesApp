# Phase 0 evidence report — stabilization and data protection

**Status:** Engineering Phase 0 gate passed 2026-08-21; household release and rollout evidence remains pending.  
**Application under test:** replacement `com.personal.favoriteplaces` 3.0.0 (version code 3), temporary label **Places New**.  
**Legacy application:** `com.example.favoriteplaces` 2.0 (version code 2).

This report is subordinate to `rolling-implementation-plan.md`. It is final for the engineering stabilization gate, not approval to release to either household phone. Every pending item below still requires direct evidence, and the old household app must remain installed throughout capture, import, readback, and owner review.

## Data invariants

| Invariant | Evidence | Result |
|---|---|---|
| Room database remains `favorites_db`, version 1 | Exported Room schema and database declaration | Pass |
| Version-1 schema identity is unchanged | SHA-256 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230` | Pass |
| No destructive Room/reset path exists | Static guard test plus main-source scan for destructive fallback, `clearAllTables`, and `deleteDatabase` | Pass |
| Every persisted field is included in portable backup | ID, place ID, title, address, notes, rating, Favorite heart, exact signed color, city, latitude, longitude | Pass |
| Restore cannot silently overwrite | Transactional additive import, `OnConflictStrategy.ABORT`, idempotent identical rows, conflicting IDs reject and roll back | Pass |
| Synthetic production-shaped v1 fixture reads unchanged | Five rows, 11 columns, expected SHA-256 `2726927dc77562ea50b42d2f8013036b8b6f384120fcfba9cd11758b48f40c0f` | Pass |

## Build and automated verification

Pinned runtime: JBR 17.0.14 at `/Users/macc/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home`. Android Studio’s current bundled JBR 21 is not the Phase 0 runtime because the legacy Kotlin/KAPT combination fails its Java module-access checks.

Verified tasks:

- JVM unit tests.
- Debug lint with no baseline or suppression hiding failures.
- Debug APK.
- Permanently signed, non-minified technical release APK.
- Debug Android-test APK.
- Direct API 37 instrumentation: 12/12 tests.
- Legacy migration Python suite: 18/18 tests.
- Kotlin cross-codec legacy migration golden test.
- `git diff --check`.
- Final exact-candidate audit: zero main-source forced-null assertions, destructive database calls, stack-print/standard-output calls, and TODO/FIXME/HACK markers.
- Privacy-safe failure logging at all 14 exception sites, with a JVM regression proving that secret-bearing exception messages are omitted.

The Gradle UTP connected-test wrapper aborts with zero tests on the API 37 preview emulator. The same generated/signed test APKs pass through `am instrument`; this is an infrastructure limitation, not presented as an application test pass from UTP.

## Identity and signing

| Evidence | Result |
|---|---|
| Recovered Google Drive APK SHA-256 | `e8223f556dea25a746061f55a843b085e063023f34990be1210fd54ac7ddc526` |
| Legacy signer SHA-256 | `f5d4e19724bb74dac311eb5d93b68be33ee6241657069d57c5f5166e7290d600` |
| Reset-Mac debug signer SHA-256 | `ffb6087015464bb7d915d77ab43e26d0673160f4c8f06099c2dc8e6c1e85beb` |
| In-place update | Impossible: application ID and lost signer cannot both match |
| Side-by-side identity | Pass on emulator and Craig's Galaxy S25+: legacy and replacement packages remain installed together |
| Replacement release signing wiring | Pass: ignored owner-only properties only; no debug signing on release |
| Permanent replacement signer | Pass locally: PKCS#12, 4096-bit RSA, V2-signed clean build, certificate SHA-256 `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`, side-by-side launch and same-signer reinstall verified. Independent recovery copy remains pending |

## Regression evidence

Automated coverage includes:

- Room v1 open/read contract and schema identity.
- Insert, update, delete, delete/Undo, duplicate place IDs, and Favorite-heart independence.
- Sorting by city, Favorite, color, and rating.
- City-plus-color grouping and exact map subset.
- Empty map data and first valid camera location.
- Edit/Save preservation of place ID, address, city, coordinates, Favorite heart, and identity.
- Card/List navigation actions, direct heart toggling, Edit/Save, and backup navigation.
- Backup export, verification, idempotent import, conflict rollback, file-size guard, and document-picker UI.
- Add-flow save-readiness/race protection, null location handling, API-aware reverse geocoding, and structured-city priority below the live network boundary.

Live Maps verification passes with the real key stored only in ignored `local.properties`: clean generated-source/manifest checks contain the configured value and not the fallback, the signed replacement renders map tiles and its imported Denver marker, and filtered logcat contains no authorization failure. Live Add remains a Places API (New) migration gate because the stabilized app still uses obsolete REST calls that must not drive the production key restriction model.

## Legacy household migration evidence

Engineering proof passes with synthetic data. The household procedure is stricter:

1. Open one record’s Edit screen in the still-installed legacy app.
2. Capture its accessibility XML and screenshot PNG using `capture_evidence.py`. The helper refuses the wrong package, legacy home screen, invalid device output, repository storage, and overwrite.
3. Transcribe UI-visible values exactly and record per-field provenance.
4. Re-resolve inaccessible place metadata from the owner-reviewed address and assign deterministic migration IDs where the hidden old ID cannot be known.
5. Run `build_backup.py`; retain its capture and record digests.
6. Import into the empty side-by-side replacement.
7. Compare count/digest, then review every card, Favorite heart, Edit value, city/color grouping, and map position with the owner.
8. Exercise Edit/Save, delete/Undo, and a live Add without removing the old app.
9. Record owner sign-off. Only then repeat on the second phone.

Exact preservation applies to the UI-reviewed legacy title, address, notes, rating, Favorite flag, and color. The non-debuggable legacy package and lost signer prevent direct proof of its private internal ID/place ID/coordinates; those fields are explicitly marked as re-resolved or migration-generated and must not be described as byte-for-byte recovered.

Craig's first household migration passed its technical gate on 2026-08-22. The guarded UI capture retained Edit XML/PNG plus list evidence for 26 distinct legacy records outside Git. A version-2 reviewed artifact preserved every visible title, address, nonempty note, rating, Favorite-heart state, and selected color; added the Google-derived, user-editable default type; and retained all 26 requested phone numbers in a separate future-use metadata sidecar rather than changing Room or hiding them in notes. Three businesses whose current Google location differs from the legacy address are explicitly flagged; their imported coordinates use the saved address instead of silently moving the record.

The empty permanent-signed replacement accepted **Import 26 places?**, completed the additive transaction, reported 26 places, and exported all 26 records back out. The source and phone export have identical parsed records and record digest `e5f77bba08b1a8d66c6c54a2dc7919a5c95cfdf747ce09c7d727d95646828aed`. The pulled installed APK is byte-identical to the approved light-only release SHA-256 `5b12be8e35c6dcbe3d5046da562b8abf0fd283485df60ed748c9061a76c15ece` and uses the permanent signer. Both packages remain installed; owner visual/map review and signer recovery remain pending before the legacy app is removed.

## Pending household release and rollout evidence

The second-phone procedure is now consolidated in [household-phone-migration-runbook.md](household-phone-migration-runbook.md). It records the exact guarded UI capture, reviewed provenance, v2→v3→v4 enrichment, side-by-side install, import/export digest proof, moved-business handling, privacy boundary, rollback rules, and independent owner sign-off used for Craig's migration.

- [ ] Independent signer recovery copy opened successfully. Local ignored signer, signed APK fingerprint, installation, and update proof pass.
- [ ] Replacement Maps/Places key restricted to package `com.personal.favoriteplaces`, permanent release SHA-1, and only native SDK APIs used.
- [ ] Craig’s real legacy capture, reviewed backup, import digest, and exact 26-record phone export pass. Owner visual/map review and final sign-off remain before removing the legacy app.
- [ ] Wife’s installed legacy package/version/signer recorded.
- [ ] Wife’s migration repeated from [the household runbook](household-phone-migration-runbook.md), using a separate private directory/capture/metadata/backup/export/manifest and independent owner sign-off.
- [ ] Final static/error/logging audit repeated against the exact signed Phase 0 candidate.
- [x] Final static/error/logging audit repeated against the exact signed Phase 0 candidate.
- [x] Rolling-plan engineering Phase 0 gate reviewed and marked complete before Phase 1 is detailed. Household migration remains a release prerequisite even after engineering work advances.
