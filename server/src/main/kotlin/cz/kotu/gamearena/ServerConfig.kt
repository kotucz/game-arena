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
    // Optional VAPID keys (base64 url-safe public key and private key) for web-push
    val vapidPublicKey: String? = null,
    val vapidPrivateKey: String? = null,
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

            val webpushConfig = if (config.hasPath("webpush")) config.getConfig("webpush") else null
            val vapidPublic = webpushConfig?.getString("publicKey")
            val vapidPrivate = webpushConfig?.getString("privateKey")

            return ServerConfig(
                port = config.getInt("port"),
                webRoot = File(config.getString("web-root")),
                databaseFile = File(config.getString("database-path")),
                adminUsername = adminConfig.getString("username"),
                adminPassword = adminConfig.getString("password"),
                firebaseConfigFile = File(firebaseConfig.getString("googleCredentialsPath")),
                vapidPublicKey = vapidPublic,
                vapidPrivateKey = vapidPrivate,
            )
        }
    }
}
