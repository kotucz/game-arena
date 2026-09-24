@file:OptIn(ExperimentalWasmJsInterop::class)

package cz.kotu.common

import kotlinx.coroutines.await

internal actual suspend fun fetchPushToken(): String? {
    val token = fetchWebPushToken().await() ?: return null
    return asKotlinString(token)
}

internal actual fun getPersistentPushTokenId(): String? = persistentWebPushTokenId()

private fun asKotlinString(value: JsAny): String = js("String(value)")
