# Agent guidance

`GameArena` is a Kotlin Multiplatform project. Platform-independent game rules
live in `core`; Compose Multiplatform UI lives in `app/shared` and depends on
`core`.
The project is in a prototype stage. However, the code should be idiomatic, 
use industrial standards and best practices. Validate with user, if the effort
does not seem reasonable.    
Plan is to support multiple game types – while still figuring out the best 
architecture for that – and to support multiple clients (desktop, web, mobile)
with shared UI code.
GotFive is a client-only game now.
Contacts is the first client/server game.

- Manage all new dependencies through `gradle/libs.versions.toml` and reference
  them via `libs.*` in Gradle files.

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
- `RegisterTokenRequest` is the shared `@Serializable` DTO for push token
  registration. It lives in the same `GamesNetworkProtocol.kt` file.
- Network request/response DTOs that are consumed by both clients and server
  belong in `core`; do not define them as server-local contracts.
- **Do not mark shared DTOs `private`**. The kotlinx.serialization compiler
  plugin cannot expose a runtime-accessible serializer for `private` top-level
  classes, causing a `SerializationException` at runtime. Use `internal` or
  `public`. The serialization plugin must be applied to the module where the
  class is defined (it is applied to `core`; the `server` module picks it up
  transitively via `api(project(":core"))`).

## Server configuration

- Server runtime settings belong in `server/src/main/resources/application.conf`
  using HOCON, accessed via a typed `ServerConfig` model.
- Expose config values through `ServerBindings`/`ServerComponent` so app setup is
  dependency-injected rather than reading `System.getenv()`.

## Database (Room)
- All Room migrations live in a single `ALL_MIGRATIONS` top-level `val` in
  `AppDatabase.kt`. Both `createDatabase()` and `TestFakes.createTempDatabase()`
  reference it via `addMigrations(*ALL_MIGRATIONS)`. Never copy migration SQL
  into test code — keep one source of truth.
- When adding a new `@Entity`, always: (1) add it to the `entities` list in
  `@Database`, (2) bump `version`, and (3) append a new `Migration` object to
  `ALL_MIGRATIONS`.

## Push notification tokens

- The `push_tokens` table is keyed by a **client-generated `tokenId`** (e.g. a
  stable UUID stored in client preferences). This allows upsert-on-refresh
  without duplicates and targeted deletion on logout.
- `tokenId` is the sole primary key — two users cannot share a `tokenId` row.
  Use `(username, service)` only as an index for efficient lookup, not as a key.
- Delete is scoped by `AND username = :username` in the DAO, so a user cannot
  remove another user's token even if they know its `tokenId`.
- Stale tokens (device uninstalled, etc.) are cleaned up **lazily**: the push-
  sending logic must delete the token row when the delivery API returns 404/410.
  No TTL or background job is needed at this stage.
- Accepted service discriminators: `"fcm"`, `"webpush"`. Add new values to
  `knownServices` in `NotificationRoutes.kt` without a schema change.

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

### Testing DI (kotlin-inject)

Use kotlin-inject to provide test-specific bindings idiomatically:

- Introduce a `ServerBindings` interface with common providers (e.g. `database`, `gamesManager`).
- Make the production `ServerComponent` implement `ServerBindings` and supply real bindings.
- Create `TestFakes` in test sources with `@get:Provides` properties for fakes (for example a temp `AppDatabase`).
- Create a `TestServerComponent(@Component val fakes: TestFakes = TestFakes()) : ServerBindings` and call the generated factory in tests: `val c = TestServerComponent::class.create()`.
- Change `Application.module(...)` to accept `ServerBindings` (defaulting to the production component). Tests pass the test component instance into `module(c)`.

Notes:
- Enable KSP for test sources (kspTest) so kotlin-inject generates `create()` for test components; run `./gradlew :server:compileTestKotlin` to generate artifacts.
- Prefer using real dependencies and only fake edges (DB/network) in tests. Remove legacy ServiceLocator usage — tests should use `TestFakes` instead.

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
