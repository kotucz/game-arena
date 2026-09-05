package cz.kotu.game.contacts.model

data class ActionSelectionState(
    val playerContacts: Set<ContactsBoardState.ContactId> = emptySet(),
    val otherContacts: Set<ContactsBoardState.ContactId> = emptySet(),
) {
    companion object {
        val None = ActionSelectionState()
    }
}
