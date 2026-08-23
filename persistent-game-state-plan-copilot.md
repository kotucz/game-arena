# Local file persistence plan for users, sessions, and games

## Problem

The project already has a local file-based Room database for users and sessions, but there is no persistence for running games. We need to store game metadata and full game state, and likely also a state history for the Contacts game so a game can be resumed after restart. The Contacts board state is already serializable, which makes snapshot persistence straightforward.

## Proposed approach

1. Keep the current local SQLite/Room setup as the persistence foundation for users and sessions.
2. Add a separate game persistence layer in the server, not a full server-local replacement for Room in the first pass.
3. Store each game as a record keyed by game ID with:
    - metadata (`id`, `type`, `players`, `createdAt`)
    - current serialized game payload (for example `ContactsBoardState`)
    - optional state history (`GameLogEntry`/event list or snapshot timeline)
    - updatedAt/createdAt timestamps
4. Serialize game state with Kotlinx Serialization and persist it in a JSON/text column or as a document blob. This matches the existing `@Serializable` model and keeps the database local in a file.
5. Restore games in `GamesManager` by reading the stored metadata and deserializing the latest state snapshot. New game types can follow the same pattern without letting the manager inspect raw game implementations.
6. Keep state/history logic in `core` as much as possible. The Contacts game should keep validation and state transitions behind `ContactsGameFacade`/`ContactsBoardState`, while the persistence layer only serializes the resulting state and logs.

## Why not a full NoSQL replacement yet

A separate embedded NoSQL database can work for local-file storage, but it is not necessary for the current requirements. The main reasons to prefer Room/SQLite here are:

- The project already uses Room and has a stable local file database, so there is no need to introduce a second persistence system just to store games.
- The game data model is not a free-form document workload; it is structured metadata plus a compact serialized state snapshot and an append-only log/history. SQLite is sufficient and simpler to reason about.
- Kotlin/Room integration is already in place, with less operational risk than introducing a new embedded database dependency and migration story.
- We can still store game payloads as JSON blobs or dedicated tables, which gives us the flexibility of document-like persistence without the overhead of full NoSQL.

In other words: we do not need a NoSQL engine to satisfy the requirement that the database stays local on disk. SQLite + JSON is the more appropriate default for this prototype. If we later discover that game archives or analytics need a document-heavy model, we can revisit a separate embedded document store without breaking the current architecture.

## Implementation phases

- Phase 1: Define storage schema and repository API for games.
    - `games` table for metadata + serialized state
    - `game_events` or `game_history` table for turn/event history
    - `game_snapshots` optional if we want periodic checkpointing instead of full history
- Phase 2: Implement server-side DAO/repository and JSON serialization helpers.
    - current `AppDatabase` remains the database entry point
    - add new DAO methods for save/load/restore/list games
- Phase 3: Integrate with `GamesManager`.
    - hydrate registry on startup
    - save the latest state whenever actions are applied
    - expose typed lookups such as `contactsGame(id)` instead of inspecting generic managed game entries
- Phase 4: Persist Contacts game state and logs.
    - save `ContactsBoardState` as the latest snapshot
    - save `GameLogEntry` list or event log for replay/history
    - ensure IDs and state remain deterministic for tests
- Phase 5: Add tests.
    - restore a saved Contacts game and verify state + logs match
    - test persistence is local-file based and does not depend on in-memory state

## Key decisions

- Prefer SQLite/Room with JSON serialization over introducing a new embedded NoSQL dependency for this prototype.
- Keep the persistence API typed and game-aware rather than a generic raw `Map<String, Any>` store.
- Store a full serialized state snapshot plus action history, so resumed games are deterministic and audit-friendly.
- Treat each game type as a typed persisted document, even if the initial implementation only stores Contacts.

## Files likely involved

- `server/src/main/kotlin/cz/kotu/gamearena/AppDatabase.kt`
- `server/src/main/kotlin/cz/kotu/gamearena/GamesManager.kt`
- `server/src/main/kotlin/cz/kotu/gamearena/Application.kt`
- `core/src/commonMain/kotlin/cz/kotu/game/contacts/model/ContactsBoardState.kt`
- `core/src/commonMain/kotlin/cz/kotu/game/contacts/model/ContactsGameFacadeImpl.kt`
- possibly new server-side DAO/entity files for persisted games and history

## Current implementation status

- Room is retained as the local file database for users/sessions and is now extended with a `games` table.
- `GamesManager` persists serialized `ContactsBoardState` snapshots plus `GameLogEntry` history and restores them on startup.
- The core `ContactsGameFacadeImpl` constructor used for restoration is now public because `server` cannot access `internal` constructors from a separate module.
- The persistence API is now suspend-based and uses `Mutex` to protect the in-memory game registry while preserving database access through the same coroutine-friendly flow.
- Startup restoration is still triggered once at app initialization, while request handlers and game creation remain suspend-friendly rather than blocking the thread.
