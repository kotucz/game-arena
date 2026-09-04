package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsBoardState.ContactId
import cz.kotu.game.contacts.model.ContactsBoardState.Player
import cz.kotu.game.contacts.model.ContactsBoardState.ResolveMultiConnect
import kotlinx.serialization.Serializable

@Serializable
data class PlayerViewState(
    val you: Player,
    val pool: List<PoolContact>,
    val racks: List<Rack>,
    val board: ContactsBoardState,
    val allowedActionTypes: Set<ActionType>,
    val resolveMultiConnect: ResolveMultiConnect?,
    val lastActionResult: ContactsBoardState.ActionResult,
) {
    @Serializable
    data class PoolContact(
        val value: ContactsBoardState.ContactValue?,
        val solved: Boolean,
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
            allowedActionTypes = setOf(),
            resolveMultiConnect = null,
            lastActionResult = ContactsBoardState.ActionResult(),
        )
    }
}

/**
 * Sanitize game state from player's PoV
 */
fun ContactsGameState.sanitizedPlayerView(player: Player): PlayerViewState {

    val pool = board.pool.map { c ->
        PlayerViewState.PoolContact(
            value = c.value,
            solved = board.isSolved(c),
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
        allowedActionTypes = board.allowedActionTypes,
        resolveMultiConnect = board.resolveMultiConnect,
        lastActionResult = board.lastActionResult,
    )
}
