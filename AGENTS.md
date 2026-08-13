# Agent guidance

## Deterministic tests

Prefer dependency injection for testable game logic. Keep the normal production
factory/default initialization, but allow tests to provide a deterministic
initial state through an internal constructor or factory. Tests should build
small explicit fixtures instead of inspecting or searching randomized game
state to find suitable inputs.
