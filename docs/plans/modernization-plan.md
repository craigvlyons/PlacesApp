# Places App Modernization Plan

**Status:** Modernization Phases 0–7 complete; household rollout safeguards remain
**Created:** 2026-08-21  
**Scope:** Android application modernization, Google Maps/Places migration, reliability, testing, and UX refresh

## Document roles

- This document is the strategic modernization reference: findings, constraints, target product behavior, phase outcomes, and safety gates.
- [`rolling-implementation-plan.md`](./rolling-implementation-plan.md) is the execution reference. Only the current phase is planned in implementation detail; the next phase is expanded only after the current phase is completed, evidenced, and reviewed.
- [`../demo/mobile-directions.html`](../demo/mobile-directions.html) is the approved UI guidance for the future product-refresh phase. Its hierarchy, interactions, spacing direction, unlabeled color treatment, and independent Favorite heart should guide implementation.
- [`../demo/index.html`](../demo/index.html) is archived and must not be used as implementation guidance.

## Non-negotiable data-safety requirement

The existing on-device SQLite/Room database contains real data used by two people. Preserving that data takes priority over every other goal in this plan.

The following rules apply to all implementation work:

1. Never delete, recreate, clear, or replace the production database as part of an upgrade.
2. Never use `fallbackToDestructiveMigration()`, `fallbackToDestructiveMigrationFrom()`, `clearAllTables()`, or uninstall/reinstall as a migration strategy.
3. Keep the existing database name, `favorites_db`, and preserve the existing `Favorite` table unless a versioned Room migration explicitly transforms it.
4. Any schema change must increment the Room database version and include an explicit, forward-only `Migration` implementation.
5. Every migration must be tested using a representative version-1 database containing real-shaped saved places, including notes, ratings, colors, Favorite-heart flags, place IDs, cities, and coordinates.
6. Tests must verify both schema correctness and exact preservation of every pre-existing row and value.
7. Before either shared device is upgraded, create and verify a recoverable backup/export of its data.
8. Upgrade one device first, verify its data and normal app behavior, and only then upgrade the second device.
9. Keep the legacy app (`com.example.favoriteplaces`) installed and untouched until its records have been captured, imported into the replacement, and verified on that device.
10. The replacement uses a new permanent application ID and signing identity because the legacy APK's signing key was lost. Android requires an update to have both the same application ID and the same signing certificate (or valid key-rotation proof), so an in-place upgrade is not possible.
11. Migrate each household dataset through a versioned, verified export/import artifact. Never claim that the replacement can directly read the legacy app's private SQLite database.
12. Prefer modern refactoring and restructuring where the existing design is obsolete, duplicated, tightly coupled, or difficult to test. Do not preserve weak structure merely to minimize the diff.
13. Refactors must finish at production quality: explicit state and ownership, supported APIs, focused components, structured concurrency/cancellation, safe errors, privacy-safe logging, tests at stable boundaries, and removal of replaced code after parity is verified.
14. Keep refactors within the active rolling phase so failures can be attributed and SQLite/data preservation can be proved after each bounded change.

No feature, library upgrade, or cleanup is complete if it risks violating these rules.

These rules protect records, not the old application's schema design. Because the replacement has a different application ID, it owns a separate Room database and does not need binary/schema compatibility with the legacy APK. The version-1 schema is retained during stabilization so imported data and toolchain changes can be verified against a known baseline; after that, the replacement schema may be refactored or extended whenever useful, provided each released replacement version advances through an explicit, tested migration without losing imported or newly created records.

## Executive assessment

The existing codebase should be modernized rather than rewritten, but it must ship as a side-by-side replacement application because the legacy signing key is unavailable. Its core product idea remains strong: search for a place, save it, personalize it, browse favorites, and view groups on a map. Compose, Room, Hilt, repository abstractions, use cases, and ViewModels provide a usable foundation.

The most urgent work is platform and Google API compatibility. The project is primarily a 2023 Android stack and currently combines three Google integration approaches:

- Maps SDK for Android.
- Places SDK for Android 3.2.0, initialized through a now-deprecated entry point.
- Direct Retrofit calls to legacy Places HTTP endpoints.
- In addition, 27 Maps Compose source files are copied into the application instead of consuming the maintained library normally.

The recommended direction is to keep the app and database, modernize the build in controlled steps, replace the legacy Places path with Places SDK for Android (New), adopt the official Maps Compose dependency, then refresh the UI.

## Confirmed product requirements

The following requirements were confirmed after the initial audit and are part of the product contract:

- Color is meaningful user data, not decorative styling, but the app must not assign names or fixed meanings to the six choices. Each person may use a color however they want. Existing colors and their stored ARGB values must be preserved exactly; the added darker blue is only another unlabeled choice.
- The refreshed design may present color more subtly and consistently, but it must remain immediately recognizable and filterable as an unlabeled swatch.
- **Favorite is independent from color.** It is represented by the existing heart/`isFavorite` flag, can coexist with any color, and must be filterable separately.
- Saved places need strong place-type filters so restaurants, coffee shops, breweries, parks, attractions, and other useful categories can be found quickly.
- Saved places need location-aware filtering so the users can quickly answer “what have we saved near where we are right now?”
- Filters must compose: for example, “coffee shops within 5 miles using this color” or “heart-marked favorite restaurants in this city.”
- Location permission remains optional. City/area and map-based filtering must still work when current location is unavailable.
- The Android app will use three top-level destinations: **Saved**, **Find**, and **Nearby**. Map is a view inside Nearby rather than a fourth destination.
- Use **Places** as the home-screen title. **Saved** remains the bottom-navigation label and **Favorite** refers only to the independent heart flag rather than the whole collection.
- Settings owns two independent unlabeled color preferences: **App color** for compact primary headers and the persistent Saved/Find/Nearby footer, and **Default color for new places** used only when Find or Nearby creates a record. Fresh installations default both to the established light blue, and the palette also includes the requested darker blue. Preference changes never recolor existing places or change Room.
- Saved-place colors remain personal data, not global brand colors; the focused Place Details screen replaces the app color with the selected place color in its own compact header and Save footer while its content and editing sheet remain neutral.
- The Saved screen is for searching, sorting, filtering, and opening places already stored. Near-me discovery belongs on Nearby so the main screen does not become overloaded.
- Place cards use a neutral surface with the saved color as a narrow accent. Every place presentation shows a bold name, then the complete address, then up to two or three truncated lines of personal notes when present. A compact numeric 0–5 rating plus one star sits at the upper right above the independent Map and Favorite-heart actions; the star is gold only when the rating is above zero and neutral when unrated. Do not repeat city or place type as separate card metadata; both remain available in details and filters.
- Opening a Saved card goes to a dedicated saved-place details screen rather than directly into a full edit form. Details prioritize a bold content name with the selected color/down-caret control at its far right, then smaller optional type, complete address/map action, rating, and a spacious notes area. Place types are neutral supporting text throughout the app; they never inherit the app accent or a saved-place color.
- On details, color, rating, and the independent Favorite heart are safe quick actions. Name/type open a compact explicit-save bottom sheet; notes open a larger explicit-save editor so long personal context remains comfortable to read and write.
- Favorite hearts belong on Saved cards, grouped Saved rows, the details header, saved-map selection, and saved Nearby results. Unsaved Find/Nearby results retain an explicit **Save** action; a heart must never create a place implicitly.
- The Settings gear opens a focused Settings screen rather than jumping directly to backup. Appearance choices use the same slim looping wheel as details; **Data & backup** remains reachable as a separate row.
- Saved filters include Type, City, Color, and the independent Favorite heart, but all controls live in one modal bottom sheet opened by a single text-only **Filter** action. The main screen does not show separate filter chips.
- The city-plus-color grouped mode remains an optional organization/filter behavior, but its group cards do not print city headings. Each contained place keeps its own name, address, notes preview, Map, and Favorite heart so both Saved layouts use the same information hierarchy.
- The selected visual baseline is the **Option B / Balanced** card direction with soft, comfortable spacing: neutral rounded cards, light outlines, and a narrow unlabeled color rail.
- The Filter action may show a small active-filter count. Selection, individual categories, and **Clear all** stay inside the bottom sheet so the collection screen remains quiet.
- Find places the current search origin directly below the search bar with a separate **Change location** action. The current device location is already the default; Change location opens directly to location autocomplete with a secondary **Choose on map** button.
- Nearby places distance, type, color, Favorite heart, and presets inside the same consolidated filter sheet. Distance defaults to 10 miles and supports numeric entry plus increment/decrement controls within the sheet.
- This remains a client-only personal app for two users. No separate backend or proxy is planned, so Google Maps/Places credentials will remain embedded in the Android application for now.
- Because an APK cannot keep an embedded key secret, protection relies on native Android SDKs, Android application restrictions, API restrictions, conservative quotas, and usage/billing monitoring.

## Initial project snapshot (before implementation)

- One Android application module.
- Approximately 8,100 lines across Kotlin and XML.
- `compileSdk = 34`, `targetSdk = 33`, `minSdk = 24`.
- Android Gradle Plugin 8.1.0, Gradle 8.0, Kotlin 1.8.10, Java 11 bytecode target.
- Compose BOM from March 2023, mixed Material 2 and Material 3 components.
- Room database version 1, named `favorites_db`.
- Hilt dependency injection and Navigation Compose.
- Maps SDK 18.1.0, Maps KTX 3.4.0, Places SDK 3.2.0.
- Retrofit/Gson models for legacy Place Autocomplete and Place Details responses.
- Only generated placeholder unit and instrumentation tests.
- No repository `.gitignore` currently exists.
- Repository was clean before this plan was added.

## How the app works today — verified from code and project screenshots

This section is the behavioral baseline for redesign work. It distinguishes current behavior from proposed features so the modernization does not accidentally replace the workflow with a generic restaurant app.

### Navigation and collection

- The current app has four routes: Favorites, Add New Favorite, Edit Favorite, and City Map. It does not currently have bottom navigation, a dedicated Nearby screen, or a global saved-place search field.
- Favorites is the start destination. A centered floating **+** opens Add New Favorite; tapping a card or list row opens Edit.
- The home screen has two display modes. **Card View** shows individual saved places. **List View** groups places first by city and then by their exact stored color.
- The chosen List/Card mode is the only preference currently persisted in SharedPreferences. Card View is the default on a fresh install.
- The sort panel offers City, Loved, Color, or Rating with ascending/descending direction. “Loved” is the current UI term for the separate `isFavorite` heart flag; the refreshed UI should call this **Favorite** while preserving the same boolean value.

### Color and Favorite

- A saved place has one of five stored ARGB colors. The colors have no built-in names or meanings; they are free-form personal organization chosen by the users.
- Color is structurally important today: it fills the individual cards and determines the city/color groups shown in List View. The group map action maps only the places in that selected city-and-color group.
- Favorite is completely independent. It is stored in `isFavorite`, displayed as an outlined or filled red heart in Card View, and can be toggled directly from that card.
- New places default to the fourth entry in the stored color list and `isFavorite = false`. The Add flow does not currently ask for either color or Favorite; color is changed later in Edit, while Favorite is changed from Card View.
- Edit currently preserves the existing Favorite flag but does not expose a heart control. Any refreshed Edit heart is therefore an intentional usability improvement, not a description of current behavior.

### Card View and List View

- Card View displays the full saved color, name, personal star rating, independent Favorite heart, address, personal notes, and a delete icon. Tapping elsewhere on the card opens Edit.
- List View displays a full-color city group card containing the city name, a map action, and the place names in that exact city/color group. Each row opens Edit and supports swipe-to-delete with an Undo snackbar.
- The two modes are not just density variants: Card View exposes rating, heart, address, and notes, while List View emphasizes city/color grouping and quick access to a group map.

### Find and save

- Add New Favorite requests location permission and uses the device location to bias a manual Google Places search. The user types a query and presses **Search**; results are not currently search-as-you-type.
- Each prediction shows name, address, and raw Google place types. Tapping a result fetches Place Details and expands an embedded map only for that selected result.
- **Save Favorite** appears inside the expanded result after selection. Saving stores Google place ID, name, formatted address, derived city, coordinates, blank notes, rating `0`, Favorite `false`, and the default color.
- Duplicate saves are rejected by Google place ID.

### Edit and maps

- Edit is deliberately sparse: five unlabeled color circles, editable name, personal rating, read-only address, editable notes, a Save floating action, and a map action. The currently selected color fills the whole screen.
- The modernization should retain that directness. The final direction keeps details content and focused editing sheets neutral while limiting the selected color to the Place Details header and slim footer, without named color statuses or a stack of settings cards.
- The City Map is opened from a List View city/color group and receives that subset through an in-memory singleton cache. It displays markers for those saved places; it is not currently a general Nearby discovery map.

## Findings

### P0 — release and platform blockers

#### Target SDK is out of policy

`app/build.gradle.kts` targets API 33. Beginning August 31, 2026, new apps and updates on Google Play must target Android 16/API 36. Existing mobile apps must target at least API 35 to remain available to new users on newer Android versions.

Impact:

- A new update cannot be submitted after the deadline without targeting API 36.
- The existing listing can lose discoverability for new users on newer devices.

Reference: <https://support.google.com/googleplay/android-developer/answer/11926878>

#### Release build uses the debug signing configuration

At audit time, the `release` build type set `signingConfig` to the debug signing configuration. This is resolved for the replacement: it now has a permanent ignored release key and fails release configuration when required signing properties are absent.

Impact:

- A differently signed build cannot update the installed application.
- Incorrect signing decisions can make the existing on-device database inaccessible from the replacement APK.

The recovered legacy APK signer was lost, which is why the replacement uses a separate identity; every future replacement update must retain the new permanent signer.

#### Current command-line build cannot run with the installed JDK

The audit attempted `testDebugUnitTest` and `lintDebug`. Gradle failed before project configuration because Gradle 8.0/Kotlin 1.8.10 cannot parse the installed JDK 26 runtime. Android Studio includes JDK 21 locally, but the project does not pin a compatible build runtime.

Impact:

- Builds are environment-dependent and presently fail from the shell.
- Tests and lint cannot provide a trustworthy baseline until the toolchain is stabilized.

This was an environment/toolchain failure, not evidence that the application source itself currently fails to compile under its original supported JDK.

### P0 — database and upgrade continuity

#### Database has no migration infrastructure yet

`FavoriteDatabase` is version 1 and the Room builder has no migrations registered. That is acceptable while the schema remains unchanged, but future entity edits will require explicit migrations.

Required action before any model/schema work:

- Archive the version-1 Room schema using Room schema export.
- Add Room migration-test infrastructure.
- Create a version-1 database fixture populated with representative production-shaped data.
- Prove that an unchanged-schema application update opens the existing database without modification.
- For later schema changes, write and test each migration step independently and as a full chain.

#### Replacement identity and migration are part of data preservation

Database safety is not limited to SQL. The recovered legacy APK is signed by a lost key, and the reset Mac's key does not match it. Android therefore cannot accept an in-place update. The replacement must use a new permanent application ID and signing identity, remain installed beside the legacy app during migration, and import a verified capture of the legacy records before the old app is removed.

Official Android update requirements: <https://developer.android.com/google/play/app-updates>

#### Backup configuration is still the generated template

The manifest enables backup and references `backup_rules.xml` and `data_extraction_rules.xml`, but both files still contain only generated sample comments and no deliberate include/exclude policy. This is not a verified two-device recovery strategy and must not be treated as the safety net for migration work.

Required action:

- Decide and test the intended Android backup/device-transfer behavior.
- Add the explicit user-visible export and verified restore path described in the migration protocol below.
- Prove recovery from exported data before either shared device receives a schema-changing build.

### P1 — Google Places migration

#### Direct legacy Places web-service calls should be removed

`GooglePlacesApi.kt` calls:

- `maps/api/place/autocomplete/json`
- `maps/api/place/details/json`

Both are legacy HTTP APIs. The API key is passed as a query parameter from the mobile application. A key embedded in an APK is recoverable, and a mobile client cannot safely protect a web-service key using fixed server IP restrictions.

Recommended action:

- Use Places SDK for Android (New) throughout the mobile application.
- Remove the Retrofit Places interface, legacy response DTOs, Gson converter, and Retrofit repository after the SDK migration is verified.
- Do not add features that require a web-service-only Places endpoint unless the client-only security and billing tradeoff is reviewed explicitly. A backend is not part of the current plan.

Google security guidance recommends client SDKs for mobile applications and application/API restrictions on keys: <https://developers.google.com/maps/api-security-best-practices>

#### Deprecated Places initialization

`PlacesApiModule.kt` calls `Places.initialize(context, key)`. Google now directs applications using Places API (New) to `Places.initializeWithNewPlacesApiEnabled()`.

Required Cloud Console work:

- Enable Places API (New).
- Confirm Maps SDK for Android remains enabled.
- Audit current key traffic and restrictions before changing the existing key.
- Use a properly Android-restricted production key tied to the permanent replacement package `com.personal.favoriteplaces` and its release signing-certificate fingerprint. Keep any temporary legacy-app key separate while the old installation remains installed.
- Use a separate restricted development key for debug builds where practical so the debug signing fingerprint does not broaden production-key access.
- Restrict each key to only Maps SDK for Android and Places API (New), plus any other API that is deliberately verified as required.
- Keep key source values outside version control through the Secrets Gradle Plugin/local properties even though the final key will necessarily be embedded in the built APK.
- Set conservative per-day/per-minute quotas suitable for two users, configure billing budget alerts, and review usage after release.
- Never log the key, include it in screenshots, or place it directly in tracked Kotlin/XML source.
- Consider Places SDK App Check after the core migration.

This does not make the embedded key secret. It limits where and how a recovered key can be used and reduces the financial impact of misuse. For this personal two-user app, that is the accepted risk posture.

Migration reference: <https://developers.google.com/maps/documentation/places/android-sdk/legacy/migrate-overview>

#### Existing `PlacesClient` is injected but unused

The app creates and injects `PlacesClient`, but search and details are performed through Retrofit. Consolidating on `PlacesClient` removes duplicate infrastructure and makes the intended architecture clear.

#### Audit every Google request before replacing the integration

The migration must start with a request inventory rather than a one-for-one rewrite. For every current and proposed Google call, record its user action, API/SDK method, request fields, location bias or restriction, cancellation behavior, expected call count, attribution requirement, error states, and billing SKU. Capture aggregate request counts and status categories only; do not log queries, predictions, addresses, coordinates, place IDs, or API keys.

The Phase 2 design review must compare the maintained Android-native options against each app workflow:

- **Find by typed name or category:** compare Autocomplete (New) plus Place Details (New) with Text Search (New). Autocomplete is appropriate for search-as-you-type selection and must use one session token through the terminating details request. Text Search is appropriate when the user submits a complete free-text business/category query and supports a location bias or restriction plus one included type.
- **Choose a different search origin:** use Autocomplete (New) for the typed city/address and preserve the map-picker path. Resolve the confirmed selection to coordinates and a display label; do not parse coordinates or locality from display strings.
- **Nearby discovery:** evaluate `PlacesClient.searchNearby()` with a circular restriction, the user-selected radius, app-owned type mappings, and explicit result limits. Do not simulate Nearby by repeatedly issuing text searches.
- **Selected place/save:** request only the stable identity and persisted fields needed by the app: place ID, display name, localized formatted address, address components, latitude/longitude, primary/raw types, and any explicitly approved additional field. Expensive enrichment such as ratings, hours, photos, phone, website, or price must remain excluded unless its product value and billing tier are approved.
- **Map-point fallback:** keep platform reverse geocoding only for a raw point selected from the map. It must remain asynchronous, optional, and error-tolerant; it is not a substitute for structured Places data when a Google Place was selected.

Use explicit field lists on every Places request because returned fields control both payload and billing. Add fake-client contract tests that assert the exact request method, fields, session-token lifecycle, bounds/type filters, cancellation of stale input, and safe handling of missing fields. Add a small live-device smoke matrix only after the fake boundary passes, then review Cloud Console usage, errors, latency, and billing before removing the legacy path.

Current SDK references: [Autocomplete (New)](https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete), [Text Search (New)](https://developers.google.com/maps/documentation/places/android-sdk/text-search), [Nearby Search (New)](https://developers.google.com/maps/documentation/places/android-sdk/nearby-search), and [Place data fields](https://developers.google.com/maps/documentation/places/android-sdk/data-fields).

#### Stop parsing formatted addresses — implemented in Phase 2

The Phase 2 implementation removed comma splitting from `PlaceCityResolver`, Add/save, and `EditFavoriteViewModel`. A formatted address is now treated as localized display text, not a stable data format.

Required direction:

- Store Google's localized formatted address unchanged in the existing `address` column.
- Derive the existing `city` value from typed address components, using a documented precedence such as `locality`, then `postal_town`, then an appropriate administrative/sublocality fallback; return an empty/unknown display value when none exists rather than guessing from commas.
- Treat address components as unordered and optional, and match their type tags rather than list positions.
- Wrap long address text in the UI without splitting it into presumed street/city segments. If structured street/city display is later desired, add explicit app-owned fields only through a reviewed Room migration.
- Preserve imported legacy address and city strings exactly. Re-resolving legacy metadata must be reviewable and must never overwrite user data silently.

Google documents `Place.getAddress()` as the localized display address and address components as the structured extraction source; components should not be used to reconstruct the formatted address: [Place details/address components](https://developers.google.com/maps/documentation/places/android-sdk/legacy/place-details) and [Place types/address component types](https://developers.google.com/maps/documentation/places/android-sdk/place-types).

#### Autocomplete request/session management — implemented locally in Phase 2

The legacy flow ran only after pressing Search, omitted session tokens, serialized location through `LatLng.toString()`, and used an unusually narrow 75-meter bias. Phase 2 replaces it with the native Places SDK (New), one token per selection session, cancellable requests, and an optional 15-mile circular bias only when a real origin exists. Live Cloud authorization and request-matrix verification remain open.

The refreshed flow should:

- Debounce text input, generally 300–500 ms.
- Wait until at least three characters before requesting suggestions.
- Cancel stale searches with `flatMapLatest` or equivalent structured concurrency.
- Create a unique autocomplete session token per user search/selection session.
- Reuse that token for the selected-place details request, then discard it.
- Request only required place fields with field masks.
- Use a location bias, not an overly restrictive tiny radius.
- Allow useful search when location permission is unavailable.
- Represent loading, empty, error, and retry states in UI state rather than logs alone.

Autocomplete and billing guidance: <https://developers.google.com/maps/documentation/places/android-sdk/place-autocomplete>

### P1 — Google Maps modernization

#### A partial Maps Compose fork is stored in application source

There are 27 files under `app/src/main/java/com/example/favoriteplaces/features/maps`, totaling roughly 184 KB. They are derived from Google's Maps Compose project, use a mixture of application and `com.google.maps.android.compose` package names, and exist while the normal Maps Compose dependency is commented out.

Impact:

- The app owns fixes, compatibility work, and behavior changes that should come from the maintained library.
- A Compose or Maps SDK upgrade is more likely to break copied internals.
- Mixed package ownership makes the dependency boundary difficult to understand.

Recommended action:

- Replace copied implementation files with the official `com.google.maps.android:maps-compose` dependency.
- Migrate the two actual map screens against public APIs only.
- Remove copied files only after both map flows pass regression testing.
- Do not change database code as part of this step.

The official library was at 8.5.0 when Phase 3 implementation began: <https://github.com/googlemaps/android-maps-compose/releases>

#### Maps SDK is several major versions behind

The app uses Maps SDK 18.1.0. Maps SDK 20.0.0 is current during this audit. The upgrade should be driven through the compatible Maps Compose release instead of separately pinning conflicting transitive versions.

Release notes: <https://developers.google.com/maps/documentation/android-sdk/release-notes>

### P1 — reliability defects

#### Null location can crash

In `AddNewFavoriteViewModel.getCurrentLocation()`, a null `Location` is logged, then `location.latitude` and `location.longitude` are dereferenced immediately afterward.

Required behavior:

- Treat null as a normal unavailable-location result.
- Keep search usable without location.
- Offer retry and explain how enabling location improves nearby ranking.

#### City extraction safety — implemented in Phase 2

The removed legacy `convertPredictionToFavorite()` split `formattedAddress` on commas and read element 1. Phase 2 now resolves ordered structured component types and explicitly refuses to invent a city from display text.

Required behavior:

- Read locality/administrative components from structured place address components.
- Provide a safe empty/unknown result when structured locality data is absent; never derive locality from the display address.
- Never reject or crash while saving a valid place because a city component is absent.

This can initially be fixed without changing the database schema by continuing to write the resulting display city into the existing `city` column.

#### Save can race place-detail loading

The selected prediction and its resolved address/coordinates live in separate mutable state values. Saving should be disabled until the selected-place details have completed successfully, or the selected details should be represented as one immutable value.

#### City map initial camera is race-prone

`CityMapViewModel` starts collecting cached items asynchronously and immediately checks the still-null value outside the collection. The initial location can remain `(0,0)`. An empty `mapItems` list would also make index 0 unsafe.

Required behavior:

- Derive camera state from the first non-empty map-items emission.
- Model empty data explicitly.
- Pass stable navigation arguments rather than relying on an in-memory singleton cache where practical.

#### Forced null assertions are avoidable

The project contains 14 `!!` assertions, including favorites IDs, rating/content fields, city map data, and edit state. Several represent reachable incomplete/loading states.

Required behavior:

- Make persisted fields non-null where the existing schema/data contract already supports that.
- Handle nullable IDs until Room has inserted an entity.
- Model loading/not-found states explicitly.
- Do not change a Room column's nullability without inspecting real data and adding a tested migration if required.

#### Geocoder work may block and uses fragile indexing

`getAddress()` calls `Geocoder.getFromLocation()` inside a coroutine launched on the ViewModel scope without explicitly moving blocking work off the main dispatcher, and accesses result index 0 through nullable chaining.

Required behavior:

- Prefer the Places result's structured address data when available.
- If platform geocoding remains necessary, use the modern asynchronous API where supported with a compatibility implementation for older devices.
- Handle no-result, network, and service-unavailable cases.

#### ViewModel scope is manually cancelled

`AddNewFavoriteViewModel.onCleared()` calls `viewModelScope.cancel()`. The lifecycle owns this scope and cancels it automatically. Individual owned jobs may be cancelled, but cancelling the lifecycle scope manually is unnecessary.

### P1 — state and data-flow problems

#### Favorites screen can trigger repeated refresh work

`LaunchedEffect(state)` invokes `updateCitiesAndColors()`. That method updates state, which can retrigger the effect. Even where Compose equality prevents an infinite loop, unrelated state changes cause unnecessary database work.

Required behavior:

- Expose a single lifecycle-aware `StateFlow<FavoritesUiState>`.
- Derive grouped views from database flows with `combine`, `map`, and `stateIn`.
- Remove UI-driven refresh loops.

#### City/color grouping creates query fan-out

For every city, the ViewModel retrieves colors and then queries favorites for each city/color pair. This is an N×M query pattern and recomputes frequently.

Required behavior:

- Observe favorites once and group them in memory for this dataset, or add a single purpose-built Room query.
- Ensure ordering is stable and testable.

The replacement data flow must support combined filters for color, place type, city/area, distance from the current location, the independent favorite-heart flag, and rating without issuing a separate database query for every filter combination.

#### State systems are mixed

The add screen combines Compose `mutableStateOf`, LiveData, a mutable Boolean, `MutableSharedFlow`, and callback-based Tasks. The result is difficult to reason about during cancellation and configuration changes.

Recommended direction:

- One immutable UI-state data class exposed as `StateFlow`.
- A separate event/effect flow only for one-time navigation and snackbar actions.
- Convert Google Tasks to cancellable suspend operations where appropriate.
- Use `collectAsStateWithLifecycle()` consistently.

#### Map selection is passed through an in-memory cache

The selected city map data is stored in a singleton repository before navigation. This can be lost during process death and is harder to restore.

Recommended direction:

- Navigate with a stable city or filter identifier.
- Have the destination query Room through `SavedStateHandle`.
- Pass only IDs/keys, not mutable data objects.

### P2 — architecture and maintainability

#### Domain entity contains UI and serialization concerns

`Favorite` is simultaneously a Room entity and domain model, imports Compose colors, exposes ARGB theme defaults, and places a Gson helper in the same file.

Long-term direction:

- Preserve the existing table with a database entity compatible with version 1.
- Map that entity to a domain model.
- Keep theme colors and JSON helpers outside the persistence model.
- Introduce this separation without renaming or recreating the table.

#### Mixed Material 2 and Material 3

Screens use Material 2 `Scaffold`, typography, swipe components, and controls alongside Material 3 theme objects and surfaces. This creates inconsistent styling and complicates dependency upgrades.

Recommended action:

- Migrate one screen at a time to Material 3.
- Use Material 3 top app bars, search, cards, sheets, snackbars, and adaptive components.
- Replace deprecated Material 2 interactions only after behavior tests exist.

#### Redundant and unused dependencies/configuration

Likely cleanup candidates include:

- Glide and its annotation processor; no Glide usage was found.
- Data Binding; the app UI is Compose-based and no binding usage was found.
- Retrofit/Gson/legacy DTOs after the Places SDK migration.
- The alpha OkHttp logging interceptor.
- Duplicate or unnecessary direct dependencies supplied transitively by Maps Compose.
- The injected-but-unused `PlacesClient` becomes used during migration rather than removed.

Dependency removal should happen only after successful compilation and regression testing.

#### Dependency versions are scattered

Versions are embedded across root and module Gradle files, and stable/alpha lifecycle artifacts are mixed.

Recommended action:

- Add `gradle/libs.versions.toml`.
- Upgrade in compatible groups rather than choosing every latest version independently.
- Prefer stable artifacts unless a specific feature requires a preview.

#### Navigation is stringly typed

Routes are manually concatenated with query parameters, including both favorite ID and color. The edit destination can load its own data by ID, making the color parameter unnecessary and potentially stale.

Recommended action:

- Use typed Navigation Compose destinations if supported by the chosen stack.
- Pass only the favorite ID.
- Define explicit not-found behavior.

#### Preferences should move from SharedPreferences

Only view-mode preference is currently stored, and the helper is synchronous. Preferences DataStore would provide a typed Flow and compose naturally with screen state. This is not a SQLite change and must not be coupled to the Room migration work.

### P2 — user experience and visual design

#### What should remain

- Fast local access to saved places.
- Search, notes, rating, the independent favorite-heart flag, grouping, and city maps.
- The established five-color personal grouping system and every existing saved color value, without assigning meanings to the colors.
- Offline availability of already-saved information.
- A simple personal tool rather than a social network.

#### Current design concerns

- Full-card saturated colors dominate place information and reduce visual hierarchy, although the personal color grouping must remain prominent enough to scan quickly.
- Many important actions use small icon-only targets.
- Sort, grouping, view mode, color, Favorite heart, and rating are exposed together as radio-button controls, creating a dense configuration panel.
- Search requires a separate button and embeds individual maps inside result cards, which is visually and computationally heavy.
- Loading, empty, permission-denied, and network-error states are incomplete.
- Strings and content descriptions are largely hard-coded rather than resources.
- Dark theme, dynamic color, adaptive layout, and modern edge-to-edge behavior need verification.

#### Proposed information architecture

All three top-level screens use the same visual order wherever applicable: top app bar, primary input or location context, minimal text actions such as Filter/Sort, result heading/count, then content. Avoid secondary headlines, permanent filter-chip rows, and duplicated actions when the app bar or bottom sheet already provides them.

**Bottom navigation**

- **Saved** — search, sort, filter, open, and edit the existing collection.
- **Find** — search Google Places and save a new place. “Find” is preferred to “Add” because it describes a persistent destination rather than an isolated action.
- **Nearby** — decide where to go now using current area, radius, list, and map views.
- Place details/edit is a child destination opened from a result or saved card, not a bottom-navigation item.
- Map is part of Nearby, not a duplicate top-level destination.

**Saved**

- Material 3 top app bar titled **Places**, followed by a search field limited to already-saved places. Retain **Saved** as the bottom-navigation label so the destination stays immediately understandable.
- Use one text-only **Filter** action above the result heading instead of permanent Type, City, Color, and Favorite controls.
- Open a consolidated modal bottom sheet containing Type, City, unlabeled Color swatches, and Favorite-heart sections plus **Clear all** and **Show places** actions.
- Keep active filter details inside the sheet; optionally append a small selection count to the Filter text without adding another chip row.
- Keep sorting separate from filtering. Initial sort choices should include name, city, rating, Favorite, and newest/recently edited if a reliable timestamp is later added.
- Do not show near-me radius controls on Saved; those belong to Nearby.
- Use neutral Material 3 cards with a narrow leading color rail.
- Each card shows a top-aligned bold place name, complete address, and personal notes truncated to at most three lines in one continuous left column. City and type remain filter/detail fields rather than repeated card labels. The trailing actions form one predictable vertical stack: compact rating at the top, Favorite heart in the middle, and Map at the bottom, with equal visual center spacing and full Material touch targets for the two actions.
- Rating and the Favorite heart may appear as quiet trailing/footer metadata if they do not crowd the notes.
- Do not display invented meanings beside the color rail. The independent Favorite state appears as a heart; otherwise that space is more useful for notes. Color filtering uses the same five unlabeled swatches.
- Tapping a card opens Place details. Preserve swipe-to-delete with Undo rather than showing a delete button on every card.
- Provide a clear collection-empty state and a distinct no-filter-matches state that retains filters and offers targeted clearing.

**Find**

- Show the active search origin directly below the search bar, defaulting to current location/current area, with a clearly clickable **Change location** action beside it.
- Tapping Change location opens one reusable chooser directly to a city/address autocomplete field. A secondary **Choose on map** button switches to point/area selection.
- After a typed or map-selected origin is confirmed, show that label at the top and bias Places search around its coordinates.
- Search-as-you-type suggestions.
- Use a single text-only **Filter** action above the suggestion count. Place-type choices live in the Search filters bottom sheet rather than a permanent row of category chips.
- Do not repeat an additional marketing-style headline above the search box; the app-bar title already establishes the screen purpose.
- Location bias when allowed, with useful global search otherwise.
- Place-type selection before or during search, using friendly categories rather than exposing Google's full raw type list.
- Result rows rather than a separate interactive map in every card. Put a small, accessible map-preview icon immediately beside **Save** on each result.
- Tapping the map icon opens a focused map preview for that result, including the pin, address, and a confirm-save action. This lets users disambiguate similar names before saving without loading a permanent map in every row.
- Preserve the current quick-save behavior unless both users explicitly prefer a customization step. A new save uses the Settings default color and Favorite-off state, followed by an unobtrusive path to details; if color is offered during save later, show only the six unlabeled swatches and never force an invented meaning.
- Notes and rating remain optional so a place can be saved quickly.
- Disable save while details are loading and prevent duplicates visibly.

**Nearby**

- Treat Nearby as one location-centered decision screen with two explicit sources: **Our places** and **New places**. The compact header contains only this Material 3 single-choice pill—do not repeat a Nearby title. The header and unselected half use the current app color; the selected half is white. Default to **Our places** on a fresh/cold start and preserve a manually chosen mode while the destination remains alive in the primary-navigation back stack.
- **Our places** is the primary behavior. Observe every Room record, retain every valid saved marker, show the current/chosen location as a separate location marker, and sort the list by locally calculated distance. Do not issue a Google Nearby Search request in this mode.
- Do not silently hide saved places behind the discovery radius. Our-places mode initially shows **Any distance** so the map restores the old ability to see the whole saved collection and visually choose what is nearby. The Saved filter may narrow to a typed mile radius, place type, one or more unlabeled colors, the independent Favorite heart, and rating; clearing restores all saved places.
- **New places** keeps the existing Google Nearby Search workflow and its 10-mile default, explicit type/category filter, 1–20 result boundary, attribution, Save action, and safe API errors. Its type filter is a five-position centered word wheel containing Any plus a short user-controlled list. Add opens a focused local search over reviewed Google Table-A identifiers; configured choices can be removed there or from Settings, and both surfaces share one ordered DataStore preference. Typing does not contact Google. Entering this mode performs a search only when there is no current result for the selected origin/filter combination; switching List/Map or briefly viewing Our places must not create a duplicate billable request.
- Keep one compact secondary row below the source control: List/Map stays at the left, while the current city/name—or compact coordinates when no reliable city label is available—is a single clickable control at the far right. Tapping it retains the focused Change-location sheet with typed autocomplete and Choose on map. Changing location immediately recalculates Our-places distances locally and invalidates, but does not eagerly refetch, New-places results until that mode is active.
- Keep List/Map as a separate view choice without clearing either source's results or filters. Avoid placing two competing full segmented controls in one row: source is the prominent header pill, and the quieter List/Map control shares only the compact location row below it.
- Keep one text-only **Filter** action above the result count. Its modal sheet adapts to the active source and makes the source explicit in its title: saved filters for Our places, Google distance/type filters for New places. Mode-specific values do not overwrite each other.
- Our-places cards reuse the Saved hierarchy plus distance: exact color rail, bold name, address, truncated notes, heart, and an open/details action. A selected saved marker opens a sheet with name, distance, address, heart, **View place**, and **Directions** so choosing somewhere to go is the shortest path.
- New-place cards/markers retain Google name/type/address/distance, attribution, map confirmation, and explicit Save. A heart never creates an unsaved Google result.
- If location is unavailable, Our places still shows saved markers and records but omits distance; Change location remains available. New places explains that an origin is required instead of issuing an invalid request. Invalid saved coordinates remain in an accessible list with a concise map warning rather than disappearing without explanation.
- Use distinct counts and empty/error language: “12 saved places,” “3 saved places within 10 miles,” or “20 new places,” never the ambiguous “places nearby.”

**Place details/edit — final Phase 7 direction**

- Treat Place details as a focused child screen opened from a Saved card, not a fourth bottom-navigation destination. Use a compact selected-color header with Back, Favorite, and a clearly labeled Edit action; primary bottom navigation stays hidden on this child route.
- Preserve the screen's quiet, direct structure rather than dividing every property into cards. The core order is a bold content name, smaller optional place type, complete read-only address/map action, rating, and a notes-first body separated mainly by spacing. The current-color/down-caret control remains at the far right of the Notes heading for lower-screen reach.
- Keep the place name in content rather than the title bar so long names and large fonts can wrap. Tapping the lower color control expands a very slim, white, one-handed Apple-style picker around the trigger's exact swatch center: the closed swatch becomes the open focused item without visually moving, previous/current/next choices loop and vertically snap into a faint center band, neighboring choices fade, and a bottom **Done** commits the focused value once. The center position—not a special ring, checkmark, or invented label—identifies the selected color; the panel uses only a faint boundary and soft shadow, and every choice exposes a full touch target with stable position semantics.
- Change color and rating directly from details. Edit name/type in one compact explicit-save modal sheet. Notes are directly editable in a multiline field that consumes the rest of the details screen; a trailing **Save** button in the selected-color footer commits only notes and is disabled until the draft differs from Room. Preserve the draft through recreation, retain it after failures, and never rewrite another place field.
- Do not display status names beneath the swatches or imply that any color means Favorite, visited, wanted, or anything else. For accessibility, announce neutral position-based descriptions such as “Color 1 of 6,” not invented semantics.
- Favorite is a separate heart control in the details app bar. Toggling it must not change the selected color, and changing color must not change the heart state.
- Keep destructive deletion out of the primary form flow. Place it under an overflow action or at the very end of the scroll content and always require explicit confirmation; it must never sit beside Save.
- Stored place type is now an optional, user-owned field added by the tested Room 1→2 migration. Existing records remain null until edited and display no invented label on cards; the Saved filter exposes **Type not set** only inside its sheet. No old value is inferred or rewritten.

**Deferred rich notes editor — researched direction, not current scope**

- Keep the current inline notes field as the fast everyday editor. A later expand action may open a dedicated full-screen writing surface with an editing toolbar; do not crowd the details screen with formatting controls.
- Initial formatting scope is **Bold** and **Bulleted list** only. Treat a Word-like live editor as a medium-sized feature, not a cosmetic text-field change: it must own selection ranges, cursor behavior, toolbar state, paste, deletion across styled boundaries, multiline bullets, keyboard/insets, accessibility, recreation, and lossless save/readback tests.
- Compose `TextFieldState` is the preferred future input foundation because it owns text, selection, composition, and restoration. `AnnotatedString` supports range styles and native bullet annotations for rendering, but formatted *editing* still needs an app-owned document model and selection-to-format commands.
- Before implementation, prototype and choose a versioned storage contract. The safest baseline keeps every existing plain note unchanged and either (a) stores a Markdown-compatible bold/list subset with explicit escaping and compatibility tests, or (b) adds an explicit tested Room migration for structured formatting while retaining a plain-text fallback. Never silently reinterpret or overwrite existing note text.

Primary references: [Android state-based Compose text fields](https://developer.android.com/develop/ui/compose/text/user-input), [Compose `AnnotatedString`](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/AnnotatedString), and [Compose bullet-list APIs](https://developer.android.com/reference/kotlin/androidx/compose/ui/text/AnnotatedString.Builder).

**Nearby map view**

- Both sources use marker selection and a source-appropriate bottom sheet for the active place.
- Our places keeps all valid saved markers loaded, uses exact saved colors plus a distinct origin marker, starts with the current area at a useful zoom, and offers an explicit fit-all-saved action. New places may fit the bounded Google result set.
- Apply the active source's same filters to List and Map so switching views never changes the result set.
- Keep Favorite independent from marker color and retain an accessible List representation for every result, including saved records whose coordinates are invalid.

**Saved single-place map**

- Use a compact app-color header whose title is the saved place name, not a generic map label. Do not show the primary navigation footer on this child screen.
- Keep the embedded marker and stable Room-backed ID route. Provide a clear directions action that hands exact coordinates and the Google place ID, when available, to the recommended Google Maps directions URL. This is an external intent only and adds no Places/Maps API request or key requirement.

#### Preserve color and Favorite as separate signals

The original five colors plus the added darker blue are an established, intentionally open-ended personal grouping system. The app must not name them or assign a workflow meaning. The stored integer color value remains the compatibility source of truth. Favorite is separately represented by the existing `isFavorite` boolean and heart icon; it is not a color and is not tied to any one color.

Modern presentation options include:

- A narrow colored leading rail on neutral Material 3 cards is the selected baseline direction.
- Full-card color fills are not the preferred direction; color should accent the item rather than dominate it.
- When several places share one grouped card, separate adjacent records with a thin neutral divider inset from both card edges. Keep the gap between separate grouped cards compact while preserving a clear card boundary.
- Color filters, Settings, and details controls show the six swatches without visible names.
- Matching colored map markers plus separate selection and Favorite-heart indicators.
- A compact swatch-based color filter available from both list and map views.

Because the colors deliberately have no app-defined meaning, accessibility semantics should identify their stable position—such as “Color 1 of 6, selected”—without inventing a label. The Favorite heart must always have explicit Favorite/Remove favorite semantics.

#### Place-type model and existing favorites

The current version-1 `Favorite` table does not store a normalized place type. Reliable offline type filtering therefore requires a later additive schema change; it must not alter or discard any current column.

Proposed safe approach:

1. First ship the platform/API modernization with the version-1 schema unchanged.
2. Add one nullable user-owned `placeType` field through an explicit Room 1→2 migration. Existing rows remain byte-for-byte intact in every old column and receive null rather than a guessed category.
3. Populate the editable initial value for new saves from Google primary/type metadata already returned by the approved request; do not add another details request or parse the formatted address.
4. Keep Saved filtering based on exact user-owned values and expose **Type not set** for legacy/null records.
5. Keep Google discovery filters separate from stored user-owned labels. New Places uses only reviewed Table-A identifiers from a local searchable catalog, with a short configurable picker list shared by Nearby and Settings.
6. Never silently rewrite an existing user-confirmed saved type when Google changes its taxonomy.

#### Location-aware saved-place filtering

Every existing favorite already stores latitude and longitude, so radius filtering and distance sorting can be implemented locally without a database migration or a network request.

Required behavior:

- Calculate straight-line distance using a well-tested geographic distance function.
- Recalculate when the device location changes materially, not continuously on every GPS update.
- Clearly show the unit and whether results are sorted or restricted by distance.
- Support city/area filtering without device location.
- Support choosing an area on the map when the user does not want to grant location permission.
- Do not exclude records with questionable legacy coordinates silently; expose an “Unknown location” state or repair option.
- Keep last-used filters as a preference, with an obvious reset action.

### P2 — accessibility, privacy, and resilience

- Move user-facing text and content descriptions into string resources.
- Verify minimum 48 dp touch targets.
- Provide meaningful semantics for rating controls, the Favorite heart, map markers, and swipe-to-delete.
- Preserve each stored color exactly while describing color controls neutrally by stable position rather than invented meaning.
- Ensure color filter controls expose the swatch and a neutral accessibility description such as “Color option 2 of 5.”
- Support font scaling and avoid clipped addresses/notes.
- Treat location as optional and request it in context, not as a prerequisite to entering the add screen.
- Explain why location improves results before requesting permission.
- Avoid logging prediction data, addresses, or other user-place activity in release builds.
- Add explicit network timeout and retry behavior.

### P2 — testing and delivery gaps

The current test files only assert `2 + 2 = 4` and the package name. There is no test coverage for the data the app must preserve.

Required test layers:

- Room schema and migration tests, including real-shaped version-1 fixtures.
- DAO tests for insert, update, delete/undo, duplicate place IDs, ordering, grouping, color, Favorite heart, place type, combined filters, and nullable legacy values.
- ViewModel tests for search cancellation, errors, permission denial, null location, selection, duplicate saves, and save races.
- Unit tests for distance calculations, radius boundaries, missing/invalid coordinates, and combined-filter behavior.
- Compose tests for empty state, filters, filter presets, search, editing, delete/undo, unlabeled color swatches, the independent Favorite heart, and accessibility semantics.
- Maps smoke tests on at least one physical device and one Google Play-enabled emulator.
- Upgrade test: install the existing production-signed version, populate data, then install the candidate build over it and verify every record.
- Process-death/restoration tests for edit and city-map destinations.

## Database migration and backup protocol

This protocol is mandatory whenever a change might interact with stored data.

### Establish the version-1 baseline

1. Export Room schemas by enabling `room.schemaLocation`.
2. Obtain or construct a version-1 database fixture whose rows cover:
   - All five existing colors.
   - Favorite-heart on and off.
   - Ratings from 0 through 5, including any nullable legacy value that can exist.
   - Empty and populated notes.
   - Multiple cities.
   - Duplicate-looking names with distinct place IDs.
   - Valid coordinates, missing/empty place IDs if legacy data allows them, punctuation, Unicode, and long text.
3. Record row counts and a deterministic digest of all user columns before migration.
4. Add `MigrationTestHelper` tests that open the fixture at version 1.

### Rules for each schema version

1. Make the smallest possible additive change.
2. Increment the Room version exactly once per released schema.
3. Implement explicit SQL in `Migration(oldVersion, newVersion)`.
4. Register every migration with `addMigrations(...)`.
5. Test the direct step and the complete version-1-to-latest chain.
6. Verify all original rows, IDs, place IDs, text, ratings, flags, colors, cities, and coordinates after migration.
7. Review generated schema JSON in source control.
8. Reject the release if Room reports an identity-hash mismatch or if any pre-existing value changes unexpectedly.

### Device rollout procedure

1. Determine whether the app is distributed through Google Play or direct APK installation and locate the matching signing key.
2. Add an in-app JSON/CSV export or another verified backup mechanism before any schema-changing release.
3. Export both devices and confirm the backup files can be read.
4. For the one-time legacy transition, install the replacement beside the legacy app and import/verify its captured records. For every later replacement update, install over the existing replacement app on a test device/database first—never uninstall or clear it.
5. Validate total saved-place count and spot-check every city, note, rating, color, Favorite-heart flag, and map location.
6. Upgrade one household device and use it normally before upgrading the second.
7. Retain the prior production artifact and backups. Android database migrations are forward-only; app binary rollback after a schema change is not assumed safe.

## Implementation sequence

The phases below are strategic outcomes, not a precommitted task backlog. Detailed implementation work is maintained only for the active phase in the rolling plan. Later phases remain intentionally unexpanded until the prior phase gate passes; their order may be adjusted from evidence gathered during implementation, but no phase may bypass the database and upgrade-safety requirements.

### Phase 0 — protect the existing installation

- Identify production signing/App Signing configuration.
- Capture current Play Console status, application ID, version code, enabled APIs, API keys, and restrictions.
- Add standard `.gitignore` without altering local secrets.
- Pin a supported JDK and establish a reproducible baseline build.
- Export Room version-1 schema and add migration-test dependencies.
- Create populated v1 fixture and preservation tests.
- Design and implement a user-visible backup/export path.

**Gate:** No later phase starts until side-by-side legacy import plus same-signer replacement reinstall preserve every fixture/imported record.

### Phase 1 — platform/toolchain modernization

- Upgrade Gradle/AGP/Kotlin/Compose in controlled compatible increments.
- Move to compile API 37 and target API 36.
- Update target-SDK behavior handling and edge-to-edge layout.
- Move versions to a version catalog.
- Preserve database version 1 if the schema is unchanged.
- Run unit tests, lint, assemble debug/release, and install-over-existing tests after each upgrade group.

**Gate:** App launches, reads all old data, edits/saves/deletes/undoes correctly, and produces a correctly signed candidate release.

### Phase 2 — Places API (New)

- Enable Places API (New) and prepare restricted Android credentials.
- Inventory and baseline every existing Google request, requested field, call count, failure/status category, restriction, and billing behavior without logging user-place data.
- Select Autocomplete, Text Search, Nearby Search, or map-point geocoding per workflow; document why each method is used instead of forcing every flow through autocomplete/details.
- Initialize the new Places SDK.
- Implement SDK-backed autocomplete with session tokens and field masks.
- Implement SDK-backed Text Search for submitted free-text discovery and Nearby Search for radius/type discovery where the audit confirms they fit.
- Implement selected-place details and structured city extraction.
- Remove comma-, regex-, and position-based parsing of formatted addresses; preserve the display address verbatim and read locality from typed address components.
- Add fake-client request-contract tests and a live-device usage/cost smoke matrix before switching production behavior.
- Make location optional and remove null/race failures.
- Run old and new paths behind an internal switch until behavior is verified.
- Remove legacy Retrofit Places code and DTOs after validation.

**Gate:** Search works with location granted, approximate-only, denied, and unavailable; duplicate prevention and save accuracy are tested; no production path parses formatted addresses; request field masks/session lifecycles are under contract test; and API usage, errors, latency, attribution, quotas, and billing metrics are reviewed.

### Phase 3 — official Maps Compose

- Add the maintained Maps Compose dependency.
- Migrate add-place preview and city map.
- Fit marker bounds and handle empty/single-marker cases.
- Replace transient map-cache navigation with stable Room-backed route IDs/filters so both maps restore after process death.
- Remove the copied Maps Compose source only after parity tests.

**Gate:** Both maps work across supported Android versions, rotation, process recreation, dark mode, and denied location.

### Phase 4 — state and architecture cleanup

- Consolidate each screen around immutable `StateFlow` UI state.
- Eliminate favorites refresh loops and grouped-query fan-out.
- Verify and build on the stable Room-backed map routes completed in Phase 3.
- Move view preference to DataStore.
- Separate database entities, domain models, and UI theme concerns without changing the existing table unexpectedly.
- Remove forced null assertions through explicit state modeling.

**Gate:** DAO/ViewModel tests cover all existing behavior and process restoration.

### Phase 5 — Material 3 product refresh

- Build refreshed Saved, Find, Nearby list/map, and Detail/Edit experiences.
- Apply the shared screen hierarchy—app bar, primary context/input, text actions, result heading, content—across Saved, Find, and Nearby.
- Add bottom navigation with Saved, Find, and Nearby as distinct destinations.
- Replace dense sort radio controls with filters, menus, and segmented controls.
- Preserve the open-ended color system with a narrow card accent, unlabeled swatch filters, neutral accessibility semantics, matching map markers, and a separate Favorite heart.
- On Saved, use one text-only Filter action and a consolidated Type/City/unlabeled-Color/Favorite bottom sheet with Clear all.
- Implement the selected Option B / Balanced card direction with soft spacing and consolidated filter sheets instead of permanent filter-control rows.
- Add a reusable Change location sheet with location autocomplete and a Choose on map path; use it from Find and Nearby while keeping current device location as the initial default.
- Put only the Our places/New places source pill in the Nearby app-color header. Keep List/Map and the clickable compact location in the row below, then one text-only Filter action beside the result count.
- Move validated distance controls into the adaptive Nearby filter sheet: Any distance for saved places and 10 miles for Google discovery.
- On Nearby Our places, add combined unlabeled-color, Favorite-heart, place-type, optional radius, rating filters, and local distance sorting; New places exposes only its independent Google distance/type controls.
- Preserve active Nearby filters when switching between list and map.
- Add loading, empty, error, retry, offline, and permission states.
- Complete accessibility, dark-mode, dynamic-color, and font-scale testing.

**Gate:** Existing capabilities remain available and all upgrade/data-preservation tests pass.

### Phase 6 — place-type schema and legacy categorization

- [x] Use one free-form, user-owned display type rather than a fixed category taxonomy.
- [x] Add nullable `placeType` through an explicit Room 1→2 migration that leaves every version-1 column and value intact.
- [x] Populate types for new Find/Nearby saves from metadata already returned by Google, without address parsing or a higher-cost Find details field.
- [x] Provide sparse Edit review, neutral card display, and exact/**Type not set** filtering for existing places.
- [x] Keep existing color values unchanged, unlabeled, and independently filterable from the Favorite heart.
- [x] Pass backup compatibility, migration-chain, clean build, device, and signed install-over-data gates.

This schema phase is complete. Existing offline favorites remain valid with a null type and can be categorized gradually by either user.

## Verification checklist for every release

- [x] Same replacement `applicationId` as the installed replacement app.
- [x] Correct permanent production signing identity.
- [x] Version code is 3 for the replacement 3.0.0 release.
- [x] No destructive Room migration method anywhere in the project.
- [x] All Room migration tests pass from version 1 to latest.
- [x] Install-over-existing test passes without uninstalling or clearing data.
- [x] Favorite count and all stored values remain unchanged unless a reviewed migration explicitly adds derived data.
- [ ] Backup/export created and validated for both devices before a schema-changing rollout.
- [x] Search tested with location granted, approximate, denied, and unavailable.
- [x] Nearby saved-place filtering and distance sorting tested locally using stored coordinates, including no-location behavior with no Google request.
- [x] Combined color, Favorite-heart, place-type, and location filters return consistent results in list and map views.
- [x] Every existing stored color retains its exact value without the app assigning or migrating a meaning.
- [x] Maps and markers tested with zero, one, and many favorites.
- [x] Offline saved-data browsing works.
- [x] Release build remains non-minified until rules are deliberately verified.
- [ ] API key has Android application restrictions, conservative quotas, and alerts; no key is logged or committed is already verified.
- [ ] Production and debug key restrictions match their package/signing fingerprints, and conservative quota/budget alerts are active.
- [x] Unit, migration, UI, lint, and release-build checks pass.
- [ ] One-device staged household rollout succeeds before the second device is upgraded.

## Optional future decisions

- The six existing colors retain their stable order and remain visibly unlabeled.
- Place type remains user-owned text; existing uncategorized favorites are reviewed gradually through Edit rather than guessed or bulk rewritten.
- Nearby Our places defaults to the complete saved collection at Any distance; New places independently defaults to 10 miles. A later product decision may change the discovery maximum.
- Photos, current opening hours, website/phone, price level, or additional categories remain optional because they add fields, UI density, and potentially billing cost.

## Definition of done

The modernization is complete when:

- The app targets current Google Play requirements and builds reproducibly.
- Legacy Places HTTP endpoints and copied Maps Compose internals are gone.
- Google credentials are appropriately restricted.
- Search, save, edit, delete/undo, organization, and city maps work under normal and failure conditions.
- The UI is coherent Material 3, accessible, responsive, and supports dark mode while preserving open-ended color grouping and the independent Favorite heart.
- Users can combine place type, color, Favorite heart, and current area filters and receive the same saved-place result set in list and map views.
- Tests cover persistence, migrations, primary ViewModels, and critical user flows.
- Most importantly, each legacy dataset is captured, imported, and verified in the side-by-side replacement, and every later replacement update preserves every saved place, note, rating, Favorite-heart flag, color, city, place ID, and coordinate through explicit migrations when needed.

## Modernization implementation outcome — 2026-08-22

The engineering modernization is complete. The app is a functional, local-first Material 3 Android replacement with Saved, Find, and Nearby navigation; official current Maps Compose and Places SDK integrations; consolidated filtering; map confirmation and area selection; independent Favorite hearts and unlabeled color accents; sparse editing; versioned export/import; and an explicit Room 1→2 migration for optional user-owned place type. Settings now persists independent app-chrome and new-place default colors through DataStore, with light blue as the fresh-install default plus darker blue and darker purple choices. Single-place maps use a compact place-name header, omit the app footer, and hand directions to Google Maps without an additional API call. It has no Firebase or separate backend.

The final clean gate passed 58 JVM tests, 20 device tests, migration tests from the earliest schema, zero-error lint, and signed debug/release/test assembly. A permanent-signed release reinstall retained both representative records and the newly edited type without clearing data. Exact hashes and the error/logging/API-cost audit are recorded in the Phase 6 evidence section of the rolling plan.

Craig's first household technical migration now passes: 26 legacy records were captured, imported into the empty permanent-signed replacement, and exported back from the phone with exact record/digest equality. All requested phone numbers are retained separately for future schema planning, and three businesses that moved retain their saved-address map coordinates instead of being silently relocated. Craig's legacy app remains installed pending his visual/map sign-off. The second household phone still needs the same independent capture/import/readback process.

The remaining deployment safeguards are user/Cloud-console work rather than application-code work: complete and sign off the second household migration, verify an independent recovery copy of the permanent signer, and add Android package/certificate restrictions plus quotas/alerts to the Google key. Keep each legacy app installed until that phone's imported record count and content have been checked.

The later-approved Phase 7 details/editing refinement is complete. Saved cards and grouped rows now open one Room-observed details destination; a focused name/type sheet plus an inline notes canvas replaced the full Edit form; notes commit from the selected-color footer while color, rating, and Favorite use isolated updates; and the requested lower, one-handed Apple-style color wheel passed its device and signed-update gates without changing Room version 2. A follow-up presentation audit centralized all ordinary type labels on neutral `onSurfaceVariant` text across details, map sheets, Nearby cards, Google results, and action sheets; the word wheel now uses a neutral selection band, while interaction-state controls retain normal selected/focused styling.

Phase 8 added independent app-chrome and new-place color preferences, the sixth darker-blue choice, and focused directions maps. Phase 9 restored Nearby's household-first purpose: it cold-starts on the complete local **Our places** collection, calculates distances and filters without a Google request, maps saved markers around the current/chosen location, and puts lazy cached **New places** discovery behind a compact title-free app-color/white source pill. The final 73-JVM/25-device clean gate, live request-count audit, and same-signed reinstall preserved Room version 2 and all visible records. No Firebase or separate backend was introduced. No further implementation phase is open; the rich-notes/card-template ideas remain optional future decisions.

Phase 10 replaced the fixed New Places type chips with the approved centered word wheel. Any type plus four broad defaults remain calm on first use; Add and Settings open the same searchable Google Table-A manager, and every configured entry can be removed. Ordered choices persist in DataStore, arbitrary/stale IDs are rejected, an intentionally empty list remains valid, and typing never creates a Google request. The final 82-JVM/27-device gate and same-signed reinstall preserved Room version 2 and all visible records. No further implementation phase is open; rich notes, card templates, and arbitrary keyword Text Search remain optional future product decisions.

The post-modernization reliability audit is also complete. It hardened Room conflict/delete semantics without a schema change, corrected stale search/location work and Nearby cache ownership, made delete/Undo and swipe failure-safe, added retry/fallback handling, centralized safe diagnostics, and advanced the tested target to Android 17/API 37. The final household-release correction locks both the Android launch window and Compose content to the approved light appearance even when the phone uses system dark mode, with explicit readable edge-to-edge system-bar styling. Its final 88-JVM/31-device/18-tool gate, unchanged Room schema hashes, signed install-over-data, and all findings are recorded in [post-modernization-quality-audit.md](post-modernization-quality-audit.md). No new implementation phase is open.

Phase 11 and Phase 12 complete the phone/action implementation. Room v3 adds a nullable phone and an explicit Google refresh review; Room v4 adds a separate nullable Google primary category so reservation eligibility never depends on the user's editable type. Find selection and explicit New Places Save capture phone/category metadata only at user-committed boundaries. Saved cards remain compact until their inset-divider disclosure opens; the final expanded row fits **Call · Reservation · Web** above the divider with the centered caret below. Release 3.2.1 keeps that caret visible on migrated records and makes a lazy two-field Google lookup only for the opened place when phone/category metadata is missing, then stores only the missing values—there is no bulk card refresh. Call hands off to the permission-free system dialer, restaurant-category records can open a prefilled OpenTable search with only the place name copied as fallback, and Web opens an explicit Google search using the saved name/address without an additional Places request or write. Backup v4 remains additive and reads v1/v2/v3; a private 26-place v4 enrichment is ready for the physical household rollout. The permanent-signed emulator update preserved all existing release data; the physical-phone install/import/export comparison remains a deployment step, not unfinished application code.

Release 3.2.3 completes the app-chrome edge-to-edge treatment: the user's selected app color now draws behind Android's transparent status bar as well as the header and navigation footer. The clock and system icons automatically use dark or white content according to the same contrast rule as the app header. This is presentation-only—Room and backup stay at version 4, no saved field changes, and the signed install-over-data gate retained all seven release-emulator records and the original first-install timestamp.

Release 3.2.4 corrects Change location map gestures without expanding the data or API surface. Find and Nearby keep the compact autocomplete bottom sheet, but both now open the same fixed full-screen map picker. The user pans or zooms the map beneath a stationary center pin and confirms that center point; the map no longer competes with a draggable sheet for vertical gestures. Close, Back, and Search instead return without changing location. The picker follows the approved light appearance and app-color system chrome. Room and backup remain version 4, Google requests and fields are unchanged, and the signed install-over-data gate retained all seven release-emulator records and the original first-install timestamp.

Release 3.2.5 supersedes only 3.2.4's center-pin selection behavior while retaining its fixed, gesture-safe picker. There is no stationary pin: the user pans freely, zooms by pinch or the restored visible +/− controls, and taps the exact map point to drop or reposition a real marker. **Use location** remains disabled until a marker has been dropped, so merely moving the camera cannot accidentally change the search origin. Close, Back, and Search instead still return without changing location. This correction changes no Room schema, backup format, Google field/request contract, permission, or saved record. The signed 3.2.4→3.2.5 install retained the original first-install timestamp and all seven release-emulator places.

Release 3.2.6 clarifies the two distinct Nearby concepts. **Our places** has no arbitrary distance cutoff by default: it shows the complete saved collection, sorted nearest-first when an origin is available. Opening its map automatically performs the same full-collection framing as **Fit all**, including the origin and every visible saved marker; the button remains available to restore that framing after manual pan/zoom. Distance remains an optional Filter choice for the explicit “within X miles” use case, alongside type, color, Favorite, and rating filters. Both Nearby maps now stay light with the rest of the light-only app, and singular/plural place counts are correct. No database, backup, Google-request, permission, or saved-data behavior changed.

Release 3.2.7 supersedes 3.2.6's worldwide default after owner review. **Our places** now requires location filtering before producing list or map results: current/chosen origin, then a 25-mile default boundary, then optional type/color/Favorite/rating filters. The distance remains directly editable from 1–100 miles, but there is no unlimited-distance choice on Nearby. The map receives only that filtered projection, automatically frames the origin plus those results, and labels the recovery control **Fit results** so it cannot imply that it will pull in the complete worldwide database. If no location is available, the screen requests one instead of showing unrelated records. This remains a local Room calculation with no Google request and no database, backup, permission, or saved-data change.

Release 3.2.8 aligns the Nearby saved-list cards with the main Saved cards. Their 6dp place-color accent is drawn from the measured card content height instead of a fixed 144dp child, so it always reaches the rounded bottom edge after address wrapping, note previews, distance, or missing-location messaging changes the card height. The content retains its original 20dp inset. A device regression compares the card and accent-layer bounds to prevent a return to fixed-height styling. This is presentation-only with no data or API change.
