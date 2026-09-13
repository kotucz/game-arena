package cz.kotu.gamearena

import kotlin.jvm.JvmInline

@JvmInline
value class ApiBaseUrl(val value: String)

/** Platform-specific default base URL; platform actuals provide sensible defaults. */
expect fun defaultApiBaseUrl(): String
