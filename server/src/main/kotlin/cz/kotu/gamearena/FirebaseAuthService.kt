package cz.kotu.gamearena

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import io.github.aakira.napier.Napier
import java.io.File
import java.io.FileInputStream

data class FirebaseUserClaims(
    val uid: String,
    val email: String?,
    val name: String?,
)

class FirebaseTokenVerifier(
    private val serverConfig: ServerConfig,
) {
    private val firebaseAuth: FirebaseAuth? = runCatching {
        ensureFirebaseInitialized(serverConfig.firebaseConfigFile)
        FirebaseAuth.getInstance()
    }.onFailure {
        Napier.w("Firebase auth verification unavailable", it)
    }.getOrNull()

    fun verify(idToken: String): FirebaseUserClaims? {
        if (idToken.isBlank() || firebaseAuth == null) return null
        return runCatching {
            val token = firebaseAuth!!.verifyIdToken(idToken)
            FirebaseUserClaims(
                uid = token.uid,
                email = token.email,
                name = token.name,
            )
        }.onFailure {
            Napier.w("Firebase ID token verification failed", it)
        }.getOrNull()
    }

    private fun ensureFirebaseInitialized(config: File) {
        if (!config.exists() || !config.isFile()) {
            throw IllegalStateException("Firebase config file not found: ${config.absolutePath}")
        }
        if (FirebaseApp.getApps().isNotEmpty()) return

        val credentials = GoogleCredentials.fromStream(FileInputStream(config))
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        FirebaseApp.initializeApp(options)
    }
}
