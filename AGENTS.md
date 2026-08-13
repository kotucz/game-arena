# Agent guidance

## Project overview

- This is the `GameArena` Kotlin Multiplatform project, targeting Android, (iOS), JVM/Desktop, JS/Wasm web, and a Ktor server.
- `core` contains platform-independent game models and rules. `app/shared` contains Compose Multiplatform UI and depends on `core`.
- The Contacts game model is under `core/.../game/contacts/model`; its UI is under `app/shared/.../game/contacts`.
- `ContactsGameFacade` is the boundary for game actions. Keep rule validation and game-state changes in the core/facade layer, not in Compose screens.
- `ContactsPlayerScreen` owns temporary UI selection state locally; selections are sent through a facade action only after confirmation.

## Deterministic tests

Prefer dependency injection for testable game logic. Keep the normal production
factory/default initialization, but allow tests to provide a deterministic
initial state through an internal constructor or factory. Tests should build
small explicit fixtures instead of inspecting or searching randomized game
state to find suitable inputs.

## Verification

- Core logic/tests: `gradlew.bat :core:jvmTest`
- Shared Compose compilation: `gradlew.bat :app:shared:compileKotlinJvm`
- Run the narrowest relevant Gradle task after changes; existing unrelated compiler warnings may remain.

## Usage reporting

After each user prompt, include a brief note about the credits or token usage
consumed when that information is available. If usage is significant, include
enough detail to help review and optimize the work. If exact credit usage is
not exposed by the runtime, say so instead of estimating it.
