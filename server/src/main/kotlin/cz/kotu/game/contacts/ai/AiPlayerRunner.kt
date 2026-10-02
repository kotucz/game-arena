package cz.kotu.game.contacts.ai

import com.google.adk.kt.agents.RunConfig
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.types.Content
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.game.contacts.model.PlayerViewState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AiPlayerRunner(
    private val playerFacade: ContactsPlayerFacade,
    private val playerFacadeLookup: PlayerFacadeLookup,
    private val scope: CoroutineScope,
    private val runner: Runner,
    private val sessionId: String,
) {

    fun startListening() {
        Napier.i("startListening")
        scope.launch {
            playerFacade.gameState.collectLatest { viewState ->
                // Only trigger execution when it's actively this AI player's turn
                if (viewState.actions.allowedActionTypes.isNotEmpty()) {
                    Napier.i("executeAiTurn START")
                    executeAiTurn(viewState)
                    Napier.i("executeAiTurn FINISH")
                }
            }
        }
    }

    private suspend fun executeAiTurn(viewState: PlayerViewState) {
        val currentLogs = playerFacade.logs.value

        // Construct a clean, stateless snapshot prompt
        val prompt = buildPrompt(viewState, currentLogs)

        val invocationId = playerFacadeLookup.registerInvocation(playerFacade)
        Napier.i { "executeAiTurn $sessionId -> $invocationId" }

        try {
            // ADK receives each game snapshot as a user message. The runner's agent already owns
            // the generated Contacts tools, so ADK can invoke them while processing this turn.
            runner.runAsync(
                userId = "user", // default for display in dev-ui
                sessionId = sessionId,
                invocationId = invocationId,
                newMessage = Content.fromText(role = "user", text = prompt),
                runConfig = RunConfig(maxLlmCalls = 1),
            ).collect { event ->
                event.content?.text()?.takeIf(String::isNotBlank)?.let { Napier.d(it) }
            }
        } finally {
            // 3. Clean up facade when turn completes
            playerFacadeLookup.clear(invocationId)
        }
    }

    private fun buildPrompt(viewState: PlayerViewState, logs: List<GameLogEntry>): String {
        return buildString {
            appendLine("=== CURRENT PLAYER VIEW STATE ===")
            appendLine(viewState.toString()) // Ensure PlayerViewState has a clean toString() representation

            appendLine("\n=== AVAILABLE ACTIONS ===")
            appendLine(viewState.actions)

            appendLine("\n=== RECENT MATCH LOGS ===")
            logs.takeLast(10).forEach { log ->
                appendLine("- $log")
            }

            appendLine("\n=== INSTRUCTIONS ===")
            appendLine("Analyze the board state, known bounds, and past logs.")
            appendLine("Call 'submitAction' with your full rationale and chosen contacts.")
        }
    }
}
