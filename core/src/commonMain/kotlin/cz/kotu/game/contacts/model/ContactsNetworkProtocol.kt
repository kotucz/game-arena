package cz.kotu.game.contacts.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** JSON DTOs used by the Contacts HTTP transport. */
@Serializable
sealed class ContactsNetworkAction {
    @Serializable
    @SerialName("action")
    data class Action(
        val actionType: ContactsBoardState.ActionType,
        val playerContacts: Set<ContactsBoardState.ContactId>,
        val otherContacts: Set<ContactsBoardState.ContactId>,
    ) : ContactsNetworkAction()

}
