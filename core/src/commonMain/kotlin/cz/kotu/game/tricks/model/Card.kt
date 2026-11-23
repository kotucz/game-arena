package cz.kotu.game.tricks.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
enum class Suit(val label: String, val isTrump: Boolean = false) {
    Blue("Blue"),
    Green("Green"),
    Yellow("Yellow"),
    Pink("Pink"),
    Trump("Trump", isTrump = true),
}

@Serializable
@JvmInline
value class Rank(val value: Int) : Comparable<Rank> {
    override fun compareTo(other: Rank): Int = value.compareTo(other.value)
    override fun toString(): String = value.toString()
}

@Serializable
data class Card(
    val suit: Suit,
    val rank: Rank,
) : Comparable<Card> {
    override fun compareTo(other: Card): Int {
        val suitComp = suit.ordinal.compareTo(other.suit.ordinal)
        return if (suitComp != 0) suitComp else rank.compareTo(other.rank)
    }

    override fun toString(): String = "${suit.label.first()}$rank"
}
