# Places App Rolling Implementation Plan

**Status:** Active planning — Phase 0 ready, implementation not started  
**Created:** 2026-08-21  
**Current phase:** Phase 0 — stabilize the existing app and protect its data

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
- Keep application ID `com.example.favoriteplaces` and the production signing identity.
- Use explicit, tested Room migrations for every future schema change.
- Keep colors unlabeled and preserve their exact stored ARGB values.
- Keep Favorite as the independent `isFavorite` heart flag.
- Do not combine unrelated upgrades merely for convenience.
- Record commands, test results, decisions, risks, and evidence in this file as work proceeds.
- Stop at the current phase gate. Do not begin or fully plan the next phase until this file records that the gate passed.

## Rolling workflow

At the start of a phase:

- Confirm its goal, scope, exclusions, dependencies, and measurable gate.
- Break only that phase into implementation workstreams.
- Mark one workstream active at a time.

At the end of a phase:

- Record the files and behavior changed.
- Record automated and device-test evidence.
- Confirm database, signing, application ID, and upgrade safety.
- List unresolved risks and newly discovered facts.
- Obtain review of the phase gate.
- Mark the phase complete, then replace the “Next phase” placeholder with a detailed plan for exactly one phase.

## Current state

- Application code and Room schema are unchanged from version 2 of the project.
- Room is version 1 and has no registered migrations or exported schema history.
- The command-line baseline currently fails before project configuration because the available JDK 26 is incompatible with the existing Gradle/Kotlin toolchain.
- The release build points at the debug signing configuration; the real installed-build signing path is not yet verified.
- `local.properties` and `.gradle/config.properties` are currently untracked, and the repository has no `.gitignore`.
- Android backup configuration files are still generated templates rather than a verified recovery strategy.
- The existing app is actively used on two devices, so install-over-existing verification and a staged one-device rollout are mandatory.

---

## Phase 0 — stabilize the existing app and protect the database

**Phase status:** Not started  
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

**Objective:** Prove what is required to install a new build over the two existing installations.

- [ ] Determine whether each device received the app from Google Play or a directly installed APK.
- [ ] Locate the production artifact and identify its package name, version code, and signing-certificate fingerprint.
- [ ] Confirm access to the corresponding signing key or Play App Signing account without copying credentials into the repository or documentation.
- [ ] Confirm the installed package is `com.example.favoriteplaces` and record the current installed version on both devices.
- [ ] Store only non-secret fingerprints and conclusions in the phase evidence log.

**Checkpoint:** A candidate build can be configured to use the same upgrade identity, or Phase 0 stops with the exact signing blocker documented.

### Workstream 0.2 — make the repository safe to build

**Objective:** Prevent secrets and generated files from being committed while preserving the existing local setup.

- [ ] Add a focused `.gitignore` covering `local.properties`, Gradle/IDE output, build output, signing files, and other generated Android files.
- [ ] Verify `local.properties` and `.gradle/config.properties` remain local and untracked.
- [ ] Scan tracked files and history for exposed Maps keys or signing material; rotate only if exposure is confirmed.
- [ ] Document required local properties using placeholders, never real key values.

**Checkpoint:** `git status` shows no local secret or generated build state as a commit candidate.

### Workstream 0.3 — establish a reproducible baseline build

**Objective:** Build the current application before modernizing it.

- [ ] Use a supported JDK 17 baseline for the existing Android Gradle Plugin 8.1/Gradle 8.0 project while retaining Java/Kotlin bytecode compatibility as currently configured.
- [ ] Document the selected JDK and exact baseline commands.
- [ ] Run clean debug compilation, unit tests, lint, debug APK assembly, and a non-minified release candidate build.
- [ ] Separate pre-existing warnings from blocking failures.
- [ ] Make only the minimum build corrections required for a repeatable baseline; do not begin dependency modernization here.

**Checkpoint:** The unchanged product builds twice from a clean state using the documented toolchain.

### Workstream 0.4 — freeze and test the Room version-1 contract

**Objective:** Make the current database structure and values testable before touching persistence code.

- [ ] Enable Room schema export and archive the generated version-1 schema JSON.
- [ ] Add Room migration-test infrastructure without changing the database version.
- [ ] Create a synthetic, production-shaped version-1 fixture covering all five exact colors, Favorite heart on/off, ratings, empty and long notes, multiple cities, place IDs, coordinates, Unicode, and nullable legacy values.
- [ ] Define a deterministic digest over every persisted column and record expected row counts.
- [ ] Add tests that open and read the fixture with the current application schema.
- [ ] Add a guard test that fails if a destructive migration method is introduced.

**Checkpoint:** Tests prove that the current code opens the version-1 fixture and returns every row and value unchanged.

### Workstream 0.5 — create a recoverable backup/export path

**Objective:** Provide a recovery mechanism independent of assumptions about Android cloud backup.

- [ ] Define a versioned export format containing every persisted field: ID, place ID, title, address, notes, rating, Favorite flag, exact color integer, city, latitude, and longitude.
- [ ] Implement a read-only export path that does not mutate Room and does not include API keys or unrelated preferences.
- [ ] Validate exported row count and deterministic content digest.
- [ ] Build and test a separate restore/verification path against a disposable test database before allowing any production import.
- [ ] Document where each household backup will be stored and how its readability will be checked.
- [ ] Review Android backup/device-transfer rules explicitly; do not treat generated template files as proven recovery.

**Checkpoint:** A version-1 test database can be exported, validated, restored into a disposable environment, and compared field-for-field with the source.

### Workstream 0.6 — capture current behavior with regression tests

**Objective:** Protect the workflow that exists today while later phases change its implementation.

- [ ] Test insert, update, delete, delete/Undo, duplicate place IDs, and Favorite-heart updates.
- [ ] Test card sorting by city, Favorite, color, and rating in both directions.
- [ ] Test city-plus-color grouping used by List View and its exact map subset.
- [ ] Test Edit preservation of address, coordinates, place ID, city, color, and Favorite flag when changing only name, rating, or notes.
- [ ] Add focused UI tests for Card View, List View, heart toggling, Edit, and the existing add/select/save flow where practical.
- [ ] Record current intentional behavior separately from defects so later tests do not freeze known crashes.

**Checkpoint:** The version-2 workflow has a meaningful automated regression baseline centered on persistence and the two organization modes.

### Workstream 0.7 — fix baseline stability defects only

**Objective:** Remove defects that prevent safe baseline use or reliable testing without starting modernization.

- [ ] Fix the null-location dereference and keep search usable when location is unavailable or denied.
- [ ] Prevent Save from racing selected-place detail loading or saving stale coordinates/address.
- [ ] Make city-map startup derive its camera from the first non-empty data emission and handle an empty subset safely.
- [ ] Remove reachable forced-null crashes in core save, edit, list, and map paths when a narrow fix is possible.
- [ ] Stop redundant Favorites refresh/query loops sufficiently to make baseline tests deterministic; defer broad state architecture cleanup.
- [ ] Keep Room version 1 and verify the digest after every persistence-adjacent fix.

**Checkpoint:** Critical current flows fail safely instead of crashing, with regression tests for each corrected defect.

### Workstream 0.8 — prove in-place upgrade safety

**Objective:** Demonstrate that the stabilized build can replace the existing app without losing data.

- [ ] Install the existing version-2 artifact on a test device or emulator without clearing data.
- [ ] Populate or restore the representative version-1 fixture.
- [ ] Install the stabilized candidate over it; never uninstall between versions.
- [ ] Verify package identity, row count, full digest, Card/List views, Favorite hearts, Edit/Save, delete/Undo, Add, and city/color maps.
- [ ] Confirm the candidate does not change the Room version or schema identity.
- [ ] Produce a short Phase 0 evidence report with build outputs, test results, signing fingerprint comparison, and database comparison.

**Checkpoint:** Repeatable install-over-existing testing preserves the complete database and existing workflow.

### Phase 0 gate

Phase 0 is complete only when all of the following are true:

- [ ] Correct application ID and upgrade signing identity are known and usable.
- [ ] The current app builds reproducibly with the documented JDK/toolchain.
- [ ] The Room version-1 schema, fixture, row counts, and full-value digest are under test.
- [ ] No destructive migration or database reset path exists.
- [ ] A test export/restore round trip preserves every field.
- [ ] Critical current workflows have regression coverage and baseline stability defects are fixed.
- [ ] An install-over-existing test passes without clearing data.
- [ ] The evidence log below is complete and reviewed.
- [ ] No platform, API, Maps, architecture, UI, or schema modernization work has leaked into Phase 0.

### Phase 0 evidence log

Fill this in during implementation; do not mark the phase complete based on intention.

| Evidence | Result | Reference |
|---|---|---|
| Installed source and versions on both devices | Pending | — |
| Production signing fingerprint/access | Pending | — |
| Reproducible baseline build | Pending | — |
| Room v1 exported schema | Pending | — |
| Fixture row count and digest | Pending | — |
| Export/restore round trip | Pending | — |
| Regression test suite | Pending | — |
| Install-over-existing test | Pending | — |
| Phase review | Pending | — |

### Phase 0 decision log

Record decisions as they are made:

| Date | Decision | Reason |
|---|---|---|
| — | — | — |

---

## Next phase — intentionally not planned yet

No later implementation phase is active or detailed in this document. Candidate outcomes remain in the strategic modernization plan, including platform/toolchain upgrades, Places API (New), official Maps Compose, architecture cleanup, the approved UI refresh, and eventual place-type persistence.

After the Phase 0 gate passes, review its evidence and risks, select the safest next outcome, and replace this placeholder with the detailed plan for exactly one next phase.

## Rolling-plan history

| Date | Change |
|---|---|
| 2026-08-21 | Created the rolling plan with Phase 0 stabilization and database protection as the only detailed phase. |
