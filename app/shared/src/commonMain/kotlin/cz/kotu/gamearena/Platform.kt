package cz.kotu.gamearena

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
