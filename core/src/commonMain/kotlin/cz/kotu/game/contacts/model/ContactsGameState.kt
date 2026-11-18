package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsBoardState.ActionType
import cz.kotu.game.contacts.model.ContactsBoardState.ContactId
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.GameOver
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.ResolveMultiConnect
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.StandardTurn
import kotlinx.serialization.Serializable

@Serializable
enum class ContactsGameOverReason {
    AllContactsSolved,
    RedConnected,
    EmptyGame,
}

@Serializable
data class ContactsGameState(
    val board: ContactsBoardState,

    /** Ordered roster with account identity; board Player values remain game-internal IDs. */
    val players: List<ContactsPlayer>,

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
            val resolveMultiConnect: ContactsBoardState.ResolveMultiConnect,
        ) : GamePhase()
        /** Game is over. */
        @Serializable
        data class GameOver(
            val reason: ContactsGameOverReason,
        ) : GamePhase()
    }

    @Serializable
    sealed class PlayerActionStatus {
        @Serializable
        data class PlayerTurn(val player: ContactsBoardState.Player) : PlayerActionStatus()

        @Serializable
        data class OriginalContact(val matchKey: String) : PlayerActionStatus()

        @Serializable
        data class WaitingForMultiConnectResolution(val player: ContactsBoardState.Player) : PlayerActionStatus()

        @Serializable
        data class GameOver(val reason: ContactsGameOverReason) : PlayerActionStatus()
    }

    @Serializable
    data class PlayerActions(
        val status: PlayerActionStatus? = null,
        val allowedActionTypes: Set<ActionType> = setOf(),
        val resolveMultiConnectContacts: Set<ContactsBoardState.ContactId>? = null,
        /** Each group is a complete legal SoloConnectRest selection for this turn. */
        val soloConnectRestGroups: List<Set<ContactId>> = emptyList(),
    )

    fun getAvailablePlayerActions(player: ContactsBoardState.Player): PlayerActions {
        return when (gamePhase) {
            is StandardTurn -> {
                if (player == gamePhase.activePlayer) {
                    val soloConnectRestGroups = board.soloConnectRestGroups(player)
                    PlayerActions(
                        allowedActionTypes = if (board.canFinishReds(player)) {
                            setOf(ActionType.FinishReds)
                        } else {
                            buildSet {
                                if (board.canStandardConnect(player)) {
                                    add(ActionType.StandardConnect)
                                }
                                if (board.hasAvailableOtherDoubleBonus(player)) {
                                    add(ActionType.DoubleConnect)
                                }
                                if (board.hasAvailableOtherTripleBonus(player)) {
                                    add(ActionType.TripleConnect)
                                }
                                if (board.hasAvailableMyDoubleBonus(player)) {
                                    add(ActionType.MyDoubleConnect)
                                }
                                if (soloConnectRestGroups.isNotEmpty()) {
                                    add(ActionType.SoloConnectRest)
                                }
                                if (board.hasAvailableHintBonus(player)) {
                                    add(ActionType.AddHint)
                                }
                            }
                        },
                        soloConnectRestGroups = soloConnectRestGroups,
                    )
                } else {
                    PlayerActions(
                        status = PlayerActionStatus.PlayerTurn(gamePhase.activePlayer),
                    )
                }
            }
            is ResolveMultiConnect -> {
                if (player == gamePhase.resolveMultiConnect.targetPlayer) {
                    PlayerActions(
                        status = PlayerActionStatus.OriginalContact(
                            board.requireContact(gamePhase.resolveMultiConnect.originalContact).matchKey,
                        ),
                        allowedActionTypes = setOf(ActionType.ResolveMultiConnect),
                        resolveMultiConnectContacts = gamePhase.resolveMultiConnect.targetContacts,
                    )
                } else {
                    PlayerActions(
                        status = PlayerActionStatus.WaitingForMultiConnectResolution(
                            gamePhase.resolveMultiConnect.targetPlayer,
                        ),
                    )
                }
            }
            is GameOver -> {
                PlayerActions(status = PlayerActionStatus.GameOver(gamePhase.reason))
            }
        }
    }

    companion object {
        fun empty(): ContactsGameState = ContactsGameState(
            board = ContactsBoardState.empty(),
            players = listOf(),
            gamePhase = GameOver(reason = ContactsGameOverReason.EmptyGame),
        )
    }
}
