package cz.kotu.game.contacts.ai

import com.google.adk.kt.agents.Context
import com.google.adk.kt.annotations.Param
import com.google.adk.kt.annotations.Tool
import cz.kotu.game.contacts.model.ContactsBoardState
import io.github.aakira.napier.Napier

class ContactsAgentTools(
    private val playerFacadeLookup: PlayerFacadeLookup,
    private val onThoughtLogged: ((String) -> Unit)? = null,
) {

    @Tool(
        description =
            "Execute a game action on your turn. " +
                    "You MUST provide and output your full rationale and deduction process before picking and outputting the action type and contacts.",
    )
    suspend fun submitAction(
        context: Context,
        @Param("Your step-by-step reasoning and deduction for this turn") rationale: String,
        @Param("The type of action to perform") actionType: ContactsBoardState.ActionType,
        @Param("Set of ContactIds selected from your own rack. Only raw value(s)") playerContacts: List<String>,
        @Param("Set of ContactIds selected from other players' racks. Only raw value(s)") otherContacts: List<String>,
    ): String {
        val gameId = context.context.session.key.id
            ?: return "Action failed: Missing session ID in context.".also { Napier.w(it) }

        Napier.i("submitAction gameId=$gameId $actionType $playerContacts $otherContacts")

        val invocationId = context.context.invocationId

        Napier.i { "submitAction $gameId -> $invocationId" }

        val playerFacade = playerFacadeLookup.getPlayerFacade(invocationId)
        if (playerFacade == null) {
            Napier.e { "Cannot find playerFacade for $gameId -> $invocationId" }
            error("Cannot find playerFacade for $gameId -> $invocationId")
        }

        // 1. Emit thought to console / custom logs
        onThoughtLogged?.invoke(rationale)

        // 2. Dispatch action via your domain API
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
