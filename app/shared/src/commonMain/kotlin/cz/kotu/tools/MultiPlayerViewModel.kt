package cz.kotu.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import cz.kotu.game.contacts.model.NetworkContactsGameFacade
import cz.kotu.gamearena.authBaseUrl
import cz.kotu.gamearena.createAuthHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.reflect.KClass

class MultiPlayerViewModel(remote: Boolean = false) : ViewModel() {
    val players = listOf(
        ContactsBoardState.Player("alice"),
        ContactsBoardState.Player("bob"),
    )

    private val localFacade = ContactsGameFacadeImpl(players)
    private val networkScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val gameFacade: ContactsGameFacade = if (remote) {
        NetworkContactsGameFacade(
            httpClient = createAuthHttpClient(),
            endpoint = authBaseUrl().trimEnd('/') + "/api/contacts",
            initialState = localFacade.gameState.value,
            scope = networkScope,
        )
    } else {
        localFacade
    }

    override fun onCleared() {
        networkScope.cancel()
    }
}

object MultiPlayerViewModelFactory : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: KClass<T>,
        extras: CreationExtras,
    ): T {
        if (modelClass == MultiPlayerViewModel::class) {
            return MultiPlayerViewModel() as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.simpleName}")
    }
}
