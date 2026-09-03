package cz.kotu.game.contacts.model

sealed class ActionSelectionState(
    open val playerContacts: Set<ContactsBoardState.ContactId> = emptySet(),
    open val otherContacts: Set<ContactsBoardState.ContactId> = emptySet(),
) {
    object None : ActionSelectionState()
    data class StandardConnect(
        val playerContact: ContactsBoardState.ContactId? = null,
        val otherContact: ContactsBoardState.ContactId? = null
    ) : ActionSelectionState(
        playerContacts = setOfNotNull(playerContact),
        otherContacts = setOfNotNull(otherContact)
    )

    data class MultiConnect(
        override val playerContacts: Set<ContactsBoardState.ContactId> = emptySet(),
        override val otherContacts: Set<ContactsBoardState.ContactId> = emptySet(),
    ) : ActionSelectionState(playerContacts, otherContacts)
}
