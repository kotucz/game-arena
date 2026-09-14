This is a Kotlin Multiplatform project targeting Android, iOS, Web, Desktop (JVM), Server.

* [/app/iosApp](./app/iosApp/iosApp) contains an iOS application. Even if you’re sharing your UI with Compose
  Multiplatform, you need this entry point for your iOS app. This is also where you should add SwiftUI code for your
  project.

* [/app/shared](./app/shared/src) is for code that will be shared across your Compose Multiplatform applications. It
  contains several subfolders:
    - [commonMain](./app/shared/src/commonMain/kotlin) is for code that’s common for all targets.
    - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name. For
      example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
      the [iosMain](./app/shared/src/iosMain/kotlin) folder would be the right place for such calls. Similarly, if you
      want to edit the Desktop (JVM) specific part, the [jvmMain](./app/shared/src/jvmMain/kotlin)
      folder is the appropriate location.

* [/core](./core/src) is for the code that will be shared between all targets in the project. The most important
  subfolder is [commonMain](./core/src/commonMain/kotlin). If preferred, you can add code to the platform-specific
  folders here too.

* [/server](./server/src/main/kotlin) is for the Ktor server application.

### Running the apps

Use the run configurations provided by the run widget in your IDE's toolbar. You can also use these commands and
options:

- Android app: `./gradlew :app:androidApp:assembleDebug`
- Desktop app:
    - Hot reload: `./gradlew :app:desktopApp:hotRun --auto`
    - Standard run: `./gradlew :app:desktopApp:run`
- Server: `./gradlew :server:run`
- Web app:
    - Wasm target (faster, modern browsers): `./gradlew :app:webApp:wasmJsBrowserDevelopmentRun`
    - JS target (slower, supports older browsers): `./gradlew :app:webApp:jsBrowserDevelopmentRun`
- iOS app: open the [/app/iosApp](./app/iosApp) directory in Xcode and run it from there.

When running the production Wasm web app through the server, build the browser
distribution before starting the server:

```shell
./gradlew :app:webApp:wasmJsBrowserDistribution
./gradlew :server:run
```

Open `http://localhost:8080/` and hard-refresh the page after rebuilding if the
browser has cached an older bundle. Web navigation uses hash URLs such as
`http://localhost:8080/#game/<gameId>`.

The browser entry point is also configured as a PWA: it ships a
`manifest.webmanifest`, a service worker for app-shell caching, and an install
button for browsers that support the install prompt. On supported devices the
app can be added to the home screen and launched as a standalone app.

### Running the web app in Docker

Build and start the server with:

```shell
docker compose up --build
```

The Wasm browser distribution is built into the image and served at `http://localhost:8080`.
The server exposes `GET /health` for container or Home Assistant health checks.

### Running tests

Use the run button in your IDE's editor gutter, or run tests using Gradle tasks:

- Android tests: `./gradlew :app:shared:testAndroidHostTest`
- Desktop tests: `./gradlew :app:shared:jvmTest`
- Server tests: `./gradlew :server:test`
- Web tests:
    - Wasm target: `./gradlew :app:shared:wasmJsTest`
    - JS target: `./gradlew :app:shared:jsTest`
- iOS tests: `./gradlew :app:shared:iosSimulatorArm64Test`

### Firebase push notifications

The server can issue Firebase Cloud Messaging pushes from the JVM process using the Firebase Admin SDK. 
Provide a service-account JSON file `application.conf` in `firebase.googleCredentialsPath` before starting the server.
Stored FCM tokens are read from the `push_tokens` table; if delivery responds with stale-registration errors such as `404` or `410`, the server removes that token automatically.

### Server authentication and sessions

The Ktor server uses a cookie-backed session with Ktor authentication. A successful login or registration creates a session cookie named `gamearena_session`, and the server validates the session token against the SQLite-backed `sessions` table.

Relevant endpoints:

- `POST /api/register` — registers a user and immediately creates a session.
- `POST /api/login` — validates credentials and creates a session.
- `POST /api/logout` — removes the DB session and clears the cookie.
- `GET /api/me` — returns the current authenticated username.
- Protected game endpoints require an authenticated session.

The authentication flow is implemented with the Ktor `Sessions` and `Authentication` plugins. Session state is kept server-side in the DB; the cookie only stores the opaque token. The server also exposes the `UserPrincipal` for authenticated route access.

For local test/dev convenience, a debug header may be used temporarily to bypass the normal cookie flow when explicitly enabled in the test environment. This is not production behavior and should remain isolated from the normal auth path.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web). If you face any issues, please report them
on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
