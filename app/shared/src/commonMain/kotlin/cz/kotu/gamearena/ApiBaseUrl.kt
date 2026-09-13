package cz.kotu.gamearena

import kotlin.jvm.JvmInline

@JvmInline
value class ApiBaseUrl(val value: String) {
    fun endpoint(path: String): String = value.trimEnd('/') + "/" + path.trimStart('/')
}

/** Platform-specific default base URL; platform actuals provide sensible defaults. */
expect fun defaultApiBaseUrl(): String
