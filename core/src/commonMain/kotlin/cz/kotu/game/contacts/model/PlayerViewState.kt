package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.ContactId
import cz.kotu.game.contacts.model.ContactsBoardState.Player
import kotlinx.serialization.Serializable

@Serializable
data class PlayerViewState(
    val you: Player,
    val pool: List<PoolContact>,
    val racks: List<Rack>,
    @Deprecated(
        message = "PlayerViewState.board is deprecated; use racks/contact solved flags instead.",
        level = DeprecationLevel.WARNING,
    )
    val board: ContactsBoardState,
    val faults: Int,
    val actions: ContactsGameState.PlayerActions,
    val lastActionResult: ContactsBoardState.ActionResult,
) {
    @Serializable
    data class PoolContact(
        val value: ContactsBoardState.ContactValue?,
        val solved: Boolean,
        val help: String?,
    )


    @Serializable
    data class RackContact(
        val id: ContactId,
        val value: ContactsBoardState.ContactValue?,
        val solved: Boolean,
        val hint: String?,
    )

    @Serializable
    data class Rack(
        val owner: Player,
        val contacts: List<RackContact>,
    )


    companion object {
        fun empty() = PlayerViewState(
            you = Player(""),
            pool = listOf(),
            racks = listOf(),
            board = ContactsBoardState.empty(),
            faults = 0,
            actions = ContactsGameState.PlayerActions(),
            lastActionResult = ContactsBoardState.ActionResult(),
        )
    }
}

fun PlayerViewState.isSolved(contactId: ContactId): Boolean {
    return racks.any { rack -> rack.contacts.any { it.id == contactId && it.solved } }
}

/**
 * Sanitize game state from player's PoV
 */
fun ContactsGameState.sanitizedPlayerView(player: Player, actions: ContactsGameState.PlayerActions): PlayerViewState {

    val pool = board.pool.map { c ->
        PlayerViewState.PoolContact(
            value = c.value,
            solved = board.isSolved(c),
            help = if (!board.isContactVisibleToPlayer(c, player)) "?" else null,
        )
    }

    val racks = board.racks.map { rack ->
        val contacts = board.rackContacts(rack).map { c ->

            val visible = board.isContactVisibleToPlayer(c, player)

            val hint = rack.hint(c)

            PlayerViewState.RackContact(
                id = c.id,
                value = if (visible) c.value else null,
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
        faults = board.faults,
        actions = actions,
        lastActionResult = board.lastActionResult,
    )
}
