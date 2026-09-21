package cz.kotu.gamearena

/**
 * Public client-side configuration for authentication and Firebase services.
 *
 * These values are public by design (delivered to web browsers and mobile apps)
 * and are used by client SDKs to identify the Firebase project.
 *
 * Administrative keys (such as service account private keys) are kept strictly on the server.
 */
object ClientAuthConfig {
    const val GOOGLE_WEB_CLIENT_ID = "541489886592-p8aksm43vf7qop0isgla56r0lqj8k27t.apps.googleusercontent.com"
    const val FIREBASE_API_KEY = "AIzaSyBmUpymqf9ziLADa9tV5pg3s-9mzzAAFuc"
    const val FIREBASE_PROJECT_ID = "game-arena-c1035"
    const val FIREBASE_APPLICATION_ID = "1:541489886592:android:065df369be9eaf9b38bd65"
}
