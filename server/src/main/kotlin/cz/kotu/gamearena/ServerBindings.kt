package cz.kotu.gamearena

import cz.kotu.game.contacts.ai.ContactsAiStart
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
        contactsAi: ContactsAiStart,
    ): GamesManager = GamesManager(
        gameDao = database.gameDao(),
        notificationService = notificationService,
        contactsAi = contactsAi,
    )

}
