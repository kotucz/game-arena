# Logging plan

## Goal
Use one Kotlin Multiplatform logging API across the project while keeping Ktor HTTP/request logging and the JVM server Logback backend intact.

## Findings
- `server` uses `logback-classic` and Ktor `call-logging`.
- `app/shared` uses Ktor `client-logging` in common client code.
- No shared KMP logging library was present before this change.

## Decision
Use Napier as the shared KMP logger for `core` and app/shared code.

Why Napier:
- lighter and simpler than Kermit for common code
- works well in a KMP project without replacing the current server logging stack
- easy to keep Ktor logging for HTTP diagnostics while adding consistent app-side logs

## Implementation status
- Added `io.github.aakira:napier` dependency to the project.
- Added Napier to `core`, `app/shared`, and `server` modules.
- Initialized Napier in app entry points (desktop, web, Android) and on server startup.
- Wired Ktor client logging to forward to Napier so app-side client requests remain visible in the same logger.
- Kept Ktor server call logging and Logback in place for request-level and JVM server logs.

## Notes
- Do not replace Ktor server logging unless a later requirement justifies it.
- Use Napier for shared-domain and app-level debug/info logs.
- Keep HTTP-specific diagnostics with Ktor logging.

## Next steps
- Add a few actual app/server log statements in meaningful decision points.
- Decide whether to configure a custom Napier backend for production log formatting on JVM.
- Consider a small shared logging utility wrapper if the project starts using logs heavily.
