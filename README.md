# UX Innovator — KMP User Management

A Kotlin Multiplatform coding challenge submission for Sliide: a polished, offline-first user management app running on Android and iOS from a single shared codebase.

---

## Features

- **Smart User Feed** — fetches the most recently created users from the GoRest API (page 1, descending by ID), displays name, email, and a "fetched at" relative timestamp (e.g. "5 minutes ago") computed in shared logic, with shimmer loading skeletons and pull-to-refresh
- **Add User** — FAB opens a `ModalBottomSheet` with real-time name/email validation; on success (HTTP 201) the list scrolls to the top where the new user appears
- **Delete with Undo** — long-press a user to trigger a confirmation dialog; confirming removes the item with an animation and shows a 4-second Snackbar Undo option that cancels the deletion before it reaches the network and restores the user to their original position
- **Adaptive Layout** — single-column list in portrait, two-column grid in landscape / at ≥ 600 dp
- **Offline-first** — SQLDelight cache is the source of truth; "No Internet" error state with retry when the cache is empty

---

## Architecture

The app follows Clean Architecture with MVVM, keeping all logic in the `shared` KMP module.

**Domain layer** (`domain/`) defines pure Kotlin models (`User`), the `UserRepository` interface, and use cases (`GetUsersUseCase`, `RefreshUsersUseCase`, `CreateUserUseCase`, `DeleteUserUseCase`, `FormatRelativeTimeUseCase`). Nothing here knows about Ktor or SQLDelight.

**Data layer** (`data/`) provides the implementation: `UserRepositoryImpl` coordinates a Ktor `HttpClient` for remote calls and a SQLDelight `UserDatabase` for local persistence. SQLDelight's `Flow`-based queries make the database the single source of truth — the UI observes the cache and background refreshes write through to it automatically.

**Presentation layer** (`presentation/`) contains Compose Multiplatform screens and `ViewModel`s (`UserListViewModel`, `AddUserViewModel`) that expose `StateFlow` state collected via `collectAsState`.

**Koin 4** wires all three layers together. A single `initKoin(driverFactory)` call at app startup (from `Application` on Android, from `MainViewController` on iOS) bootstraps the DI graph with platform-specific SQLite drivers as the only `expect/actual` seam.

---

## Tech Stack

| Technology | Library | Version |
|---|---|---|
| Kotlin Multiplatform | `org.jetbrains.kotlin.multiplatform` | 2.4.0 |
| Compose Multiplatform | `org.jetbrains.compose` | 1.11.1 |
| Networking | Ktor Client | 3.1.3 |
| Local cache | SQLDelight | 2.0.2 |
| Dependency injection | Koin | 4.1.0 |
| Async / state | kotlinx-coroutines | 1.10.2 |
| Timestamps | `kotlin.time` (stdlib) | 2.4.0 |
| Serialization | kotlinx-serialization-json | 1.8.1 |

---

## Project Structure

```
ux_innovator/
├── androidApp/          # Thin Android shell (MainActivity, Application)
├── iosApp/              # Thin Xcode project (ContentView.swift)
└── shared/              # All logic: domain, data, presentation, DI
    └── src/
        ├── commonMain/  # Shared Kotlin + Compose (99% of the code)
        ├── androidMain/ # Android SQLite driver + Koin bootstrap
        └── iosMain/     # iOS SQLite driver + ComposeUIViewController
```

---

## Getting Started

### Prerequisites

- Android Studio Meerkat (2024.3) or newer
- Kotlin 2.4.0+
- Xcode 16+ (for iOS builds)

### GoRest API Token

The app calls the [GoRest public API](https://gorest.co.in), which requires a Bearer token for all API requests (GET, POST, and DELETE). Without a token the user list will be empty — GoRest scopes results by token.

1. Go to [https://gorest.co.in](https://gorest.co.in) and sign in with GitHub or Google.
2. Copy the Bearer token shown on your profile page.
3. Open `shared/src/commonMain/kotlin/com/sliide/uxinnovator/Config.kt` and replace the `GOREST_TOKEN` placeholder (`"YOUR_GOREST_TOKEN_HERE"`) with your token:

```kotlin
const val GOREST_TOKEN = "YOUR_GOREST_TOKEN_HERE"
```

### Build & Run

**Android:**
1. Open the repository root in Android Studio.
2. Paste your token into `Config.kt`.
3. Select the `androidApp` run configuration and press Run.

**iOS:**
1. Paste your token into `Config.kt`.
2. Open `iosApp/iosApp.xcodeproj` in Xcode (or use Android Studio's iOS run configuration).
3. Select a simulator or device and press Run.

> `libsqlite3.tbd` is already linked in the Xcode project — no extra setup needed.

---

## Key Design Decisions

**`kotlin.time.Instant` instead of `kotlinx.datetime.Instant`** — `kotlinx-datetime` 0.7.1 (pulled in transitively) makes `kotlinx.datetime.Instant` a typealias for `kotlin.time.Instant`. Using the stdlib type directly avoids the indirection and compiles cleanly on Kotlin 2.4 without extra imports.

**Undo via coroutine cancellation** — `UserListViewModel.confirmDelete` launches a coroutine that waits 4 seconds before issuing the DELETE request. `undoDelete` cancels that `Job`. Because the API call is inside the same coroutine, cancellation guarantees the network call never fires — no race conditions, no state cleanup needed. On undo the user is re-inserted at their original index and the list scrolls to that position.

**Two-column grid instead of master-detail** — at ≥ 600 dp the `LazyVerticalGrid` switches to 2 columns. A master-detail panel was not warranted given the limited data per user (name, email, status, fetched timestamp).

**GoRest pagination — page 1 is newest** — GoRest returns users sorted by descending ID, so page 1 contains the most recently created users. The app fetches page 1 with the auth token (required even for reads, since GoRest scopes results per token).

**`fetchedAt` timestamp** — the GoRest API does not return a reliable `created_at` field, and the concept of "when a user was fetched" doesn't exist server-side. `fetchedAt` is a client-side timestamp set to `Clock.System.now()` at the moment the record is written to SQLDelight. It updates on every refresh and is displayed as a relative time (e.g. "3 minutes ago") computed in shared logic.

**GoRest required fields on create** — the GoRest API requires `gender` and `status` fields on `POST /users`, but neither is part of the challenge requirements and neither is surfaced in the UI. New users are created with `gender = "female"` and `status = "active"` as fixed defaults. Both fields are serialized via `encodeDefaults = true` on the Ktor JSON config so they are always included in the request body.

**Offline-first via SQLDelight `Flow`** — `GetUsersUseCase` returns a `Flow<List<User>>` backed by a SQLDelight query. The UI subscribes once and stays live. `RefreshUsersUseCase` fetches from the network and writes to the database; the `Flow` re-emits automatically. The network is purely a write-through source.

**Single `shared` Gradle module** — all Android and iOS targets share one module. One `build.gradle.kts`, one `sourceSets` block, one place to add dependencies. The only platform-specific code is the SQLite driver factory (`expect/actual`) and the platform entry points.

**HTTP retry** — Ktor's `HttpRequestRetry` plugin (3 retries, exponential backoff) handles the transient DNS failures that GoRest's shared public API occasionally produces.

**No localization** — all UI strings are hardcoded English, with no `strings.xml` / resource-based string lookup. This was a deliberate scope cut for speed of implementation, not an oversight.

---

## AI Workflow

This project was built using [Claude Code](https://claude.ai/claude-code) (Claude Sonnet 4.6) as the primary development assistant, on a Mac. Development happened mostly in Android Studio, with a small amount of work done directly in Xcode (Xcode project file editing, simulator builds/launches). Architecture was designed upfront: the challenge brief was given to the model, which proposed the Clean Architecture + MVVM layering, the `expect/actual` strategy, the coroutine-cancel approach for undo, and the full file tree before any code was written.

During implementation, parallel agent sessions handled independent workstreams — UI polish, adaptive layout, error states, and tests in parallel. Real compiler and runtime errors (Ktor serialization mismatches, the `kotlinx.datetime` alias conflict, GoRest pagination behaviour, Koin/lifecycle version incompatibilities on iOS) were fed back verbatim and diagnosed from actual output rather than speculation.

Key debugging moments resolved through iterative logging and curl verification:
- GoRest does not return `created_at` → fallback to `Clock.System.now()` as `fetchedAt`
- GoRest scopes GET results by auth token → added `Authorization` header to all requests
- GoRest sorts users descending by ID → switched from last-page to page-1 fetch
- Koin 4.0.4 incompatible with `lifecycle-viewmodel-compose` 2.11.x on iOS → upgraded to Koin 4.1.0
