package cz.kotu.game.contacts.model

// Reducers and action executors operating on immutable ContactsBoardState

fun ContactsBoardState.handleAddHint(
    player: ContactsBoardState.Player,
    playerContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult {
    val playerContact = playerContacts.single()
    val newState = this.withHintFor(playerContact)
    return ActionExecutionResult.Success(newState) {
        player(player)
        text("hinted")
        contact(playerContact)
    }
}

fun ContactsBoardState.handleStandardConnect(
    player: ContactsBoardState.Player,
    playerContact: ContactsBoardState.Contact,
    otherContact: ContactsBoardState.Contact,
): ActionExecutionResult {
    // mismatch -> fault
    if (!contactsMatch(playerContact, otherContact)) {
        val boom = otherContact.type == ContactsBoardState.ContactType.Red
        val newState = withFaultFor(otherContact)
        return ActionExecutionResult.Success(newState) {
            player(player)
            text("mismatched")
            contact(playerContact)
            text("with")
            contact(otherContact)
            if (boom) text("BOOM! Red Connected! [Game Over]") else text("FAILURE!")
        }
    }

    val newState = withSolvedContacts(playerContact, otherContact)
    return ActionExecutionResult.Success(newState) {
        player(player)
        text("connected")
        contact(playerContact)
        text("with")
        contact(otherContact)
        text("SUCCESS!")
    }
}

fun ContactsBoardState.handleMultiConnect(
    player: ContactsBoardState.Player,
    actionType: ContactsBoardState.ActionType,
    playerContact: ContactsBoardState.Contact,
    otherContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult {
    val targetRack = racks.single { rack -> otherContacts.all { it.id in rack.contactIds } }
    val newState = copy(
        resolveMultiConnect = ContactsBoardState.ResolveMultiConnect(
            targetPlayer = targetRack.owner,
            originalContact = playerContact.id,
            targetContacts = otherContacts.map { it.id }.toSet(),
        )
    )
    return ActionExecutionResult.Success(newState) {
        player(player)
        text("connecting")
        contact(playerContact)
        text("with")
        contacts(otherContacts)
        text("from")
        player(targetRack.owner)
        text("chooses")
    }
}

fun ContactsBoardState.handleMyDoubleConnect(
    player: ContactsBoardState.Player,
    actionType: ContactsBoardState.ActionType,
    playerContacts: Set<ContactsBoardState.Contact>,
    otherContact: ContactsBoardState.Contact,
): ActionExecutionResult {
    if (!actionType.matches(playerContacts.size, 1)) {
        return ActionExecutionResult.Failure("Invalid number of selected contacts")
    }

    val playerContact = playerContacts.firstOrNull { contactsMatch(it, otherContact) } ?: playerContacts.first()
    // Delegate to standard connect handler
    return handleStandardConnect(player, playerContact, otherContact)
}

fun ContactsBoardState.handleSoloConnectRest(
    player: ContactsBoardState.Player,
    playerContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult {
    val newState = withSolvedContacts(*playerContacts.toTypedArray())
    return ActionExecutionResult.Success(newState) {
        player(player)
        text("solo connected")
        contacts(playerContacts)
    }
}

fun ContactsBoardState.handleFinishReds(
    player: ContactsBoardState.Player,
    playerContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult {
    val newState = withSolvedContacts(*playerContacts.toTypedArray())
    return ActionExecutionResult.Success(newState) {
        player(player)
        text("finished reds")
        contacts(playerContacts)
    }
}

fun ContactsBoardState.handleResolveMultiConnect(
    player: ContactsBoardState.Player,
    targetContact: ContactsBoardState.Contact,
): ActionExecutionResult {
    val resolution = resolveMultiConnect ?: return ActionExecutionResult.Failure("No multi connect to resolve")
    if (resolution.targetPlayer != player) return ActionExecutionResult.Failure("Only the target player can resolve the multi-connect")
    if (targetContact.id !in resolution.targetContacts) return ActionExecutionResult.Failure("Selected contact is not a multi-connect target")

    val originalContact = requireContact(resolution.originalContact)
    // determine original player
    val originalPlayer = racks.firstOrNull { originalContact.id in it.contactIds }?.owner
        ?: return ActionExecutionResult.Failure("Original player not found")

    // Resolve with exactly same validation/result as a normal StandardConnect made by the original contact's owner
    val connectResult = handleStandardConnect(originalPlayer, originalContact, targetContact)
    return when (connectResult) {
        is ActionExecutionResult.Failure -> connectResult
        is ActionExecutionResult.Success -> {
            val cleared = connectResult.state.copy(resolveMultiConnect = null)
            val connectBuilder = connectResult.logBuilder
            ActionExecutionResult.Success(cleared) {
                connectBuilder()
            }
        }
    }
}

fun ContactsBoardState.applyAction(
    player: ContactsBoardState.Player,
    actionType: ContactsBoardState.ActionType,
    playerContacts: Set<ContactsBoardState.Contact>,
    otherContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult {
    val error = isActionLegal(player, actionType, playerContacts, otherContacts)
    if (error != null) return ActionExecutionResult.Failure(error)

    return when (actionType) {
        ContactsBoardState.ActionType.AddHint -> handleAddHint(player, playerContacts)
        ContactsBoardState.ActionType.StandardConnect -> handleStandardConnect(player, playerContacts.single(), otherContacts.single())
        ContactsBoardState.ActionType.DoubleConnect,
        ContactsBoardState.ActionType.TripleConnect -> handleMultiConnect(player, actionType, playerContacts.single(), otherContacts)
        ContactsBoardState.ActionType.MyDoubleConnect -> handleMyDoubleConnect(player, actionType, playerContacts, otherContacts.single())
        ContactsBoardState.ActionType.SoloConnectRest -> handleSoloConnectRest(player, playerContacts)
        ContactsBoardState.ActionType.FinishReds -> handleFinishReds(player, playerContacts)
        ContactsBoardState.ActionType.ResolveMultiConnect -> handleResolveMultiConnect(player, playerContacts.single())
    }
}
