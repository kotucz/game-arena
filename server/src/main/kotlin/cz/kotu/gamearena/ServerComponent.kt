package cz.kotu.gamearena

import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides

/** production server component */
@Component
abstract class ServerComponent(
    @get:Provides override val serverConfig: ServerConfig,
) : ServerBindings {

    abstract override val database: AppDatabase
    abstract override val gamesManager: GamesManager

    @Provides
    fun provideDatabase(serverConfig: ServerConfig): AppDatabase = createDatabase(serverConfig.databaseFile)

}
