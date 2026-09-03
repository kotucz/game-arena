package cz.kotu.game.contacts.model

sealed interface ActionExecutionResult {
    data class Success(val state: ContactsBoardState, val logBuilder: LogBuilder.() -> Unit) : ActionExecutionResult
    data class Failure(val message: String) : ActionExecutionResult
}
