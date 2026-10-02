package cz.kotu.game.contacts.ai

import cz.kotu.game.contacts.model.ContactsPlayerFacade
import kotlin.uuid.Uuid

/**
 * LlmAgent has tools but is shared among runners. Use invocationId to identify the runner callback target.
 */
class PlayerFacadeLookup {

    /** invocationId -> ContactsPlayerFacade */
    private val playerFacades: MutableMap<String, ContactsPlayerFacade> = mutableMapOf()

    fun registerInvocation(playerFacade: ContactsPlayerFacade): String {
        val invocationId = Uuid.random().toString()
        playerFacades[invocationId] = playerFacade
        return invocationId
    }

    fun clear(invocationId: String) {
        playerFacades.remove(invocationId)
    }

    fun getPlayerFacade(invocationId: String): ContactsPlayerFacade? {
        return playerFacades[invocationId]
    }


}