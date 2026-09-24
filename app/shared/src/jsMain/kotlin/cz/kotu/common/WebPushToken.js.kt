package cz.kotu.common

import kotlinx.coroutines.await

internal actual suspend fun fetchPushToken(): String? = fetchWebPushToken().await()

internal actual fun getPersistentPushTokenId(): String? = persistentWebPushTokenId()
