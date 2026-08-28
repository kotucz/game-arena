package cz.kotu.gamearena

import me.tatarka.inject.annotations.Provides

interface ServerBindings {
    val database: AppDatabase
    val gamesManager: GamesManager

    @Provides
    fun provideGamesManager(database: AppDatabase): GamesManager = GamesManager(gameDao = database.gameDao())
}
