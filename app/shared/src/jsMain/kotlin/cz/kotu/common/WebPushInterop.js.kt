@file:JsQualifier("GameArenaPush")

package cz.kotu.common

import kotlin.js.JsQualifier
import kotlin.js.Promise

internal external fun fetchWebPushToken(): Promise<String?>

internal external fun persistentWebPushTokenId(): String?
