package cz.kotu.game.contacts.model

import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.GameOver
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.ResolveMultiConnect
import cz.kotu.game.contacts.model.ContactsGameState.GamePhase.StandardTurn

internal fun ContactsGameState.applyAction(
    player: ContactsBoardState.Player,
    actionType: ContactsBoardState.ActionType,
    playerContacts: Set<ContactsBoardState.ContactId>,
    otherContacts: Set<ContactsBoardState.ContactId>,
    addRichGameLog: (LogBuilder.() -> Unit) -> Unit,
): ContactsGameState {
    val result = board.applyActionIds(player, actionType, playerContacts, otherContacts)
    val nextState = copy(
        board = result.state,
        gamePhase = nextGamePhase(actionType, result.state),
    )
    addRichGameLog(result.logBuilder)
    return nextState
}

private fun ContactsGameState.nextGamePhase(
    actionType: ContactsBoardState.ActionType,
    newBoard: ContactsBoardState,
): ContactsGameState.GamePhase {
    return when (gamePhase) {
        is StandardTurn -> {
            when (actionType) {
                ContactsBoardState.ActionType.StandardConnect,
                ContactsBoardState.ActionType.AddHint,
                ContactsBoardState.ActionType.MyDoubleConnect,
                ContactsBoardState.ActionType.SoloConnectRest,
                ContactsBoardState.ActionType.FinishReds,
                    -> {
                    StandardTurn(
                        nextPlayer(gamePhase.activePlayer),
                    )
                }

                ContactsBoardState.ActionType.DoubleConnect,
                ContactsBoardState.ActionType.TripleConnect,
                    -> {
                    val resolveMultiConnect = newBoard.resolveMultiConnect!!
                    ResolveMultiConnect(
                        restorePlayer = gamePhase.activePlayer,
                        resolveMultiConnect = resolveMultiConnect,
                    )
                }

                ContactsBoardState.ActionType.ResolveMultiConnect -> throw InvalidActionException("Not in ResolveMultiConnect phase")
            }
        }

        is ResolveMultiConnect -> {
            when (actionType) {
                // only legal from ResolveMultiConnect phase
                ContactsBoardState.ActionType.ResolveMultiConnect -> {
                    StandardTurn(
                        nextPlayer(gamePhase.restorePlayer),
                    )
                }

                else -> throw InvalidActionException("Only ResolveMultiConnect")
            }
        }

        is GameOver -> {
            throw InvalidActionException("Game is over - no actions")
        }
    }
}

// todo only player with contacts
private fun ContactsGameState.nextPlayer(afterPlayer: ContactsBoardState.Player): ContactsBoardState.Player =
    players[(players.indexOf(afterPlayer) + 1) % players.size]