package cz.kotu.gamearena

import cz.kotu.game.contacts.ContactsGameViewModel
import io.ktor.client.HttpClient
import cz.kotu.game.gotfive.GameViewModel
import cz.kotu.tools.MultiPlayerViewModel
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Provides
import me.tatarka.inject.annotations.Scope

@Scope
annotation class AppScope

@AppScope
@Component
abstract class AppComponent {
    abstract val httpClient: HttpClient
    abstract val authClient: AuthClient
    abstract val gamesClient: GamesClient
    abstract val gameViewModelFactory: () -> GameViewModel
    abstract val multiPlayerViewModelFactory: (String, (String) -> HttpClient) -> MultiPlayerViewModel
    abstract val contactsGameViewModelFactory: (String, String) -> ContactsGameViewModel

    @Provides
    @AppScope
    fun provideHttpClient(): HttpClient = createAuthHttpClient()
}
