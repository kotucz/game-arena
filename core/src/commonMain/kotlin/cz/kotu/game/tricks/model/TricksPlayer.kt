package cz.kotu.game.tricks.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
@JvmInline
value class TricksPlayerId(val value: String) {
    override fun toString(): String = value
}

@Serializable
data class TricksPlayer(
    val id: TricksPlayerId,
    val username: String,
)
