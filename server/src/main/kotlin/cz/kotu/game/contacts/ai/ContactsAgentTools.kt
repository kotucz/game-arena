package cz.kotu.game.contacts.ai

import com.google.adk.kt.annotations.Param
import com.google.adk.kt.annotations.Tool
import cz.kotu.game.contacts.model.ContactsBoardState
import cz.kotu.game.contacts.model.ContactsPlayerFacade
import io.github.aakira.napier.Napier

class ContactsAgentTools(
    private val playerFacade: ContactsPlayerFacade,
    private val onThoughtLogged: (String) -> Unit,
) {

    @Tool(
        description =
            "Execute a game action on your turn. " +
                    "You MUST provide your full rationale and deduction process before picking action targets.",
    )
    suspend fun submitAction(
        @Param("Your step-by-step reasoning and deduction for this turn") rationale: String,
        @Param("The type of action to perform") actionType: ContactsBoardState.ActionType,
        @Param("Set of ContactIds selected from your own rack") playerContacts: List<Int>,
        @Param("Set of ContactIds selected from other players' racks") otherContacts: List<Int>,
    ): String {
        Napier.i("submitAction $actionType $playerContacts $otherContacts")

        // 1. Emit thought to console / custom logs
        onThoughtLogged(rationale)

        // 2. Dispatch action via your domain API
        val result = playerFacade.action(
            actionType = actionType,
            playerContacts = playerContacts.map { ContactsBoardState.ContactId(it) }.toSet(),
            otherContacts = otherContacts.map { ContactsBoardState.ContactId(it) }.toSet(),
        )

        return result.fold(
            onSuccess = { "Action executed successfully. Rationale logged: $rationale" },
            onFailure = { error -> "Action failed: ${error.message}. Please adjust your deduction and try again." },
        )
    }
}