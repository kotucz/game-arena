# Agent guidance

`GameArena` is a Kotlin Multiplatform project. Platform-independent game rules
live in `core`; Compose Multiplatform UI lives in `app/shared` and depends on
`core`.

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
- `ContactsGameFacade.Action` is an ID-based network transport DTO, not the
  in-process UI API. Network adapters resolve its IDs and invoke typed facade
  methods; add typed methods for new commands rather than a generic dispatcher.

## Deterministic tests

Preserve production defaults, but allow tests to inject deterministic initial
state. Use small explicit fixtures rather than searching randomized state.

## Verification

- Run the narrowest relevant task:
  - Core: `gradlew.bat :core:jvmTest`
  - Shared UI: `gradlew.bat :app:shared:compileKotlinJvm`
