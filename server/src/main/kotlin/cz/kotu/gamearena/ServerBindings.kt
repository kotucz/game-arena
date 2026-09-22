package cz.kotu.gamearena

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
annotation class ServerScope

@ServerScope
interface ServerBindings {
    val database: AppDatabase
    val gamesManager: GamesManager
    val notificationService: PushNotificationService
    val serverConfig: ServerConfig
    val tokenVerifier: TokenVerifier

    @Provides
    @ServerScope
    fun provideGamesManager(
        database: AppDatabase,
        notificationService: PushNotificationService,
    ): GamesManager = GamesManager(
        gameDao = database.gameDao(),
        notificationService = notificationService,
    )

    @Provides
    @ServerScope
    fun provideNotificationService(database: AppDatabase, serverConfig: ServerConfig): PushNotificationService =
        FirebaseAdminPushNotificationService(database, serverConfig)
}
