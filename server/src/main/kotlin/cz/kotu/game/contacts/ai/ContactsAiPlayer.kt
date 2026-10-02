package cz.kotu.game.contacts.ai

import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.sessions.SessionService
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.gamearena.AiPlayerConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope

interface ContactsAiStart {
    fun start(coroutineScope: CoroutineScope, playerFacade: ContactsPlayerFacade, sessionId: String) {}
}

object NoopContactsAiStart : ContactsAiStart

class ContactsAiPlayer(
    private val aiPlayerConfig: AiPlayerConfig,
    private val sessionService: SessionService,
) : ContactsAiStart {

    private val playerFacadeLookup = PlayerFacadeLookup()

    val agent: LlmAgent by lazy {
        createAgent(
            playerFacadeLookup = playerFacadeLookup,
            aiPlayerConfig = aiPlayerConfig,
        )
    }

    val runner: Runner by lazy {
        InMemoryRunner(
            agent = agent,
            appName = APP_NAME,
            sessionService = sessionService,
        )
    }

    override fun start(
        coroutineScope: CoroutineScope,
        playerFacade: ContactsPlayerFacade,
        sessionId: String,
    ) {
        val aiRunner = AiPlayerRunner(
            playerFacade = playerFacade,
            playerFacadeLookup = playerFacadeLookup,
            scope = coroutineScope,
            runner = runner,
            sessionId = sessionId,
        )
        aiRunner.startListening()
    }

    companion object {

        // for dev-ui app and agent name must match
        const val APP_NAME = "ContactsAI"
        const val AGENT_NAME = "ContactsAI"

        fun createAgent(
            aiPlayerConfig: AiPlayerConfig,
            playerFacadeLookup: PlayerFacadeLookup,
        ): LlmAgent {
            // 1. Configure the Gemini Model
            val model = Gemini(
                name = aiPlayerConfig.gemini.model,
                apiKey = checkNotNull(aiPlayerConfig.gemini.apiKey) {
                    "GEMINI_API_KEY must be configured to run the Contacts AI player"
                },
            )

            val tools = ContactsAgentTools(
                playerFacadeLookup = playerFacadeLookup,
                onThoughtLogged = { reasoning ->
                    Napier.i("[AI DEDUCTION]: $reasoning")
                },
            )

            // 2. Instantiate the ADK LlmAgent
            return LlmAgent(
                name = AGENT_NAME,
                description = "Cooperative AI player for Contacts / Bomb Busters",
                model = model,
                includeContents = LlmAgent.IncludeContents.NONE,
                instruction = Instruction(
                    """
        You are a cooperative player in the deduction board game 'Contacts'.
        Analyze the current PlayerViewState, public bounds, and logs.
        Work with your teammates by deducing hidden contact IDs and submitting valid actions.
    """.trimIndent(),
                ),
                tools = tools.generatedTools(),
            )
        }
    }
}
