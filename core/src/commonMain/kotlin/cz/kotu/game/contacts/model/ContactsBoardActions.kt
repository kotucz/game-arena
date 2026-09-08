package cz.kotu.game.contacts.model

class InvalidActionException(message: String) : IllegalArgumentException(message)

sealed interface ActionExecutionResult {
    data class Success(
        val state: ContactsBoardState,
        val next: Next,
        val logBuilder: LogBuilder.() -> Unit,
    ) : ActionExecutionResult
}

sealed class Next {
    data class EndOfTurn(
        val player: ContactsBoardState.Player,
    ) : Next()

    data class ResolveMultiConnect(
        val resolveMultiConnect: ContactsBoardState.ResolveMultiConnect,
    ) : Next()

    data class GameOver(
        val message: String,
    ) : Next()
}
