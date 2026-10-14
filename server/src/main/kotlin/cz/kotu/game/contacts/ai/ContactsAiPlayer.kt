package cz.kotu.game.contacts.ai

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import com.google.adk.kt.agents.Instruction
import com.google.adk.kt.agents.LlmAgent
import com.google.adk.kt.models.Gemini
import com.google.adk.kt.runners.InMemoryRunner
import com.google.adk.kt.runners.Runner
import com.google.adk.kt.sessions.SessionService
import com.google.adk.kt.types.GenerateContentConfig
import com.google.adk.kt.types.ThinkingConfig
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.gamearena.AiPlayerConfig
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope

interface ContactsAiStart {
    fun start(
        coroutineScope: CoroutineScope,
        playerFacade: ContactsPlayerFacade,
        sessionId: String,
        messageSender: GameMessageSender? = null,
    ): AiPlayerRunner? {
        return null
    }
}

object NoopContactsAiStart : ContactsAiStart

class ContactsAiPlayer(
    private val aiPlayerConfig: AiPlayerConfig,
    private val sessionService: SessionService,
) : ContactsAiStart {

    private val playerFacadeLookup = PlayerFacadeLookup()

    // ADK Agent & Runner (kept for compatibility)
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

    // JetBrains Koog prompt executor and model configuration
    val promptExecutor: MultiLLMPromptExecutor by lazy {
        val apiKey = checkNotNull(aiPlayerConfig.gemini.apiKey) {
            "GEMINI_API_KEY must be configured to run the Contacts AI player"
        }
        MultiLLMPromptExecutor(
            LLMProvider.Google to GoogleLLMClient(
                apiKey = apiKey,
                httpClientFactory = KtorKoogHttpClient.Factory(),
            )
        )
    }

    val koogModel: LLModel by lazy {
        LLModel(
            provider = LLMProvider.Google,
            id = aiPlayerConfig.gemini.model,
            capabilities = listOf(
                LLMCapability.Temperature,
                LLMCapability.Completion,
                LLMCapability.MultipleChoices,
                LLMCapability.Tools,
                LLMCapability.ToolChoice,
            ),
        )
    }

    val instructions: String by lazy {
        PromptLoader.load("ai/contacts/contacts-rules-2-all-colors.md")
    }

    override fun start(
        coroutineScope: CoroutineScope,
        playerFacade: ContactsPlayerFacade,
        sessionId: String,
        messageSender: GameMessageSender?,
    ): AiPlayerRunner {
        val aiRunner = AiPlayerRunner(
            playerFacade = playerFacade,
            playerFacadeLookup = playerFacadeLookup,
            scope = coroutineScope,
            runner = runner,
            promptExecutor = promptExecutor,
            model = koogModel,
            instructions = instructions,
            sessionId = sessionId,
            messageSender = messageSender,
        )
        aiRunner.startListening()
        return aiRunner
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

//            val instructions = PromptLoader.load("ai/contacts/contacts-rules-blue-only.md")
            val instructions = PromptLoader.load("ai/contacts/contacts-rules-2-all-colors.md")

            // 2. Instantiate the ADK LlmAgent
            return LlmAgent(
                name = AGENT_NAME,
                description = "Cooperative AI player for Contacts / Bomb Busters",
                model = model,
                includeContents = LlmAgent.IncludeContents.NONE,
                generateContentConfig =  GenerateContentConfig(
                    thinkingConfig = ThinkingConfig(
                        includeThoughts = true,
                        thinkingBudget = 1024,
                    ),
                ),
                instruction = Instruction(instructions),
                tools = tools.generatedTools(),
            )
        }
    }
}
