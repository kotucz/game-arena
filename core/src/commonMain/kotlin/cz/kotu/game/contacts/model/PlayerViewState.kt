package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsBoardState.ContactId
import cz.kotu.game.contacts.model.ContactsBoardState.ContactType
import cz.kotu.game.contacts.model.ContactsBoardState.Player
import cz.kotu.game.contacts.model.ContactsBoardState.ResolveMultiConnect
import kotlinx.serialization.Serializable

@Serializable
data class PlayerViewState(
    val you: Player,
    val pool: List<Contact>,
    val racks: List<Rack>,
    val board: ContactsBoardState,
    val allowedActionTypes: Set<ActionType>,
    val resolveMultiConnect: ResolveMultiConnect?,
) {
    @Serializable
    data class Contact(
        val id: ContactId?,
        val number: Int?,
        val type: ContactType?,
        val solved: Boolean,
        val hint: String?,
    )

    @Serializable
    data class Rack(
        val owner: Player,
        val contacts: List<Contact>,
    )


    companion object {
        fun empty() = PlayerViewState(
            you = Player(""),
            pool = listOf(),
            racks = listOf(),
            board = ContactsBoardState.empty(),
            allowedActionTypes = setOf(),
            resolveMultiConnect = null,
        )
    }
}

/**
 * Sanitize game state from player's PoV
 */
fun ContactsGameState.sanitizedPlayerView(player: Player): PlayerViewState {

    val pool = board.pool.map { c ->
        PlayerViewState.Contact(
            id = null, // no position reference
            number = c.number,
            type = c.type,
            solved = board.isSolved(c),
            hint = board.getHint(c),
        )
    }

    val racks = board.racks.map { rack ->
        val contacts = board.rackContacts(rack).map { c ->

            val visible = board.isContactVisibleToPlayer(c, player)

            val hint = rack.hint(c)

            PlayerViewState.Contact(
                id = c.id,
                number = if (visible) c.number else null,
                type = if (visible) c.type else null,
                solved = board.isSolved(c),
                hint = hint,
            )
        }

        PlayerViewState.Rack(
            owner = rack.owner,
            contacts = contacts,
        )
    }

    return PlayerViewState(
        you = player,
        pool = pool,
        racks = racks,
        board = board,
        allowedActionTypes = board.allowedActionTypes,
        resolveMultiConnect = board.resolveMultiConnect,
    )
}
