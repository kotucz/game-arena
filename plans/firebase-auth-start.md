# Google / Firebase Authentication Plan

## Summary

The current auth system is username/password based and uses the username as the app identity. That needs to change to a provider-based identity model, with Firebase UID as the canonical user identifier.

This matches the project direction:
- KMPauth should handle Google OAuth / sign-in UI on the client
- Firebase ID token should be verified by the server
- the server should authorize users by stable Firebase UID, not by username

## Current state in the repo

The existing implementation is still built around username/password:

- `app/shared/.../AuthManager.kt`
    - auth state is based on `currentUsername`
    - login/register both store the username as the authenticated identity

- `server/.../routes/AuthRoutes.kt`
    - `/api/login` and `/api/register` accept `username` + `password`
    - creates a server session tied to the username

- `server/.../User.kt`
    - `User` table uses `username` as the main key
    - `passwordHash` is stored and checked

- `server/.../plugins/Security.kt`
    - `UserPrincipal(username)` is used as the authenticated principal
    - sessions are associated with username

This is the key architectural issue: username is treated as the identity, but for Google/Firebase auth the stable identity should be a provider-issued UID.

## Recommended design

### Canonical user ID
Use Firebase UID as the primary identity.

Recommended identity fields:
- `firebaseUid`: stable auth identity, unique, primary key or unique indexed field
- `username`: display name / game handle
- `email`: profile metadata
- `authProvider`: `"google"` / `"password"` (if legacy login remains temporarily)

This keeps the user identity stable across devices and providers, while still allowing a friendlier username for game UI.

## Why KMPauth + Firebase is the right combination

KMPauth:
- provides cross-platform Google sign-in flow
- works well for desktop/web/mobile app clients

Firebase:
- gives a stable user UID
- allows Google provider integration
- lets backend verify Firebase ID tokens securely
- matches the app’s existing Firebase push infrastructure

## Server-side flow

### New auth flow
1. Client signs in with Google via KMPauth or Firebase Auth
2. Client receives Firebase ID token
3. Client sends token to server endpoint such as:
    - `POST /api/auth/google`
    - or `POST /api/auth/firebase`
4. Server verifies token using Firebase Admin SDK
5. Server finds or creates user by `firebaseUid`
6. Server creates session cookie
7. Server principal is the user record, not raw username

### Session model
The session should store the user identity, not the display username.

Instead of:
- session -> username

Prefer:
- session -> userId / firebaseUid

This avoids identity drift if the username changes later.

## Required code changes

### 1. Replace username-based identity in server
Files impacted:
- `server/.../User.kt`
- `server/.../plugins/Security.kt`
- `server/.../routes/AuthRoutes.kt`

Changes:
- add `firebaseUid` column / field
- use it as unique identity
- make `username` a profile field rather than authentication key
- update `UserPrincipal` to carry user id / username

### 2. Replace current auth manager model
Files impacted:
- `app/shared/.../AuthManager.kt`

Changes:
- replace `currentUsername`-driven logic with `currentUserId` / `currentUser`
- login flow should become Google/Firebase sign-in, not username/password form
- maintain auth state based on user session

### 3. Replace login UI
Files impacted:
- `app/shared/.../AuthScreen.kt`

Changes:
- remove username/password form
- add “Continue with Google” / Firebase auth action
- optionally retain legacy email/password flow only temporarily as a migration path

### 4. Keep session cookies but bind them to stable user identity
Files impacted:
- `server/.../plugins/Security.kt`

Changes:
- `createSession(call, database, userId)` rather than `username`
- principal should carry `userId` and `username`

## Migration strategy

A safe migration path is:

1. Introduce `firebaseUid` alongside existing `username` model
2. Add Google/Firebase login endpoint
3. Verify existing users by matching `firebaseUid` on first login
4. If a user already has a password account, link it to the Google identity
5. Keep password auth only as a temporary legacy path
6. After migration, drop username/password as the main auth model

## Recommendation

I recommend:
- drop the current username/password-centric auth as the primary path
- use Google sign-in via KMPauth
- use Firebase UID as the canonical user identifier
- keep username only as a user-visible handle

That is the cleanest architecture for this project and aligns with both the existing Firebase infrastructure and the probable future multi-client app design.

## Final verdict

Yes: Firebase UID should be the key user identifier.

Yes: KMPauth is a good fit for Google login.

Yes: the current username/password model can be dropped if the app is moving to Google-first auth.

If you want to keep migration compatibility, keep email/password only temporarily, but make UID-based auth the long-term identity source.