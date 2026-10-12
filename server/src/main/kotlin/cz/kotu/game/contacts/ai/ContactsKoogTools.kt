package cz.kotu.game.contacts.ai

import ai.koog.agents.core.tools.annotations.LLMDescription
import ai.koog.agents.core.tools.annotations.Tool
import ai.koog.agents.core.tools.reflect.ToolSet
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import io.github.aakira.napier.Napier

@LLMDescription("Tools for playing the Contacts / Bomb Busters game.")
class ContactsKoogTools(
    private val playerFacade: ContactsPlayerFacade,
    private val onThoughtLogged: ((String) -> Unit)? = null,
) : ToolSet {

    @Tool
    @LLMDescription(
        "Execute a game action on your turn. " +
            "You MUST provide your full rationale and deduction process before picking and executing the action type and contacts."
    )
    suspend fun submitAction(
        @LLMDescription("Your step-by-step reasoning and deduction for this turn")
        rationale: String,
        @LLMDescription("The type of action to perform")
        actionType: ContactsBoardState.ActionType,
        @LLMDescription("Set of ContactIds selected from your own rack. Only raw integer value(s) as strings")
        playerContacts: List<String>,
        @LLMDescription("Set of ContactIds selected from other players' racks. Only raw integer value(s) as strings")
        otherContacts: List<String>,
    ): String {
        Napier.i("submitAction $actionType $playerContacts $otherContacts")

        onThoughtLogged?.invoke(rationale)

        val result = playerFacade.action(
            actionType = actionType,
            playerContacts = playerContacts.map { ContactsBoardState.ContactId(it.toInt()) }.toSet(),
            otherContacts = otherContacts.map { ContactsBoardState.ContactId(it.toInt()) }.toSet(),
        )

        return result.fold(
            onSuccess = {
                "Action executed successfully. Your turn is over".also {
                    Napier.i("submitAction result: $it")
                }
            },
            onFailure = { error ->
                "Invalid action. Reason: ${error.message}. Adjust your deduction and try again.".also {
                    Napier.w("submitAction result: $it", error)
                }
            },
        )
    }
}
