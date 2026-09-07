package cz.kotu.game.contacts.model

class InvalidActionException(message: String) : IllegalArgumentException(message)

sealed interface ActionExecutionResult {
    data class Success(val state: ContactsBoardState, val logBuilder: LogBuilder.() -> Unit) : ActionExecutionResult
}
