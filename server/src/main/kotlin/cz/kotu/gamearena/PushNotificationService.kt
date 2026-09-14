package cz.kotu.gamearena

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.ErrorCode
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import io.github.aakira.napier.Napier
import java.io.File
import java.io.FileInputStream

interface PushNotificationService {
    suspend fun sendToUser(
        username: String,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): Int

    suspend fun sendToUsers(
        usernames: Collection<String>,
        title: String,
        body: String,
        data: Map<String, String> = emptyMap(),
    ): Int
}

class NoopPushNotificationService : PushNotificationService {
    override suspend fun sendToUser(
        username: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Int = 0

    override suspend fun sendToUsers(
        usernames: Collection<String>,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Int = 0
}

class FirebaseAdminPushNotificationService(
    private val database: AppDatabase,
    private val serverConfig: ServerConfig,
    private val firebaseMessaging: FirebaseMessaging? = null,
) : PushNotificationService {

    private val messaging: FirebaseMessaging? = firebaseMessaging ?: runCatching {
        val firebaseConfigFile = serverConfig.firebaseConfigFile
        Napier.i("firebaseMessaging $firebaseConfigFile")
        ensureFirebaseInitialized(firebaseConfigFile)
        FirebaseMessaging.getInstance().also {
            Napier.i("firebaseMessaging READY $it")
        }
    }.onFailure {
        Napier.e("firebaseMessaging", it)
    }.getOrNull()

    override suspend fun sendToUser(
        username: String,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Int = sendToUsers(listOf(username), title, body, data)

    override suspend fun sendToUsers(
        usernames: Collection<String>,
        title: String,
        body: String,
        data: Map<String, String>,
    ): Int {
        val uniqueUsernames = usernames.filter { it.isNotBlank() }.distinct()
        if (uniqueUsernames.isEmpty() || messaging == null) return 0

        val tokens = database.pushTokenDao().findByUsernames(uniqueUsernames)
            .filter { it.service == "fcm" }

        var sent = 0
        for (token in tokens) {
            val message = Message.builder()
                .setToken(token.token)
                .setNotification(
                    Notification.builder()
                        .setTitle(title)
                        .setBody(body)
                        .build(),
                )
                .putAllData(data)
                .build()

            try {
                messaging.send(message)
                sent += 1
            } catch (exception: FirebaseMessagingException) {
                val staleRegistrationToken = isStaleRegistrationToken(exception)
                Napier.w(exception) { "staleRegistrationToken: $staleRegistrationToken ${token.tokenId}" }
                if (staleRegistrationToken) {
                    database.pushTokenDao().delete(token.tokenId, token.username)
                    continue
                }
                throw exception
            }
        }
        return sent
    }

    private fun ensureFirebaseInitialized(config: File) {
        if (FirebaseApp.getApps().isNotEmpty()) return

        val credentials = GoogleCredentials.fromStream(FileInputStream(config))
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        FirebaseApp.initializeApp(options)
    }

    private fun isStaleRegistrationToken(exception: FirebaseMessagingException): Boolean {
        return when (exception.errorCode) {
            ErrorCode.NOT_FOUND, ErrorCode.INVALID_ARGUMENT -> {
                true
            }

            else -> false
        }
    }
}
