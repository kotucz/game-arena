package cz.kotu.tools

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsGameFacade
import cz.kotu.game.contacts.model.ContactsGameFacadeImpl
import kotlin.reflect.KClass

class MultiPlayerViewModel : ViewModel() {
    val players = listOf(
        ContactsBoardState.Player("alice"),
        ContactsBoardState.Player("bob"),
    )

    val gameFacade: ContactsGameFacade = ContactsGameFacadeImpl(players)
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
