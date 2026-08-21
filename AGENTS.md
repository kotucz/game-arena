# Agent guidance

`GameArena` is a Kotlin Multiplatform project. Platform-independent game rules
live in `core`; Compose Multiplatform UI lives in `app/shared` and depends on
`core`.

## Games manager

- `GamesManager` owns the in-memory registry of `ManagedGame` instances keyed by
  string IDs. Persistence is not part of the current implementation.
- Shared game metadata is represented by `GameMetadata`: ID, stable string game
  type, player usernames, and creation time.
- New game kinds should provide their own typed runtime entry while exposing
  the common metadata needed by the manager and game-list endpoint.
- Use typed lookups such as `contactsGame(id)` for game-specific routes. Do not
  cast or inspect generic managed-game entries in `Application.module()`.
- The current Contacts game type discriminator is the stable string `"contacts"`.

## Shared network contracts

- `RunningGame` is the shared `@Serializable` client/server contract for the
  running-games endpoint. It lives in
  `core/src/commonMain/kotlin/cz/kotu/gamearena/model/GamesNetworkProtocol.kt`.
- `RunningGame` contains `id`, `type`, `players`, and ISO-8601 `createdAt`.
- Network response DTOs that are consumed by both clients and server belong in
  `core`; do not duplicate them as server-local contracts.

## Contacts

- Keep validation and state changes in `core` behind `ContactsGameFacade` and
  board-state helpers, not in Compose screens. UI may keep temporary selection
  state and submits it only after confirmation.
- `ContactsBoardState.pool` is the sole store of full `Contact` values. All other
  persisted state stores `ContactId`; name ID collections explicitly (for
  example, `contactIds`).
- Prefer resolved `Contact` values in UI and typed facade methods. Resolve IDs
  through board-state helpers such as `contact`, `requireContact`, and
  `contacts`; use query helpers instead of inspecting collections directly.
- `ContactsNetworkAction` is an ID-based network transport DTO, not the
  in-process UI API. Network adapters resolve its IDs and invoke typed facade
  methods; add typed methods for new commands rather than a generic dispatcher.

## Deterministic tests

Preserve production defaults, but allow tests to inject deterministic initial
state, game ID generators, and clocks. Use small explicit fixtures rather than
searching randomized state; use explicit `ManagedGame` fixtures for future game
types.

## Verification

- Do not run gradlew commands. Just tell me to do it or ask before you do.
- If possible, run verification via IntelliJ API.
- Use `--console=plain`
- In PowerShell, invoke the Gradle wrapper as `.\gradlew.bat <task>`; do not
  prefix it with an additional slash.
- Run the narrowest relevant task:
  - Core: `gradlew.bat :core:jvmTest`
  - Shared UI: `gradlew.bat :app:shared:compileKotlinJvm`
  - Server: `gradlew.bat :server:test`

If a command invocation or project-specific command fails, document the
corrected command or the cause in this file or the README when it is useful for
future agents. Notify the user about it

## Gemini CLI

# Private Project Memory - Manual File Update Workflow

If `replace` or `write_file` tools fail due to "Cannot enable privileged approval modes in an untrusted folder", the following procedure should be used:
1. Identify the files and necessary code changes.
2. Present the exact code updates to the user in the chat.
3. Ask the user to apply them manually.
4. Record that this was done in this file and continue with the next task.
