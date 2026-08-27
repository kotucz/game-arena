# Executive Summary: KMP SSE Architecture & Networking Troubleshooting

## 1. Architectural Design & Lifecycle Management

### Server-Sent Events Lifecycle (`respondSSE`)
* **Graceful Stream Termination:** SSE relies on persistent HTTP connections and has no native end-of-stream signal. To close a session cleanly (e.g., game over):
  1. Server emits a terminal event: `ServerSentEvent(event = "game-over", data = "FINISHED")`.
  2. Ktor completes the `respondSSE` block normally, closing the HTTP stream cleanly.
  3. Client checks for `event == "game-over"`, updates local state to `Finished`, and exits the reconnection `while` loop to prevent automatic retries.
* **Server-Side Heartbeats (Keep-Alive):** Interleave a 15-second comment ping flow (`ServerSentEvent(comments = "ping")`) with domain event flows using Kotlin's `merge()`. This resets proxy/NAT idle timers without polluting client data payloads.

### Client-Side Reactive Connection Management
* **Lifecycle-Bound Flow (`stateIn` + `WhileSubscribed`):**
  * Convert cold SSE flows into a hot `StateFlow` via `stateIn(scope, SharingStarted.WhileSubscribed(5_000), initialState)`.
  * The `5_000` ms grace period prevents socket tearing during screen rotations/configuration changes.
* **UI Collection Mechanics:**
  * **`collectAsStateWithLifecycle()`** (from `androidx.lifecycle.compose`): Pauses collection when the UI drops below the `STARTED` state (e.g., app in background / screen locked). Unsubscribing triggers the 5-second `WhileSubscribed` timeout, closing the underlying OkHttp SSE socket to save battery, mobile data, and backend resources.
  * **`collectAsState()`**: Maintains active subscription as long as the composable is in memory, leaving network streams open in the background (unsuitable for active streaming).

---

## 2. Network Proxy & Infrastructure Troubleshooting

### Proxy Topology
* **Setup:** KMP Client → Cloudflare Edge → Cloudflare Tunnel (`cloudflared` Add-on) → Docker Container (Ktor Server on Home Assistant at `192.168.x.x:yyyy`).

### Issues Diagnosed & Resolved

| Symptom | Primary Cause | Resolution |
| :--- | :--- | :--- |
| **502 Bad Gateway / Tunnel Crashes** | UDP/QUIC packet loss and NAT timeout drops on port 7844 between local router and Cloudflare edge. | Disabled HTTP/3 (QUIC) in Cloudflare Dashboard under **Speed → Optimization → Protocol Optimization**, forcing TCP fallback. |
| **5-Second TTFB Latency on POST Requests** | HTTP/1.1 head-of-line blocking and chunked response buffering inside `cloudflared` during active SSE connections. | Added `disableChunkedEncoding: true` to the Home Assistant Cloudflare add-on schema for `gamearena.kotu.cz`. |
| **Add-on Schema Warnings** | Injecting unsupported YAML keys (`protocol: http2`, `originRequest`, `post_arguments`) into strict HA Supervisor add-on config. | Cleaned up invalid YAML options and utilized valid schema flags (`disableChunkedEncoding: true`). |

---

## 3. Final Target Configuration

### Home Assistant Cloudflare Tunnel Add-on (`config.yaml`)
```yaml
external_hostname: *
additional_hosts:  
  - hostname: gamearena.kotu.cz
    service: http://192.168.x.x:yyyy
    disableChunkedEncoding: true
```

### Verification Checklist
- [x] Client terminates SSE loop on `game-over` event.
- [x] Client uses `collectAsStateWithLifecycle()` to pause network streams in background.
- [x] Server emits 15s comment pings to prevent idle proxy timeouts.
- [x] HTTP/3 (QUIC) disabled at Cloudflare Edge to enforce stable TCP/H2 multiplexing.
- [x] `disableChunkedEncoding: true` set in HA Tunnel config to resolve POST response latency.
