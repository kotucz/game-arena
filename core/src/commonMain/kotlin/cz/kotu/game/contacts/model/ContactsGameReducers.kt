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
    when (gamePhase) {
        is StandardTurn -> {
            when (actionType) {
                ContactsBoardState.ActionType.StandardConnect,
                ContactsBoardState.ActionType.AddHint,
                ContactsBoardState.ActionType.MyDoubleConnect,
                ContactsBoardState.ActionType.SoloConnectRest,
                ContactsBoardState.ActionType.FinishReds,
                ContactsBoardState.ActionType.DoubleConnect,
                ContactsBoardState.ActionType.TripleConnect,
                    -> {
                    // TODO verify player is active

                    val result = board.applyActionIds(player, actionType, playerContacts, otherContacts)
                    addRichGameLog(result.logBuilder)
                    return copy(
                        board = result.state,
                        gamePhase = nextGamePhase(newBoard = result.state, result.next),
                    )
                }

                ContactsBoardState.ActionType.ResolveMultiConnect -> throw InvalidActionException("Not in ResolveMultiConnect phase")
            }
        }

        is ResolveMultiConnect -> {
            when (actionType) {
                ContactsBoardState.ActionType.ResolveMultiConnect -> {
                    val result =
                        board.handleResolveMultiConnect(player, playerContacts.single(), gamePhase.resolveMultiConnect)
                    addRichGameLog(result.logBuilder)
                    return copy(
                        board = result.state,
                        gamePhase = nextGamePhase(result.state, result.next),
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

private fun ContactsGameState.nextGamePhase(
    newBoard: ContactsBoardState,
    next: Next,
): ContactsGameState.GamePhase {
    return when (next) {
        is Next.EndOfTurn -> {
            nextPhase(newBoard, afterPlayer = next.player)
        }

        is Next.ResolveMultiConnect -> {
            ResolveMultiConnect(
                resolveMultiConnect = next.resolveMultiConnect,
            )
        }

        is Next.GameOver -> {
            GameOver(next.message)
        }
    }
}

private fun ContactsGameState.nextPhase(
    newBoard: ContactsBoardState,
    afterPlayer: ContactsBoardState.Player,
): ContactsGameState.GamePhase {
    val afterIndex = players.indexOf(afterPlayer)
    for (i in players.indices) {
        val player = players[(afterIndex + 1 + i) % players.size]
        if (newBoard.hasUnsolvedContacts(player)) {
            return StandardTurn(player)
        }
    }
    return GameOver("Win: all solved!")
}