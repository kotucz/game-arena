package cz.kotu.gamearena

import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
annotation class ServerScope

@ServerScope
interface ServerBindings {
    val database: AppDatabase
    val gamesManager: GamesManager
    val serverConfig: ServerConfig

    @Provides
    @ServerScope
    fun provideGamesManager(database: AppDatabase): GamesManager = GamesManager(gameDao = database.gameDao())
}
