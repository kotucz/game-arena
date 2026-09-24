package cz.kotu.gamearena

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    initNapier()

    ComposeViewport {
        App(
            requestPushToken = {
                suspendCancellableCoroutine { continuation ->
                    fetchWebPushToken { token -> continuation.resume(token) }
                }
            },
        )
    }
}
