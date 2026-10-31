package cz.kotu.gamearena

import kotlin.time.Instant

data class RunningGameInfo(
    val id: String,
    val type: String,
    val playerUids: List<String>,
    val createdAt: Instant,
    val status: String,
)
