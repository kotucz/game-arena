package cz.kotu.gamearena

import cz.kotu.game.contacts.ai.AiComponent
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

/** production server component */
@Component
abstract class ServerComponent(
    @get:Provides @get:ServerScope override val serverConfig: ServerConfig,
) : ServerBindings, AiComponent {

    abstract override val database: AppDatabase
    abstract override val gamesManager: GamesManager
    abstract override val notificationService: PushNotificationService
    abstract override val tokenVerifier: TokenVerifier

    @Provides
    @ServerScope
    fun provideAiConfig(serverConfig: ServerConfig): AiPlayerConfig = serverConfig.aiPlayer

    @Provides
    @ServerScope
    fun provideDatabase(serverConfig: ServerConfig): AppDatabase = createDatabase(serverConfig.databaseFile)

    @Provides
    @ServerScope
    fun provideTokenVerifier(serverConfig: ServerConfig): TokenVerifier = FirebaseTokenVerifier(serverConfig)

    @Provides
    @ServerScope
    fun provideNotificationService(
        database: AppDatabase,
        serverConfig: ServerConfig,
    ): PushNotificationService = FirebaseAdminPushNotificationService(database, serverConfig)

}
