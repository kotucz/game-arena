package cz.kotu.gamearena

import com.typesafe.config.ConfigFactory
import io.github.aakira.napier.Napier
import java.io.File

data class ServerConfig(
    val port: Int,
    val webRoot: File,
    val databaseFile: File,
    val firebaseConfigFile: File,
    val adminUsername: String,
    val adminPassword: String,
    /** Public Firebase web SDK config served to the browser at /api/firebase-config. */
    val firebaseWebConfig: Map<String, String> = emptyMap(),
    /** FCM Web Push VAPID key — included in /api/firebase-config so the browser can call getToken(). */
    val firebaseWebVapidKey: String? = null,
) {
    companion object {
        fun load(): ServerConfig {
            val baseConfig = ConfigFactory.load()
            val overridePath = System.getenv("GAMEARENA_CONFIG")

            Napier.i("ServerConfig override: $overridePath")

            val resolvedConfig = overridePath
                ?.let { File(it) }
                ?.takeIf { it.exists() }
                ?.let { ConfigFactory.parseFileAnySyntax(it).withFallback(baseConfig) }
                ?: baseConfig

            val config = resolvedConfig.getConfig("gamearena")
            val adminConfig = config.getConfig("admin")
            val firebaseConfig = config.getConfig("firebase")

            val webConfig: Map<String, String> =
                if (firebaseConfig.hasPath("webConfig")) {
                    val wc = firebaseConfig.getConfig("webConfig")
                    listOf("apiKey", "authDomain", "projectId", "storageBucket", "messagingSenderId", "appId")
                        .associateWith { wc.getString(it) }
                } else {
                    emptyMap()
                }

            val webVapidKey =
                if (firebaseConfig.hasPath("webVapidKey")) firebaseConfig.getString("webVapidKey") else null

            return ServerConfig(
                port = config.getInt("port"),
                webRoot = File(config.getString("web-root")),
                databaseFile = File(config.getString("database-path")),
                adminUsername = adminConfig.getString("username"),
                adminPassword = adminConfig.getString("password"),
                firebaseConfigFile = File(firebaseConfig.getString("googleCredentialsPath")),
                firebaseWebConfig = webConfig,
                firebaseWebVapidKey = webVapidKey,
            )
        }
    }
}
