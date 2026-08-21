# Places App Modernization Plan

**Status:** Approved strategic direction; execution tracked in the rolling plan  
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
9. Do not change `applicationId` from `com.example.favoriteplaces`; doing so would install a separate app and strand the existing app data.
10. Do not change release signing identity. Android will reject an in-place upgrade signed with a different key.

No feature, library upgrade, or cleanup is complete if it risks violating these rules.

## Executive assessment

The app should be modernized in place rather than rewritten. Its core product idea remains strong: search for a place, save it, personalize it, browse favorites, and view groups on a map. Compose, Room, Hilt, repository abstractions, use cases, and ViewModels provide a usable foundation.

The most urgent work is platform and Google API compatibility. The project is primarily a 2023 Android stack and currently combines three Google integration approaches:

- Maps SDK for Android.
- Places SDK for Android 3.2.0, initialized through a now-deprecated entry point.
- Direct Retrofit calls to legacy Places HTTP endpoints.
- In addition, 27 Maps Compose source files are copied into the application instead of consuming the maintained library normally.

The recommended direction is to keep the app and database, modernize the build in controlled steps, replace the legacy Places path with Places SDK for Android (New), adopt the official Maps Compose dependency, then refresh the UI.

## Confirmed product requirements

The following requirements were confirmed after the initial audit and are part of the product contract:

- Color is meaningful user data, not decorative styling, but the app must not assign names or fixed meanings to the five choices. Each person may use a color however they want. Existing colors and their stored ARGB values must be preserved exactly.
- The refreshed design may present color more subtly and consistently, but it must remain immediately recognizable and filterable as an unlabeled swatch.
- **Favorite is independent from color.** It is represented by the existing heart/`isFavorite` flag, can coexist with any color, and must be filterable separately.
- Saved places need strong place-type filters so restaurants, coffee shops, breweries, parks, attractions, and other useful categories can be found quickly.
- Saved places need location-aware filtering so the users can quickly answer “what have we saved near where we are right now?”
- Filters must compose: for example, “coffee shops within 5 miles using this color” or “heart-marked favorite restaurants in this city.”
- Location permission remains optional. City/area and map-based filtering must still work when current location is unavailable.
- The Android app will use three top-level destinations: **Saved**, **Find**, and **Nearby**. Map is a view inside Nearby rather than a fourth destination.
- Use **Places** as the branded home-screen title. **Saved** remains the bottom-navigation label and phrases such as “4 saved places” describe the collection; **Favorite** refers only to the independent heart flag rather than the whole collection.
- Keep the main top and bottom app bars white or softly neutral. Use deep teal for the brand title, active navigation, and primary actions, with a pale-teal pill behind the active navigation icon. Saved colors are personal grouping colors, not global brand colors; the focused Edit screen may use the selected place color in its header and bottom navigation.
- The Saved screen is for searching, sorting, filtering, and opening places already stored. Near-me discovery belongs on Nearby so the main screen does not become overloaded.
- Place cards use a neutral surface with the saved color as a narrow accent. Cards show the name, place type, city/area, and two or three truncated lines of personal notes or description.
- Saved filters include Type, City, Color, and the independent Favorite heart, but all controls live in one modal bottom sheet opened by a single text-only **Filter** action. The main screen does not show separate filter chips.
- The current List View's city-plus-color grouping and group map are real organizational features, not merely old styling. The refreshed filter flow must still let users reach the same city/color subset and view that exact subset on a map, even if the presentation changes from stacked group cards to filters and the Nearby map.
- The selected visual baseline is the **Option B / Balanced** card direction with soft, comfortable spacing: neutral rounded cards, light outlines, and a narrow unlabeled color rail.
- The Filter action may show a small active-filter count. Selection, individual categories, and **Clear all** stay inside the bottom sheet so the collection screen remains quiet.
- Find places the current search origin directly below the search bar with a separate **Change location** action. The current device location is already the default; Change location opens directly to location autocomplete with a secondary **Choose on map** button.
- Nearby places distance, type, color, Favorite heart, and presets inside the same consolidated filter sheet. Distance defaults to 10 miles and supports numeric entry plus increment/decrement controls within the sheet.
- This remains a client-only personal app for two users. No separate backend or proxy is planned, so Google Maps/Places credentials will remain embedded in the Android application for now.
- Because an APK cannot keep an embedded key secret, protection relies on native Android SDKs, Android application restrictions, API restrictions, conservative quotas, and usage/billing monitoring.

## Current project snapshot

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
- The modernization should retain that directness. The agreed experiment keeps the form neutral and limits the selected color to the Edit header and bottom navigation, but it must not add named color statuses or a stack of settings cards.
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

The `release` build type currently sets `signingConfig` to the debug signing configuration. An actual upgrade must use the same production signing identity as the installed/Play-distributed version.

Impact:

- A differently signed build cannot update the installed application.
- Incorrect signing decisions can make the existing on-device database inaccessible from the replacement APK.

Before changing build configuration, identify how version 2 was signed and confirm Play App Signing status and key ownership.

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

#### Package name and signing key are part of data preservation

Database safety is not limited to SQL. Keeping the same `applicationId` and signing identity is required for Android to treat a new APK/AAB as an upgrade of the existing app. These values must be treated as protected compatibility contracts.

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
- Use a properly Android-restricted production key tied to `com.example.favoriteplaces` and the production signing-certificate fingerprint.
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

#### Autocomplete lacks modern request/session management

The current search runs only after pressing a Search button, does not use autocomplete session tokens, and passes a location string created with `LatLng.toString()`. That representation is not the documented `latitude,longitude` web-service format. The configured 75-meter radius is also unusually narrow for restaurant discovery.

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

The official library was at 8.4.0 during this audit: <https://github.com/googlemaps/android-maps-compose/releases>

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

#### City extraction can crash or save incorrect data

`convertPredictionToFavorite()` splits `formattedAddress` on commas and reads element 1. Addresses do not have a universal comma layout, and locality placement differs by country.

Required behavior:

- Read locality/administrative components from structured place address components.
- Provide a safe fallback such as an empty or display-derived locality.
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
- Each card shows place name, normalized place type, city/area, and personal notes or description truncated to two or three lines.
- Rating and the Favorite heart may appear as quiet trailing/footer metadata if they do not crowd the notes.
- Do not display invented meanings beside the color rail. The independent Favorite state appears as a heart; otherwise that space is more useful for notes. Color filtering uses the same five unlabeled swatches.
- Tapping a card opens details/edit. Preserve swipe-to-delete with Undo rather than showing a delete button on every card.
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
- Preserve the current quick-save behavior unless both users explicitly prefer a customization step. A new save may keep the existing default color and Favorite-off state, followed by an unobtrusive path to Edit; if color is offered during save, show only the five unlabeled swatches and never force an invented meaning.
- Notes and rating remain optional so a place can be saved quickly.
- Disable save while details are loading and prevent duplicates visibly.

**Nearby**

- Use a compact, unboxed location header: stack the `CURRENT LOCATION` label above the location name and place a small **Edit** action beside it. Do not repeat the larger location-card treatment used in earlier concepts.
- Put the List/Map segmented control in the top app bar, before the location and results content.
- Show one text-only **Filter** action immediately above the nearby result count.
- Open one consolidated sliding bottom sheet containing distance, Type, Color, the independent Favorite heart, and neutral quick presets such as “Closest first,” “Favorites nearby,” and “Highly rated.”
- Inside the sheet, distance uses minus, numeric input, and plus; it defaults to 10 miles, steps sensibly, and validates a safe range.
- Calculate distance and sorting locally from saved coordinates so this remains useful offline after a location has been obtained.
- Provide list/map switching without changing or clearing the active result set.
- Show distance on Nearby cards; omit it from Saved unless the user explicitly sorts by current distance there later.
- Support city/area selection and map-area selection when current location is unavailable or permission is denied.
- A distinct no-results state should offer to widen the radius or clear a specific filter.

**Place details/edit**

- Treat Edit as a focused child screen opened from a Saved card, not a fourth bottom-navigation destination. Use a back action, **Edit place** title, and one explicit **Save** action in the top app bar.
- Keep the edit form itself white or softly neutral. Apply the currently selected place color only to the top app bar and persistent bottom navigation; changing the color swatch updates both chrome areas immediately. Use dark, contrast-checked content over the established light colors.
- Preserve the current screen's quiet, direct structure rather than dividing every property into cards. The core order is the five unlabeled color swatches, editable name, place type, rating, compact read-only address/map action, and notes, separated only by spacing and thin dividers.
- Do not display status names beneath the swatches or imply that any color means Favorite, visited, wanted, or anything else. For accessibility, announce neutral position-based descriptions such as “Color option 1 of 5,” not invented semantics.
- Favorite is a separate heart control in the Edit app bar. Toggling it must not change the selected color, and changing color must not change the heart state.
- Keep destructive deletion out of the primary form flow. Place it under an overflow action or at the very end of the scroll content and always require explicit confirmation; it must never sit beside Save.
- Stored place type is migration-dependent because version 1 has no type column. The Edit UI may appear only after the tested additive migration, or show **Uncategorized** until that migration and review flow are complete; no existing record may be rewritten or dropped to support it.

**Nearby map view**

- Map with marker selection and a bottom sheet for the active place.
- Fit camera bounds to all selected markers.
- Apply the same color, Favorite-heart, place-type, and distance/area filters as the list so switching views does not change the result set.
- Use the saved color for markers and a separate heart indicator for Favorite where appropriate; neither should alter the other.
- Accessible fallback list for the same results.

#### Preserve color and Favorite as separate signals

The existing five colors are an established, intentionally open-ended personal grouping system. The app must not name them or assign a workflow meaning. The stored integer color value remains the compatibility source of truth. Favorite is separately represented by the existing `isFavorite` boolean and heart icon; it is not a sixth color and is not tied to any one color.

Modern presentation options include:

- A narrow colored leading rail on neutral Material 3 cards is the selected baseline direction.
- Full-card color fills are not the preferred direction; color should accent the item rather than dominate it.
- Color filters and Edit controls show the five swatches without visible names.
- Matching colored map markers plus separate selection and Favorite-heart indicators.
- A compact swatch-based color filter available from both list and map views.

Because the colors deliberately have no app-defined meaning, accessibility semantics should identify their stable position—such as “Color option 1 of 5, selected”—without inventing a label. The Favorite heart must always have explicit Favorite/Remove favorite semantics.

#### Place-type model and existing favorites

The current version-1 `Favorite` table does not store a normalized place type. Reliable offline type filtering therefore requires a later additive schema change; it must not alter or discard any current column.

Proposed safe approach:

1. First ship the platform/API modernization with the version-1 schema unchanged.
2. Define a small stable app-owned category set, separate from Google's evolving raw place types. Initial examples may include Restaurant, Coffee/Tea, Bar/Brewery, Dessert, Shopping, Outdoors, Entertainment, Lodging, and Other.
3. Add nullable category/raw-type columns in a versioned migration. Existing rows remain intact and initially become “Uncategorized” rather than being guessed incorrectly.
4. Populate type for new saves from Places API (New).
5. Offer a review flow for existing favorites. The app may suggest a category from refreshed Place Details when a valid place ID is available, but must not silently overwrite a user-confirmed choice.
6. Keep filtering usable while uncategorized records remain, including a dedicated “Uncategorized” filter.

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
4. Install the upgrade over the existing app on a test device/database first—never uninstall.
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

**Gate:** No later phase starts until an in-place upgrade test preserves every fixture record.

### Phase 1 — platform/toolchain modernization

- Upgrade Gradle/AGP/Kotlin/Compose in controlled compatible increments.
- Move to compile/target API 36.
- Update target-SDK behavior handling and edge-to-edge layout.
- Move versions to a version catalog.
- Preserve database version 1 if the schema is unchanged.
- Run unit tests, lint, assemble debug/release, and install-over-existing tests after each upgrade group.

**Gate:** App launches, reads all old data, edits/saves/deletes/undoes correctly, and produces a correctly signed candidate release.

### Phase 2 — Places API (New)

- Enable Places API (New) and prepare restricted Android credentials.
- Initialize the new Places SDK.
- Implement SDK-backed autocomplete with session tokens and field masks.
- Implement selected-place details and structured city extraction.
- Make location optional and remove null/race failures.
- Run old and new paths behind an internal switch until behavior is verified.
- Remove legacy Retrofit Places code and DTOs after validation.

**Gate:** Search works with location granted, approximate-only, denied, and unavailable; duplicate prevention and save accuracy are tested; API usage and billing metrics are reviewed.

### Phase 3 — official Maps Compose

- Add the maintained Maps Compose dependency.
- Migrate add-place preview and city map.
- Fit marker bounds and handle empty/single-marker cases.
- Remove the copied Maps Compose source only after parity tests.

**Gate:** Both maps work across supported Android versions, rotation, process recreation, dark mode, and denied location.

### Phase 4 — state and architecture cleanup

- Consolidate each screen around immutable `StateFlow` UI state.
- Eliminate favorites refresh loops and grouped-query fan-out.
- Replace transient map cache navigation with stable route IDs/filters.
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
- Put List/Map in the Nearby app bar and place a single text-only Filter action above the result count.
- Move the validated editable distance stepper, defaulting to 10 miles, into the Nearby filter sheet.
- On Nearby, add combined color, Favorite-heart, place-type, area/radius filters, neutral presets, and local distance sorting.
- Preserve active Nearby filters when switching between list and map.
- Add loading, empty, error, retry, offline, and permission states.
- Complete accessibility, dark-mode, dynamic-color, and font-scale testing.

**Gate:** Existing capabilities remain available and all upgrade/data-preservation tests pass.

### Phase 6 — place-type schema and legacy categorization

- Finalize the stable app-owned place categories.
- Design an additive Room schema that leaves all version-1 columns and values intact.
- Add nullable category/raw-type fields through an explicit tested migration.
- Populate types for new places and provide a reviewable categorization path for existing favorites.
- Keep existing color values unchanged and independently filterable.
- Follow the full backup, fixture, migration-chain, and staged-device rollout protocol.

This schema phase must not block shipping the API/platform refresh and local location filtering. Until it ships, type filtering can be offered for live Google search results but not claimed as complete for existing offline favorites.

## Verification checklist for every release

- [ ] Same `applicationId` as the installed app.
- [ ] Correct production signing identity.
- [ ] Version code increased.
- [ ] No destructive Room migration method anywhere in the project.
- [ ] All Room migration tests pass from version 1 to latest.
- [ ] Install-over-existing test passes without uninstalling or clearing data.
- [ ] Favorite count and all stored values remain unchanged unless a reviewed migration explicitly adds derived data.
- [ ] Backup/export created and validated for both devices before a schema-changing rollout.
- [ ] Search tested with location granted, approximate, denied, and unavailable.
- [ ] Near-me filtering and distance sorting tested offline using stored coordinates.
- [ ] Combined color, Favorite-heart, place-type, and location filters return consistent results in list and map views.
- [ ] Every existing stored color retains its exact value without the app assigning or migrating a meaning.
- [ ] Maps and markers tested with zero, one, and many favorites.
- [ ] Offline saved-data browsing works.
- [ ] Release build is minified/obfuscated only after rules are verified.
- [ ] API keys are restricted and no key is logged or committed.
- [ ] Production and debug key restrictions match their package/signing fingerprints, and conservative quota/budget alerts are active.
- [ ] Unit, migration, UI, lint, and release-build checks pass.
- [ ] One-device staged household rollout succeeds before the second device is upgraded.

## Decisions still needed

- Confirm whether the current app is installed from Google Play or direct APKs.
- Confirm access to the production signing key or Play App Signing account.
- Decide whether version 2 already contains the exact version-1 Room schema in this repository.
- Decide on the backup format and where users will store/share backups.
- Confirm the stable display order of the five current colors and keep them visibly unlabeled; do not ask either user to define a permanent meaning.
- Decide the initial friendly place-type category list and how existing uncategorized favorites will be reviewed.
- Decide the maximum allowed Nearby radius and whether to remember the most recent value; the initial default is 10 miles.
- Decide whether restaurant photos, current opening hours, website/phone, price level, or categories are worth their additional Places fields and billing.

## Definition of done

The modernization is complete when:

- The app targets current Google Play requirements and builds reproducibly.
- Legacy Places HTTP endpoints and copied Maps Compose internals are gone.
- Google credentials are appropriately restricted.
- Search, save, edit, delete/undo, organization, and city maps work under normal and failure conditions.
- The UI is coherent Material 3, accessible, responsive, and supports dark mode while preserving open-ended color grouping and the independent Favorite heart.
- Users can combine place type, color, Favorite heart, and current area filters and receive the same saved-place result set in list and map views.
- Tests cover persistence, migrations, primary ViewModels, and critical user flows.
- Most importantly, both existing installations upgrade in place with every saved place, note, rating, Favorite-heart flag, color, city, place ID, and coordinate intact.
