@file:OptIn(kotlin.js.ExperimentalWasmJsInterop::class)
@file:JsQualifier("GameArenaPush")

package cz.kotu.common

import kotlin.js.JsAny
import kotlin.js.JsQualifier
import kotlin.js.Promise

internal external fun fetchWebPushToken(): Promise<JsAny?>

internal external fun persistentWebPushTokenId(): String?
