package cz.kotu.gamearena

import cz.kotu.game.contacts.ContactsGameViewModel
import cz.kotu.game.gotfive.GameViewModel
import cz.kotu.tools.MultiPlayerViewModel
import io.ktor.client.HttpClient
import kotlinx.coroutines.flow.MutableSharedFlow
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
    abstract val authManager: AuthManager
    abstract val gamesClient: GamesClient
    abstract val gameViewModelFactory: () -> GameViewModel
    abstract val multiPlayerViewModelFactory: (String, (String) -> HttpClient) -> MultiPlayerViewModel

    // username parameter removed — ContactsGameViewModel now injects AuthManager directly
    abstract val contactsGameViewModelFactory: (String) -> ContactsGameViewModel

    /**
     * Single shared event flow: the Ktor 401 interceptor emits into it, and
     * [AuthManager] exposes it as a read-only [SharedFlow]. No circular dependency —
     * both [HttpClient] and [AuthManager] depend on this flow, not on each other.
     */
    @Provides
    @AppScope
    fun provideUnauthorizedEvents(): MutableSharedFlow<Unit> =
        MutableSharedFlow(extraBufferCapacity = 1)

    @Provides
    @AppScope
    fun provideHttpClient(unauthorizedEvents: MutableSharedFlow<Unit>): HttpClient =
        createAuthHttpClient(onUnauthorized = { unauthorizedEvents.tryEmit(Unit) })
}
