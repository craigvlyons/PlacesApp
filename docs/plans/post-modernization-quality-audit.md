# Post-modernization reliability and code-quality audit

**Date:** 2026-08-22  
**Status:** Complete  
**Database constraint:** Room remains version 2; no table, column, migration, database name, or stored record was changed.

## Outcome

The complete production source was reviewed across Room persistence, backup/import, Preferences DataStore, Google Places and Nearby requests, fused location, geocoding, maps/directions, ViewModels/coroutines, screen failure states, logging/privacy, dependency configuration, and obsolete code. The audit fixed the reliability findings below and added regression coverage at the affected boundaries.

## Findings fixed

### Persistence and data integrity

- Replaced the application insert path's `OnConflictStrategy.REPLACE` with `ABORT`; a primary-key collision can no longer silently overwrite a different saved place.
- Added one Room transaction that atomically checks a nonblank Google place ID and inserts. The prior ViewModel check-then-insert sequence had a race window; the repository now reports a typed `SavedPlaceAlreadyExistsException`.
- Made DAO deletion return its affected-row count and made the repository reject missing-row deletes instead of reporting success.
- Kept legacy duplicate Google place IDs readable. The released schema is not rewritten and existing duplicates are not deleted or merged.
- Added device tests proving primary-key conflicts preserve the original row and application inserts reject a new duplicate Google place ID.

### Google request, location, and coroutine lifecycle

- Editing a Find query now immediately cancels its search/details jobs, abandons the autocomplete billing session, clears its selection, and stops loading. Previously, a late response could repopulate results for text the user had already changed.
- Applied the same cancellation/session ownership to Find and Nearby location autocomplete, map-origin selection, and device-location selection. Completion handlers now update state only when they still own the active session.
- Added unexpected-exception handling to Find place/origin search and selection. Cancellation is still rethrown before broad handling.
- Repaired Nearby's one-entry discovery cache. A failed request for a second filter can no longer leave an empty screen when the user returns to the last successful filter.
- Replaced two Haversine implementations with one validated domain function and clamped floating-point input before square roots, including antipodal and symmetry coverage.
- Nearby now reports and omits incomplete Google records, and treats a nonempty response containing no usable record as a retryable unavailable result instead of a false empty success.
- Nearby saves are keyed per Google place rather than globally, so saving one result no longer silently discards a different result's tap.

### User mutations and recovery states

- Delete/Undo is now Room-confirmed. The screen no longer announces a deletion or offers Undo before the DAO succeeds, and every Undo carries the exact deleted record rather than a single replaceable global slot.
- Swipe-to-delete now resets to Room truth. A failed DAO delete leaves the row visible rather than trapping its composable in a locally removed state.
- Saved-place delete, restore, and Favorite-heart mutations are serialized per record. Sort and view preference changes are latest-write-wins.
- The Saved screen's Room/DataStore observation can be retried in place after a failure rather than remaining failed until the app is restarted.
- Details writes validate nonblank names, opaque colors, and nullable 1–5 ratings at the repository boundary. Completed mutation jobs are removed rather than retained.
- Preference observation failures in Settings, app appearance, and Nearby are privacy-safely logged and fall back to a usable state.

### Backup, maps, startup, and diagnostics

- Backup operations now clear their working state in one `finally` path; each operation cannot accidentally leave the screen permanently busy.
- Starting a new import inspection clears an older pending import first.
- Backup validation no longer reflects attacker-controlled format values, field names, or large duplicate-ID lists into a user-visible Snackbar.
- Invalid restored map coordinates are discarded, removed from saved UI state, and privacy-safely categorized without logging coordinate values.
- Directions reject nonfinite/out-of-range coordinates before creating an intent and log unavailable, blocked, and invalid launches through the privacy-safe logger.
- Recoverable map-camera fallback paths now produce category-only diagnostics instead of silently swallowing the failure.
- Places SDK initialization failure no longer terminates the application; Find/Nearby use the existing unavailable repositories and show authorization-safe UI.
- Removed the production-only backup helper that was used solely by a test, removed an unused exception, normalized DAO naming, and changed absent place-ID defaults from an ambiguous empty string to `null` without altering Room's schema.
- Added a source guard that rejects direct `Log`, `println`, `printStackTrace`, and standard-output use outside `PrivacySafeLog`.

### Platform currency

- Advanced `targetSdk` from 36 to 37 after reviewing Android 17 target behavior. The app already compiled with API 37 and does not use the affected reflection, Bluetooth, SMS, audio, or non-SDK paths.
- Retained Gradle 9.5.0 deliberately. It is Android Gradle Plugin 9.3's documented default and minimum; lint's only remaining warning is its generic notice that Gradle 9.7.1 exists, not an application-code defect.

Primary research: [Android architecture recommendations](https://developer.android.com/topic/architecture/recommendations), [Android data-layer error handling](https://developer.android.com/topic/architecture/data-layer), [Room insert conflict behavior](https://developer.android.com/reference/androidx/room/Insert), [DataStore guidance](https://developer.android.com/topic/libraries/architecture/datastore), [Places autocomplete sessions](https://developers.google.com/maps/documentation/places/android-sdk/place-session-tokens), [Nearby Search fields and responses](https://developers.google.com/maps/documentation/places/android-sdk/nearby-search), [Places attribution policy](https://developers.google.com/maps/documentation/places/android-sdk/policies), [Android 17 setup](https://developer.android.com/about/versions/17/setup-sdk), and [AGP 9.3 compatibility](https://developer.android.com/build/releases/agp-9-3-0-release-notes).

## Verification evidence

- 88/88 JVM tests pass.
- 31/31 Android API-37 emulator tests pass.
- 18/18 offline legacy-migration tool tests pass.
- Debug lint: 0 errors and no application-code warnings. The sole advisory is the deliberate Gradle-wrapper version noted above.
- Debug APK, Android-test APK, signed release APK, and release lint-vital all build.
- Signed release certificate SHA-256 remains `a7800d55e3c0c4ea56cd822ee3dbc60bbdc2bc01c679d56c34acd6d6942291f4`.
- Current signed release APK SHA-256 is `5b12be8e35c6dcbe3d5046da562b8abf0fd283485df60ed748c9061a76c15ece` after the verified light-only appearance correction.
- Same-signer `adb install -r` succeeded with package `com.personal.favoriteplaces`, version 3 / 3.0.0, target API 37.
- First-install time remains `2026-08-21 13:47:21`; the live Saved screen reports all 7 current records after reinstall.
- Room remains version 2. Schema hashes remain v1 `725d7a1992b1539f31f21cc00094fe90c3228d9dfc1d503bfcee26052840d230` and v2 `224603f257c69b000b75c2e411d508da190dac325b6a4f30918b500834b491b2`.
- Post-install log review found no app fatal, Room, SQLite, or migration failure.
- With the emulator's system night mode explicitly enabled, the installed release retained the approved light canvas/cards and readable status/navigation system UI; all 7 emulator records remained visible.

## Deliberate boundaries, not hidden code debt

- The release remains non-minified until the reflection-based, versioned Gson backup contract has an explicit release/R8 compatibility test. This favors recoverable household data over an unverified APK-size optimization.
- Maps/Places keys remain in the client by explicit product decision. They cannot be secret in an APK; Android package/certificate restrictions, API restrictions, quotas, and billing alerts remain required Cloud-console controls.
- Google permits place IDs to be cached indefinitely but restricts caching other Places content. The app's established offline saved-place product retains user-selected name/address/coordinates/type. Before any public distribution, review that storage model and the public Terms/Privacy requirements against the current [Places policy](https://developers.google.com/maps/documentation/places/android-sdk/policies); do not silently remove household data to resolve a policy question.
- Rich notes and alternative card templates remain optional product work. They are not reliability blockers and must receive their own storage/migration plan if selected.
