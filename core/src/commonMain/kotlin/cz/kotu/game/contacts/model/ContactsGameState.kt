package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.GameOver
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.ResolveMultiConnect
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.StandardTurn
import kotlinx.serialization.Serializable

@Serializable
data class ContactsGameState(
    val board: ContactsBoardState,

    val players: List<ContactsBoardState.Player>,

    val gamePhase: GamePhase,

    ) {

    @Serializable
    sealed class GamePhase {
        /** Standard turn — this player plays an action. */
        @Serializable
        data class StandardTurn(
            val activePlayer: ContactsBoardState.Player,
        ) : GamePhase()
        /** A multi-connect needs resolution — the target player must choose. */
        @Serializable
        data class ResolveMultiConnect(
            val restorePlayer: ContactsBoardState.Player, // who started this turn (for resuming after resolve)
            val resolveMultiConnect: ContactsBoardState.ResolveMultiConnect,
        ) : GamePhase()
        /** Game is over. */
        @Serializable
        data class GameOver(
            val message: String,
        ) : GamePhase()
    }

    @Serializable
    data class PlayerActions(
        val actionStatusText: String? = null,
        val allowedActionTypes: Set<ActionType> = setOf(),
        val resolveMultiConnectContacts: Set<ContactsBoardState.ContactId>? = null,
    )

    fun getAvailablePlayerActions(player: ContactsBoardState.Player): PlayerActions {
        return when (gamePhase) {
            is StandardTurn -> {
                if (player == gamePhase.activePlayer) {
                    PlayerActions(
                        allowedActionTypes = setOf(
                            ActionType.StandardConnect,
                            ActionType.DoubleConnect,
                            ActionType.TripleConnect,
                            ActionType.MyDoubleConnect,
                            ActionType.SoloConnectRest,
                            ActionType.FinishReds,
                            ActionType.AddHint,
                        ),
                    )
                } else {
                    PlayerActions(
                        actionStatusText = "It is ${gamePhase.activePlayer.username}'s turn",
                    )
                }
            }
            is ResolveMultiConnect -> {
                if (player == gamePhase.resolveMultiConnect.targetPlayer) {
                    PlayerActions(
                        actionStatusText = "Original contact: ${board.requireContact(gamePhase.resolveMultiConnect.originalContact).matchKey}",
                        allowedActionTypes = setOf(ActionType.ResolveMultiConnect),
                        resolveMultiConnectContacts = gamePhase.resolveMultiConnect.targetContacts,
                    )
                } else {
                    PlayerActions(
                        actionStatusText = "Waiting for ${gamePhase.resolveMultiConnect.targetPlayer.username} to resolve the multi-connect",
                    )
                }
            }
            is GameOver -> {
                PlayerActions(actionStatusText = gamePhase.message)
            }
        }
    }

    companion object {
        fun empty(): ContactsGameState = ContactsGameState(
            board = ContactsBoardState.empty(),
            players = listOf(),
            gamePhase = GameOver("Empty game"),
        )
    }
}
