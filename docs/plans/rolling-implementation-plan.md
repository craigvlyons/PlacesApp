# Places App Rolling Implementation Plan

**Status:** Phase 10 complete; modernization implementation and post-plan type-picker refinement are functional
**Created:** 2026-08-21  
**Current phase:** Complete — Phase 10 gate passed 2026-08-22

## Purpose

This is the execution plan for the modernization described in [`modernization-plan.md`](./modernization-plan.md). It is intentionally rolling:

1. Only the current phase receives implementation-level tasks.
2. A phase must be implemented, verified against its gate, and reviewed before it is marked complete.
3. Only after completion is the next phase selected and planned in detail.
4. Findings from completed work may change the next phase; the strategic modernization plan is direction, not permission to execute every later phase automatically.

The approved future UI reference is [`../demo/mobile-directions.html`](../demo/mobile-directions.html). It guides the product-refresh phase when that phase is reached. It does not override data safety, migration rules, verified current behavior, or the rolling-phase gate.

## Rules that apply to every phase

- Preserve the existing `favorites_db` database and every saved value.
- Never use destructive Room fallback, clear app data, uninstall as an upgrade step, or replace the database.
- Keep the legacy application ID `com.example.favoriteplaces` reserved for the untouched old app. Give the replacement a new permanent application ID and signing identity so both can coexist during migration.
- Use explicit, tested Room migrations for every future schema change.
- Do not treat the legacy schema as permanently locked. The replacement has its own database and may refactor or extend it after the stabilization baseline; preserve records and upgrade continuity through explicit migrations rather than preserving obsolete schema structure.
- Keep colors unlabeled and preserve their exact stored ARGB values.
- Keep Favorite as the independent `isFavorite` heart flag.
- Prefer deliberate refactoring and restructuring over preserving obsolete or confusing implementation patterns. Each change must follow current Android/Kotlin practices, establish clear ownership and boundaries, remove superseded code after parity is proved, and include proportionate tests, error handling, cancellation, privacy-safe logging, and documentation.
- “Good enough to compile” is not an acceptable endpoint. Before each phase gate, improve temporary compatibility work, duplicated paths, weak naming, broad exception handling, hidden mutable state, and avoidable technical debt to production-quality code or document a concrete external blocker and removal condition.
- Refactoring permission does not bypass phase boundaries: keep each restructure scoped to the active outcome so its behavior and data-preservation effects can be audited independently.
- Do not combine unrelated upgrades merely for convenience.
- Record commands, test results, decisions, risks, and evidence in this file as work proceeds.
- Before the Places phase is planned in implementation detail, audit every current/proposed Google request and research the best maintained Android SDK operation for each workflow. The audit must cover field masks, sessions, bounds/types, cancellation, errors, attribution, quotas/cost, privacy-safe telemetry, and removal of formatted-address regex/comma parsing as specified in `modernization-plan.md`.
- Stop at the current phase gate. Do not begin or fully plan the next phase until this file records that the gate passed.

## Rolling workflow

At the start of a phase:

- Confirm its goal, scope, exclusions, dependencies, and measurable gate.
- Break only that phase into implementation workstreams.
- Mark one workstream active at a time.

At the end of a phase:

- Record the files and behavior changed.
- Record automated and device-test evidence.
- Confirm database, signing, application ID, and side-by-side migration safety.
- List unresolved risks and newly discovered facts.
- Obtain review of the phase gate.
- Mark the phase complete, then replace the “Next phase” placeholder with a detailed plan for exactly one phase.

## Current state

- The replacement uses application ID `com.personal.favoriteplaces`, version code 3/version name 3.0.0, and temporary label **Places New**, so it can coexist with the untouched legacy package during migration. The Kotlin namespace remains unchanged; Room is version 2 and retains the same `favorites_db` file and original columns plus the migrated nullable `placeType` field.
- Room exports schemas 1 and 2, registers the explicit tested 1→2 migration in its sole production builder, and has no destructive fallback.
- The command-line baseline now builds with JBR 17.0.14 after aligning Java, Kotlin, and KAPT targets to JVM 17.
- The permanent replacement signer is now a 4096-bit RSA key in an ignored, owner-only PKCS#12 file under `local-signing/`, as approved by the user. Release credentials remain in ignored, owner-only `release-signing.properties` and are never printed. Clean release assembly produces a V2-signed APK; its external recovery copy remains pending.
- `local.properties` is removed from Git tracking while remaining on disk and is ignored. Generated `.gradle/` state is ignored and Phase 0 commands put the project cache under `/tmp` rather than recreating it in the project.
- A real Maps/Places key is now present only in ignored `local.properties`. A clean signed build injects it into both `BuildConfig` and the merged manifest with no fallback value, and the emulator renders Google map tiles plus the imported marker with zero authorization-failure log lines. Cloud-side Android/application restrictions and quotas still require review before household release.
- Historical revisions contain Google API-key-shaped values. No value is reproduced in these docs; the production key must be treated as historically exposed and checked for rotation plus Android application/API restrictions.
- The last distributed APK was recovered and inspected. Its now-lost debug signing key differs from the reset Mac's current debug key, so an in-place update is impossible unless another copy of the old keystore is found.
- The user's Samsung phone now has authorized ADB access after Developer options/USB debugging were enabled and Samsung Auto Blocker was disabled temporarily. The installed APK was pulled read-only and matched the supplied Google Drive artifact byte-for-byte.
- A read-only ADB/UI-automation proof confirmed the old Compose list exposes card text/actions and the Edit screen exposes title, address, and notes as accessibility nodes. Visible heart, rating, and color states can be captured from the rendered UI; hidden Google place ID/coordinates must be re-resolved from the saved name/address because Android still blocks the private raw database.
- Android backup/device-transfer rules now explicitly include the database and view preference; cloud database backup requires client-side encryption. The portable JSON backup remains the primary recovery mechanism because it is user-verifiable.
- The existing app is actively used on two devices, so side-by-side migration, one-device-first verification, and retention of each legacy app until sign-off are mandatory.
- Craig's Galaxy S25+ completed the first real household technical migration on 2026-08-22: 26 guarded legacy UI captures produced a version-2 import, all 26 requested phone numbers were retained in a private future-use sidecar, three moved businesses retained their legacy-address coordinates, and the empty replacement imported/exported identical records with digest `e5f77bba08b1a8d66c6c54a2dc7919a5c95cfdf747ce09c7d727d95646828aed`. The installed APK exactly matches the permanent-signed light-only build. Both packages remain installed pending owner visual/map sign-off.

---

## Phase 0 — stabilize the existing app and protect the database

**Phase status:** Complete — engineering gate passed 2026-08-21; household release prerequisites remain tracked separately
**Goal:** Establish a reproducible, tested version-1 baseline and a recoverable data path before dependency, API, architecture, UI, or schema modernization.

### Phase 0 scope

Phase 0 may change build configuration, tests, documentation, backup/export support, and narrowly scoped crash/race defects required to make the current app dependable. It must keep Room at version 1 and preserve current user-visible behavior unless a stability fix is explicitly documented.

Phase 0 does not include:

- Room schema or entity-column changes.
- Place-type persistence or migrations.
- Google Places API migration.
- Maps Compose replacement.
- Material 3 redesign or bottom navigation.
- New Saved, Find, or Nearby behavior.
- Broad architecture refactoring.

### Workstream 0.1 — establish upgrade identity

**Objective:** Establish a permanent replacement identity and prove that it can coexist safely with both legacy installations.

- [x] Determine whether each device received the app from Google Play or a directly installed APK: household releases are shared through Google Drive and installed directly from APK files.
- [x] Locate the last distributed artifact and identify its package name, version code, and signing-certificate fingerprint.
- [x] Confirm access to the corresponding signing key or Play App Signing account without copying credentials into the repository or documentation: the old key was lost after the Mac reset and no Play App Signing path exists.
- [ ] Confirm the installed package is `com.example.favoriteplaces` and record the current installed version on both devices. Craig's phone is confirmed; the second household phone remains pending.
- [x] Store only non-secret fingerprints and conclusions in the phase evidence log.

**Checkpoint:** The signing blocker is documented and a candidate replacement can be installed beside the legacy app with a new permanent identity.

Checkpoint passed on the emulator with the signed replacement and untouched legacy package installed together. Second-household identity confirmation remains a rollout prerequisite, not an uncertainty about the replacement signer.

### Workstream 0.2 — make the repository safe to build

**Objective:** Prevent secrets and generated files from being committed while preserving the existing local setup.

- [x] Add a focused `.gitignore` covering `local.properties`, Gradle/IDE output, build output, signing files, and other generated Android files.
- [x] Verify `local.properties` remains local/untracked and generated `.gradle/` state is ignored rather than treated as project content.
- [x] Scan tracked files and history for exposed Maps keys or signing material; historical API-key exposure was confirmed without copying values into documentation.
- [x] Verify local key state and research the safe restriction boundary: a real key now stays in ignored `local.properties`; clean-build inspection proves it replaces the non-secret fallback without exposing the value. It must be restricted to the permanent package/release SHA-1 plus native Maps/Places SDKs before household release. The current legacy REST flow cannot be certified safely Android-restricted and will not be reused as the production configuration.
- [x] Document required local properties using placeholders, never real key values.

**Checkpoint:** `git status` shows no local secret or generated build state as a commit candidate.

### Workstream 0.3 — establish a reproducible baseline build

**Objective:** Build the current application before modernizing it.

- [x] Use a supported JDK 17 baseline for the existing Android Gradle Plugin 8.1/Gradle 8.0 project and align Java, Kotlin, and KAPT targets to 17 so the baseline compiles consistently.
- [x] Document the selected JDK and exact baseline commands.
- [x] Run clean debug compilation, unit tests, lint, debug APK assembly, and a non-minified release candidate build.
- [x] Separate pre-existing warnings from blocking failures.
- [x] Make only the minimum build corrections required for a repeatable baseline; do not begin dependency modernization here.

**Checkpoint:** The unchanged product builds twice from a clean state using the documented toolchain.

### Workstream 0.4 — capture and test the Room version-1 baseline

**Objective:** Make the current database structure and values testable before touching persistence code.

- [x] Enable Room schema export and archive the generated version-1 schema JSON.
- [x] Add Room migration-test infrastructure without changing the database version.
- [x] Create a synthetic, production-shaped version-1 fixture covering all five exact colors, Favorite heart on/off, ratings, empty and long notes, multiple cities, place IDs, coordinates, Unicode, and nullable legacy values.
- [x] Define a deterministic digest over every persisted column and record expected row counts.
- [x] Add tests that open and read the fixture with the current application schema.
- [x] Add a guard test that fails if a destructive migration method is introduced.

**Checkpoint:** Tests prove that the current code opens the version-1 fixture and returns every row and value unchanged.

### Workstream 0.5 — create a recoverable backup/export path

**Objective:** Provide a recovery mechanism independent of assumptions about Android cloud backup.

- [x] Define a versioned export format containing every persisted field: ID, place ID, title, address, notes, rating, Favorite flag, exact color integer, city, latitude, and longitude.
- [x] Implement an isolated read-only export path that does not mutate Room and does not include API keys or unrelated preferences, plus a user-facing Android document-picker export action.
- [x] Validate exported row count and deterministic content digest; reject missing fields, unsupported versions, duplicate IDs, invalid types, and changed content.
- [x] Build and test a separate restore/verification path against a disposable in-memory Room database before allowing any production import. Import is additive, idempotent, transactional, and refuses conflicting IDs without partial writes.
- [ ] Document where each household backup will be stored and how its readability will be checked.
- [x] Review Android backup/device-transfer rules explicitly; do not treat generated template files as proven recovery. Craig's phone reports backup enabled with Google Backup Transport selected, and an explicit 2026-08-21 backup completed successfully; portable export/restore verification remains required.

**Checkpoint:** A version-1 test database can be exported, validated, restored into a disposable environment, and compared field-for-field with the source.

### Workstream 0.6 — capture current behavior with regression tests

**Objective:** Protect the workflow that exists today while later phases change its implementation.

- [x] Test insert, update, delete, delete/Undo, duplicate place IDs, and Favorite-heart updates.
- [x] Test card sorting by city, Favorite, color, and rating in both directions.
- [x] Test city-plus-color grouping used by List View and its exact map subset.
- [x] Test Edit preservation of address, coordinates, place ID, city, and Favorite flag when changing editable name, notes, rating, or color.
- [x] Add focused UI tests for Card View, List View, heart toggling, Edit, backup navigation, and persistence across Edit/Save. The live Google-backed add/select/save path is deliberately not treated as deterministic baseline automation; its save-readiness, structured-city selection, and Room persistence are tested below the network boundary, and the end-to-end live flow remains a device gate after Places API (New) is configured.
- [x] Record current intentional behavior separately from defects. The behavioral baseline in `modernization-plan.md` documents Card/List differences, unlabeled stored colors, the independent Favorite heart, the sparse Edit flow, and current Add/City Map behavior; its reliability-findings section records defects that tests must not preserve.

**Checkpoint:** The version-2 workflow has a meaningful automated regression baseline centered on persistence and the two organization modes.

### Workstream 0.7 — fix baseline stability defects only

**Objective:** Remove defects that prevent safe baseline use or reliable testing without starting modernization.

- [x] Fix the null-location dereference and fail safely when location is unavailable or denied.
- [x] Prevent Save from racing selected-place detail loading or saving stale coordinates/address.
- [x] Make city-map startup derive its camera from the first non-empty data emission and handle an empty subset safely.
- [x] Remove reachable forced-null crashes in core save, edit, list, and map paths when a narrow fix is possible.
- [x] Stop redundant Favorites refresh/query loops sufficiently to make baseline tests deterministic; one ordered Room flow now derives both card and city/color group state.
- [x] Move reverse geocoding off the main thread on Android 12L and below, use the asynchronous listener API on Android 13+, preserve coroutine cancellation, and expose safe user messages without logging addresses or search predictions.
- [x] Derive the stored city from structured geocoder locality fields with a tested fallback instead of assuming one comma-delimited address position.
- [x] Keep Room version 1 and verify the digest after every persistence-adjacent fix.

**Checkpoint:** Critical current flows fail safely instead of crashing, with regression tests for each corrected defect.

### Workstream 0.8 — prove side-by-side migration safety

**Objective:** Demonstrate that the stabilized replacement can coexist with the legacy app, accept a verified migrated dataset, and preserve it without touching the old installation.

- [x] Install the existing version-2 artifact and the replacement candidate side-by-side on a test device or emulator.
- [x] Populate the legacy-shaped representative version-1 fixture and create its migration artifact.
- [x] Import into the replacement without uninstalling, clearing, or mutating the legacy app.
- [x] Verify both package identities, replacement row count/full digest, Card/List views, Favorite hearts, Edit/Save preservation, same-signer reinstall persistence, delete/Undo coverage, and city/color map rendering. The live legacy Google Add call is explicitly a Places-migration gate because an Android-restricted native-SDK key must not be broadened to preserve the obsolete REST path.
- [x] Confirm the candidate does not change the Room version or schema identity.
- [x] Produce a Phase 0 evidence report with build outputs, test results, signing fingerprint comparison, database comparison, preservation boundary, and explicit pending household gates. It remains a draft until the permanent signer and both household rollouts are evidenced.

**Checkpoint:** Repeatable side-by-side migration preserves the complete dataset and existing workflow while the legacy copy remains available for comparison.

### Phase 0 gate

Phase 0 is complete only when all of the following are true:

- [x] The replacement application ID and signing identity are known, usable, and distinct from the legacy app.
- [x] The current app builds reproducibly with the documented JDK/toolchain.
- [x] The Room version-1 schema, fixture, row counts, and full-value digest are under test.
- [x] No destructive migration or database reset path exists.
- [x] A test export/restore round trip preserves every field.
- [x] Critical current workflows have regression coverage and baseline stability defects are fixed.
- [x] A side-by-side import and verification test passes without clearing or uninstalling either app.
- [x] The engineering evidence log below is complete and reviewed; household release/rollout prerequisites are explicitly separated and remain open.
- [x] No platform, API, Maps, architecture, UI, or schema modernization work has leaked into Phase 0.

### Phase 0 evidence log

Fill this in during implementation; do not mark the phase complete based on intention.

| Evidence | Result | Reference |
|---|---|---|
| Installed source and versions on both devices | Source confirmed as direct APK installation from Google Drive. Craig's phone is confirmed as package `com.example.favoriteplaces`, version code `2`, version name `2.0`, direct-installed 2025-08-24. Its installed APK exactly matches the supplied baseline; second phone pending | `/Users/macc/projects/personal/places-apk/app-release.apk`; APK SHA-256 `e8223f556dea25a746061f55a843b085e063023f34990be1210fd54ac7ddc526` |
| Production signing fingerprint/access | Baseline APK is V2-signed by lost Android Debug certificate SHA-256 `f5d4e19724bb74dac311eb5d93b68be33ee6241657069d57c5f5166e7290d600`; in-place update is impossible. The permanent replacement PKCS#12 signer is created locally with owner-only permissions and ignored by Git. Signed candidate certificate SHA-256 is `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`; SHA-1 is `397613d89234e73241ebbce6bc479823e4601313`. The generation helper refuses replacement without changing either secret file. Independent recovery copy remains pending | `apksigner verify --print-certs`; `app/build.gradle.kts`; `tools/release/create_signing_key.sh`; `release-signing-runbook.md` |
| Reproducible baseline build | Clean JBR 17 key-configured build passed all 138 tasks: unit tests, debug lint, debug APK, signed non-minified release APK, and Android-test APK. After the audit logging fix, focused unit/lint/release verification passed and the signed APK SHA-256 is `030372b3b83733b6884d0eb0b443512042dc5f33b3510233bf2200b8e1ec16f2` | JBR 17.0.14; commands below |
| Room v1 exported schema | Generated without changing database version; schema JSON SHA-256 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230` | `app/schemas/com.example.favoriteplaces.feature_favorites.data.data_source.db.FavoriteDatabase/1.json` |
| Fixture row count and digest | Passed on read-only Pixel emulator: five rows / all 11 stored columns; expected SHA-256 `2726927dc77562ea50b42d2f8013036b8b6f384120fcfba9cd11758b48f40c0f` | `FavoriteDatabaseV1ContractTest.kt` |
| Export/restore round trip | Passed on JVM and API 37 Pixel emulator. Version-1 deterministic JSON digest is `2334a8715a4302b4b57eef47578eb2757713cda73fae166ce0a6d2f401cd4bfa`; file export/import reproduces all rows, is idempotent, and a conflict rolls back with no partial insert. Android document-picker UI was launched and inspected successfully. Android cloud backup also succeeded for approximately 324 KB | `FavoriteBackupCodecTest.kt`, `FavoriteBackupRoomRoundTripTest.kt`, `FavoriteBackupStoreTest.kt`, `DataBackupScreen.kt` |
| Regression test suite | JVM sorting, Edit-field preservation, structured-city resolution, import planning, city-map emission/empty-state, legacy-converter compatibility, and destructive-fallback guards pass. Direct instrumentation passes 12/12 tests for the Room v1 contract, file backup/restore and rollback, CRUD/Undo, Favorite-heart independence, duplicate place IDs, city/color grouping, Card/List actions, heart toggling, Edit/Save preservation, and backup navigation. Only the live Google-backed Add flow remains a later device gate | `PlaceCityResolverTest.kt`, `FavoriteEditorTest.kt`, `FavoriteBackupImportPlannerTest.kt`, `LegacyMigrationGoldenTest.kt`, `FavoriteDatabaseV1ContractTest.kt`, `FavoriteDaoBehaviorTest.kt`, `LegacyWorkflowUiTest.kt` |
| Side-by-side migration test | Passed engineering proof on API 37 Pixel emulator: legacy `com.example.favoriteplaces` 2.0 and signed replacement `com.personal.favoriteplaces` 3.0.0 remain installed together. A two-record v1 backup imported through the document picker with full digest preservation; Card/List showed Unicode title, address, notes, ratings, independent heart states, and both city groups. Same-signer `install -r` preserved the imported data, and the key-configured city map rendered tiles plus the expected marker with zero authorization failures. Delete/Undo and Edit/Save preservation remain covered by direct instrumentation. The legacy package was never cleared, removed, or mutated | package inspection; document-picker import; UI readback; same-signer reinstall; Maps/logcat smoke; direct Android instrumentation `OK (12 tests)` |
| Craig household migration | Passed technical gate on the Galaxy S25+: 26 distinct legacy records captured with hashed Edit/list evidence outside Git; v2 backup inspected and imported transactionally into an empty replacement; post-import phone export has identical parsed records and digest `e5f77bba08b1a8d66c6c54a2dc7919a5c95cfdf747ce09c7d727d95646828aed`; 15 nonempty notes, 3 ratings, 1 Favorite heart, 2 exact legacy colors, and 26 default types retained. A separate checksummed sidecar retains all 26 requested phone numbers and flags 3 moved/current-address mismatches. Installed APK equals release SHA-256 `5b12be8e35c6dcbe3d5046da562b8abf0fd283485df60ed748c9061a76c15ece`; both packages remain installed pending owner sign-off | Private evidence manifest under `/Users/macc/Documents/Places-Migration/Craig-S25-2026-08-22`; app document-picker import/export; package/APK/signature inspection |
| Phase 0 evidence report | Draft consolidates the exact data invariants, build/test commands, identity fingerprints, regression scope, household preservation boundary, and every remaining proof required before phase completion | `phase-0-evidence-report.md` |
| Phase review | Engineering gate passed. No schema/database-version change, destructive reset, legacy-package mutation, key leakage, or unreviewed modernization entered Phase 0. Independent signer recovery, Cloud key restrictions/quotas, and both one-device-first household migrations remain mandatory before release | Final quality audit below; `phase-0-evidence-report.md` |

### Phase 0 implementation research

Ambiguous platform behavior was resolved from primary Android documentation before implementation:

| Topic | Finding applied in Phase 0 | Primary reference |
|---|---|---|
| Android application updates | Android accepts an update only when the application ID and signing certificate match. The lost legacy signer therefore requires a side-by-side replacement identity, not an attempted overwrite | [How app updates work](https://developer.android.com/google/play/app-updates) |
| Portable user-selected backups | Storage Access Framework create/open document contracts provide persistent, user-selected local or cloud-provider files without broad storage permission | [Access documents and other files](https://developer.android.com/training/data-storage/shared/documents-files) |
| Compose UI regression tests | Semantics-based node queries/actions are the supported boundary for testing Compose user behavior; stable test tags were added only where user-facing text was not a reliable selector | [Test your Compose layout](https://developer.android.com/develop/ui/compose/testing) |
| Navigation testing | A test navigation controller can verify destinations and actions without coupling tests to internal composable implementation | [Test Navigation Compose](https://developer.android.com/guide/navigation/testing/compose) |
| API 37 instrumentation compatibility | AndroidX Test 1.7/Espresso 3.7 removes obsolete reflective `InputManager` access; upgrading test-only dependencies fixed direct API 37 execution without changing production dependencies | [AndroidX Test release notes](https://developer.android.com/jetpack/androidx/releases/test) |
| Reverse geocoding | Android 13 introduced the asynchronous `GeocodeListener` API and deprecated blocking geocoder calls. The implementation uses the listener on API 33+ and confines the compatibility call to `Dispatchers.IO` below 33 | [Geocoder reference](https://developer.android.com/reference/android/location/Geocoder) |
| Permanent APK signing | A directly distributed app must use the same private signing key for its entire update lifetime. Android recommends strong passwords, safe independent keystore storage, and keeping signing credentials out of build files; losing a self-managed key makes future updates impossible | [Sign your app](https://developer.android.com/studio/publish/app-signing) |
| Maps/Places key restrictions | A production Android key must be restricted to the replacement package plus release certificate SHA-1 and to only the native Android SDKs it uses. Google warns that older mobile REST web-service calls may not enforce Android restrictions and recommends native SDKs, so the current legacy REST key cannot be declared safely production-ready before the Places migration | [Google Maps Platform security guidance](https://developers.google.com/maps/api-security-best-practices) |
| Read-only legacy UI evidence | UI Automator is intended for opaque-box interaction with visible elements and accessibility semantics, and ADB supports streaming device screenshots directly to the host. The capture helper therefore records only the currently visible legacy Edit hierarchy and PNG, verifies the legacy package is foreground, and never attempts private app-storage access | [UI Automator](https://developer.android.com/training/testing/other-components/ui-automator-legacy), [Android Debug Bridge](https://developer.android.com/tools/adb) |

### Phase 0 decision log

Record decisions as they are made:

| Date | Decision | Reason |
|---|---|---|
| 2026-08-21 | Keep Room at version 1 and introduce only exported schema/test infrastructure. | The household installations must remain readable without any migration or data rewrite during stabilization. |
| 2026-08-21 | Use JBR 17.0.14 and align Java/Kotlin/KAPT bytecode targets to 17. | The system JDK 26 is incompatible with this Gradle 8.0 baseline; target 17 builds consistently. |
| 2026-08-21 | Put Gradle's project cache in a temporary directory for Phase 0 command-line builds. | The user intentionally removed the project `.gradle` directory and does not want it treated as project content. |
| 2026-08-21 | Keep a real restricted Maps key in ignored `local.properties`, with a non-secret compile placeholder in `local.defaults.properties`. | This is a personal on-device app with no backend, as approved by the user. |
| 2026-08-21 | Treat the last Google Drive APK as the released upgrade-identity baseline. | Both household installations are directly installed APKs, so their signing certificate must match the next release APK. |
| 2026-08-21 | Do not install the current candidate over a household copy. | Although package/version configuration matches, its signing certificate differs from the distributed APK and Android would reject the update. |
| 2026-08-21 | Keep a no-ADB fallback, but use authorized ADB on Craig's phone where safe. | Enabling Developer options/USB debugging and disabling Samsung Auto Blocker exposed the device; read-only APK verification and Android backup then succeeded. |
| 2026-08-21 | Keep each old app installed until its replacement dataset is independently verified. | Without the old signing key, the replacement needs a new application ID and cannot directly access the old app's private SQLite database. |
| 2026-08-21 | Use a strict, deterministic JSON backup envelope at format version 1. | It preserves explicit nulls and every Room v1 field, provides count/digest verification, and rejects silent or structurally ambiguous imports. |
| 2026-08-21 | Execute persistence tests on a read-only/no-snapshot emulator rather than a household phone. | It proves real Android SQLite/Room behavior while keeping both live installations and databases outside the test environment. |
| 2026-08-21 | Block Add/Save until details for the exact selected place have loaded. | The old flow could save a new prediction with the previous address or coordinates if Save won the asynchronous race. |
| 2026-08-21 | Treat missing GPS/geocoder/map subset data as an error or empty state rather than dereferencing it. | These are normal runtime conditions and must not crash the stabilized baseline. |
| 2026-08-21 | Migrate legacy user data through read-only UI capture plus verified import, not by claiming direct SQLite access. | The non-debuggable old app protects its private database, but its list/Edit accessibility trees expose the user-authored fields; hidden Places metadata can be re-resolved and reviewed. |
| 2026-08-21 | Keep the modernized app local-first with Room; defer Firebase. | The existing dataset is only a few hundred kilobytes, so Firebase would not materially solve phone storage while adding authentication, sync conflicts, security rules, and operational scope. A versioned export/import flow through Android's document picker provides portable Google Drive backups without a custom backend. |
| 2026-08-21 | Replace the obsolete in-place-upgrade requirement with a permanent side-by-side migration. | Official Android update rules require the same application ID and signer; the legacy signer is lost. The old app must remain installed while the new app imports and verifies each household dataset. |
| 2026-08-21 | Use Android's Storage Access Framework for portable backup files. | `ACTION_CREATE_DOCUMENT` and `ACTION_OPEN_DOCUMENT` provide user-selected access to local and cloud document providers such as Google Drive without broad storage permission. |
| 2026-08-21 | Use a self-managed permanent release signer for the directly shared replacement APK, with its keystore and credentials outside Git and a separately verified recovery copy. | Direct Google Drive APK distribution has no Play App Signing recovery path; the lost legacy debug key proved that a debug or single-Mac signing identity is not durable enough. |
| 2026-08-21 | Do not claim the legacy REST Places key is safely Android-restricted. | Current Google guidance recommends native Android SDKs and warns that older mobile web-service endpoints may not support Android application restrictions. The production key restriction is coupled to the later Places SDK migration and the permanent release SHA-1. |
| 2026-08-21 | Store the working permanent signer inside ignored `local-signing/` and its generated credential properties at the ignored repository root, as explicitly approved by the user. | This keeps direct-APK builds reproducible on the current Mac without exposing credentials to Git. It does not replace the still-required independent recovery copy. |

### Baseline build commands and results

Selected Java runtime: `/Users/macc/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home` (`17.0.14`). Each run used a newly created `/tmp/placesapp-phase0-*` directory for `--project-cache-dir`.

```sh
JAVA_HOME=/Users/macc/Library/Java/JavaVirtualMachines/jbr-17.0.14/Contents/Home \
  ./gradlew --no-daemon --project-cache-dir "$PHASE0_CACHE" \
  clean testDebugUnitTest lintDebug assembleDebug assembleRelease
```

The full clean command passed twice before signing. After creating the permanent signer, a clean `testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest` run passed all 138 tasks under the pinned JBR 17 runtime. Direct instrumentation remains 12/12. Android Studio now bundles JBR 21 on this Mac; invoking that path reproduces an expected legacy KAPT module-access failure, so Phase 0 commands must use the explicit JBR 17 path until the build-tool modernization phase. Remaining output is baseline warning debt, chiefly the old AGP 8.1/compileSdk 34 compatibility warning and a deprecated migration-test-helper constructor; no warning was suppressed to create a false clean result.

The release artifact is signed by the permanent replacement certificate and installs beside the untouched legacy package. `apksigner` verifies one 4096-bit RSA V2 signer, `aapt2` verifies package `com.personal.favoriteplaces` version code 3/name 3.0.0, a pulled installed APK matches the built APK byte-for-byte, and `adb install -r` succeeds with the same signer while preserving imported data. The real local key replaces the compile fallback in generated BuildConfig and the manifest, and a live emulator map renders successfully. It is still not a household release because cloud restrictions/quotas, independent signer recovery, and both household migrations remain release prerequisites.

### Phase 0 interim quality audit — 2026-08-21

- **Data integrity:** Room remains at version 1 and its exported schema hash is unchanged. Restore uses `OnConflictStrategy.ABORT` inside a Room transaction; it never clears or overwrites rows. Duplicate backup IDs, changed digests, unsupported versions, oversized files, missing fields, and ID conflicts are rejected.
- **Error handling:** Backup I/O runs on `Dispatchers.IO`; streams are closed with `use`; UI operations expose progress and safe failure messages. Edit, Saved, Add, map, GPS, geocoder, and selected-place loading no longer depend on reachable forced-null assertions. Long-running paths rethrow `CancellationException` rather than reporting lifecycle cancellation as a user failure. Reverse geocoding is asynchronous on API 33+ and moved to `Dispatchers.IO` on older supported devices.
- **Logging:** Backup, Saved, Edit, Add, location, geocoder, and legacy Places failures use stable action tags plus privacy-safe exception-class categories without serializing throwable messages. Logs exclude backup JSON, notes, addresses, coordinates, API keys, search queries, predictions, request URLs, and other record contents. Place-title, map-projection, Street View location, and permission-state debug logs were removed.
- **State correctness:** The former `LaunchedEffect(state)` refresh loop and repeated city/color queries were removed. One cancellable ordered Room flow now derives both card and city/color group state.
- **Migration tooling:** The offline legacy-capture converter validates all persisted fields, field-level provenance, owner review, timezone-bearing capture time, existing in-folder XML/PNG evidence, IDs, ratings, exact signed color integers, and coordinates; rejects empty or malformed captures; writes atomically without overwriting an existing artifact; and reports both capture and record digests. The ADB evidence helper verifies device readiness, valid UI XML/PNG, foreground legacy package plus Edit fields, private file modes, outside-repository output, and no replacement. Eighteen Python tests plus a Kotlin golden test pass. A live emulator negative probe also correctly refused the legacy home screen. Actual household captures and screenshots stay outside Git. The documented preservation boundary distinguishes exact UI-reviewed values from inaccessible metadata that must be re-resolved or migration-generated.
- **Release identity:** Permanent local signing material is PKCS#12, owner-only, ignored, generated without printing credentials, and guarded against overwrite. Clean signed build, APK certificate/manifest inspection, byte-identical installed pull, side-by-side launch, and same-signer reinstall all pass. Only the independent recovery copy remains open.
- **Build quality:** Unit tests, lint, debug APK, signed release APK, and Android-test APK pass under JBR 17. Direct instrumentation passes 12/12 tests on the no-snapshot API 37 emulator. The Gradle UTP wrapper still aborts with zero tests on that preview emulator, while the same signed test APKs pass directly with `am instrument`; this is recorded as a wrapper/infrastructure issue rather than hidden or bypassed with a lint/test baseline.
- **Exact-candidate audit:** Main source contains zero forced-null assertions, destructive database calls, stack-print/standard-output calls, and TODO/FIXME/HACK markers. The audit found that throwable messages from Retrofit or document operations could contain request URLs, keys, search text, or file details. All 14 failure sites now use `PrivacySafeLog`, which records only the stable action and exception class; a JVM test proves a secret-bearing throwable message is excluded. Unit tests, lint, signed release assembly, same-signer reinstall, and imported-record readback pass after this fix.
- **Release/rollout prerequisites outside the engineering Phase 0 gate:** independent signer recovery copy; cloud-side restricted replacement key and quotas; second household package/version confirmation; live Add verification after Places API (New) is configured; and the actual one-device-first household capture/import/readback verification while each old app remains installed.

---

## Phase 1 — platform/toolchain modernization

**Phase status:** Complete — engineering gate passed 2026-08-21
**Goal:** Move the project from its 2023 build stack to a current, reproducible Android 16 toolchain while keeping the application behavior, permanent identity, signing configuration, Room version-1 schema, and imported data unchanged.

### Phase 1 scope and exclusions

This phase may change Gradle/AGP/Kotlin/KSP, build DSL, dependency declarations, stable AndroidX/Compose infrastructure versions, compile/target SDK, edge-to-edge inset handling, predictive-back compatibility, and tests required by those changes.

This phase does not include:

- Places API (New), request behavior, or removal of legacy Retrofit code.
- Official Maps Compose migration or deletion of copied Maps code.
- Product UI redesign, bottom navigation, Saved/Find/Nearby behavior, or new filters.
- Room version/entity/table/column changes, DataStore migration, or place-type persistence.
- Raising `minSdk` above 24 unless a later explicit product decision is supported by device evidence.
- Changing application ID, release signer, backup format, exact stored colors, or Favorite-heart semantics.

### Phase 1 implementation research — 2026-08-21

| Topic | Decision for this phase | Primary reference |
|---|---|---|
| Final build stack | Target stable AGP 9.3.x with Gradle 9.5, JDK 17, and built-in Kotlin. AGP 9.3 supports API 37 and requires Gradle 9.5/JDK 17. Use exact versions, never dynamic selectors | [AGP 9.3 release notes](https://developer.android.com/build/releases/agp-9-3-0-release-notes) |
| Controlled bridge | Upgrade first to stable AGP 8.13.2/Gradle 8.13 before crossing the AGP 9 built-in-Kotlin boundary. AGP 8.13 supports Android 16 QPR2/API 36.1 and Kotlin 2.3-compatible bytecode | [AGP 8.13 release notes](https://developer.android.com/build/releases/agp-8-13-0-release-notes) |
| Built-in Kotlin | On AGP 9 remove `org.jetbrains.kotlin.android`, migrate away from `kotlin-kapt`, and remove redundant `kotlinOptions.jvmTarget`; do not hide incompatibility with permanent `android.builtInKotlin=false`/`android.newDsl=false` opt-outs | [Migrate to built-in Kotlin](https://developer.android.com/build/migrate-to-built-in-kotlin) |
| Annotation processing | Move Room and Hilt from kapt to KSP2 before enabling built-in Kotlin. Room and Dagger/Hilt support KSP; KSP is the recommended replacement because kapt is maintenance-only. Retain Room schema arguments and prove schema identity | [Migrate from kapt to KSP](https://developer.android.com/build/migrate-to-ksp), [Hilt Gradle setup](https://dagger.dev/hilt/gradle-setup.html) |
| Compose | Replace the old compiler-extension pin with the Compose compiler Gradle plugin and use the stable Compose BOM `2026.08.00`. Do not adopt alpha/RC artifacts just because they are newer | [Compose compiler/BOM setup](https://developer.android.com/develop/ui/compose/setup-compose-dependencies-and-compiler) |
| Stable libraries | Prefer stable releases: Room 2.8.4, Navigation 2.9.8, Lifecycle 2.11.0, and Activity 1.13.0. Keep Navigation 2 rather than combining a Navigation 3 rewrite with the toolchain phase | [Room](https://developer.android.com/jetpack/androidx/releases/room), [AndroidX stable versions](https://developer.android.com/jetpack/androidx/versions), [Lifecycle](https://developer.android.com/jetpack/androidx/releases/lifecycle), [Activity](https://developer.android.com/jetpack/androidx/releases/activity) |
| Android SDK | Compile against API 37 because the selected current Compose BOM requires it, while deliberately targeting stable Android 16/API 36 for this behavior-migration phase. Android 16 target behavior requires real edge-to-edge inset handling and supported predictive-back/navigation APIs; target 37 is a separate runtime-behavior opt-in, not a lint-only version bump | [Android 17 SDK setup](https://developer.android.com/about/versions/17/setup-sdk), [Android 16 targeted changes](https://developer.android.com/about/versions/16/behavior-changes-16) |
| Secret injection | Keep the official Secrets Gradle Plugin 2.0.1 unless an actual AGP incompatibility is reproduced. At every build-stack checkpoint assert that the ignored local key replaces the fallback without printing its value | [Secrets Gradle Plugin](https://developers.google.com/maps/documentation/android-sdk/secrets-gradle-plugin) |

### Workstream 1.1 — centralize versions without changing resolution

- [x] Add `gradle/libs.versions.toml` containing the exact currently resolved plugin and dependency versions.
- [x] Convert root/module build scripts to catalog aliases in small mechanical groups while preserving all coordinates and scopes.
- [x] Remove duplicate explicit versions only after build/dependency resolution shows equivalent coordinates.
- [x] Add `.kotlin/` to generated-state ignores before a modern Kotlin plugin can create it.
- [x] Run unit tests, lint, debug/release assembly, key-injection checks, APK identity/signature checks, and Room schema/digest checks.

**Checkpoint:** Version declaration is centralized with no dependency upgrade, schema change, APK identity change, or behavior regression.

Checkpoint passed: the catalog build completed 137 tasks; unit tests, lint, debug/release/test APK assembly, permanent signer fingerprint, package/version, configured-key/fallback checks, unchanged Room schema SHA-256, same-signer reinstall, and imported-record UI readback all pass.

### Workstream 1.2 — upgrade to the AGP 8.13 bridge

- [x] Update Gradle wrapper to 8.13 and AGP to 8.13.2 under JDK 17.
- [x] Use the public compile-SDK DSL while temporarily preserving compile/target values until the bridge build passes.
- [x] Resolve deprecated/removed build DSL only through documented public APIs; do not suppress compatibility failures.
- [x] Re-run the full checkpoint and install the signed release over the signed emulator replacement to verify imported-record persistence.

**Checkpoint:** AGP 8.13/Gradle 8.13 builds cleanly and the signed reinstall preserves Room v1 data.

### Workstream 1.3 — modernize Kotlin, Compose, and stable AndroidX

- [x] Upgrade to Kotlin 2.4.10 and apply the Compose compiler Gradle plugin.
- [x] Upgrade the Compose BOM to stable `2026.08.00`, removing individually pinned Compose versions that fight the BOM.
- [x] Replace alpha Lifecycle/Hilt Navigation artifacts with stable versions; update Activity, Lifecycle, Navigation 2, Core, AppCompat, Material, coroutines, serialization, networking, and test libraries in reviewable groups.
- [x] Fix only source/API compatibility required by each group; do not redesign screens.
- [x] Remove the obsolete Material 2 UI dependency after converting remaining components to Material 3 equivalents.
- [x] After every group, run compile/tests/lint and compare Room schema, backup golden digest, behavior tests, and key injection.

**Checkpoint:** Current stable UI/runtime infrastructure builds and behaves like the stabilized app with no alpha/RC dependency required.

### Workstream 1.4 — migrate Room and Hilt processing from kapt to KSP2

- [x] Upgrade Room to 2.8.4 and use Hilt/Dagger 2.58 for the AGP 8 bridge; then move Hilt with the AGP 9 boundary.
- [x] Apply stable KSP2, move Room/Hilt compiler configurations from `kapt` to `ksp`, and preserve Room schema-output/incremental arguments.
- [x] Remove the redundant AndroidX Hilt compiler after dependency analysis proved no supported annotation required it.
- [x] Remove kapt and its configuration after confirming no kapt dependency remained.
- [x] Generate and compare the Room schema; fail on any version-1 JSON/schema identity change.
- [x] Run migration/backup tests plus direct instrumentation before proceeding.

**Checkpoint:** KSP generates valid Room/Hilt code, kapt is absent, and the exact Room v1 contract remains unchanged.

### Workstream 1.5 — cross to AGP 9.3 and built-in Kotlin

- [x] Update Gradle wrapper to 9.5 and AGP to stable 9.3.1.
- [x] Upgrade Hilt/Dagger from the bridge-compatible 2.58 to stable 2.60.1 only after AGP 9 is active.
- [x] Remove the Kotlin Android plugin and use AGP built-in Kotlin; migrate remaining DSL to supported public interfaces.
- [x] Keep Java/Kotlin bytecode at 17 and verify command-line builds with pinned JBR 17.
- [x] Verify the Secrets plugin, release signing, Room schema export, KSP, lint, and Android tests under the final build stack.
- [x] Retain no built-in-Kotlin or DSL compatibility opt-out flags.

**Checkpoint:** The project uses AGP 9.3/Gradle 9.5 built-in Kotlin without legacy DSL/KAPT escape hatches.

### Workstream 1.6 — compile/target Android 16 and handle platform behavior

- [x] Compile against installed API 37 and target API 36 while preserving `minSdk = 24`.
- [x] Enable edge-to-edge explicitly and make every current Scaffold consume its system-bar content padding.
- [x] Keep supported Navigation behavior for predictive back; no global opt-out or compatibility flag is present.
- [x] Exercise Card/List/Edit/Add, backup/import, document picker, keyboard/insets, and map paths through the API-37 emulator regression suite. Oldest-supported-device runtime coverage remains a pre-household-release matrix item because no API-24 device is currently attached.
- [x] Install the final signed candidate over the emulator replacement and prove the imported records remain visible.

**Checkpoint:** Target-36 behavior is correct, signed reinstall preserves all data, and no content/action is obscured by system bars or IME.

### Phase 1 quality audit and gate

Before completion:

- [x] Audit dependency graph for duplicate/alpha/RC/obsolete artifacts and resolve every avoidable warning.
- [x] Audit build scripts for deprecated DSL, compatibility opt-outs, hidden repositories, dynamic versions, duplicated coordinates, generated-state leakage, and accidental secret/signing output.
- [x] Audit target-36 error handling and logging, including cancellation, permission, backup, Maps, and process failures; broad boundary catches preserve cancellation and emit privacy-safe category-only logs plus user-safe messages.
- [x] Run clean unit tests, lint, debug/release/APK-test assembly, direct instrumentation, key/fallback checks, `apksigner`, Room schema hash, same-signer reinstall, and UI/data readback.
- [x] Record every unavoidable warning with owner/reason/removal condition; no merely compile-only compatibility work remains.

**Gate:** Passed. The app uses AGP 9.3.1/Gradle 9.5, Kotlin 2.4.10, KSP 2.3.10, JDK/JVM 17, compile API 37/target API 36, stable supported non-Google infrastructure, built-in Kotlin, Material 3, and explicit edge-to-edge handling. The signed candidate preserves the imported Room v1 data, and Places/Maps behavior and the schema remain unchanged.

### Phase 1 verification and quality-audit evidence

- Clean `clean testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest`: **147 tasks successful**.
- Direct API-37 emulator instrumentation: **12/12 tests passed**, including Room v1 contract/DAO, backup round-trip/conflict handling, and Card/List/Edit/Favorite-heart workflows. Debug uses the isolated `com.personal.favoriteplaces.debug` ID so tests cannot overwrite release data.
- Room schema SHA-256 remains `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230`; destructive migration/clear APIs are absent.
- Release signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`; reinstall of package `com.personal.favoriteplaces`, version 3/3.0.0, target 36 succeeded and UI readback still shows imported **Root Down** and **Café Élan**.
- Release `BuildConfig` and manifest contain a configured non-placeholder key without exposing it. Runtime dependency inspection found no alpha, beta, RC, snapshot, or dynamic version.
- Lint reports 0 errors and five deliberate warnings: target 36 (target 37 behavior opt-in deferred until researched), Gradle 9.5 (the AGP 9.3 documented pairing; revisit with the next AGP line), Maps SDK 18.1, Maps KTX 3.4, and Places 3.2 (owned by the Google integration phases). Compiler warnings are confined to the copied Maps Compose source and disappear when Phase 3 replaces that fork with the maintained dependency.
- Quality review removed obsolete Material 2 UI usage/dependency, deprecated Compose test/scaffold APIs, kapt, built-in-Kotlin escape hatches, unsafe logging, and avoidable state boxing. Cancellation is rethrown at asynchronous boundaries and user/place data is not logged.

---

## Phase 2 — Places API (New) request modernization

**Phase status:** Complete — engineering gate passed 2026-08-21; public legal-page publication remains a household-release prerequisite
**Goal:** Replace direct legacy Places web-service calls and fragile address parsing with a typed, testable Places SDK for Android (New) integration while preserving Room v1 and the current search/save workflow.

### Phase 2 scope and exclusions

This phase owns initialization of Places SDK 5.3.0, autocomplete/session lifecycle, selected-place detail fields, structured city extraction, cancellation/error mapping, request/cost auditing, removal of legacy Retrofit Places code and unused legacy widgets, and tests for the resulting request contract.

It does not include the official Maps Compose replacement, Nearby/Text Search product discovery, visual redesign, new bottom navigation, place-type persistence/schema changes, or Firebase. Those remain later rolling phases.

### Current Google request audit — 2026-08-21

| Existing operation | Verified problem | Phase 2 direction |
|---|---|---|
| Legacy REST autocomplete | One request is sent only after Search, with `types=establishment`, a 75-meter location bias, no session token, no status-body validation, and even the sentinel `0,0` origin when location is unavailable | Use programmatic Autocomplete (New) through `PlacesClient`, create one token per query/selection session, use a realistic circular location bias only when an actual origin exists, never fabricate an origin, and retain explicit cancellation |
| Legacy REST place details | Requests `name,geometry,formatted_address`, but code also expects `address_components`; that field is never requested, so city extraction normally falls through to comma-position parsing | Fetch only the reviewed SDK fields needed for save: display name, formatted address, location, address components, plus the minimal type fields required for current result parity; derive city from typed components and never parse the display address |
| Location/map point | Android Geocoder already uses the supported async API on API 33+ and IO confinement below it | Keep it as the map-point fallback in this phase; represent unavailable/empty/error distinctly and do not mix it with an autocomplete billing session |
| Networking/models | Retrofit/Gson carries many generated Place Details fields that the app never requests or uses; response HTTP success does not prove Places status success | Put an app-owned interface and models in front of `PlacesClient`, map `ApiException` status categories to stable domain failures, log no query/address/place data, then delete the legacy endpoint, DTO, Retrofit converter, and unused autocomplete manager after parity |

### Phase 2 implementation research — 2026-08-21

- Places SDK for Android **5.3.0** is the current stable release. New API features require `Places.initializeWithNewPlacesApiEnabled()` and Places API (New) enabled for the Android-restricted key.
- Programmatic Autocomplete (New) supports circular location bias/restriction, origin distance, up to five primary types, countries, language/region, and session tokens. Bias and restriction are mutually exclusive.
- A token begins with the first autocomplete request and ends with the selected place-details request; it must not be reused. Abandoned sessions and missing/reused tokens have different billing behavior, so token ownership belongs in a focused search-session component rather than the Composable.
- Place Details field lists determine both payload and highest applicable billing SKU. The implementation will request only fields proven necessary by tests and record the resulting SKU before live rollout.
- Google's SDK contract says the localized formatted address is for display while address components are for structured extraction. City logic will prioritize `locality`, then documented regional fallbacks, and return an explicit unknown value for review instead of splitting commas.
- The final detail mask is only `FORMATTED_ADDRESS`, `LOCATION`, and `ADDRESS_COMPONENTS`, all in **Place Details Essentials**. The saved name comes from the chosen autocomplete prediction, avoiding the higher **Place Details Pro** tier required by `DISPLAY_NAME`; place ID comes from the selection itself. Type fields remain on predictions and are not requested again until the later type-persistence design proves a need.
- Programmatic predictions displayed without an embedded Google map require visible Google Maps attribution. The current Add results now show a text attribution; the visual refresh should replace it with the official logo asset where space allows and retain accessibility labeling. Public Terms/Privacy links and any returned third-party attribution are release requirements.

Primary references: [Autocomplete (New)](https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete), [session tokens](https://developers.google.com/maps/documentation/places/android-sdk/place-session-tokens), [Places SDK versions](https://developers.google.com/maps/documentation/places/android-sdk/versions), [release notes](https://developers.google.com/maps/documentation/places/android-sdk/release-notes), [Place data fields](https://developers.google.com/maps/documentation/places/android-sdk/data-fields), and [usage/billing](https://developers.google.com/maps/documentation/places/android-sdk/usage-and-billing).

### Workstream 2.1 — establish a testable Places boundary

- [x] Define app-owned prediction, selected-place, origin/bounds, request, and typed failure models that do not expose Google SDK or legacy Retrofit DTOs to ViewModels/UI.
- [x] Define a small cancellable Places search interface and fakes; add tests for optional origin, token/session replacement, exact selected-place fields, and structured-city save behavior.
- [x] Refactor Add to one immutable UI state with explicit loading/results/selection readiness so stale results/details cannot be saved.

### Workstream 2.2 — implement Autocomplete and Place Details (New)

- [x] Upgrade Places SDK to 5.3.0 and initialize New exactly once in `Application`; a missing/placeholder key produces a safe unavailable repository instead of crashing Add.
- [x] Implement programmatic autocomplete with cancellation, a minimum useful query length, one session token per active query/selection flow, optional circular bias only for a real location, and no location permission requirement.
- [x] Fetch the selected place with the same token and the three-field Essentials mask; map formatted address verbatim, coordinates exactly, and city from typed address components.
- [x] Preserve duplicate prevention by place ID and prevent Save until the exact selected detail result is ready.

### Workstream 2.3 — remove janky parsing and legacy networking

- [x] Replace comma/position address fallbacks with structured component resolution plus an explicit reviewable unknown-city path; display stored addresses verbatim instead of splitting them for Edit.
- [x] Remove `GooglePlacesApi`, 30 legacy prediction/details/network files, Places Retrofit use cases, the unused legacy autocomplete manager, Gson converter, and Retrofit/OkHttp dependencies.
- [x] Keep Android Geocoder only for a user-selected map point and isolate it behind an app-owned resolver.

### Workstream 2.4 — live request, cost, and failure verification

- [x] Verify Places API (New) is enabled and that the current API restrictions authorize both the signed release and isolated debug package without printing the key. Application restrictions are not currently enabled; a separate replacement-only restricted key remains the preferred release hardening so the embedded legacy key can stay untouched through migration.
- [x] Run a privacy-safe live matrix for exact-name, partial-name, Unicode, duplicate-looking names, denied/precise/approximate location, no result, session replacement/cancellation, offline recovery, auth and quota categories, selected-place save, same-signer update preservation, and duplicate-save prevention. Quota was verified through the typed fake/error boundary rather than intentionally consuming billable quota.
- [x] Capture call counts, session starts/completions/abandonment, requested fields, latency categories, status categories, and applicable SKUs without queries, addresses, coordinates, place IDs, tokens, keys, or user notes.
- [x] Compare result/save parity with the legacy path, then remove the old path rather than retaining a permanent dual implementation.
- [x] Apply compliant 12sp `Google Maps` text attribution beside programmatic results and preserve/render returned third-party Place Details attribution HTML with links. Accurate local-first Terms and Privacy pages now exist in `docs/legal/`; publishing them and wiring their final public URLs into the later About/settings surface remain household-release prerequisites because publication is an external deployment.

Initial live authorization probe: the configured release key still rendered Maps, but the first Places SDK 5.3.0 Autocomplete (New) request returned the typed `Authorization` category. Cloud Console evidence then showed that the key is API-restricted to five services and already includes Places API (New); the owner's earlier “no restriction” description referred to the missing application restriction, not the API list. After the Cloud configuration propagated, a repeat signed-release probe successfully returned current Autocomplete (New) results and selected-place details. No key value was logged.

The legacy app continuing to work confirms that the key value and its existing Maps/legacy Places authorization remain valid, but did not by itself prove that Places API (New) was enabled. The successful repeat probe now proves New API authorization for the signed replacement. If application restrictions are added later, they must authorize both apps during migration: legacy package `com.example.favoriteplaces` with APK-derived SHA-1 `CAC21A835062766C67E942295239A10FF77B2B58`, and replacement package `com.personal.favoriteplaces` with release SHA-1 `397613D89234E73241EBBCE6BC479823E4601313`. Reading the legacy public certificate from the saved APK does not require the lost private signing credentials.

### Phase 2 quality audit and gate

- [x] Audit every Google request for purpose, fields, bounds/types, token, cancellation, error surface, attribution, restriction, quota, billing, and privacy-safe telemetry.
- [x] Audit architecture for SDK leakage, duplicated state, stale selections, broad untyped failures, hidden mutable token ownership, and dead legacy models/dependencies.
- [x] Run unit/lint/build/instrumentation, key/fallback, signed reinstall, Room hash, backup digest, and imported-data UI checks on the exact local candidate. A final clean rerun remains part of the post-live-matrix gate.
- [x] Record unavoidable warnings with owner/reason/removal condition and fix all provisional code before passing.

**Gate:** Passed. Search and save work with precise, approximate, denied, and unavailable location; the exact selected place cannot race or save stale details; no production code parses formatted addresses; SDK calls use reviewed field lists and correct single-use session lifecycles; legacy Places REST/DTO/dependencies are gone; live authorization/cost/error behavior is documented; and Room v1/imported data remain unchanged.

### Phase 2 interim verification evidence — 2026-08-21

- Final clean `clean testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest connectedDebugAndroidTest`: **148 tasks successful**; 34/34 JVM tests, 12/12 API-37 device tests, zero failures, and zero lint errors.
- Direct API-37 emulator instrumentation passes 12/12 tests after the exact address-display and Places-boundary changes.
- Room remains version 1; exported schema SHA-256 remains `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230`.
- Same-signer `adb install -r` of the signed Phase 2 release succeeds, and imported `Root Down` plus Unicode `Café Élan ☕` remain visible afterward.
- Signed-release Autocomplete (New) now returns current exact/partial results, and selecting the Denver `Root Down` prediction successfully loads Essentials-only details plus the confirmation map. Save was deliberately not pressed, so the imported Room dataset was not mutated. The earlier Authorization response was transient Cloud configuration state and is now resolved.
- Controlled telemetry covered nine Autocomplete calls (eight successful and one offline Network failure), two successful Essentials Place Details calls, session starts/abandonment, under-250ms through 1–3s latency buckets, a live Authorization failure and recovery, and typed Quota mapping. Logs contained no record/request identifiers or values. Exact, partial, Unicode, duplicate-name, empty-result, denied/precise/approximate location, offline/recovery, details, save, update-preservation, and duplicate-save paths passed.
- The audit replaced the bare fused-location priority call with an explicit one-shot `CurrentLocationRequest`: high accuracy when available, a two-minute acceptable cache age, ten-second timeout, cancellable execution, and null as a normal un-biased fallback. Android's required combined fine/coarse runtime request now accepts approximate-only permission; live coarse-only search emitted `location_bias=true`.
- Rapid Save is now single-flight, preventing two inserts from double taps while preserving the database schema. A JVM regression proves one insert.
- Lint has four deliberate next-phase warnings: target API 36 while compiling 37, Gradle 9.5 while 9.7.1 is newer than AGP 9.3's supported pairing, legacy Maps SDK 18.1, and legacy Maps KTX 3.4. Their owner/removal condition is the official Maps Compose/target modernization phase; the former old Places SDK warning is gone.
- Exact signed release identity remains package `com.personal.favoriteplaces` 3/3.0.0 and signer SHA-256 `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. The configured non-placeholder key is present without being printed. Same-signer reinstall succeeds and preserves `Root Down` plus Unicode `Café Élan ☕`.
- Room remains version 1 with schema SHA-256 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230`; no destructive migration/reset API or production forced-null assertion exists.

## Phase 3 — official Maps Compose

**Status:** Complete — quality gate passed 2026-08-21.

**Scope:** Replace the copied Maps Compose implementation with the maintained library, migrate the Add confirmation and saved-place map, and make map destinations reconstructable from Room after rotation or process recreation. This phase does not redesign the product UI, add Nearby, change the Room schema, or opt into target API 37 behavior.

### Phase 3 implementation research — 2026-08-21

- Maps Compose **8.5.0** is the current stable release. It was published on 2026-08-17 while Phase 3 was being implemented; Maven Central metadata and Google's changelog confirm it as a release rather than the separate 9.0.0 release candidate. Its published graph uses Maps SDK 20/Maps KTX 6.2, Kotlin 2.4.10, and the June 2026 Compose BOM; this project already meets or exceeds those platform requirements.
- The official dependency is `com.google.maps.android:maps-compose:8.5.0`. It supplies the compatible Maps SDK/KTX graph, so the app must remove its direct Maps 18.1 and Maps KTX 3.4 dependencies instead of pinning conflicting older versions.
- Maps SDK 20 uses the upgraded renderer. Google documents an optional `org.apache.http.legacy` manifest declaration for compatibility with devices whose Google Play services still select the legacy renderer; add it as `required=false`.
- `CameraPositionState` is the camera source of truth. Camera updates that depend on laid-out map dimensions must wait for map load. Multi-marker content will use calculated bounds and padding; a single marker will use a stable close zoom; empty content will show an explicit non-map state.
- `rememberUpdatedMarkerState` is the maintained marker-state API. Saved markers will not be draggable because the old UI never persisted drag results and silently presenting an editable marker would be data loss/misrepresentation.
- The current singleton map cache cannot survive process death. Phase 3 therefore pulls the stable-route/cache item forward from Phase 4: a group map carries URI-encoded city plus color and reloads the exact Room query; a single map carries the favorite ID and reloads that record. Routes contain no user notes, addresses, coordinates, or API data.
- Maps Compose 8.3.1+ responds to system dark/light changes; 8.4.0 added Compose compatibility fixes, and 8.5.0 adds current marker/info-window crash fixes. Verification still covers light/dark rendering, rotation/recreation, denied location, empty/single/multiple markers, and signed-update data preservation.

Primary references: [Maps Compose changelog](https://github.com/googlemaps/android-maps-compose/blob/main/CHANGELOG.md), [official repository and setup](https://github.com/googlemaps/android-maps-compose), [8.5.0 Maven artifact](https://repo1.maven.org/maven2/com/google/maps/android/maps-compose/8.5.0/), [Maps SDK release notes](https://developers.google.com/maps/documentation/android-sdk/release-notes), [renderer requirements](https://developers.google.com/maps/documentation/android-sdk/renderer), and [Maps SDK configuration](https://developers.google.com/maps/documentation/android-sdk/config).

### Workstream 3.1 — maintained dependency and compatibility boundary

- [x] Add Maps Compose 8.5.0 and remove direct legacy Maps SDK/KTX pins.
- [x] Add the optional legacy-renderer compatibility declaration and verify the merged manifest contains one key declaration and no unintended permissions.
- [x] Keep Google/Maps SDK types inside map UI adapters; Room/domain/navigation models remain app-owned.

### Workstream 3.2 — process-safe map destinations

- [x] Replace transient singleton map-cache navigation with explicit group (`city`, `color`) and single-record (`favoriteId`) routes backed by existing Room queries.
- [x] Model loading, content, empty, and failure as immutable City Map UI state; reject invalid IDs/arguments and invalid coordinates without crashing.
- [x] Add route encoding/decoding and ViewModel tests for spaces, slashes, Unicode, negative colors, missing records, and recreation from `SavedStateHandle`.
- [x] Remove the obsolete cache repository/use cases/models/converters and Hilt bindings after all callers migrate.

### Workstream 3.3 — map parity and camera behavior

- [x] Migrate Add's selected-place confirmation to official `GoogleMap`, `CameraPositionState`, and `rememberUpdatedMarkerState` APIs.
- [x] Migrate saved-place maps with non-draggable titled markers, empty-state handling, single-marker zoom, and loaded-map bounds fitting for multiple markers.
- [x] Make map errors and unavailable coordinates visible and recoverable; do not require location permission for either saved-coordinate map.

### Workstream 3.4 — remove the source fork

- [x] Delete the copied `features/maps` tree and any dead custom map wrappers only after production imports use the official package.
- [x] Confirm no copied `com.google.maps.android.compose` implementation, deprecated fork API, or old Maps dependency remains.

### Phase 3 quality audit and gate

- [x] Audit map state/navigation for process lifetime assumptions, PII in routes/logs, invalid coordinates, non-persisted interactions, camera races, error handling, and accessibility semantics.
- [x] Run clean JVM tests, lint, debug/release builds, instrumentation, and light/dark plus denied-location device smoke tests for Add and City maps.
- [x] Install the exact signed release over the Phase 2 release and prove Room schema hash plus imported records are unchanged.
- [x] Record warnings with owner/removal condition and replace every provisional implementation before passing.

**Gate:** Passed. Both maps use the maintained dependency and work for empty, single, and multiple markers across rotation/process recreation, light/dark mode, and denied location; copied map source and transient cache navigation are gone; signed-update data and Room v1 remain unchanged.

### Phase 3 verification evidence — 2026-08-21

- The exact clean command `clean testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest connectedDebugAndroidTest --no-parallel` completed **148/148 tasks successfully**: 39/39 JVM tests, 15/15 API-37 device tests, zero test failures, and zero lint errors.
- Dependency resolution is Maps Compose 8.5.0 → Maps KTX 6.2.0 → Maps SDK 20.0.0. Direct Maps 18.1/KTX 3.4 pins, the 27-file copied map implementation, the unused map wrapper, and seven transient-cache model/repository/use-case files are gone.
- Merged debug/release manifests contain the Google key metadata once and the optional `org.apache.http.legacy` compatibility declaration; Maps contributes only its expected network/Wi-Fi state permissions.
- Device coverage includes a two-marker group with bounds fitting, a single-record map, activity recreation for both stable destinations, physical rotation, an actual process kill/task restore, empty/missing/invalid-coordinate states, and Unicode/reserved-character route round trips.
- With fine and coarse location denied, the signed release rendered both the saved map and a live Autocomplete (New) `Root Down` confirmation map. Save was not pressed, so live Room data was not mutated.
- A live light/dark audit found that a null map color scheme stays light; both maps now explicitly use `ComposeMapColorScheme.FOLLOW_SYSTEM`. Google map tiles and surrounding UI switch correctly, and each map exposes a useful accessibility description.
- The error audit added typed user-visible Room failure states and hardened privacy-safe diagnostics so a logging backend failure cannot interrupt the operation it is reporting. No route or diagnostic includes address, notes, coordinates, place ID, token, key, or API response data.
- Exact signed-release reinstall succeeded. `Favorites`, `Café Élan ☕`, and `Root Down` remained visible; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4` and Room v1 schema SHA-256 remains `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230`.
- Lint now has only two deliberate warnings: target API 36 while compiling 37 (behavior opt-in remains owned by a later researched release phase) and Gradle 9.5 while 9.7.1 is newer (9.5 remains AGP 9.3's documented compatible pairing). Both legacy Maps warnings are eliminated.

## Phase 4 — state and architecture cleanup

**Status:** Complete — engineering gate passed 2026-08-21.

**Scope:** Establish clear Room/data/domain/presentation boundaries, make every screen expose one immutable lifecycle-collected `StateFlow`, replace synchronous `SharedPreferences` with migrated Preferences DataStore, eliminate Favorites resubscription/group fan-out, and restore meaningful drafts/preferences after process death. This phase intentionally does not change the Room table, visual design, navigation structure beyond the Phase 3 map routes, place categories, Nearby behavior, or target API.

### Phase 4 implementation research — 2026-08-21

- Android's current UI-layer guidance recommends unidirectional data flow: a screen ViewModel owns one immutable UI-state snapshot, state flows down, events flow up, and Compose collects streams with lifecycle awareness. One-off navigation/snackbar effects remain separate from durable render state.
- Room is the single source of truth for saved places. An `@Entity` represents the table row, not the app's domain/UI model. Introduce a data-layer `FavoriteEntity` with the exact released table/column/nullability/primary-key contract and lossless mappers; the domain `Favorite` then has no Room or Compose/theme dependency. Room remains version 1 and the exported schema/hash must remain byte-for-byte unchanged.
- Preferences DataStore **1.2.1** is current. It provides asynchronous transactional Flow/update APIs and requires exactly one DataStore instance per file/process. Use the top-level `preferencesDataStore` delegate and a data-layer repository rather than letting ViewModels access `Context` or raw keys.
- Use `SharedPreferencesMigration` for the existing `app_preferences` file so `list_view` and any valid `favorite_order` survive. Keep the old SharedPreferences file in backup rules during the migration window and add the DataStore file explicitly; invalid legacy order text maps to the established default without crashing.
- `SavedStateHandle` is the appropriate backup for small UI values needed after system process death. Persist Add's query and Edit's primitive draft fields, not Place SDK sessions/results, database objects, notes in routes, or large collections. After Add recreation, the query remains but the billing session/selection intentionally resets and the user searches again.
- Favorites currently cancels and recreates its Room collection for each sort and duplicates list/card booleans plus derived grouping. Combine one Room stream, one persisted settings stream, and sort selection into one state pipeline; derive city/color groups once per emission. Remove unused DAO city/color fan-out methods and dead state/models.

Primary references: [Android UI layer and UDF](https://developer.android.com/topic/architecture/ui-layer), [guide to app architecture and single source of truth](https://developer.android.com/topic/architecture), [DataStore 1.2.1 and singleton rules](https://developer.android.com/topic/libraries/architecture/datastore), [SharedPreferencesMigration](https://developer.android.com/reference/androidx/datastore/migrations/SharedPreferencesMigration), [SavedStateHandle process restoration](https://developer.android.com/topic/libraries/architecture/viewmodel/viewmodel-savedstate), and [Room entities](https://developer.android.com/training/data-storage/room/defining-data).

### Workstream 4.1 — persistence/domain boundary without schema change

- [x] Add `FavoriteEntity` in the data layer with explicit `tableName = "Favorite"`, exact v1 fields, and lossless entity/domain mappers.
- [x] Make DAOs Room-entity-only and repositories domain-only; add mapping round-trip tests covering nulls, Unicode, colors, IDs, heart, rating, and coordinates.
- [x] Remove Room/Compose/theme imports and palette ownership from the domain model; move city/color grouping models to presentation and remove dead domain/state files.
- [x] Prove Room stays version 1 and the exported schema JSON/hash is unchanged after the class split.

### Workstream 4.2 — Preferences DataStore with legacy migration

- [x] Add Preferences DataStore 1.2.1 as a singleton data-layer store/repository and expose an immutable saved-screen settings flow.
- [x] Migrate `list_view` and valid `favorite_order` from `app_preferences` exactly once; handle corruption/I/O and invalid values with safe defaults plus privacy-safe diagnostics.
- [x] Persist view mode and ordering asynchronously; update backup/data-extraction rules to retain both the migration source and DataStore destination.
- [x] Add migration, default/invalid-value, update, singleton-injection, and recreation coverage; delete `PreferencesHelper` and its presentation-layer helper wiring.

### Workstream 4.3 — one immutable state per screen

- [x] Convert Add and Edit from multiple Compose `State` holders to private `MutableStateFlow` plus public `StateFlow.asStateFlow()`; keep Backup, City Map, and Favorites consistent.
- [x] Remove Compose/Maps SDK event types from ViewModels, use app-owned primitive/domain inputs, and collect every screen state with `collectAsStateWithLifecycle`.
- [x] Persist Add query and Edit primitive draft fields through `SavedStateHandle`; reload original records from Room and never serialize SDK sessions, entity objects, or sensitive record content into routes/logs.
- [x] Model loading, ready, missing, validation, saving, and failure explicitly enough that Save/Map cannot race an unloaded record; keep one-time effects screen-specific.

### Workstream 4.4 — reactive Favorites and dead-path removal

- [x] Replace cancel/recollect sorting with one combined Room/settings/order state pipeline and one derived city/color grouping pass.
- [x] Replace duplicated `isListView`/`isCardView` booleans with one view-mode value and remove unused map/order visibility state.
- [x] Remove unused DAO/repository/use-case city/color fan-out methods while retaining the Phase 3 exact group query.
- [x] Remove cross-ViewModel effect types, empty events, dead wrappers/helpers, forced-null assertions, duplicate heart state, and provisional code found by the audit.

### Phase 4 quality audit and gate

- [x] Add DAO/repository mapper, DataStore migration, and Add/Edit/Favorites/Map/Backup ViewModel tests for success, validation, persistence failures, and process-restored inputs.
- [x] Audit coroutine ownership, stream sharing, lost updates, mutable collection exposure, SDK/framework leakage, error/logging order, sensitive saved state, and dead code.
- [x] Run exact clean JVM/lint/debug/release/instrumentation plus activity recreation, draft restoration, and signed-update preservation checks.
- [x] Record every warning and prove entity separation/DataStore work changed no Room field, record, signer, key handling, or release identity.

**Gate:** All screens expose one immutable lifecycle-collected state stream; Room is the saved-place source of truth and retains the exact v1 schema; DataStore safely migrates existing preferences; Add/Edit/Favorites restore their intended small state after recreation; no refresh loop, grouped-query fan-out, cross-ViewModel event type, forced-null assertion, or persistence/UI model coupling remains; DAO/ViewModel behavior and signed-update preservation pass.

### Phase 4 verification evidence — 2026-08-21

- The exact clean command `clean testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest connectedDebugAndroidTest --no-parallel` completed **148/148 tasks successfully** with 48 JVM tests and 17 API-37 device tests.
- `FavoriteEntity` owns the unchanged Room contract, repositories map to the framework-free `Favorite` domain model, and mapping coverage preserves every nullable/non-null persisted field. Room remains version 1; schema SHA-256 remains `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230`.
- Preferences DataStore 1.2.1 has one application-scoped instance, migrates the exact legacy `app_preferences` keys, safely defaults malformed ordering, recovers I/O/corruption paths, and is included alongside the migration source in backup rules. An isolated device test proves migration and subsequent writes.
- Add restores only its query; Edit restores title/notes/color/rating while reloading identity/address/coordinates/heart from Room. Device activity recreation proves an unsaved Edit title survives without mutating Room. SDK sessions/results and records are not stored in routes or `SavedStateHandle`.
- Favorites holds one Room subscription across sort/view changes, derives sorting and city/color groups once, and follows repository truth for Favorite hearts. Redundant DAO streams, copied UI-domain grouping types, cross-screen events, duplicate optimistic heart state, dead wrappers, and all forced-null assertions are removed.
- Lint has zero errors and only the two already accepted toolchain warnings (target 36 while compiling 37; Gradle 9.5 while 9.7.1 is available). The JDK reports non-project future-runtime warnings for native access/final-field reflection; neither is an Android lint or app behavior failure.
- Exact signed-release reinstall succeeded and retained `Favorites`, `Café Élan ☕`, `Montréal`, `Denver`, and `Root Down`. Release signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`; package, version, API-key handling, and database identity are unchanged.

## Phase 5 — Material 3 product refresh

**Status:** Complete — product and engineering gate passed 2026-08-21.

**Scope:** Implement the approved soft-and-spacious mobile direction as a real Material 3 Android experience: a persistent Saved/Find/Nearby app shell, modern Saved cards and consolidated filters, a calm Find flow with explicit origin changing and map confirmation, a purpose-built Nearby list/map workflow, and a sparse Edit screen whose selected unlabeled color accents only its header and bottom navigation. Keep the database at version 1 in this phase; complete saved-place type filtering only after the explicit Phase 6 migration adds app-owned type data.

### Phase 5 implementation research — 2026-08-21

- Material 3 `NavigationBar` is the platform component for three to five equal destinations on compact screens. Saved, Find, and Nearby are three distinct primary destinations; settings/backup and edit/map remain secondary destinations. Bottom-destination navigation must use `launchSingleTop`, `restoreState`, and `popUpTo(...){ saveState = true }` so switching tabs does not accumulate duplicates or discard each tab's state.
- Use `ModalBottomSheet` for secondary filters and Change location. Remove a sheet from composition after hiding it, keep applied filter values in the owning ViewModel, and keep temporary edits local to the sheet until Apply. This prevents every choice from reshuffling content behind the modal.
- Keep touch targets at least 48dp and prefer Material components with built-in semantics. Unlabeled color swatches remain visually unnamed, but accessibility announces selection and position (for example, “Color 2 of 5”), never an invented meaning. Favorite remains an independent heart action with a clear selected state.
- Dynamic color is suitable for app chrome on Android 12+, with a deliberate teal fallback for older devices and dark mode. Saved-place colors are user data, not theme roles: render the exact ARGB as a narrow accent and marker while keeping text on neutral, contrast-safe Material surfaces.
- `searchNearby()` requires a circular location restriction and explicit fields; radius must be greater than zero and at most 50,000 meters, and result count is 1–20. Use 10 miles by default, clamp typed/stepped input to the SDK range, request only ID/display name/formatted address/location/primary type, cap at 20, support type inclusion, cancel stale searches best-effort, and show required attribution. Sorting by distance is local because the SDK's distance rank does not replace a stable app display order in every request configuration.
- Reuse the established Autocomplete (New) session boundary for Change location: typed location → prediction → details coordinates/display label. A raw map choice remains an app-owned coordinate plus optional reverse-geocoded label. Neither path derives coordinates or city by parsing display text.
- The checked-in demo remains the visual source of truth, with later user corrections taking precedence: title **Places**; compact default-teal header and persistent footer; cards use a narrow color edge, bold name, address, two or three note lines, per-place Map, and a separate heart without repeated city/type labels; filter controls live in one sheet; Find shows Current location under search with Change location; Nearby places List/Map first, stacked origin second, one text Filter action above count; Place Details remains sparse and uses the chosen color only in its compact header/Save footer.

Primary references: [Material 3 navigation bar](https://developer.android.com/develop/ui/compose/components/navigation-bar), [multiple back stacks](https://developer.android.com/guide/navigation/backstack/multi-back-stacks), [Material 3 bottom sheets](https://developer.android.com/develop/ui/compose/quick-guides/content/create-bottom-sheet), [Compose accessibility defaults](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), [Material 3 dynamic color](https://developer.android.com/develop/ui/compose/designsystems/material3), [Autocomplete (New)](https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete), and [Nearby Search (New)](https://developers.google.com/maps/documentation/places/android-sdk/nearby-search).

### Workstream 5.1 — theme and primary navigation shell

- [x] Define complete light/dark fallback color schemes, retain dynamic color, and establish shared spacing/shapes/components from the approved demo.
- [x] Add a single top-level scaffold with Saved, Find, and Nearby `NavigationBar` destinations and state-preserving navigation behavior.
- [x] Keep bottom navigation off Edit, maps, backup/settings, and modal flows; tint Edit's app bar and navigation region with the selected exact place color without making the full form colorful.
- [x] Add navigation semantics and tests for selection, back behavior, duplicate prevention, and recreation.

### Workstream 5.2 — Saved Places refresh

- [x] Replace the title with **Saved Places** and replace full-color cards with soft neutral cards plus a narrow exact-color edge, title, optional type placeholder/data, two-to-three-line notes, and an independently tappable Favorite heart.
- [x] Replace the dense inline sort controls and old Card/List split with one text Filter action opening a modal sheet for sort, City, unlabeled Color, Favorite, and Clear all. Preserve an explicit grouped/map entry without reintroducing a second competing home layout.
- [x] Make active filters visible in one concise summary row only after application; support empty results, Clear filters, loading, and Room failure states.
- [x] Keep delete/Undo, Edit, group map, Favorite, settings/backup, and exact stored colors accessible.

### Workstream 5.3 — Find and Change location

- [x] Recompose Find around a Material search field, stacked Current location label/value with a Change location text action, concise predictions, a map confirmation icon next to Save, required attribution, and explicit loading/empty/error states.
- [x] Add a reusable Change location bottom sheet with typed Autocomplete (New) and Choose on map paths; confirm before changing the origin and keep device location as the default.
- [x] Keep search manual for this phase unless measured UI behavior justifies debounce; preserve one billing session per typed search/selection and all Phase 2 field/cancellation/privacy contracts.
- [x] Ensure Save still writes the complete existing v1 record and duplicate/missing-city failures remain safe.

### Workstream 5.4 — Nearby discovery

- [x] Add a typed `NearbyPlacesRepository` boundary and Places SDK implementation with exact field mask, circular restriction, result cap, included type mapping, cancellation, attribution, safe failure categories, and privacy-safe request telemetry.
- [x] Add a Nearby ViewModel with one immutable state: List/Map mode, confirmed origin, 10-mile default editable radius, type selection, results, local distance order, loading/empty/error/retry, and saved-place membership.
- [x] Build Nearby with List/Map in the app bar, a compact stacked origin block, one text Filter action above result count, and a modal sheet containing radius stepper/input and place-type filters.
- [x] Allow a result to be saved through the same validated record construction path and show exact-color/Favorite accents only after it exists locally. Preserve filters when switching List/Map and across recreation.

### Workstream 5.5 — Edit, secondary screens, and quality gate

- [x] Restyle Edit as the approved sparse form: colored app bar and bottom action/navigation region only, unlabeled swatches, name, rating, address, notes, map, Save, loading/error states, and no invented color meanings.
- [x] Bring saved maps and Data & backup into the same app-bar/surface/error language without broadening their behavior.
- [x] Add compact-phone, portrait/landscape, large-font, dark/light/dynamic-color, denied-location, offline, empty, error, TalkBack semantics, and process-recreation coverage.
- [x] Audit Google request fields/counts/errors/cancellation/attribution/cost, navigation back stacks, state ownership, touch targets, contrast, sensitive logs/routes, and remove superseded UI code.
- [x] Run the full clean build/device/signed-update/schema/data-preservation gate and record evidence before planning Phase 6.

**Gate:** The approved Saved/Find/Nearby/Edit direction is functional on Android, all existing capabilities and records remain available, Nearby and Change location use reviewed native SDK paths, primary-tab and process state restore correctly, accessibility/dark/error states pass, no obsolete dense/full-color UI remains, and Room is still byte-identical version 1. Saved-place type filtering may be visibly marked unavailable until Phase 6, but the final modernization cannot finish until Phase 6 completes it.

**Phase 5 evidence and audit:** The clean 148-task gate passed unit tests, zero-error lint, debug/release/test APK assembly, and 18/18 API 37 device tests. A live Places SDK Nearby request returned 20 Denver results with the reviewed six-field mask, distance ranking, 10-mile radius, structured primary types, and no authorization error; no result was saved during the probe. The audit corrected a permission-aware navigation test, expanded every color control to at least 48dp, made all saved cities horizontally reachable, verified cancellation is rethrown before broad error handling, removed superseded UI/REST/map-source paths, and found no forced-null assertions or TODO/FIXME/HACK markers. The exact permanent signer reinstalled successfully and retained the two imported records (`Café Élan ☕` and `Root Down`) with their exact cyan/orange accents. Room remains version 1 and schema SHA-256 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230`.

## Phase 6 — saved-place type migration and filtering

**Status:** Complete — implementation, audit, and signed migration gate passed 2026-08-21.

**Scope:** Add one nullable, user-owned `placeType` field to saved places through an explicit Room 1→2 migration; preserve every version-1 value exactly; populate new Find/Nearby saves from already-returned Google type metadata without adding a higher-cost Place Details field; let the user review or edit the type; show it on neutral saved cards; and add Type/Not set to the consolidated Saved filter. Keep the arbitrary five colors unnamed and keep Favorite as an independent heart.

### Phase 6 implementation research — 2026-08-21

- Room's current migration guidance requires an incremental migration whenever the entity/table changes, registration with `addMigrations`, exported schema history in version control, and both schema validation and explicit data assertions. Destructive fallback permanently deletes user data and remains prohibited. A manual migration with the literal full SQL `ALTER TABLE Favorite ADD COLUMN placeType TEXT DEFAULT NULL` is intentionally used so the preservation behavior is obvious and reviewable.
- A single nullable `TEXT` column is sufficient. It is app/user-owned display data such as “Mexican restaurant” or “Brewery,” not a Google enum or invented fixed taxonomy. Existing rows migrate to null and appear as **Type not set** until either person edits them; the migration will not guess from titles, notes, colors, or addresses.
- Google can return many types, a primary type may be absent, and its supported list evolves. Find Autocomplete already returns a list of types with each prediction. Requesting `PRIMARY_TYPE` during the terminating Place Details call would raise that call to the Pro field tier (and may change session billing), so Find will select and humanize the first useful non-generic prediction type already received. Nearby is already a Pro search and already returns `PRIMARY_TYPE`, so it can use that value. Neither path parses formatted addresses or makes a second metadata request.
- Backup format advances from 1 to 2. New exports include nullable `placeType`; imports continue to validate and accept the exact old version-1 field set/digest by mapping the missing field to null. Version-specific exact-field validation prevents silent schema drift.
- Type filtering belongs in the existing Saved modal sheet. It lists all distinct nonblank stored types plus **Type not set**, applies only on **Apply**, appears in the active summary, and clears with the existing Clear action. It does not add chips or filtering controls to the main Saved screen.

Primary references: [Room database migrations and testing](https://developer.android.com/training/data-storage/room/migrating-db-versions), [Room `MigrationTestHelper`](https://developer.android.com/reference/androidx/room/testing/MigrationTestHelper), [Places type semantics](https://developers.google.com/maps/documentation/places/android-sdk/place-types), [Autocomplete prediction types](https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete), and [Place data fields and billing tiers](https://developers.google.com/maps/documentation/places/android-sdk/data-fields).

### Workstream 6.1 — explicit Room 1→2 migration

- [x] Add nullable `placeType` to persistence/domain models, bump only `FavoriteDatabase` to version 2, define literal `MIGRATION_1_2`, and register it in the sole production builder.
- [x] Export and review schema 2 while retaining schema 1 unchanged; keep all destructive migration APIs absent.
- [x] Use `MigrationTestHelper` against the production-shaped v1 fixture to validate schema 2, row count, the digest of all original columns, exact null/default edge cases, and `placeType IS NULL` on every migrated row.
- [x] Add an all-migrations database-open test so a future release cannot register only the newest hop.

### Workstream 6.2 — type ownership and Google mapping

- [x] Add a small deterministic formatter/selector for already-returned Google types that rejects generic address/establishment values, humanizes underscores without address regex, and returns null safely.
- [x] Carry prediction types through the selected-place boundary without requesting another Place Details field; save the derived display type from Find.
- [x] Save Nearby's already-returned primary type through the same formatter and allow null when Google supplies no useful type.
- [x] Add unit/request-contract tests proving no new Find field or extra request was added, mapping is deterministic, and missing/unknown values are safe.

### Workstream 6.3 — Edit, cards, and Saved filters

- [x] Add one optional sparse Edit field for place type and restore its unsaved draft; preserve identity/address/coordinates/color/heart exactly.
- [x] Keep stored type available in details and exact Saved filters without inventing a label for null records. A later approved card refinement intentionally removes repeated type/city labels from Saved cards while preserving the data and filters.
- [x] Extend the one Saved filter sheet/state pipeline with Type and **Type not set**, all distinct values reachable in a scrollable row, concise active summary, Apply/Clear behavior, and case-insensitive deterministic ordering.
- [x] Cover edit/save/recreation, exact filtering, null filtering, combination with city/color/Favorite, and no duplicate Room subscriptions.

### Workstream 6.4 — backup compatibility and final gate

- [x] Emit strict backup version 2 with `placeType`; continue accepting version 1 and prove its existing golden digest imports as null without rewriting the source file.
- [x] Prove v2 export/import preserves every field, type null/non-null values, deterministic digest, idempotence, and conflict rollback.
- [x] Audit migration registration, errors/cancellation/logging, type normalization, Google fields/cost, backup compatibility, accessibility/touch targets, dead code, and all database writes.
- [x] Run the complete clean JVM/lint/debug/release/device gate, then install the permanent-signed release over the preserved Room-v1 release data and verify automatic migration/readback of both imported records before and after setting one type.
- [x] Record schema-1/schema-2 hashes, migration/data digests, test totals, signer/readback evidence, and the final modernization audit. Do not finish the modernization goal with any destructive fallback, Firebase dependency, unplanned API request, or unresolved good-enough implementation.

**Gate:** A version-1 household database upgrades automatically to version 2 without changing any old value; old records remain readable and editable with type unset; new Find/Nearby records receive only reviewed already-available type metadata; users can edit and filter exact place types or Type not set; v1 and v2 backups are safe; all migrations and release reinstall pass; and the modern local-first Android app is functional with no Firebase.

**Phase 6 evidence and final audit:** The clean 148-task gate passed 58/58 JVM tests, zero-error debug lint, debug/release/test APK assembly, and 20/20 API 37 device tests. `MigrationTestHelper` preserved every v1 field and produced null `placeType` values; the production migration chain opened the earliest schema. Schema SHA-256 values are v1 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230` and v2 `224603f257c69b000b75c2e411d508da190dac325b6a4f30918b500834b491b2`. Backup v1 golden digest `2334a8715a4302b4b57eef47578eb2757713cda73fae166ce0a6d2f401cd4bfa` remains readable as null type; the representative v2 digest is `c565ee801b0ea67005f885d0dc942eee771feac7298d59e2d5c3a635b0253b13`. The signed release APK SHA-256 is `2cbaed34d815b4cf2e8addbd5981c73c4faa068c5c314005960d815e0a96fd60`, signed by certificate SHA-256 `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. An exact `adb install -r` retained the original first-install timestamp and both migrated records; `Café Élan ☕` retained null type and cyan accent, while `Root Down` retained its user-entered `Restaurant` type and orange accent. No migration/Room/fatal error appeared. The final source audit found no destructive database fallback, Firebase dependency, forced-null assertion, unfinished marker, ad-hoc printing, secret logging, extra Find details field, or duplicate production database builder. Broad coroutine handlers rethrow cancellation. The only build warning is Gradle's own native-access warning; application code is warning-clean.

## Phase 7 — saved-place details and focused editing

**Status:** Complete — implementation, audit, and signed Room-v2 preservation gate passed 2026-08-22.

**Scope:** Replace the current card-to-full-form jump with a calm saved-place details screen. Lead with a clear bold name, followed by smaller optional place type, address/map action, rating, and a notes-first body. Keep content neutral but carry the selected saved-place color through a compact details header and Save footer. Keep the color control in the lower Notes action row for one-handed reach, and keep rating plus the independent Favorite heart as quick actions on the details screen. Edit name/type in a focused explicit-save modal sheet; edit notes directly in the remaining details canvas and commit them from the footer. Add Favorite access where saved places are already represented, without turning the heart into a second Save action for unsaved Google results. Preserve Room schema version 2 and every stored value.

### Phase 7 implementation research — 2026-08-22

- The current Saved card and grouped row navigate directly to `EditFavoriteScreen`; there is no details route. Card View already exposes the Favorite heart, while grouped rows do not. The saved map uses standard marker info windows without an app-owned selected-place sheet. Nearby tracks only a set of saved place IDs and therefore can show **Saved** but cannot identify the local record or its Favorite state.
- A details destination should receive only `favoriteId`, then observe that row from Room. This keeps navigation stable across process recreation and ensures card, detail, map, and quick-action state all follow the database rather than route copies. Deleting the record elsewhere should produce a safe missing-place state and return path.
- Material 3 `ModalBottomSheet` remains appropriate for the compact name/type form. Notes instead remain directly editable on the details canvas, retain an unsaved draft through recreation, occupy all space left after the compact metadata, and expose an explicit Save in the colored footer. This removes an unnecessary transition for the app's primary writing surface.
- Color, rating, and Favorite are single-purpose controls and may update from the details screen. Use field-specific repository/DAO mutations rather than rewriting a stale full record; serialize duplicate taps, expose saving/error state, and reconcile UI from the Room stream. A failed mutation must retain the old database value and present a concise retryable message.
- Every interactive control needs a minimum 48dp target and meaningful state semantics. The five colors remain announced only by stable position; rating reports its selected value; the Favorite heart reports Add/Remove Favorite. Larger text must reflow without hiding the address, notes, sheet Save action, or back navigation.
- For one-handed use, the details screen keeps the bold place name in the content area and places the selected color swatch plus down-caret at the far right of the Notes heading. Tapping the complete 48dp control expands a very slim white Apple-style picker around that exact swatch position: the closed swatch and open focused circle use the same rendering and screen center, so the color appears stationary while previous/next choices expand around it. Neighboring choices fade, the focused choice snaps into a faint center band without gaining a separate selection ring, and a compact **Done** action at the bottom commits one field-specific write. The panel adds almost no horizontal padding and uses only a faint border and soft shadow. Keeping the name out of the top app bar gives long names and large fonts room to wrap.

Primary references: [Compose Material 3 bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets), [partial and expandable bottom sheets](https://developer.android.com/develop/ui/compose/components/bottom-sheets-partial), [Compose accessibility API defaults and 48dp targets](https://developer.android.com/develop/ui/compose/accessibility/api-defaults), and [Compose semantics](https://developer.android.com/develop/ui/compose/accessibility/semantics).

### Workstream 7.1 — reactive details boundary and navigation

- [x] Add a typed `PlaceDetails` route carrying only `favoriteId`; Saved cards and grouped rows open it instead of Edit.
- [x] Add `observeFavoriteById(id)` through DAO and repository boundaries while retaining snapshot access where appropriate; use one immutable lifecycle-collected details state.
- [x] Handle loading, missing/deleted record, Room read failure, recreation, and back navigation without passing names, notes, coordinates, color, or other record content through the route.
- [x] Remove the old color route argument and retire the standalone full-form Edit destination only after behavior parity and navigation tests pass.

### Workstream 7.2 — calm details hierarchy

- [x] Build a details screen with a compact selected-color header and matching Save footer around neutral content ordered as: large bold name; smaller optional type; complete wrapping address with adjacent map action; rating; then a directly editable notes field that fills the remainder, with the selected-color/down-caret control at the far right of its heading. Automatically choose readable bar content contrast for arbitrary stored colors.
- [x] Implement the color control as a slim rounded Apple-style picker wheel anchored around the trigger circle: the full swatch/caret control is at least 48dp; the closed and open focused swatches share the same size, styling, and screen center; vertically snapping choices loop without dead space; neighboring choices fade; bottom **Done** commits once; and TalkBack announces stable position and selection state without names or checkmarks.
- [x] Evaluate the original vertical menu and an expandable horizontal selector on the narrow API 37 device. The narrow static menu was visually rejected; the looping wheel stayed on-screen at the right edge and preserved one-handed reach, so neither duplicate alternative shipped.
- [x] Keep the independent Favorite heart in the details top app bar and provide one clearly labeled Edit action for name/type. Hide primary bottom navigation on this child destination.
- [x] Make empty type and notes states quiet and actionable without invented categories or placeholder content. Notes display preserves user line breaks and supports long, Unicode, and blank content.
- [x] Retain existing swipe deletion with Undo on Saved; retain exact color values, Favorite independence, and the stable single-place map route without adding a primary destructive detail action.

### Workstream 7.3 — focused editing and safe quick actions

- [x] Add a compact name/type sheet with prefilled values, title validation, type normalization, explicit Save, progress/error state, and dirty-dismiss confirmation.
- [x] Add an inline multiline notes editor that fills the remaining details space, preserves its draft through recreation and failed writes, and commits only through the lower-right Save button inside the selected-color footer.
- [x] Allow color and rating changes directly on details and Favorite toggling from the header. Implement field-specific updates so one quick action cannot overwrite a newer note, name, type, heart, rating, or color.
- [x] Reconcile every mutation from Room truth, prevent duplicate submissions, preserve coroutine cancellation, log only privacy-safe operation context, and offer retry without logging record content.

### Workstream 7.4 — consistent Favorite affordances

- [x] Retain the existing heart on Saved cards and add the same stateful heart to grouped Saved rows, with no conflict between row-open and heart touch targets.
- [x] Add a selected-place sheet to saved maps with name/type, Favorite heart, and **View place** action; marker selection alone does not mutate data.
- [x] Change Nearby saved-state tracking from a set of Google place IDs to a minimal local ID/Favorite projection. Once a result is saved, show a heart that updates that local record; unsaved Nearby results keep **Save**.
- [x] Keep Find results as explicit **Save** only. A heart never implicitly creates a record or blurs the difference between saving and favoriting.

### Workstream 7.5 — regression, audit, and gate

- [x] Unit-test reactive details loading/missing/error states, field-specific mutation isolation, duplicate-action suppression, failure recovery, draft restoration, and normalization.
- [x] Device-test card/group/map/details navigation; layout order; color dropdown positioning/dismissal and one-handed changes; the name/type sheet; inline notes editing and footer Save; dirty draft recreation; heart/color/rating updates; map return; delete/Undo; and accessibility semantics/touch targets.
- [x] Prove every pre-Phase-7 workflow remains available: 63/63 JVM tests, 24/24 API 37 device tests, zero-error lint, and the signed release install over the preserved v2 database passed.
- [x] Audit errors, cancellation, privacy-safe logging, stale-record races, touch targets, semantics, keyboard/insets, dead Edit code, route contents, and all Room writes. Field-specific SQL replaced stale full-record editing; the obsolete Edit route/model/composables/tests were removed after parity.
- [x] Verify Room remains version 2 and both exported schema hashes remain byte-for-byte unchanged; no migration, Firebase, extra Google field, or new Google request was added.

**Gate:** Every saved-place representation opens one Room-backed details destination; the screen follows the approved information order and prioritizes a directly editable notes canvas; the compact header and Save footer use the selected color; the one-handed color wheel opens from the far right of the Notes heading; name/type edits safely in a focused sheet; notes save only from the footer; color, rating, and Favorite quick actions never overwrite unrelated fields; Favorite affordances are consistent for saved records without becoming implicit Save controls; accessibility, recreation, errors, signed reinstall, and exact Room-v2 preservation all pass.

**Phase 7 evidence and audit:** The final clean 148-task gate passed 63/63 JVM tests, zero-error debug lint, debug/release/test APK assembly, and 24/24 API 37 device tests; the separate legacy migration tools passed 18/18 tests. Tests cover exact partial-update isolation, missing rows, reactive deletion, validation/normalization, multiline Unicode notes, inline draft restoration, preservation across unrelated Room emissions, failed-save retry state, footer Save enablement, duplicate quick-action suppression, write failure rollback, details/map route identity and recreation, grouped hearts, the selected-color details bars, and the color picker. Live inspection on the signed 412dp-class emulator confirmed the Saved screen's compact default-teal header and persistent default-teal navigation footer. Both Saved layouts now use consistent neutral cards with the exact saved-color rail, bold name, complete address, two or three truncated note lines, and separate per-place Map and heart actions; repeated city and type labels are absent. Detail inspection confirmed the selected saved-place color in a compact header and 64dp footer, readable controls, a directly editable notes field filling all remaining content space, and a lower-right footer Save that does not overlap the canvas. It also confirmed the far-right Notes swatch, very slim white rounded looping wheel, three visible snapping choices, faint center focus band, faded neighbors, no separate selection ring, bottom **Done**, and no checkmark or color name. The panel is only slightly wider than its circles and **Done** action, with minimal padding, a faint boundary, and a soft shadow. Closed/open screenshots confirm the focused swatch retains the same size, styling, horizontal center, and vertical center when the wheel expands, creating the requested stationary-color transition. The implementation initially exposed a lazy-layout intrinsic-measurement crash during live popup testing; an exact-size wheel contract fixed it, and subsequent live plus automated runs produced no app fatal error. The final permanent-signed `com.personal.favoriteplaces` version 3.0.0 APK reinstalled successfully with its original first-install timestamp (`2026-08-21 13:47:21`) and preserved Room-v2 release data. An earlier empty-screen observation was the separately installed legacy package `com.example.favoriteplaces`, not the modern build. The final release APK SHA-256 is `fbf115c4fe2a306d747c97357875c703fbe722c2d5cd28e062d43ad11d53a79d`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Room remains version 2; schema hashes remain v1 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230` and v2 `224603f257c69b000b75c2e411d508da190dac325b6a4f30918b500834b491b2`. No Room, migration, SQLite, or fatal error appeared in the final run. The final source audit found no destructive database call, forced-null assertion, Firebase, ad-hoc output, unfinished marker, sensitive logging, obsolete Edit path, new Google field, or new Google request. Coroutine cancellation is preserved before broad failure handling, and all user mutations converge on Room truth.

## Phase 8 — personal appearance defaults and focused directions map

**Status:** Complete.

**Scope:** Add a real Settings destination with independent app-chrome and new-card default colors, reuse the approved unlabeled wheel selector for both, add the requested darker blue choice, and make the established light blue the fresh-install default. Existing saved colors remain exact and unchanged. Simplify the single-place map to a compact place-name header, no primary footer, and an explicit handoff to Google Maps directions.

### Phase 8 implementation research — 2026-08-22

- These values are preferences, not saved-place fields. Store them in the existing singleton Preferences DataStore; do not change Room or rewrite existing records. Add/Find and Nearby read the current default only when creating a new record.
- The shared palette remains unlabeled in the UI because colors have user-defined meaning. The existing light blue remains the default card color and becomes the default app color on installations with no stored preference; a darker blue is added as another exact ARGB choice.
- Google currently recommends Maps URLs for broad directions handoff and documents Android `ACTION_VIEW` map/navigation intents. Use an encoded HTTPS Maps directions URL with coordinates and `api=1`, resolve the intent before launching, and show a privacy-safe error if no handler exists. This requires no new Google API request, field, key, SDK, or Room data.

Primary references: [Google Maps URLs](https://developers.google.com/maps/documentation/urls/get-started), [Google Maps Android intents](https://developers.google.com/maps/documentation/android-sdk/intents), and [Android common map intents](https://developer.android.com/guide/components/intents-common).

### Workstream 8.1 — reusable appearance preferences

- [x] Extend the Preferences DataStore contract with validated app-color and new-card-color integers, safe defaults, independent writes, error logging, and migration/default tests.
- [x] Extract the approved slim looping color wheel into one reusable composable without introducing color names, checkmarks, or duplicate picker behavior.
- [x] Add a compact Settings screen reached from the Saved gear, with **App color**, **Default color for new places**, and the existing **Data & backup** destination.
- [x] Apply the app color reactively to primary headers/footer and theme roles with readable contrast; retain each saved place's own color on its details header/footer.
- [x] Apply the default-card preference only to newly created Find and Nearby records. Never recolor an existing row.

### Workstream 8.2 — focused single-place map

- [x] Remove excess single-map chrome: compact header, place name as title, and no primary bottom footer.
- [x] Add an accessible directions action that opens Google Maps/another capable handler at the saved coordinates and reports an unavailable-handler failure without crashing.
- [x] Retain stable ID-only navigation, Room observation, map marker, recreation, invalid-coordinate handling, and group-route compatibility until separately removed.

### Workstream 8.3 — regression, audit, and gate

- [x] Test DataStore defaults/writes, both independent colors, Find/Nearby creation defaults, reusable wheel semantics, map header, directions URL, footer absence, and activity recreation. Live launch additionally proved Google Maps receives the directions handoff.
- [x] Run clean JVM/lint/debug/release/device gates; audit preference and intent failures, cancellation, logs, accessibility, insets, and duplicate/dead picker code.
- [x] Reinstall the signed release over the existing package and verify saved Room-v2 records, schema hashes, first-install timestamp, and existing place colors remain unchanged.

**Gate:** Appearance choices persist independently and update the app without touching existing places; new Find/Nearby saves receive the selected default; the light blue is the safe fresh-install default and the darker blue is selectable; all picker instances share one production implementation; single-place maps show a compact place-name header, no app footer, and a working directions handoff; failure, accessibility, recreation, full build, and signed data-preservation checks pass with Room still at version 2.

**Phase 8 evidence and audit:** The clean 148-task gate passed 68/68 JVM tests, zero-error debug lint, debug/release/test APK assembly, and 24/24 API 37 device tests. Unit/device coverage proves fresh light-blue defaults, independent preference writes, transparent-value rejection, Find and Nearby default-color creation, URL encoding including optional Google place ID, Settings/backup navigation, reusable picker semantics, stable single-map recreation, place-name title, directions affordance, and primary-footer absence. Live 412dp inspection confirmed readable light-blue Saved/Find/Nearby chrome, a compact Settings layout, both six-choice unlabeled wheels, the added darker blue, and the compact map consuming the screen below its place-name header. The directions action successfully opened `com.google.android.apps.maps` for the saved destination. The signed `com.personal.favoriteplaces` 3.0.0 release reinstalled with first-install time preserved at `2026-08-21 13:47:21`; saved records remained visible with their prior exact accent colors. Room remains version 2 with unchanged schema hashes v1 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230` and v2 `224603f257c69b000b75c2e411d508da190dac325b6a4f30918b500834b491b2`. The final APK SHA-256 is `b1cb98e1e6306f773eb3836992743c3459d159267422bf7b6b3ca6a27327c4ef`. The source audit found one shared picker implementation, no destructive database operation, no Room/schema edit, no Firebase, no additional Google API field/request/key, and no sensitive intent or preference logging. Cancellation remains preserved before broad write handling; directions catches unavailable/security failures and reports a user-safe message.

## Phase 9 — Nearby saved collection first, discovery second

**Status:** Complete — implementation, audit, and signed Room-v2 preservation gate passed 2026-08-22.

**Product decision:** Proceed. Nearby should answer the household's first question—“which of our saved places should we go to from here?”—before offering Google discovery. Use one location-centered screen with **Our places** and **New places** sources, defaulting to Our places. This restores the legacy saved-map purpose while retaining the modern discovery work as an explicit secondary mode.

**Scope:** Rebuild the existing Google-only Nearby state into two source-specific pipelines sharing one confirmed origin and List/Map choice. Our places observes Room, shows the entire saved collection in List and every valid saved coordinate on Map by default, computes distance locally, filters locally, and never invokes Nearby Search. New places retains the reviewed Places SDK search, attribution, Save, exact field mask, and billing/error behavior. This phase does not change Room version 2, any saved value, backup format, Google field mask, API key, or package identity.

### Phase 9 implementation research — 2026-08-22

- The current `NearbyViewModel` always calls `NearbyPlacesRepository.search(...)` after origin/filter changes. Its `places` collection contains only Google `NearbyPlace` results; Room is observed only to decorate those results with local saved/heart membership. There is no saved-record distance projection or all-saved map source, which is the exact behavioral mismatch reported by the user.
- Material 3's `SingleChoiceSegmentedButtonRow` is the correct two-option source control. Source and List/Map are independent binary decisions, so they must not be flattened into four tabs or placed as two equal, visually competing controls in one row. Put the prominent **Our places / New places** pill inside the app-color header and keep List/Map as the quieter view control immediately below.
- Room already exposes the complete saved collection with coordinates, type, color, Favorite, rating, notes, and reactive updates. Our-places distance and radius filtering can be deterministic local projections; no schema, migration, geocoder, or Google request is needed.
- Google Nearby Search still requires an origin circle, explicit fields, and 1–20 results. It remains appropriate only for New places. Lazy entry plus a request key prevents source/view toggles from generating redundant billable calls.
- The user's restored requirement is **all saved places on the map**. Therefore Our places starts with Any distance, includes the current/chosen origin marker, and sorts its list by distance when available. A radius is an explicit filter, not a silent default. New places retains its independent 10-mile default.

Primary references: [Material 3 segmented buttons](https://developer.android.com/develop/ui/compose/components/segmented-button) and [Places SDK Nearby Search (New)](https://developers.google.com/maps/documentation/places/android-sdk/nearby-search).

### Workstream 9.1 — interaction contract and demo review

- [x] Update the checked-in mobile HTML demo with compact-phone Our places/New places and List/Map controls, the final one-row location treatment, adaptive distance/type versus saved filters, six unlabeled colors, and the app-color/white source-pill treatment. Production marker, location, empty, and error states use the same approved hierarchy.
- [x] Keep the approved final order: compact app-color header with source pill; one quieter row with List/Map at left and the clickable city/name or coordinate fallback at far right; count plus one Filter action; concise active-filter summary only when needed; content. The location control retains the typed-autocomplete/Choose-on-map sheet.
- [x] Confirm on the demo that **Our places** and **New places** remain the clearest labels. Do not substitute ambiguous terms such as Nearby/Explore without explicit user approval.

### Workstream 9.2 — source-specific state and saved-place projection

- [x] Add an explicit `NearbySource` with `OurPlaces` as the default. Restore a selected source through recreation while allowing a true cold/fresh navigation state to begin on Our places; preserve List/Map and each source's independent filters through `SavedStateHandle` without storing records or SDK sessions.
- [x] Replace the single mixed `places` field with clear source-specific immutable state. Observe Room once and project saved records into a Nearby presentation model containing stable local ID, exact coordinates/color/heart, text fields, and optional calculated distance.
- [x] Add one tested distance calculator and deterministic ordering: finite distances first, nearest first, then case-insensitive name and stable ID. Recalculate on origin or Room change without database fan-out.
- [x] Show all saved records by default. Keep invalid-coordinate records accessible in List with distance unavailable; omit only their markers and expose a concise map warning/count.
- [x] Guarantee that Our-places mode never calls `NearbyPlacesRepository`, never requires network/Google authorization after an origin is known, and remains useful without location by showing saved data without distances.

### Workstream 9.3 — adaptive source filters

- [x] Retain one Filter action and one modal surface, but render source-specific drafts and commit only on Apply. Dismissing never changes the current result set.
- [x] Our-places filters: Any distance by default or an explicit validated mile radius; exact stored place type including Type not set; six unlabeled color choices; independent Favorite-only; and optional minimum rating. Clear restores the full saved collection.
- [x] New-places filters: independent 10-mile default radius plus the established friendly Google category/type. Preserve the reviewed SDK radius bounds and do not expose saved-only color/Favorite/rating controls.
- [x] Preserve both filter sets while switching sources. Changing/clearing one source must not overwrite the other, and the result count/summary must identify the active source precisely.

### Workstream 9.4 — Nearby hierarchy, list, map, and decisions

- [x] Build the compact app-color header with only the accessible two-option source pill: current app color behind a transparent unselected half and a white selected half, without a redundant Nearby title. Keep the persistent bottom navigation because Nearby remains a primary destination.
- [x] Keep the shared Change location typed-autocomplete/map chooser behind the compact clickable city/name-or-coordinate control. Origin changes immediately update saved distances; New-place results become stale but refetch only while New places is active.
- [x] Our-places List reuses the modern Saved hierarchy, adding distance when available and opening Place Details by stable local ID. Retain the exact color rail, show type once, omit repeated city, keep notes and the separate heart, and do not add a second save action.
- [x] Our-places Map renders all valid saved markers in their stored color hue plus a visually distinct origin marker. Initial camera behavior makes the current area useful without deleting off-screen markers and **Fit all** reveals the entire filtered collection.
- [x] Selecting a saved marker opens a compact sheet with name, address, distance, heart, **View place**, and **Directions**. Reuse the Phase 8 Google Maps directions handoff and stable Room details route.
- [x] New-places List/Map retains name, type, address, distance, Google attribution, explicit Save, duplicate prevention, and selected-result confirmation. Saving updates Room and the result's saved/heart state without changing source or filters.

### Workstream 9.5 — lazy discovery and request discipline

- [x] Give New places a canonical request key derived only from confirmed origin, radius, and category. Search on first entry or when that key changes; reuse the current successful result across List/Map and source toggles.
- [x] Cancel stale searches best-effort, accept results only for the active request key, preserve cancellation before broad failures, and keep privacy-safe lifecycle/latency/SKU logging without coordinates, names, addresses, or keys.
- [x] Distinguish Room failures, location absence, network, authorization, quota, invalid request, zero results, and stale cached discovery. Our-places failures are never presented as Google/API failures.
- [x] Re-audit the exact existing Google six-field mask, 20-result limit, type mapping, attribution, and request count. This phase adds no Google field and reduces normal API use because Nearby starts locally.

### Workstream 9.6 — regression, audit, and preservation gate

- [x] Unit-test source default/restoration, source-specific state isolation, all-saved default, distance math/order/boundaries, invalid coordinates, filter composition, origin behavior, lazy request keys, stale-result rejection, duplicate suppression, and save/heart Room reconciliation.
- [x] Device/live-test all four source/view combinations; header pill semantics; location and filter sheets; counts/summaries; marker selection; details/directions/Save paths; 1.3× fonts; dark mode; missing-location Our places; cached New-place toggles; recreation; and primary-tab restoration. Existing typed failure tests cover New-place API errors without depending on an intentionally broken live key.
- [x] Audit accessibility, map camera behavior, marker identity/color, error handling/logging/cancellation, duplicated map/list composables, dead Google-only state, and any just-good-enough branching. Shared action/filter/location components are used where behavior is common while source rules remain explicit.
- [x] Run the full clean JVM/lint/debug/release/device gate, then reinstall the permanent-signed APK over the current package. Verify every Room-v2 record/value, both schema hashes, preferences, first-install timestamp, and backup compatibility remain unchanged.

**Gate:** Nearby cold-starts in Our places; all saved records are represented and every valid coordinate can be mapped without a Google Nearby request; current/chosen location supplies local distance and sorting without silently hiding distant saved places; the source pill cleanly enters lazy, cached New-place discovery; filters, counts, List/Map, marker actions, errors, recreation, and accessibility are source-correct; no new field/key/schema/API scope is introduced; and the signed Room-v2 preservation gate passes.

**Phase 9 evidence and final audit:** The exact clean 148-task gate passed 73/73 JVM tests, zero-error debug lint, debug/release/test APK assembly, and 25/25 API-37 device tests; the separate legacy migration tools remain 18/18. New coverage proves Our places is the cold default, saved records work without location or network, distances and deterministic ordering, invalid-coordinate list retention, composed local filters, source/filter/view restoration, lazy request caching, late stale-result rejection, duplicate-safe save, and reactive hearts. Live inspection covered Our/New × List/Map, the compact clickable coordinate fallback and retained autocomplete/map sheet, the adaptive filter sheet, saved marker selection with View/Directions, current-area camera plus Fit all, 1.3× font scale, and dark mode. One authorized first-entry New-places request returned 20 results with the unchanged six-field mask; List/Map and Our/New toggles produced zero duplicate requests. The final header contains only the app-color source pill with a white selected half, as approved.

## Phase 10 — customizable Google place-type wheel

**Status:** Complete — implementation, audit, and signed Room-v2 preservation gate passed 2026-08-22.

**Scope:** Replace the fixed New Places type chips with a calm centered word wheel modeled on the approved color picker. Keep a short user-controlled picker list, make every non-Any entry removable, and provide one focused manager—reachable from Nearby Add and Settings—that searches a local catalog of valid Google Nearby filter types. Persist only picker preferences in DataStore; do not change Room, saved records, or the database schema.

### Phase 10 implementation research — 2026-08-22

- Nearby Search accepts only Google Table A type identifiers as request filters; Table B values may be returned but are not valid Nearby filters. A general type such as `restaurant` can include more-specific restaurant results, so the fresh picker should begin with only a few broad, useful entries.
- Google Place Autocomplete predicts actual places, names, and addresses; it is not a type-catalog endpoint. The Add field should therefore search a reviewed local catalog immediately and make the user select a valid suggestion. Arbitrary phrases belong to a later, explicitly labeled Text Search feature rather than silently changing this filter's API behavior.
- Material filter/input chips are appropriate for compact selected values, while a modal bottom sheet is appropriate for secondary management. The primary filter remains quiet: distance, one five-row snapping word wheel, and Add. Type management replaces the sheet content temporarily so the full catalog never competes with the main filter.
- The word wheel should reuse the color picker's centered snapping model but widen it for labels and show five positions rather than three. The fixed center band is the selection; adjacent labels fade and scale subtly; there is no checkmark, dropdown caret, or separate selected chip inside the wheel.
- The picker always retains an implicit **Any type** entry. The initial configurable list is Restaurant, Coffee shop, Bar, and Bakery. Added entries append in deterministic order, every configured entry can be removed, and a conservative maximum prevents the quick picker itself from becoming another overwhelming catalog.
- Picker customization is device-local presentation state. Store ordered Google type IDs in the existing Preferences DataStore, validate them against the catalog on every read/write, preserve an intentionally empty configured list, and recover safely from stale type IDs after future Google catalog changes. No Room migration is permitted.

Primary references: [Google Place Types (New)](https://developers.google.com/maps/documentation/places/android-sdk/place-types), [Nearby Search type restrictions](https://developers.google.com/maps/documentation/places/android-sdk/nearby-search), [Place Autocomplete behavior](https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete), [Text Search arbitrary queries](https://developers.google.com/maps/documentation/places/android-sdk/text-search), [Compose chips](https://developer.android.com/develop/ui/compose/components/chip), and [Compose modal bottom sheets](https://developer.android.com/develop/ui/compose/quick-guides/content/create-bottom-sheet).

### Workstream 10.1 — reviewed catalog and safe preferences

- [x] Add a deterministic Table-A catalog with stable Google IDs, friendly labels, common search aliases, and unit coverage for validity, uniqueness, matching, and legacy fixed-category restoration.
- [x] Persist an ordered, validated, bounded picker list in Preferences DataStore, including the distinction between a missing preference and an intentionally empty list.
- [x] Observe preference changes in Nearby state and recover a removed active filter to Any without stale requests or crashes.

### Workstream 10.2 — centered word wheel and focused management

- [x] Add a reusable five-position snapping word wheel with a fixed center selection band, faded neighbors, tap-to-center behavior, TalkBack selection semantics, and deterministic test tags.
- [x] Replace New Places type chips with the wheel and a compact Add action.
- [x] Add one focused management view shared by Nearby Add and Settings, with local type search, useful suggestions when empty, explicit Add/Remove actions, picker-limit feedback, and a clear return path.

### Workstream 10.3 — request integration, regression, and audit

- [x] Send exactly the selected validated Google type, or omit type filtering for Any, without adding fields or background Google requests.
- [x] Test picker persistence, add/remove behavior, active-filter fallback, request construction, state restoration, and catalog search aliases.
- [x] Run the full JVM/lint/build/device gate and audit preference-write failures, cancellation, privacy-safe logging, accessibility, keyboard/insets, recomposition/request duplication, database safety, and unchanged Room schema hashes.

**Gate:** New Places presents a compact five-position word wheel backed only by valid reviewed Google types; the same list is manageable from Nearby Add and Settings; users can add or remove picker entries without seeing the full catalog unless they ask for it; customization survives restart; Any remains reachable; no keystroke makes a Google request; the selected type produces exactly one reviewed Nearby filter; and Room version 2 plus every saved record remain untouched.

**Phase 10 evidence and final audit:** The final gate passed 82/82 JVM tests, 27/27 API-37 device tests, 18/18 legacy-migration tool tests, debug/release/test APK assembly, release lint vital, and debug lint with zero errors. The two remaining lint notices are toolchain advisories for target SDK 36 and Gradle 9.5.0, not application-code findings. Live emulator inspection covered the compact five-label wheel, fixed highlighted center, faded neighbors, tap/scroll selection, fully visible Apply/Clear actions, the shared Nearby/Settings manager, removable configured types, local search, common additions, keyboard entry, and return paths. Search typing makes no Google request; applying a choice sends one reviewed Table-A identifier, while Any omits `includedTypes`. A singleton update use case validates IDs and serializes mutations from both entry points so simultaneous add/remove operations cannot lose changes.

**Post-phase type-color audit — 2026-08-22:** Ordinary place-type metadata is neutral supporting text and no longer uses the app accent on details, saved-map sheets, Nearby saved cards, Google discovery cards, or shared action sheets. These paths now use one shared presentation component to prevent drift. The word wheel's center band is also neutral. Color remains only where it communicates a real user-defined place color or a standard selected/focused control state. Room, preferences, Google requests, and stored values are unchanged.

**Post-phase palette/rating refinement gate — 2026-08-22:** The shared unlabeled palette now contains seven stable choices with darker purple appended last, leaving all existing positions and values intact. Saved cards and grouped rows now show a compact clamped 0–5 number and single star; nonzero uses the established gold while zero/null is neutral. Rating, Favorite, and Map form one vertical trailing stack in that order. Grouped cards use thin neutral dividers inset from both ends between places, plus a tighter eight-dp gap between separate cards. Automated coverage verifies palette stability, both Settings pickers, nonzero card/list presentation, details-to-unrated persistence, the resulting zero card state, the vertical action order, and required grouped dividers. The current gate passed 83/83 JVM tests, 29/29 API-37 device tests, debug lint with zero errors, and signed release assembly. Live signed inspection confirmed the layout and all four existing records after an in-place reinstall.

The permanent-signed `com.personal.favoriteplaces` 3.0.0 release reinstalled successfully without uninstalling or clearing data. First-install time remains `2026-08-21 13:47:21`, and all four existing saved records remained visible after reinstall. Room remains version 2 with unchanged schema hashes v1 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230` and v2 `224603f257c69b000b75c2e411d508da190dac325b6a4f30918b500834b491b2`. After the palette, rating, divider, action-stack, and compact card-spacing refinements, the current release APK SHA-256 is `265132cd1084462a63822d4ca238eaa8be9bee6580d7ad59cca33cbaf5628577`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. No Room/schema edit, destructive fallback, Firebase dependency, additional Google field/API/key, keystroke network call, forced-null assertion, unfinished marker, ad-hoc output, sensitive logging, app fatal error, or migration failure was found. Preference writes validate IDs and report failures; mutation sequencing prevents lost updates; initial preference restoration gates discovery so stale restored choices cannot create duplicate requests; coroutine cancellation remains preserved before broad error handling.

### Post-modernization reliability audit — 2026-08-22

The cross-app error-handling, logging, and code-quality audit is complete. It replaced overwrite-prone application inserts with an atomic Room transaction plus typed duplicate failure, made deletes verify their affected row, repaired stale autocomplete/location sessions and Nearby cache ownership, centralized numerically safe distance calculation, made Nearby saves per-place, corrected Room-confirmed delete/Undo and failure-safe swipe behavior, added retryable Saved loading, hardened preference/backup/startup/directions/map fallbacks, removed test-only production code, and added a source-level privacy-safe logging guard. Room stayed at version 2 with both schema hashes unchanged.

The final gate passed 88/88 JVM tests, 31/31 API-37 device tests, 18/18 migration-tool tests, zero-error lint, debug/test/release assembly, and signed install-over-data. The replacement now targets API 37. First-install time remains `2026-08-21 13:47:21`, and the live Saved screen reports all 7 records after reinstall. Signer and Room schema hashes remain unchanged. Full findings and deliberate boundaries are in [post-modernization-quality-audit.md](post-modernization-quality-audit.md).

## Phase 11 — Google listing refresh and phone preservation

**Status:** In progress — implementation complete; migration, live-data, signed-install, and final audit gates remain.

**Scope:** Rename the home heading to **Places** and let a user explicitly check a saved Google listing for changed address, map coordinates, city, and phone. Nothing is updated until the user reviews and confirms the differences. Add nullable phone storage through an explicit Room 2→3 migration and backup-format v3; never recreate, clear, or destructively migrate the database.

### Phase 11 implementation research — 2026-08-22

- Place Details (New) uses the saved Place ID plus an explicit field list. The refresh requests only `ID`, `FORMATTED_ADDRESS`, `LOCATION`, `ADDRESS_COMPONENTS`, and `NATIONAL_PHONE_NUMBER`; it does not request Google name, type, rating, reviews, or other unused data.
- Address, location, and address components are Essentials fields. National phone is an Enterprise field, so refresh is a deliberate Edit-screen action only—never startup, background, card rendering, or keystroke behavior.
- Google can return a current canonical Place ID. It is reviewed as part of the same update and stored only on confirmation so future refreshes continue to target the current listing.
- A missing phone in a Google response does not erase an existing phone. Missing/invalid address, location, or structured city makes the response incomplete and leaves the saved place unchanged.
- User-owned values—name, custom type, notes, color, personal rating, and Favorite heart—are outside the refresh write statement and therefore cannot be overwritten by Google.
- Room schema changes require an explicit migration and exported schema verification. Version 3 adds only nullable `phoneNumber`; the 2→3 migration must preserve every released v2 value, while the full 1→2→3 chain must remain valid.
- Backup v3 includes phone. Versions 1 and 2 remain readable. Import may fill only a currently null phone when all other fields match; an older backup cannot erase a phone, and different non-null phones remain a conflict with transaction rollback.

Primary references: [Place Details (New)](https://developers.google.com/maps/documentation/places/android-sdk/details-place), [Place data fields and billing tiers](https://developers.google.com/maps/documentation/places/android-sdk/data-fields), [Place IDs](https://developers.google.com/maps/documentation/places/android-sdk/place-id), and [Room database migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions).

### Workstream 11.1 — schema and portable data

- [x] Rename the home heading from Saved Places to Places while retaining Saved as the bottom-navigation destination label.
- [x] Add nullable phone to the domain/entity mapping and Room v3 with the sole SQL change `ALTER TABLE Favorite ADD COLUMN phoneNumber TEXT DEFAULT NULL`.
- [x] Add a 2→3 preservation test and retain the production 1→2→3 chain; do not alter schema-v1 or schema-v2 files/hashes.
- [x] Add deterministic backup v3 phone support, strict v1/v2/v3 decoding, safe missing-phone enrichment, non-erasure behavior, and transactional conflicts.

### Workstream 11.2 — explicit Google review

- [x] Add a narrowly scoped `SavedPlaceRefreshRepository` backed by `PlacesClient.fetchPlace`, cancellation propagation, typed API failures, latency buckets, and privacy-safe logs containing no Place ID, name, address, phone, coordinates, or API key.
- [x] Put **Check Google for updates** inside Edit. Disable it when no Google ID exists, explain what it checks, and show progress without duplicate requests.
- [x] Compare the response to current Room truth and show only actual changes. Require **Update** confirmation; **Keep saved details**, dismiss, errors, incomplete results, and no-change results perform no database write.
- [x] Apply Google-managed fields in one targeted Room statement and keep all user-owned columns outside that statement. Show a phone on Place Details only when stored.

### Workstream 11.3 — release and preservation gate

- [x] Pass JVM, migration-tool, lint, debug/release/test assembly, and API-37 instrumentation gates including review/preservation/error paths.
- [x] Create a verified private v3 backup from Craig's reviewed 26-place migration evidence so the 26 captured phone numbers can safely fill null phone columns after the schema migration.
- [ ] Install the permanent-signed 3.1.0 APK over the existing replacement package without uninstall/clear; verify first-install timestamp, signer, 26-record count, every preexisting field, Room v3, and the old app still installed.
- [ ] Import the private v3 enrichment backup, export again, and prove only missing phone values changed. Exercise one live Google refresh review without accepting an unintended business move.
- [ ] Re-audit exception boundaries, cancellation, logs, field masks/SKU, UI density/accessibility, backup downgrade behavior, transaction atomicity, and any just-good-enough implementation; record final evidence and close the phase.

**Gate:** The heading reads Places; every existing row migrates in place; v1/v2 backups remain safe; v3 preserves phones; Google refresh happens only on an explicit Edit action; every difference is reviewed; cancellation/errors/dismissal change nothing; confirmation can update only canonical Google ID, address/city/coordinates, and phone; all user-owned fields remain exact; and the signed install-over-26-record phone database passes a record-level preservation comparison.

**Phase 11 interim evidence — 2026-08-22:** 96/96 JVM tests, 33/33 API-37 instrumentation tests, and 23/23 offline migration tests pass. Debug lint reports zero errors and one Gradle-version advisory; debug, Android-test, and permanent-signed release APKs assemble. The exact released Room-v2 emulator database migrated in place to v3 during a signed 3.0.0→3.1.0 install: first-install time stayed `2026-08-21 13:47:21`, all seven records remained visible, and the heading changed to Places. Schema-v1 and schema-v2 file SHA-256 values remain `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230` and `224603f257c69b000b75c2e411d508da190dac325b6a4f30918b500834b491b2`; new schema-v3 file SHA-256 is `2a2b2fa718800fc8e22084521ace079e132ffa91d4ee153f2e9adc7ce372967b`. The private 26-record v3 backup contains 26 nonblank phones, record digest `f1df51eaaf9a4b6ac1ad74ca9b4c040fd095b59ca1801d187cac1c4a3023099c`, and file SHA-256 `687eb0eaf65e1c7cfb986f7641b48a2fac2668818b447786e897cc14a6f8558d`; an actual app import verified Kotlin/Python codec compatibility and displayed the stored phone. One live Google Enterprise request succeeded in 1–3 seconds with the five-field mask and privacy-safe logs. Review dismissal made no write. Harmless `USA`/punctuation, phone-format, and sub-block coordinate differences are normalized so they do not create false update prompts. The current signed APK SHA-256 is `d2b6e2fcd8d6a13d3a9223a4be6e98802fba5cccde0e915759c434ab14b4885c`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. The remaining gate is the physical-phone install/enrichment/export comparison; ADB disconnected before that step.

## Phase 12 — compact call and reservation actions

**Status:** Implementation complete — code, migration, backup, UI, signed-emulator, and quality gates pass; physical-phone household rollout remains.

**Product decision:** Use the existing inset divider as a quiet disclosure control. Cards remain focused on scanning saved places; one centered chevron expands only the actions for that place, and at most one place is expanded. In the expanded state, the compact **Call · Reservation · Web** row sits above the inset divider and the chevron remains centered below it. Use the same treatment in grouped and single-card layouts. The chevron remains visible for older records whose new phone/category columns are still empty. Opening it makes one lazy, place-specific Google lookup only when metadata is missing, persists only missing phone/category values, and then reveals whichever data-gated actions are eligible. Call is shown only when a valid stored phone exists. Reservation is shown only when the durable Google primary category is restaurant-like; the user's editable type remains independent and never controls eligibility. Web is always available and opens an explicit Google web search using only the saved name and address.

### Phase 12 implementation research — 2026-08-22

- Android's safe phone handoff is `ACTION_DIAL` with a `tel:` URI. It lets the user review the number before calling and requires no direct-call permission. The app catches unavailable and blocked handlers and never logs the number. [Android common intents](https://developer.android.com/guide/components/intents-common#Phone)
- Android's standard browser handoff is an `ACTION_VIEW` intent with an HTTPS URI. Web opens an encoded Google search only after the user taps it, requests no additional Places field, changes no saved data, and handles unavailable/blocked browser launches with a generic message and privacy-safe log. [Android common web intents](https://developer.android.com/guide/components/intents-common#Browser)
- OpenTable's public search accepts an encoded `term` parameter. The app opens a prefilled HTTPS search and copies only the place name as a disclosed fallback; address, phone, notes, and coordinates never reach the clipboard. An external search does not claim the restaurant participates in OpenTable. [OpenTable populated search](https://www.opentable.com/s?covers=2&term=Root+Down), [Android clipboard](https://developer.android.com/develop/ui/views/touch-and-input/copy-paste)
- Google `PRIMARY_TYPE` is durable provider metadata, unlike the user-owned display type. It is stored separately and normalized. Restaurant eligibility accepts `restaurant`, specific `_restaurant` values, and the documented restaurant-family exceptions used by Google; cafes, bakeries, and arbitrary custom labels do not qualify.
- `NATIONAL_PHONE_NUMBER` and `PRIMARY_TYPE` are fetched only at user-committed boundaries: Find's selected Place Details request, New Places' explicit Save, Edit's explicit Google refresh, or the first explicit expansion of a saved card still missing action metadata. Startup, collapsed-card rendering, filtering, typing, and Nearby result rendering make no enrichment request. The card-specific lookup requests only those two fields and never loads every saved place.
- Room v4 adds only nullable `googlePrimaryType` with an explicit 3→4 `ALTER TABLE`; the production 1→2→3→4 chain retains all older rows. Backup v4 adds the same nullable field while exact v1/v2/v3 imports stay readable. Import can fill only missing phone/category metadata and remains additive, transactional, conflict-safe, and non-erasing.

### Workstream 12.1 — durable metadata and request discipline

- [x] Add nullable `googlePrimaryType` to domain/entity mappings and Room v4 using only `ALTER TABLE Favorite ADD COLUMN googlePrimaryType TEXT DEFAULT NULL`.
- [x] Add direct 3→4 and earliest-schema production-chain tests; retain all earlier schemas and prohibit destructive migration.
- [x] Advance backup to v4 with exact version-specific shapes/digests, v1/v2/v3 compatibility, safe missing-metadata enrichment, conflict rollback, and updated document-picker copy.
- [x] Persist phone plus Google primary type from a selected Find result; request both only in the terminating user selection.
- [x] On explicit New Places Save, make one details request for phone/type; save the place without them if enrichment fails. Do not enrich result lists.
- [x] Include Google type in explicit Edit refresh review and its targeted Google-owned write while preserving name, custom type, notes, color, rating, and Favorite.

### Workstream 12.2 — card disclosure and external handoffs

- [x] Add one reusable divider/chevron disclosure for grouped and single cards, with place-specific Show/Hide accessibility labels, stable test tags, inline animation, and one-expanded-place state.
- [x] Keep the collapsed row compact, retain the inset divider, and show only actions actually available for that record.
- [x] Normalize a stored phone to a `tel:` URI and open the system dialer with typed unavailable/security handling and privacy-safe generic logs.
- [x] Open an encoded OpenTable HTTPS search, copy only the restaurant name, announce the result, and handle browser/app and clipboard failures independently.
- [x] Add an always-available Web action beside Call and the shortened Reservation action; encode only the saved name/address into a Google HTTPS search and use the standard browser handoff without another Places API request or clipboard write.
- [x] Base reservation visibility exclusively on durable Google category, never the editable custom type.
- [x] Keep the disclosure visible on migrated records and lazily fetch only that opened place's missing phone/category metadata; persist through a missing-only Room update, show loading/failure/empty states, avoid duplicate requests in the screen session, and never bulk-refresh cards.

### Workstream 12.3 — household data, regression, and quality gate

- [x] Build a strict offline v3→v4 enrichment tool and tests. Generate Craig's private 26-record v4 backup with all 26 phones/categories; preserve the three reviewed moved-business records with null Place IDs so they cannot be silently relocated.
- [x] Unit-test eligibility, URI encoding/normalization, v1/v3 backup compatibility, metadata merging/conflicts, Google field masks, Nearby committed-save enrichment, and refresh preservation.
- [x] Device-test Room 3→4 plus the full chain, backup round-trip/enrichment, and collapsed/expanded card semantics in the full API-37 suite.
- [x] Run zero-error lint, debug/test/release assembly, privacy/error/cancellation/dead-code audits, and the permanent-signed emulator 3.1.0→3.2.0 migration with record/first-install verification.
- [ ] On the physical household phone, install without uninstall/clear, import the private v4 backup, export and compare all 26 records, and manually verify Dialer/OpenTable. Keep the old app installed until owner sign-off.
- [x] Consolidate Craig's proven record-recovery procedure into [a repeatable household migration runbook](household-phone-migration-runbook.md), including why private SQLite could not be pulled, guarded per-record capture, v2→v3→v4 generation, moved-business decisions, import/export digest proof, privacy, rollback, and a separate second-phone checklist.

**Gate:** Cards stay compact until a place-specific disclosure is opened; Call and reservation actions are correctly data-gated and failure-safe; no direct-call permission or sensitive logging exists; Google metadata is acquired only at reviewed user boundaries; every old database and backup version migrates without loss; the 26-place enrichment is reproducible; and signed install-over-data evidence passes before household release.

**Phase 12 implementation evidence and audit — 2026-08-22:** 102/102 JVM tests, 35/35 API-37 device tests, and 25/25 offline migration-tool tests pass. The direct Room 3→4 test proves every v3 value survives and the new category is null; the production builder opens the earliest schema through all migrations. Debug lint has zero errors and only the accepted Gradle 9.7.1 availability advisory; lint-vital and debug/test/permanent-signed release assembly pass. Static and manual audits found no destructive database path, Firebase, direct-call permission, sensitive action logging, unfinished marker, ad-hoc output, swallowed coroutine cancellation, or dead phone-only DAO path.

The private v4 file contains all 26 records, phones, and reviewed Google categories with record digest `fed5bf64002e5a04610d4e043def52e7a5f1f85cad60ba86a23d9945bb94d85c` and file SHA-256 `02a57da709485f316630a8d46ec1bff9137882e61a5010452ebe9de4937aa13f`. A real app import showed all 26 places and the expected action eligibility. Live compact-phone inspection confirmed the requested order—actions above the inset divider, chevron below—and one-expanded-card behavior. Call opened the system dialer with Paper Dosa's stored `(505) 930-5521` without placing a call or requesting permission. Make reservation opened OpenTable's populated Paper Dosa search; OpenTable correctly explained that this specific restaurant was not on its network, demonstrating that the app is a search handoff rather than a false availability promise.

The permanent-signed 3.2.0/version-code-5 APK installed over 3.1.0 without uninstall or clear. First-install time remained `2026-08-21 13:47:21`, all seven release-emulator records and visible notes remained, and no Room/migration/SQLite/app-fatal error appeared. Schema-v1/v2/v3 hashes remain unchanged; schema-v4 SHA-256 is `1914a968f42f58b969488db1cc4ff0fd490fcc297c5d15657ec8db8690bfdd00`. Final APK SHA-256 is `c92b6aad1817260ad7924f0c600b5f1f4b617b528f2ec02f7472d8ef62a98686`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Only the physical-phone install/import/export comparison and owner sign-off remain; the old app must stay installed until then.

**Phase 12 disclosure correction — 2026-08-22:** Release 3.2.1/version-code 6 fixes the migrated-record gap found during owner review. The caret no longer disappears merely because the newly added Room columns are null. Opening such a card now performs one on-demand Place Details request for exactly `NATIONAL_PHONE_NUMBER` and `PRIMARY_TYPE`, writes only previously missing metadata, and reuses the stored result; it never scans or loads all places. The request follows Google's field-mask guidance and limits this Enterprise-tier lookup to the two action fields: [Place Details (New)](https://developers.google.com/maps/documentation/places/android-sdk/details-place), [Place data fields](https://developers.google.com/maps/documentation/places/android-sdk/data-fields). The action row and caret were tightened from 34dp to 24dp disclosure height with smaller padding while retaining the requested action-above-divider/caret-below order and a full-width labeled control. The installed release-emulator database upgraded in place with its original first-install timestamp and seven records intact; a live missing-metadata expansion executed exactly one two-field request and surfaced the typed invalid-listing error without a crash or database change. The successful repository path has a regression test proving one fetch, missing-only persistence, and no second request; a separate field-contract test locks the request to those two fields. The corrected gate passes 104 JVM tests, 35 API-37 device tests, zero-error lint, lint-vital, and signed release assembly. Final 3.2.1 APK SHA-256 is `feb5ae78bb0a333a9ee73dc56ea17bb2d6653d6852395e63bf9cb833b32fe8d3`; signer SHA-256 remains unchanged.

**Phase 12 Web follow-up — 2026-08-22:** Release 3.2.2/version-code 7 adds the requested third bottom-card action. The live compact-phone row shows **Call · Reservation · Web** on one line with proportional widths and the unchanged divider/caret order. Web opens Chrome to a successful Google result for the exact imported place using an encoded saved name plus address; it makes no Places SDK request, requests no new API field or permission, writes nothing to the clipboard/database, and reports a missing/blocked handler without exposing the query in logs. URL construction is regression-tested for trimming, encoding, and empty input; device coverage locks the action's collapsed/expanded visibility. The final gate passes 105 JVM tests, 35 API-37 device tests, 25 migration-tool tests, zero-error lint, lint-vital, signed release assembly, and live Web handoff. Final 3.2.2 APK SHA-256 is `0bd996470bda8e5cdfea570ab3a3ca00dfed86fa0aa715acde2ffebe03821914`; signer SHA-256 remains unchanged.

**Phase 12 status-bar chrome follow-up — 2026-08-22:** Release 3.2.3/version-code 8 extends the selected app color through Android's transparent edge-to-edge status-bar inset, so the clock and system icons now visually join the compact app header instead of sitting on a white strip. This follows Android's current guidance to draw the app background behind the transparent status bar on API 35+ rather than trying to restore an opaque platform bar: [Compose edge-to-edge setup](https://developer.android.com/develop/ui/compose/system/setup-e2e), [Android system bars](https://developer.android.com/design/ui/mobile/guides/foundations/system-bars). The Compose content remains inset below the system UI, so no title or control is overlapped. Status icons use the same tested contrast decision as `onPrimary`: dark icons for light app colors and white icons for dark app colors. The launch theme supplies the fresh-install blue as the pre-Compose status color on platform versions that honor `statusBarColor`; after preferences load, the exact saved app color takes over. Live API-37 inspection confirmed cyan/dark-icon and dark-blue/white-icon states, plus the permanent release's purple/white-icon state. The same-signer 3.2.2→3.2.3 install preserved first-install time `2026-08-21 13:47:21` and all seven release-emulator records; no Room, migration, SQLite, or app-fatal error appeared. The final gate passes 106 JVM tests, 35 API-37 device tests, 25 migration-tool tests, zero-error lint, lint-vital, and permanent-signed release assembly. Final 3.2.3 APK SHA-256 is `bff6ab33f17af8a497b2db73281f4004dfb3b6f0ec22837cd82c492fba0f2cc5`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Room and backup remain version 4 with no schema or saved-data change.

**Phase 12 location-map interaction correction — 2026-08-22:** Release 3.2.4/version-code 9 removes the gesture conflict from both Find and Nearby Change location. Text entry and autocomplete remain in the compact modal sheet, but **Choose on map** now opens one shared fixed full-screen `Dialog` instead of embedding `GoogleMap` inside the sheet's vertically draggable surface. Android's `Dialog` API is intended for custom, explicitly sized complex input, while the Maps SDK expects its normal pan and zoom gestures to control the camera: [Compose dialogs](https://developer.android.com/develop/ui/compose/components/dialog), [Maps controls and gestures](https://developers.google.com/maps/documentation/android-sdk/controls), [Maps camera and view](https://developers.google.com/maps/documentation/android-sdk/views). The fixed picker has no drag handle or movable container. A stationary center pin identifies the selected point; users pan or pinch the light map and then tap **Use location**. **Search instead**, Close, and system Back return to the unchanged autocomplete sheet without changing the origin. The picker draws app color behind its edge-to-edge system bars, applies contrast-correct system icons, and stays light even when the phone uses night mode. A live long vertical swipe moved the map from Denver toward Highlands Ranch while the header, instructions, center pin, and footer remained fixed; confirming closed the picker and updated the origin. New device regression coverage performs a vertical map swipe and proves the fixed dialog remains visible, then proves return to text search. The final gate passes 106 JVM tests, 36 API-37 device tests, 25 migration-tool tests, zero-error lint, lint-vital, and permanent-signed release assembly. The same-signer 3.2.3→3.2.4 install retained first-install time `2026-08-21 13:47:21` and all seven release-emulator records with no Room, migration, SQLite, or app-fatal error. Final APK SHA-256 is `96df943c77caa5958c1a6b1fd317bdc5210cfb26d73605cbb9eecd7aaa1ef24d`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Room, backup, Google fields, API calls, permissions, and saved data are unchanged.

**Phase 12 tap-to-pin and zoom-controls correction — 2026-08-22:** Release 3.2.5/version-code 10 retains the shared fixed full-screen picker but supersedes 3.2.4's stationary center pin. The map now supports normal drag navigation and pinch zoom, restores the visible Google Maps +/− zoom controls, and drops or repositions a real marker only when the user taps an exact map point. **Use location** is disabled until that marker exists, preventing an unintentional camera position from being accepted. **Search instead**, Close, and system Back remain no-change exits. Manual API-37 verification proves the marker appears at the tapped point, the button changes from disabled to enabled, and the zoom controls remain visible. Device regression coverage locks the initial disabled state, gesture-safe fixed dialog, and safe return to autocomplete; Google Maps' native-view tap was verified manually because Compose's synthetic touch does not dispatch that event into the embedded map view. The final gate passes 106 JVM tests, 36 API-37 device tests, 25 migration-tool tests, zero-error debug lint, lint-vital, and permanent-signed release assembly. The same-signer 3.2.4→3.2.5 install retained first-install time `2026-08-21 13:47:21` and all seven release-emulator records. No Room, migration, SQLite, or app-fatal error appeared. Final APK SHA-256 is `3322a99559a55d2f864007f502c2ae710f5ffe81107ee4edef5292c51a498d7f`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Room, backup, Google fields, API calls, permissions, and saved data are unchanged.

**Phase 12 Nearby Fit-all default correction — 2026-08-22:** Release 3.2.6/version-code 11 keeps the original household-first meaning of **Our places** without guessing a default radius. The default list contains the complete saved collection and uses the selected/current origin only to calculate distances and sort nearest-first. Opening Map now automatically fits the camera to the origin plus every visible saved marker; **Fit all** repeats that operation after the user pans or zooms. Choosing **Within** in the Filter sheet is the explicit way to apply a 1–100-mile location boundary, while type, color, Favorite, and rating continue to narrow the same local projection without a Google request. Live API-37 inspection framed all seven release-emulator records from Denver through Colorado Springs to Santa Fe on first map open. Both Our places and New places maps are forced to the approved light map scheme, and the visible count now uses the correct singular/plural label. The final gate passes 106 JVM tests, 36 API-37 device tests, 25 migration-tool tests, zero-error debug lint, lint-vital, and permanent-signed release assembly. Reinstalling the final signed version retained first-install time `2026-08-21 13:47:21` and all seven saved records. Final APK SHA-256 is `4949fe6606f48fb1671756c4444e35f7b98cfd41e2e98dfc7b348d9a9002dbd7`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Room, backup, Google fields, API calls, permissions, and saved data are unchanged.

**Phase 12 Nearby location-filter-first correction — 2026-08-22:** Release 3.2.7/version-code 12 supersedes 3.2.6's worldwide default after the household collection scope was clarified. **Our places** now computes a local projection in a strict order: current/chosen origin → required distance boundary → optional saved-place filters → list/map rendering. The default boundary is 25 miles, chosen to cover a metro area without mixing worldwide travel records; the Filter sheet accepts 1–100 miles and no longer offers an unlimited-distance state. Without an origin, saved results stay withheld and the screen links directly to location selection. Map first-open framing and the renamed **Fit results** control receive only the already filtered projection plus the origin, so camera behavior can never reintroduce out-of-area records. Live API-37 inspection at Denver showed exactly one of seven release records within 25 miles and framed only its marker plus the origin; Colorado Springs and Santa Fe records were absent. Both Nearby maps retain the approved light scheme and the count correctly reads “1 saved place.” The final gate passes 106 JVM tests, 36 API-37 device tests, 25 migration-tool tests, zero-error debug lint, lint-vital, and permanent-signed release assembly. The same-signer 3.2.6→3.2.7 install retained first-install time `2026-08-21 13:47:21` and all seven stored records. Final APK SHA-256 is `9292422a796e1fa4990674720ad4a8316648063369e1ae4ad5b788e318345fea`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Room, backup, Google fields, API calls, permissions, and saved data are unchanged.

**Phase 12 Nearby card-accent correction — 2026-08-22:** Release 3.2.8/version-code 13 removes the Nearby list card's fixed 144dp accent child and uses the same measured-height drawing technique as the main Saved card. The 6dp place-color accent now spans the full rounded card height for short or wrapped addresses, zero to two note-preview lines, the distance row, and missing-location messaging, while preserving the existing 20dp content inset. A new API-37 regression asserts that the tagged accent layer and card have equal bounds; live inspection confirms the strip reaches the lower rounded edge beneath the distance row. The final gate passes 106 JVM tests, 36 API-37 device tests, 25 migration-tool tests, zero-error debug lint, lint-vital, and permanent-signed release assembly. The same-signer 3.2.7→3.2.8 install retained first-install time `2026-08-21 13:47:21` and all seven stored records. Final APK SHA-256 is `96de4b3131d15e0d008d1f85473424332a596466bf721f7e67404b1e32e15cfd`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Room, backup, Google fields, API calls, permissions, and saved data are unchanged.

**Phase 12 grouped-city title restoration — 2026-09-13:** Release 3.2.9/version-code 14 restores location context to the grouped Saved presentation without repeating the city on every place row. Each city/color group card begins with a bold, single-line city title above its places; long names truncate safely and blank legacy values render as **City not set**. The ordinary ungrouped Saved cards remain unchanged and continue to omit city. Device coverage verifies that the title is displayed above the grouped place content, that grouped map/navigation behavior still restores after recreation, and that the filter-sheet setup can reliably reach its Apply action at different viewport sizes. The final gate passes 106 JVM tests, 36 API-37 device tests, 25 migration-tool tests, zero-error debug lint, lint-vital, and permanent-signed release assembly. Live inspection shows **Denver** and **Colorado Springs** on the existing seven-record dataset. The same-signer 3.2.8→3.2.9 install retained first-install time `2026-08-21 13:47:21` and all seven stored records. Final APK SHA-256 is `13e54172ef8d04c49c31b64edc38597dc27665d7519a2b0ce231c2c1424fea6b`; signer SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Room, backup, Google fields, API calls, permissions, and saved data are unchanged.

**Phase 12 physical-owner rollout evidence — 2026-09-13:** The permanent-signed 3.2.9 APK installed over 3.2.8 on Craig's physical Galaxy S25+ (`SM-S936U`) with `adb install -r`; no uninstall or clear operation was used. A pre-install UI readback showed **34 places**. After the update, package `com.personal.favoriteplaces` reports version code 14/version 3.2.9, the same private data directory, and unchanged first-install time `2026-08-22 12:41:50`; UI readback still shows **34 places** and the new **Santa Fe** grouped-city heading. The installed artifact verifies with APK Signature Scheme v2 and signer SHA-256 `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. The owner's update path is therefore proven on physical hardware. Wife-device recovery/import, an independently stored encrypted signer bundle, and household sign-off remain operational gates.

**Phase 12 launcher-name cleanup — 2026-09-13:** Release 3.2.10/version-code 15 retires the temporary side-by-side migration label **Places New**. The replacement package and permanent signer remain unchanged, while the Android launcher, app drawer, and system-facing application label now read simply **Places**. Unit tests, zero-error debug lint, release vital lint, and permanent-signed release assembly pass. Direct APK inspection confirms package `com.personal.favoriteplaces`, version code 15/version 3.2.10, application label **Places**, APK Signature Scheme v2 verification, and unchanged signer SHA-256 `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`. Final APK SHA-256 is `033e50ac908c9a40bf1c066bb3a4b9b00e46a2c1b0cbac29fb975e41be86e071`. This is a resource-only product-name correction: Room, migrations, backups, saved records, API requests, permissions, and navigation are unchanged.

### Light-only release correction — 2026-08-22

The app now deliberately ignores the phone's night-mode setting. Compose always uses the approved light scheme, the Android launch window uses the same light canvas instead of an inherited dark-capable Activity theme, and the edge-to-edge status/navigation icon styles retain contrast against the light top canvas and colored footer. Obsolete dark palette code was removed. An API-37 emulator configured with system night mode enabled remained light after a same-signer reinstall; status icons were dark on the light canvas, the bottom gesture indicator remained light over the purple footer, and all 7 emulator records remained visible. JVM tests, zero-error debug lint, lint-vital, and signed release assembly pass. Room remains version 2 with unchanged schemas and records. The resulting signed APK SHA-256 is `5b12be8e35c6dcbe3d5046da562b8abf0fd283485df60ed748c9061a76c15ece`.

### Future visual-template brainstorm — deliberately outside Phase 7 implementation

These are prompts for a later demo/review, not additional Phase 7 scope:

- **Calm summary card (implemented baseline):** retain the narrow color rail, bold one-line name, complete address, two or three note lines, and separate Map/heart actions. City/type stay in filters and details instead of being repeated on the card.
- **Notes-forward card:** increase the note preview to three or four lines and reduce city/type prominence. Useful if personal context matters more than density, but it will show fewer places per screen.
- **Compact grouped row (implemented baseline):** the grouped presentation uses the same bold name, address, truncated notes, Map, and heart hierarchy as cards, with one compact city title at the top of each city/color group card rather than repeating city on every place row.
- **Quiet notes canvas (implemented baseline):** a directly editable multiline surface and empty-state prompt, with Save separated into the colored footer. Best for making notes feel like the main content without another navigation step.
- **Outlined notes panel:** a lightly bordered rounded region that gives long notes a clear boundary. Easier to recognize as a distinct section, but visually busier.
- **Journal-style notes:** dates, multiple entries, or pinned snippets. This could be valuable later but requires product decisions and likely a new versioned data model; it is not implied by the current single notes field.

Before choosing a future template, compare each option with short, long, multiline, Unicode, and empty notes at normal and large font sizes. Do not add photos, timestamps, structured note blocks, or another schema migration merely to make a mockup look richer.

### Deferred rich-notes workstream — research only, plan immediately before implementation

This is intentionally not scheduled as another implementation phase yet. The inline plain-text editor remains the production baseline.

- [ ] Prototype a full-screen editor opened from details while retaining the inline field for quick changes.
- [ ] Limit the first toolbar to **Bold** and **Bulleted list**; avoid headings, fonts, colors, tables, images, and other Word-like scope until the core editor is proven.
- [ ] Base cursor, selection, composition, and restoration on Compose `TextFieldState`; model formatting ranges explicitly and render them with `AnnotatedString`/bullet annotations.
- [ ] Decide the storage contract only after testing plain-text compatibility. Compare an escaped Markdown subset against an explicit versioned Room migration with a plain-text fallback; preserve every existing note byte-for-byte and update backup validation before shipping either choice.
- [ ] Test collapsed and ranged selections, toggling bold, starting/ending/splitting/merging bullet items, multiline paste, undo/redo expectations, TalkBack toolbar state, keyboard/insets, rotation/process restoration, write failure, export/import, and signed migration/readback.

Research basis: Android recommends state-based text fields for reliable text, selection, composition, and restoration. Compose provides `AnnotatedString` span styling and bullet-list annotations for rendering, but these APIs do not supply a complete Word-like editor or persistence model. This makes bold plus bullets a contained but still medium-sized future feature rather than a quick decoration. References: [Compose text input](https://developer.android.com/develop/ui/compose/text/user-input), [`AnnotatedString`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/AnnotatedString), and [`AnnotatedString.Builder` bullet APIs](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/AnnotatedString.Builder).

### Card action disclosure — superseded by Phase 12

This research snapshot is retained for decision history. The user explicitly opened implementation as Phase 12; the current checklist and evidence are authoritative above.

- [x] Turn the inset divider between grouped places into a quiet disclosure affordance: center a small downward chevron immediately below the line. Give the full control a place-specific TalkBack label; rotate it upward and change the label to **Hide actions** while expanded.
- [x] Expand one inline action row for the place directly above that divider, without expanding or recoloring the entire card. Keep only one grouped row open at a time and use the same treatment for single-place cards.
- [x] Show **Call** only when a nonblank stored phone exists and use Android's permission-free dialer handoff.
- [x] Show **Make reservation** only for a reliably restaurant-like durable Google category, independent from the user-owned display type.
- [x] Retrieve phone at user-committed Find selection/New Places Save boundaries rather than result rendering.
- [x] Open a prefilled encoded OpenTable search and copy only the restaurant name as an announced fallback.
- [x] Treat OpenTable as an external search and handle missing handlers without losing card state.
- [x] Cover grouped/single layouts, absent actions, accessibility semantics, external handoff construction, and the compact collapsed baseline through automated and live gates.

Product decision: the disclosure row is a good fit because it keeps infrequent actions out of the default scan path and gives the existing divider a purpose. A centered chevron is visually understandable only if its touch target and accessibility label identify which place it controls. The OpenTable search itself accepts a `term` query on its official search page, while OpenTable documents both web and Android-app reservation discovery; implementation must still test whether the installed app preserves that prefilled query. References: [OpenTable search with a populated term](https://www.opentable.com/s?covers=2&term=Root+Down) and [OpenTable app page](https://www.opentable.com/page.aspx?pageid=16).

## Rolling-plan history

| Date | Change |
|---|---|
| 2026-08-21 | Created the rolling plan with Phase 0 stabilization and database protection as the only detailed phase. |
| 2026-08-21 | Started Phase 0; added repository safeguards, established the repeatable JDK 17 build, exported Room schema v1, and added the production-shaped database contract fixture. |
| 2026-08-21 | Added permanent side-by-side application identity, versioned document-picker backup/import, explicit backup rules, transactional conflict-safe restore, stability fixes, and passing JVM/Android verification; recorded the interim Phase 0 quality audit. |
| 2026-08-21 | Added semantics-based Card/List/Edit/heart/backup UI regressions, upgraded test-only Espresso for API 37, moved geocoding to API-aware cancellable execution with structured city resolution, hardened logging/error paths, and reached 12/12 direct instrumentation tests. |
| 2026-08-21 | Added a strict non-overwriting offline legacy-capture converter with five Python tests and an app-codec golden test; documented primary-source implementation research and the remaining household rollout gates. |
| 2026-08-21 | Added external release-signing configuration and a recovery runbook, confirmed that no real Maps/Places key is present locally, corrected the production-key package guidance, removed remaining location/place debug logs and dead map state, and reverified the full build plus 12/12 instrumentation tests. |
| 2026-08-21 | Hardened legacy capture from filename-only evidence to field-level provenance plus verified in-folder XML/PNG artifacts, explicit owner review, capture/record digests, safe parsing, and 11 passing converter tests; documented the exact-versus-re-resolved preservation boundary. |
| 2026-08-21 | Added a non-overwriting ADB evidence helper that captures UI XML and PNG only from a visible legacy Edit screen into private outside-Git bundles; 18 total migration-tool tests pass and a live emulator negative probe refused the legacy home screen as designed. |
| 2026-08-21 | Added the standalone draft Phase 0 evidence report, consolidating verified invariants and explicitly withholding completion on the permanent signer, restricted native-SDK key, and both household migrations. |
| 2026-08-21 | Replaced the legacy Places REST/Retrofit stack with Places SDK 5.3.0 (New), app-owned typed models/session boundary, cancellable requests, Essentials-only details, structured city extraction, immutable Add state, safe missing-key behavior, and 26 JVM plus 12 device tests. Local build/schema/reinstall/data gates pass; live Cloud authorization and request/cost/error verification remain open. |
| 2026-08-21 | Closed Phase 2 after live API authorization, exact/partial/Unicode/no-result/offline/location/save/duplicate verification; added privacy-safe lifecycle/latency/SKU telemetry, typed failure tests, reliable cancellable fused location, proper approximate permission handling, single-flight save, compliant Google Maps plus third-party attribution, legal-page drafts, 34 JVM tests, 12 device tests, and an unchanged Room v1 schema/reinstall readback. |
| 2026-08-21 | Researched and detailed Phase 3 around official Maps Compose 8.5.0/Maps SDK 20, pulled stable Room-backed map routes forward to satisfy process recreation, and defined camera, compatibility, source-fork removal, privacy, and signed-update gates. |
| 2026-08-21 | Completed Phase 3 on Maps Compose 8.5.0/Maps SDK 20; removed the copied fork and transient cache, added Room-restorable group/single routes, robust map/camera/error/dark-mode behavior, 39 JVM plus 15 device tests, and exact signed-update/schema/data preservation evidence. |
| 2026-08-21 | Researched and detailed Phase 4 around immutable UDF state, Room entity/domain separation without a schema change, Preferences DataStore 1.2.1 with legacy migration, saved drafts, a single reactive Favorites pipeline, and full error/restoration/data-preservation gates. |
| 2026-08-21 | With user approval, created ignored owner-only PKCS#12 release signing material in the repository, verified refusal to replace it, passed a clean 138-task build, recorded the permanent SHA-1/SHA-256 fingerprints, installed beside legacy, matched the pulled APK byte-for-byte, and proved same-signer reinstall. |
| 2026-08-21 | Completed engineering Phase 0 after a key-configured map smoke, full side-by-side preservation proof, exact-candidate quality audit, and privacy-safe exception logging; moved household migration, signer recovery, and Cloud restrictions to mandatory release prerequisites. |
| 2026-08-21 | Researched and detailed Phase 1, added the preference for production-quality refactoring/restructuring, and completed Workstream 1.1 by centralizing every unchanged version in a catalog with full build, signing, key, schema, reinstall, and imported-data verification. |
| 2026-08-21 | Completed Phase 1 on AGP 9.3.1/Gradle 9.5, Kotlin 2.4.10, KSP 2.3.10, compile 37/target 36, stable Material 3 infrastructure, and 12/12 emulator tests; preserved the Room schema/imported data and detailed Phase 2 only after the quality gate passed. |
| 2026-08-21 | Clarified that the separate replacement database is not permanently locked to the legacy schema: refactoring is preferred, while imported and future records remain protected by explicit tested migrations. |
| 2026-08-21 | Completed Phase 4 with an unchanged Room v1 entity/domain split, migrated singleton Preferences DataStore, lifecycle-collected screen state, Add/Edit draft restoration, one reactive Favorites subscription, dead-state removal, 48 JVM plus 17 device tests, and signed-update data preservation; researched and detailed Phase 5 only after its gate passed. |
| 2026-08-21 | Completed Phase 5 with the approved soft-and-spacious Saved/Find/Nearby/Edit experience, native Nearby and Change-location flows, consolidated filters, independent Favorite hearts and exact color accents, a clean 148-task build, 18/18 device tests, live authorized Nearby evidence, and unchanged Room v1 data/schema; opened Phase 6 only after the signed-update gate passed. |
| 2026-08-21 | Completed Phase 6 and the modernization implementation with an explicit tested Room 1→2 migration, user-owned nullable place types, reviewed no-extra-cost Google type mapping, Edit/card/filter integration, strict v1/v2 backups, 58 JVM plus 20 device tests, a clean 148-task gate, and permanent-signed reinstall preservation. The app remains local-first with no Firebase. |
| 2026-08-22 | Planned Phase 7 without implementation: one Room-observed saved-place details route, notes-first hierarchy, focused name/type and notes sheets, isolated quick mutations, consistent Favorite affordances, unchanged Room v2, and a separate future card/notes template brainstorm. |
| 2026-08-22 | Refined the Phase 7 details hierarchy for one-handed color changes: keep the bold name in content, align the current swatch/down-caret at its far right, use a five-choice anchored dropdown as baseline, and retain an expandable horizontal selector only as a tested fallback. |
| 2026-08-22 | Completed Phase 7 with Room-observed details, a verified one-handed color dropdown, focused name/type and notes sheets, field-specific race-safe writes, consistent saved-place hearts, removal of the obsolete full Edit path, 61 JVM plus 24 device tests, a clean 148-task gate, and unchanged signed Room-v2 data/schema. |
| 2026-08-22 | Replaced the visually rejected narrow color menu with a slim looping Apple-style wheel, center snap band, faded neighbors, bottom one-time **Done**, and no labels/checkmark; moved its trigger to the far right of the Notes row and shortened the adjacent pencil action to **Edit**. The popup expands around a shared closed/open swatch center so the color appears stationary. Live crash discovery hardened the wheel's measurement contract, and the complete JVM/lint/release/device gate passed. |
| 2026-08-22 | Refined the color wheel into a very slim white panel with almost no side padding, a faint boundary, soft shadow, and no separate ring around the selected circle; retained the stationary closed/open swatch transition and center-band selection cue. |
| 2026-08-22 | Restored the selected saved-place color to the Place Details header and a slim matching footer after the neutral details refactor had dropped the previously approved accent treatment; retained neutral content and automatically contrasted bar controls. |
| 2026-08-22 | Made notes directly editable in the details canvas, let the field consume the remaining screen, moved explicit Save into the lower-right of the selected-color footer, removed the redundant notes sheet, and compacted the details header by eliminating its duplicated status-bar inset. Added a researched but unscheduled full-screen rich-notes direction limited initially to bold and bulleted lists. |
| 2026-08-22 | Refined Saved with the stable default teal in its compact header and persistent navigation footer. Both card and grouped layouts now omit repeated city/type labels and consistently show bold name, address, two or three truncated note lines, per-place Map, heart, and the narrow saved-color rail. |
| 2026-08-22 | Completed Phase 8 with independent persisted app/new-place colors, fresh light-blue defaults, a sixth darker-blue choice, one shared unlabeled wheel, a compact Settings/backup flow, Find/Nearby creation defaults, and a compact place-named map with no app footer and a verified Google Maps directions handoff; 68 JVM plus 24 device tests and the signed Room-v2 preservation gate passed. |
| 2026-08-22 | Planned Phase 9 after correcting the Nearby product model: default to all Room-saved **Our places** plus current/chosen location, place an **Our places / New places** segmented pill in the header, retain List/Map and one adaptive Filter sheet, make Google discovery lazy and secondary, and preserve Room v2 with no new API fields or requests in the saved mode. |
| 2026-08-22 | Completed Phase 9 and the approved modernization direction: Nearby now defaults to the local saved collection, shares a compact clickable location with List/Map, uses a title-free app-color/white source pill, provides adaptive local/Google filters, maps saved colors around the current area with Fit all and marker actions, and lazily caches Google discovery. The clean 148-task, 73-JVM, 25-device, 18-tool, live API, signed reinstall, and unchanged Room-v2 preservation gates passed. No Phase 10 is opened; the remaining card/rich-notes ideas stay explicitly deferred until selected. |
| 2026-08-22 | Audited every place-type presentation, centralized ordinary metadata on one neutral label, removed accent coloring from details/maps/Nearby/Google-result labels, and changed the type wheel to a neutral center band. Selection/focus color remains only on interactive controls; no data, schema, preference, or request behavior changed. |
| 2026-08-22 | Completed the post-modernization reliability audit: hardened atomic Room writes/deletes, autocomplete and location cancellation, Nearby caching and distance math, delete/Undo and swipe failure behavior, backup/preferences/startup/maps error paths, privacy-safe logging guards, target API 37, and retry states; passed 88 JVM, 31 device, and 18 migration-tool tests plus signed reinstall with all 7 live records and unchanged Room schemas. |
| 2026-08-22 | Locked the app to the approved light appearance regardless of system night mode, replaced the inherited dark-capable launch theme, corrected edge-to-edge system-bar contrast, removed dead dark-palette code, and produced a newly verified permanent-signed APK without changing Room or saved data. |
| 2026-08-22 | Completed Craig's first household technical migration: captured 26 legacy records without private-database access, upgraded the offline converter to backup v2 with cross-codec coverage, retained all 26 requested phone numbers in a separate future-use sidecar, preserved saved-address coordinates for 3 moved businesses, imported transactionally into the empty permanent-signed replacement, and proved exact record/digest equality by exporting the phone database back out. The legacy app remains installed pending owner sign-off. |
| 2026-08-22 | Appended an unlabeled darker-purple seventh choice to the shared color palette used by place accents, app chrome, and the default new-place color. Preserved all six existing ARGB values and option positions; no stored preference, saved place, or Room schema changed. |
| 2026-08-22 | Restored rating visibility to Saved card and grouped-list presentations as one compact 0–5 number plus star at the upper right above Map/Favorite. Nonzero ratings use the established gold star; zero uses a neutral star. The display is read-only, clamps malformed legacy values for presentation, and exposes one concise TalkBack rating description. |
| 2026-08-22 | Refined Saved cards and grouped rows to use a consistent rating → Favorite → Map vertical action stack. Added thin gray dividers inset from both ends only between places sharing a grouped card, and tightened separate grouped-card spacing from 12 dp to 8 dp without changing saved colors or data. |
| 2026-08-22 | Fine-tuned Saved card density by top-aligning the name/address/notes column and keeping notes beside the action stack instead of below it. Rating, Favorite, and Map use equal visual center spacing, while grouped rows add balanced padding above and below the stack so ratings and maps do not crowd dividers. |
| 2026-08-22 | Reduced excess card height by using a compact read-only rating slot, retaining full Material touch targets for Favorite and Map, trimming grouped-row/card padding, and allowing exactly three truncated note lines. The resulting rail is 132 dp rather than 144 dp, with an additional subtle 2-dp gap below grouped dividers so the following rating does not crowd the line. |
| 2026-08-22 | Added 2 dp between each Saved address and its note preview, then removed 4 dp from the card's bottom padding. This improves text separation while pulling the lower card edge upward consistently in grouped and individual-card views. |
| 2026-08-22 | Compacted the final trailing rail from 132 dp to 120 dp by removing the empty pre-heart gap while retaining adjacent full-size Material heart/map targets. Shifted only the heart glyph so the visible rating, heart, and map centers remain evenly spaced, trimmed another 4 dp from the lower card edge plus 2 dp from grouped-row vertical padding, and settled on 4 dp of breathing room immediately below grouped dividers. |
| 2026-08-22 | Recorded the approved future card-action disclosure: a centered chevron beneath inset dividers expands Call and restaurant-only Make reservation actions; phone is acquired only at user-committed Place Details/Save points, Call opens the dialer, and OpenTable should receive a prefilled encoded search with clipboard only as a tested fallback. Detailed implementation remains gated behind Phase 11 physical-phone preservation. |
| 2026-08-22 | Opened and implemented Phase 12 at the user's direction: Room/backup v4 now retain durable Google categories; Find selection, explicit Nearby Save, and Edit refresh acquire phone/category metadata at user-committed boundaries; grouped and single cards reveal Call/OpenTable actions above the divider with the caret below; 102 JVM, 35 device, and 25 tool tests plus lint, live handoffs, and a signed 3.1.0→3.2.0 data-preserving emulator update pass. Physical-phone rollout remains. |
| 2026-08-22 | Corrected the Phase 12 migrated-record disclosure in 3.2.1: every card retains its compact caret; opening a record with missing action metadata requests only that one Google listing's phone and primary type, persists missing values, and never bulk-loads saved places. Reduced disclosure spacing, added loading/empty/failure handling plus no-repeat and exact-field regression tests, and passed 104 JVM, 35 device, lint, signed build, and install-over-data gates. |
| 2026-08-22 | Documented Craig's successful 26-record phone recovery as a standalone second-phone runbook: separate private evidence, ADB identity/authorization, guarded Edit-screen capture, exact visible-field/provenance review, safe Google re-resolution, v2/v3/v4 artifacts, side-by-side signed install, transactional import, post-import export/digest proof, moved-business rules, rollback, and independent wife/owner sign-off. Corrected the migration-tool README now that phone/category storage is explicitly versioned. |
| 2026-08-22 | Added the final compact-card Web action in 3.2.2. Expanded cards now fit Call, shortened Reservation, and Web on one row; Web sends only the saved name/address to an explicit Google HTTPS search after the tap, with no new Places field/API request, permission, clipboard use, or database write. Live compact rendering and Google result handoff plus 105 JVM/35 device/25 tool/lint/signed gates pass. |
| 2026-08-22 | Extended the selected app color through the transparent Android status-bar inset in 3.2.3, retained safe edge-to-edge insets, and made clock/icon brightness follow the app-color contrast. Cyan, dark-blue, and permanent-release purple states were visually verified; 106 JVM, 35 device, 25 tool, lint, signed-build, and same-signer install-over-seven-record gates pass with no database change. |
| 2026-08-22 | Replaced the draggable-sheet map embedded in Change location with one shared fixed full-screen picker in 3.2.4. Find and Nearby retain their compact autocomplete sheet; map mode now pans/zooms under a stationary center pin and returns safely to search. Live swipe/confirm inspection plus 106 JVM, 36 device, 25 tool, lint, signed-build, and install-over-seven-record gates pass with no database or Google-request change. |
| 2026-08-22 | Corrected the fixed picker interaction in 3.2.5: removed the stationary center pin, restored visible +/− zoom controls, made an exact map tap drop or move the marker, and kept Use location disabled until a pin exists. Manual tap/zoom verification plus 106 JVM, 36 device, 25 tool, lint, signed-build, and same-signer install-over-seven-record gates pass with no database or Google-request change. |
| 2026-08-22 | Corrected Nearby's initial Our places map in 3.2.6: the complete saved collection remains the default, first map open automatically fits the origin and all visible saved markers, and Within-distance remains an optional filter. Both Nearby maps are light-only, count grammar is fixed, and all test/lint/signed-upgrade gates pass with seven records retained. |
| 2026-08-22 | Superseded the worldwide Nearby default in 3.2.7. Our places now applies a required 25-mile location boundary before all optional filters and before list/map rendering; Fit results can frame only that local projection. Live Denver inspection showed one of seven records and excluded Colorado Springs/Santa Fe, while all tests, lint, signed upgrade, and data-retention gates passed. |
| 2026-08-22 | Made the Nearby saved-card place-color accent measure and draw to the complete card height in 3.2.8, matching the main Saved card instead of stopping at a fixed 144dp. Added a device bounds regression and passed all test, lint, signed-upgrade, and seven-record retention gates. |
| 2026-09-13 | Restored the city title at the top of grouped Saved cards in 3.2.9 while leaving ungrouped cards unchanged. Added placement and recreation regressions, hardened the filter-sheet test setup for smaller viewports, and passed 106 JVM/36 device/25 migration-tool, lint, signed-upgrade, and seven-record retention gates. |
| 2026-09-13 | Triaged the wife's failed new-phone transfer without changing either phone: integrity-verified Craig's 26-record and newer 34-record version-4 Drive exports; these can seed her new app but do not recover her prior local-only records. Added a fail-closed encrypted export/restore workflow for the existing permanent signer so future computers can reproduce compatible releases without committing or replacing the key; the round trip and pinned signer fingerprint pass. |
| 2026-09-13 | Installed permanent-signed 3.2.9 over 3.2.8 on Craig's physical Galaxy S25+ without uninstall/clear. The first-install timestamp and private data directory stayed unchanged, and UI readback retained all 34 places while showing the new grouped-city heading. |
| 2026-09-13 | Completed 3.2.10/version-code 15 with the Android launcher/application label shortened from the temporary **Places New** migration name to **Places**. Unit/lint/release-build gates and direct artifact label/version/signer inspection pass; package identity, database, and behavior remain unchanged. |
