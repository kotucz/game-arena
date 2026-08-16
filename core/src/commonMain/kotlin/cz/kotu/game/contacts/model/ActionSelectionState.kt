package cz.kotu.game.contacts.model

sealed class ActionSelectionState(
    open val playerContacts: Set<ContactsBoardState.Contact> = emptySet(),
    open val otherContacts: Set<ContactsBoardState.Contact> = emptySet()
) {
    object None : ActionSelectionState()
    data class StandardConnect(
        val playerContact: ContactsBoardState.Contact? = null,
        val otherContact: ContactsBoardState.Contact? = null
    ) : ActionSelectionState(
        playerContacts = setOfNotNull(playerContact),
        otherContacts = setOfNotNull(otherContact)
    )
    data class MultiConnect(
        override val playerContacts: Set<ContactsBoardState.Contact> = emptySet(),
        override val otherContacts: Set<ContactsBoardState.Contact> = emptySet()
    ) : ActionSelectionState(playerContacts, otherContacts)
}
