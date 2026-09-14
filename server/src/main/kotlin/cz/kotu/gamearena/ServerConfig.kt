package cz.kotu.gamearena

import com.typesafe.config.ConfigFactory
import io.github.aakira.napier.Napier
import java.io.File

data class ServerConfig(
    val port: Int,
    val webRoot: File,
    val databaseFile: File,
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
            return ServerConfig(
                port = config.getInt("port"),
                webRoot = File(config.getString("web-root")),
                databaseFile = File(config.getString("database-path")),
            )
        }
    }
}
