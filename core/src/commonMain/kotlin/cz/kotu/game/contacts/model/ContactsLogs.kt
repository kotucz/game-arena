package cz.kotu.game.contacts.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable


@Serializable
sealed class LogToken {
    @Serializable
    @SerialName("text")
    data class Text(val text: String) : LogToken()

    @Serializable
    @SerialName("player")
    data class Player(
        val username: String,
    ) : LogToken()

    @Serializable
    @SerialName("contacts")
    data class Contact(
        val contactId: ContactsBoardState.ContactId,
    ) : LogToken()

}


@DslMarker
annotation class LogDslMarker

@LogDslMarker
class LogBuilder {
    private val tokens = mutableListOf<LogToken>()

    // Direct token appenders
    fun text(value: String) {
        tokens.add(LogToken.Text(value))
    }

    fun player(player: ContactsBoardState.Player) {
        tokens.add(LogToken.Player(username = player.username))
    }

    fun contact(contact: ContactsBoardState.Contact) {
        tokens.add(LogToken.Contact(contactId = contact.id))
    }

    fun contacts(contacts: Iterable<ContactsBoardState.Contact>) {
        contacts.forEach { contact -> contact(contact) }
    }

    fun contacts(vararg contacts: ContactsBoardState.Contact) {
        contacts.forEach { contact -> contact(contact) }
    }

    fun build(): List<LogToken> =
        tokens.toList()
}