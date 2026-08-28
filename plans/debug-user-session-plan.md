Current plan for debug headers

1. Reintroduce a constrained debug-header path only for local/test use
- Keep production auth fully cookie/session based.
- Allow a header such as `X-Debug-Username` only in non-production/dev/test conditions.
- Do not widen this into a general bypass for all auth checks.

2. Add an explicit helper that resolves the effective principal
- `currentSession(call, database)` should remain the compatibility layer.
- Extend it to:
  - read `X-Debug-Username`
  - if present and allowed, resolve a fake authenticated user without mutating session state
  - otherwise fall back to the real cookie/session lookup
- This should not call `call.sessions.set(...)` in `ApplicationCallPipeline.Setup`.

3. Prefer auth provider validation over ad hoc checks
- When a route truly needs auth, use the Ktor auth machinery (`authenticate("auth-session")`).
- Inside the auth provider’s validation, keep real session-token lookup only.
- For debug/test mode, keep a separate dev-only branch or preflight helper that returns a `UserPrincipal` without touching plugin lifecycle ordering.

4. Avoid early plugin access
- Never use `call.sessions` before the `Sessions` plugin is installed and ready.
- Do not place session mutation in `ApplicationCallPipeline.Setup`.
- If debug mode needs a temporary principal, resolve it in a route-level helper or inside the auth-validation path only after the session plugin is available.

5. Scope the feature carefully
- Only enable debug headers for:
  - local development
  - integration tests
  - explicit test client flows
- Keep it disabled or ignored in normal production runtime.

6. Validation
- Use server test suite to confirm:
  - normal cookie login still works
  - debug header works only when enabled
  - unauthorized requests still fail without either cookie or header

7. Optional cleanup after the feature is working
- Remove any legacy manual auth checks once all protected routes consistently use the auth plugin.
- Preserve a small helper for test/dev convenience, but keep it clearly isolated.

Suggested implementation shape

- `Authentication`:
  - `session<String>("auth-session") { validate { ... } }`
- helper:
  - `private fun currentSessionOrDebug(call: ApplicationCall, database: AppDatabase): Session?`
- route usage:
  - protected endpoints under `authenticate("auth-session")`
  - inside handlers, read `call.principal<UserPrincipal>()`

Important rule:
- Do not mutate `call.sessions` in setup interceptors. If needed, create and attach a dev-only session in the route or auth-validation path only after the plugin is ready.
