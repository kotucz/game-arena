@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@file:JsQualifier("GameArenaPush")

package cz.kotu.gamearena

import kotlin.js.JsQualifier

external fun fetchWebPushToken(onResult: (token: String?, tokenId: String?) -> Unit)
