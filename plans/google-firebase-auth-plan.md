# Google/Firebase Authentication Plan

## Direction
The app should move to Firebase-first authentication. Google sign-in should be handled by KMPauth on the client, while the server verifies the Firebase ID token and treats the Firebase UID as the canonical user identity.

## Why this is the right model
- Firebase UID is stable across devices and auth flows.
- Firebase already exists in the project for push notifications.
- Google auth via KMPauth is cross-platform and fits the KMP architecture.
- The current username/password model is a prototype-only identity scheme and should be phased out because the app is not live yet.

## Identity model
- Canonical identity: Firebase UID
- UI/display name: username
- Profile metadata: email, provider, avatar if needed
- Session ownership: session is mapped to a user record keyed by Firebase UID, not raw username

## Server changes
- Add a Firebase-backed auth endpoint such as `POST /api/auth/firebase`.
- Verify the Firebase ID token server-side with Firebase Admin.
- Create or update the user record keyed by the verified `firebaseUid`.
- Use `UserPrincipal(userId, username)` rather than only `username`.
- Continue to use secure session cookies for the authenticated app session.

## Client changes
- Replace username/password login UI with a Google/Firebase sign-in experience.
- KMPauth should provide the Google authentication flow.
- Store the Firebase or Google identity token as needed for authenticated API calls.
- Move app auth state to user identity rather than username-only state.

## Push notifications
- Keep the notification token flow scoped to the same stable identity.
- If the web-push integration later requires changes, fix it as a compatibility follow-up without changing the Firebase-first auth direction.

## Migration note
Because the app is not live yet, we can replace the current username/password auth model directly rather than designing a complex migration path.
