# Project Roadmap & Backlog

## Bugs & Stability

- [ ] **Auth Performance**: Auth takes long to load.
- [ ] **Connection Drops**: Client occasionally disconnects unexpectedly.
- [ ] **Request Size / Timeout**: Request too long.
- [ ] **Post-Login Reload**: Bug reloading games immediately after login.
- [x] **SSE Infinite 404 Retry Loop**:
  - Issue: `2026-08-30 22:16:25.702 [DefaultDispatcher-worker-10] INFO Ktor.Server - 404 Not Found: GET - /api/games/67cce016-fd03-45a7-a157-82ae2466c776/contacts/events in 0ms retries in loop`
  - Needs proper backoff / termination when game is missing.
- [ ] **Deep Link Auth Restoration**:
  - `App.kt` keeps username only in memory. A refreshed game deep link can resolve its route but still show "Authentication required."
  - Authentication and session restoration should be handled independently from route navigation.

---

## Server & Infrastructure

- [x] **Persist DB**: Database persistence configured.
- [x] **Deployment**: Deploy Home Assistant (HA), setup domain `gamearena.kotu.cz` / `garnena`.
- [x] **Server Application DI**: Implement structured dependency injection for Ktor server application.
- [ ] **Generic Game Server Architecture**:
  - Game server should be generic; Contacts is only one of multiple planned games.
  - Make server routes/paths and `GamesManager` generic across game types.
- [x] **HTTP/2 & HTTP/3 Configuration**: Configure HTTP/2 / HTTP/3 to maintain keep-alive sessions.
- [x] **Server Error Handling & Resilience** ([Gemini Discussion Reference](https://gemini.google.com/app/8a545ac4292259a4)):
  - [x] Show errors in the app UI / debug log.
  - [x] Retry SSE on exception with sensible exponential backoff.
  - [x] Implement SSE heartbeat / ping mechanism.
- [x] Breakdown Application file into logical modules 
- [x] Client data sanitization - keep client data model free from secret/hidden data

---

## Authentication & Session Management

- [x] **Auth Flow**: Refine auth flow in App.
- [ ] **Session & Token Architecture**:
  - Pre-flight check: if session token is missing before an API call, trigger 401 locally without making network request.
  - Dispatch unauthorized signal on 401s across the app.
  - Manage and persist `session_token` + `username`.
- [ ] **Login UI Enhancements**:
  - [ ] Password manager support (proper autofill hints/attributes).
  - [x] Tab navigation to next input field.
  - [x] Enter key to submit credentials.
  - [ ] "Remember me" option.
  - [ ] "Forgot password" flow.
  - [x] Proper logout handling.
- [ ] Google OAuth login 

---

## Multiplayer & Networking

- [ ] **Online Multiplayer Implementation**:
  - [x] Game state serialization.
  - [x] Pick game from list $\rightarrow$ Join screen (`player`, `gameId`).
  - [x] Inject `gameId` into `ClientNetworkFacade` (remove fixed game instance).
  - [ ] Game facade $\rightarrow$ network $\rightarrow$ server game facade implementation:
    - *Decision*: Single game instance per request/player vs per game?
    - *Current*: Temporary in-memory instance per game.
- [ ] **Matchmaking & Lobby**:
  - [x] Create game.
  - [x] Join game.
  - [x] View "My games".
  - [ ] Game selector in app (potentially initial rollout as hidden / debug option).
  - [ ] sorting last created, last modified, my turn
- [ ] **Turn Notifications**: "Your turn" notifications (push or in-app).
- [ ] Players take turns
- [ ] Game concluded 
  -  [ ] Terminate event streams
- [ ] Multiplayer testing - multiple clients simultaneously on desktop/browser

---

## User experience

- [ ] Themes/redesign - (light/dark)
- [ ] Sounds
- [ ] Vibration - haptic feedback
- [ ] Localization
    
---

## Contacts Game Features & Rules

- [ ] **Initial Hints**: Add initial hint generation/display.
- [x] **OR Connection**:
  - [ ] Player skill.
  - [x] X/Y – two numbers.
- [ ] **Bonuses**:
  - [ ] `Hint =`
  - [ ] `Hint !=`
- [x] **Colors / Markers**:
  - [x] Yellow
  - [x] Red
- [ ] **Screen Refinements**: `ContactsPlayerScreen` UI & interactions.
  - [x] Animation of changes (flipping tiles)
  - [x] Animation of fault - shake tile
- [ ] Rich logs
  - [x] highlight contacts when hover on logs 
  - show only number when location is not known
- [ ] pool show which number is owned by which player (if known) e.g. from connection attempt
- [x] player on turn
- [ ] skip player with all contacts solved
- [ ] conclude game - all contacts solved/too many faults/red connected
- [ ] actions per player

---

## Observability & Logging

- [x] **Structured Game Logs**:
  - Format: `timestamp`, `model`, `toString(...)`.
  - Player-dependent visibility: mask secrets, perspective (you vs opponent), player color.
- [ ] **Client Error Logging**: Visible error console / log viewer in app for easier diagnostics.

---

## Optimization

- [ ] **Smaller WASM Bundle**: Analyze and optimize Kotlin/WASM binary size.
