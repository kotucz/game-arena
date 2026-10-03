package cz.kotu.game.contacts.ai

import cz.kotu.game.contacts.model.ContactsPlayerFacade
import kotlin.uuid.Uuid

/**
 * LlmAgent has tools but is shared among runners. Use invocationId to identify the runner callback target.
 */
class PlayerFacadeLookup {

    /** invocationId -> ContactsPlayerFacade */
    val playerFacades: MutableMap<String, ContactsPlayerFacade> = mutableMapOf()

    val lock = Any()

    fun registerInvocation(playerFacade: ContactsPlayerFacade): String  = synchronized(lock) {
        val invocationId = Uuid.random().toString()
        playerFacades[invocationId] = playerFacade
        return invocationId
    }

    fun clear(invocationId: String) = synchronized(lock) {
        playerFacades.remove(invocationId)
    }

    fun getPlayerFacade(invocationId: String): ContactsPlayerFacade? = synchronized(lock) {
        return playerFacades[invocationId]
    }


}