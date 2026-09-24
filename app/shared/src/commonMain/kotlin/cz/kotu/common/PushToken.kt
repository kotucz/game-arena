package cz.kotu.common

/** A platform push token and its stable client ID when the platform provides one. */
data class PushToken(
    val token: String,
    val tokenId: String? = null,
)
