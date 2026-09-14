package cz.kotu.gamearena.routes

import io.github.aakira.napier.Napier
import io.ktor.http.CacheControl
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.http.content.staticFiles
import io.ktor.server.response.respondFile
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import java.io.File

fun Route.staticRoutes(webRoot: File) {
    Napier.i("staticRoutes webRoot: $webRoot")

    suspend fun respondAppShell(call: ApplicationCall) {
        val file = File(webRoot, "index.html")
        call.response.headers.append(
            HttpHeaders.CacheControl,
            CacheControl.NoCache(CacheControl.Visibility.Private).toString()
        )
        call.respondFile(file)
    }

    get("/") {
        respondAppShell(call)
    }

    // Static assets are versioned and change infrequently, so cache them aggressively.
    // Keep the app shell uncached so clients pick up new bundle hashes after deployment.
    staticFiles("/", webRoot) {
        cacheControl { resource ->
            staticAssetCacheControl(resource)
        }
    }
}

private fun staticAssetCacheControl(resource: File): List<CacheControl> = when {
    resource.name.equals(
        "index.html",
        ignoreCase = true
    ) -> listOf(CacheControl.NoCache(CacheControl.Visibility.Private))

    resource.name.lowercase() in setOf("manifest.webmanifest", "sw.js", "pwa.js") ->
        listOf(CacheControl.NoCache(CacheControl.Visibility.Private))

    resource.extension.lowercase() in setOf("wasm", "js", "css", "svg", "png", "ico", "woff", "woff2", "ttf", "otf") ->
        listOf(CacheControl.MaxAge(365 * 24 * 60 * 60, visibility = CacheControl.Visibility.Public))

    else -> emptyList()
}
