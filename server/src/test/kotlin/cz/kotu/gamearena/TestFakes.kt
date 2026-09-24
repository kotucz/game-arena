package cz.kotu.gamearena

import me.tatarka.inject.annotations.Provides
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import java.io.File
import java.nio.file.Files

private fun createTempDatabase(): AppDatabase {
    val tmp = Files.createTempFile("gamearena-test-", ".db").toFile()
    tmp.deleteOnExit()
    tmp.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(tmp.path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(*ALL_MIGRATIONS)
        .build()
}

class FakeTokenVerifier : TokenVerifier {
    private val claimsMap = java.util.concurrent.ConcurrentHashMap<String, FirebaseUserClaims>()

    fun setClaims(token: String, claims: FirebaseUserClaims) {
        claimsMap[token] = claims
    }

    override fun verify(idToken: String): FirebaseUserClaims? {
        claimsMap[idToken]?.let { return it }
        // Bearer tokens must use token68 characters; a colon is not valid.
        if (idToken.startsWith("test-token-")) {
            val uid = idToken.removePrefix("test-token-")
            return FirebaseUserClaims(uid = uid, email = "$uid@example.com", name = uid)
        }
        return null
    }
}

class TestFakes(
    @get:Provides
    val database: AppDatabase = createTempDatabase(),
    @get:Provides
    val notificationService: PushNotificationService = NoopPushNotificationService(),
    @get:Provides
    val serverConfig: ServerConfig = ServerConfig(
        port = 8080,
        webRoot = File("."),
        databaseFile = File("data/test-gamearena.db"),
        adminUsername = "admin",
        adminPassword = "test-secret",
        firebaseConfigFile = File("."),
    ),
    @get:Provides
    val tokenVerifier: TokenVerifier = FakeTokenVerifier(),
)
