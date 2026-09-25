package cz.kotu.gamearena

import com.mmk.kmpauth.core.KMPAuth
import com.mmk.kmpauth.core.KMPAuthConfiguration
import com.mmk.kmpauth.firebase.firebase
import com.mmk.kmpauth.google.google

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

fun initKmpAuthMobile() {
    KMPAuth.initialize {
        google()
        // firebase is initialized automatically on Android/iOS google-services.json/plist
    }
}

fun initKmpAuthDesktop() {
    KMPAuth.initialize {
        google(
            serverId = ClientAuthConfig.GOOGLE_WEB_CLIENT_ID,
            // redirectUri is used on desktop only
            // must be configured at https://console.cloud.google.com/auth/clients/ Authorized redirect URIs
            redirectUri = "http://localhost:8087/callback",
        )
        firebase()
    }
}

fun initKmpAuthWeb() {
    KMPAuth.initialize {
        google()
        firebase()
    }
}

private fun KMPAuthConfiguration.google() {
    google(
        serverId = ClientAuthConfig.GOOGLE_WEB_CLIENT_ID,
    )
}

private fun KMPAuthConfiguration.firebase() {
    firebase(
        apiKey = ClientAuthConfig.FIREBASE_API_KEY,
        projectId = ClientAuthConfig.FIREBASE_PROJECT_ID,
        applicationId = ClientAuthConfig.FIREBASE_APPLICATION_ID,
    )
}
