package cz.kotu.game.contacts.ai

import ai.koog.http.client.ktor.KtorKoogHttpClient
import ai.koog.prompt.executor.clients.google.GoogleLLMClient
import ai.koog.prompt.executor.llms.MultiLLMPromptExecutor
import ai.koog.prompt.llm.LLMCapability
import ai.koog.prompt.llm.LLMProvider
import ai.koog.prompt.llm.LLModel
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import cz.kotu.gamearena.AiPlayerConfig
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
) : ContactsAiStart {

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
            scope = coroutineScope,
            promptExecutor = promptExecutor,
            model = koogModel,
            instructions = instructions,
            sessionId = sessionId,
            messageSender = messageSender,
        )
        aiRunner.startListening()
        return aiRunner
    }

}
