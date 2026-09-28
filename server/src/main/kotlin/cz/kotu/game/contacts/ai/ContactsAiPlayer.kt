package cz.kotu.game.contacts.ai

import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.runners.InMemoryRunner
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.gamearena.AiPlayerConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope

class ContactsAiPlayer(
    private val contactsPlayerFacade: ContactsPlayerFacade,
    private val gameId: String,
) {

    fun start(coroutineScope: CoroutineScope, aiPlayerConfig: AiPlayerConfig) {
        // 1. Configure the Gemini Model
        val model = Gemini(
            name = aiPlayerConfig.gemini.model,
            apiKey = checkNotNull(aiPlayerConfig.gemini.apiKey) {
                "GEMINI_API_KEY must be configured to run the Contacts AI player"
            },
        )

        val tools = ContactsAgentTools(
            playerFacade = contactsPlayerFacade,
            onThoughtLogged = { reasoning ->
                Napier.i("[AI DEDUCTION]: $reasoning")
            },
        )

        // 2. Instantiate the ADK LlmAgent
        val contactsAgent = LlmAgent(
            name = "ContactsDeductionBot",
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

        // 3. The runner owns session handling and invokes the agent with user messages.
        val runner = AiPlayerRunner(
            playerFacade = contactsPlayerFacade,
            scope = coroutineScope,
            runner = InMemoryRunner(contactsAgent),
            sessionId = gameId,
        )
        runner.startListening()
    }
}
