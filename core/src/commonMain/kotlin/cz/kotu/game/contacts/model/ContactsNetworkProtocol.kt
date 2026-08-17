package cz.kotu.game.contacts.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


/** JSON DTOs used by the Contacts HTTP transport. */
@Serializable
sealed class ContactsNetworkAction {
    @Serializable
    @SerialName("connect")
    data class Connect(
        val playerContact: ContactsBoardState.ContactId,
        val otherContact: ContactsBoardState.ContactId,
    ) : ContactsNetworkAction()
    @Serializable
    @SerialName("action")
    data class Action(
        val actionType: ContactsBoardState.ActionType,
        val playerContacts: Set<ContactsBoardState.ContactId>,
        val otherContacts: Set<ContactsBoardState.ContactId>,
    ) : ContactsNetworkAction()

    @Serializable
    @SerialName("resolveMultiConnect")
    data class ResolveMultiConnect(
        val targetContact: ContactsBoardState.ContactId,
    ) : ContactsNetworkAction()
}
