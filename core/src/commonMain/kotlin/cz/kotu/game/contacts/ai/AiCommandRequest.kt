package cz.kotu.game.contacts.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiCommandRequest(
    val prompt: String,
)
