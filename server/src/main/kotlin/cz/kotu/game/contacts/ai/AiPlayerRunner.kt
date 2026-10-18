package cz.kotu.game.contacts.ai

import ai.koog.agents.core.agent.AIAgent
import ai.koog.agents.core.tools.ToolRegistry
import ai.koog.prompt.executor.model.PromptExecutor
import ai.koog.prompt.llm.LLModel
import com.google.adk.kt.runners.Runner
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.game.contacts.model.GameLogEntry
import cz.kotu.game.contacts.model.PlayerViewState
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

typealias GameMessageSender = (String) -> Unit

class AiPlayerRunner(
    private val playerFacade: ContactsPlayerFacade,
    private val scope: CoroutineScope,
    private val promptExecutor: PromptExecutor,
    private val model: LLModel,
    private val instructions: String,
    private val sessionId: String,
    private val playerFacadeLookup: PlayerFacadeLookup? = null,
    private val runner: Runner? = null,
    private val messageSender: GameMessageSender? = null,
) {

    private val json: Json = Json {
        prettyPrint = true
    }

    private val agent: AIAgent<String, String> by lazy {
        val tools = ContactsKoogTools(
            playerFacade = playerFacade,
            onThoughtLogged = { reasoning ->
                Napier.i("[AI DEDUCTION]: $reasoning")
                messageSender?.invoke(reasoning)
            },
        )
        AIAgent(
            promptExecutor = promptExecutor,
            llmModel = model,
            systemPrompt = instructions,
            toolRegistry = ToolRegistry {
                tools(tools)
            },
        )
    }

    fun startListening() {
        Napier.i("startListening")
        scope.launch {
            playerFacade.gameState.collect { viewState ->
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
        val prompt = buildPrompt(viewState, currentLogs) {
            playTurnInstructions()
        }

        Napier.i { "executeAiTurn $sessionId starting Koog agent run" }

        try {
            val response = agent.run(prompt)
            Napier.d { "Koog agent response: $response" }
            messageSender?.invoke(response)
        } catch (e: CancellationException) {
            // collect now waits until this is finished.
            // but if the parent scope is canceled, this ensures to finish gracefully
            throw e
        } catch (e: Exception) {
            Napier.e("Error executing Koog AI turn", e)
        }
    }

    suspend fun command(command: String) {
        Napier.i { "AI prompt command received: $command" }

        val viewState = playerFacade.gameState.first()
        val currentLogs = playerFacade.logs.value

        val prompt = buildPrompt(viewState, currentLogs) {
            appendLine(command)
        }

        try {
            val response = agent.run(prompt)
            Napier.d { "Koog agent command response: $response" }
            messageSender?.invoke(response)
        } catch (e: CancellationException) {
            // collect now waits until this is finished.
            // but if the parent scope is canceled, this ensures to finish gracefully
            throw e
        } catch (e: Exception) {
            Napier.e("Error executing Koog AI turn", e)
        }
    }

    private fun buildPrompt(
        viewState: PlayerViewState,
        logs: List<GameLogEntry>,
        instructions: StringBuilder.() -> Unit,
    ): String {
        return buildString {
            appendLine("\n=== CURRENT PLAYER VIEW STATE ===")

            appendLine(json.encodeToString(viewState))

            appendLine("\n=== RECENT MATCH LOGS ===")
            logs.takeLast(10).forEach { log ->
                appendLine("- $log")
            }

            appendLine("\n=== INSTRUCTIONS ===\n")

            instructions()
        }
    }

    private fun StringBuilder.playTurnInstructions() {
        appendLine("Analyze the board state, known bounds, and past logs. ")
        appendLine("Call 'submitAction' with your full rationale and chosen contacts.")
    }
}
