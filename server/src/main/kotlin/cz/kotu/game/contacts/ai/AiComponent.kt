package cz.kotu.game.contacts.ai

import com.google.adk.kt.agents.BaseAgent
import com.google.adk.kt.artifacts.InMemoryArtifactService
import com.google.adk.kt.sessions.SessionService
import com.google.adk.kt.sessions.room.RoomSessionService
import com.google.adk.kt.webserver.AdkServerConfig
import com.google.adk.kt.webserver.loaders.AgentLoader
import cz.kotu.game.contacts.ai.ContactsAiPlayer.Companion.AGENT_NAME
import cz.kotu.gamearena.AiPlayerConfig
import cz.kotu.gamearena.ServerScope
import io.github.aakira.napier.Napier
import me.tatarka.inject.annotations.Provides

@ServerScope
interface AiComponent {
    val aiSessionService: SessionService
    val adkServerConfig: AdkServerConfig

    @Provides
    @ServerScope
    fun provideAiSessionService(aiConfig: AiPlayerConfig): SessionService {
        val sessionDatabaseFile = aiConfig.sessionStorageFile
        sessionDatabaseFile.parentFile?.mkdirs()
        return RoomSessionService.fromPath(sessionDatabaseFile.absolutePath)
    }

    @Provides
    @ServerScope
    fun provideAdkServerConfig(
        contactsAi: ContactsAiPlayer,
        aiSessionService: SessionService,
    ): AdkServerConfig = AdkServerConfig(
        agentLoader = object : AgentLoader {

            override fun listAgents(): List<String> {
                return listOf(AGENT_NAME)
            }

            override fun loadAgent(agentName: String): BaseAgent? {
                return when (agentName) {
                    AGENT_NAME -> contactsAi.agent
                    else -> {
                        Napier.e { "loadAgent: $agentName is not defined" }
                        null
                    }
                }
            }
        },
        sessionService = aiSessionService,
        artifactService = InMemoryArtifactService(),
        port = 8082,
        webUiEnabled = true,
    )


    @Provides
    @ServerScope
    fun provideContactsAiPlayer(aiPlayerConfig: AiPlayerConfig): ContactsAiPlayer {
        return ContactsAiPlayer(
            sessionService = aiSessionService,
            aiPlayerConfig = aiPlayerConfig,
        )
    }

    @Provides
    @ServerScope
    fun provideContactsAiStart(contactsAiPlayer: ContactsAiPlayer): ContactsAiStart = contactsAiPlayer
}