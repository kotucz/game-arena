package cz.kotu.gamearena

import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class GamesManager(
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
) {
    private val games = ConcurrentHashMap<String, Game>()

    @Synchronized
    fun createContactsGame(): Game {
        var id: String
        do {
            id = idGenerator()
            require(id.isNotBlank()) { "Game ID must not be blank" }
        } while (games.containsKey(id))

        val game = Game(
            id = id,
            contacts = ServerContactsGameFacade(
                ContactsGameFacadeImpl(
                    listOf(
                        ContactsBoardState.Player("alice"),
                        ContactsBoardState.Player("bob"),
                    ),
                ),
            ),
        )
        games[id] = game
        return game
    }

    fun game(id: String): Game? = games[id]

    data class Game(
        val id: String,
        val contacts: ServerContactsGameFacade,
    )
}
