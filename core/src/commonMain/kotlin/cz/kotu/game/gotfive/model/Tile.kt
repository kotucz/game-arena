package cz.kotu.game.gotfive.model

data class Tile(val number: Int) {
    val color: Color = Color.entries[number % Color.entries.size]
    val dots: Int = 1 + (number / 5) % 3
}

enum class Color(
    val hex: String,
    val red: Int,
    val green: Int,
    val blue: Int,
) {
    Red("#E53935", 229, 57, 53),
    Green("#4CAF50", 76, 175, 80),
    Blue("#1E88E5", 30, 136, 229),
    Yellow("#FFB300", 255, 179, 0),
    Purple("#8E24AA", 142, 36, 170),
}

object Tiles {
    val all: List<Tile> = (0..59).map { Tile(it) }
}
