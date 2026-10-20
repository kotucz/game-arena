package cz.kotu.game.contacts.ai

import java.util.concurrent.ConcurrentHashMap

object PromptLoader {
    private val cache = ConcurrentHashMap<String, String>()

    fun load(resourcePath: String): String {
        return cache.getOrPut(resourcePath) {
            val normalizedPath = resourcePath.removePrefix("/")
            val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(normalizedPath)
                ?: object {}.javaClass.classLoader.getResourceAsStream(normalizedPath)
                ?: error("Prompt resource file not found on classpath: $resourcePath")

            stream.bufferedReader().use { it.readText() }
        }
    }
}