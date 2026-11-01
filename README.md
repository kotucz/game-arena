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

### Server authentication and identity

The server uses Firebase ID-token authentication. Clients send a Firebase identity token as a bearer token (or as `idToken` in the registration request), and the server verifies it through the Firebase Admin SDK before looking up or creating the user record keyed by `firebaseUid`.

Relevant endpoints:

- `POST /api/auth/register` — verifies the Firebase token, ensures a unique username, and creates the user record.
- `POST /api/auth/user` — same registration path retained as a compatibility alias.
- `GET /api/me` — returns the currently authenticated user profile (`uid`, `username`, `pictureUrl`).
- `POST /api/logout` — clears the client-side auth state; there is no longer a server-side session cookie in the active auth model.
- Protected game endpoints require bearer authentication via the `auth-firebase` scheme.

The active auth flow is implemented with Ktor `Authentication` and `UserPrincipal(firebaseUid, username, pictureUrl)`. The stable identity is the Firebase UID rather than a username, which keeps the app aligned with Google/Firebase sign-in and multi-device membership.

This project no longer uses the legacy debug-header login flow or the old username/password session model in production.

---

Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html),
[Compose Multiplatform](https://github.com/JetBrains/compose-multiplatform/#compose-multiplatform),
[Kotlin/Wasm](https://kotl.in/wasm/)…

We would appreciate your feedback on Compose/Web and Kotlin/Wasm in the public Slack
channel [#compose-web](https://slack-chats.kotlinlang.org/c/compose-web). If you face any issues, please report them
on [YouTrack](https://youtrack.jetbrains.com/newIssue?project=CMP).
