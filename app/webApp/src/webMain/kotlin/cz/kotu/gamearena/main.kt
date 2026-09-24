package cz.kotu.gamearena

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import cz.kotu.common.PushToken
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initNapier()

    ComposeViewport {
        App(
            requestPushToken = {
                suspendCancellableCoroutine { continuation ->
                    fetchWebPushToken { token, tokenId ->
                        continuation.resume(token?.let { PushToken(it, tokenId) })
                    }
                }
            },
        )
    }
}
