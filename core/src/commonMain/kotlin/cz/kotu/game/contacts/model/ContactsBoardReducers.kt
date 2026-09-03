package cz.kotu.game.contacts.model

// Reducers and action executors operating on immutable ContactsBoardState

fun ContactsBoardState.handleAddHint(
    player: ContactsBoardState.Player,
    playerContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult {
    if (playerContacts.size != 1) return ActionExecutionResult.Failure("Invalid number of selected contacts")
    val playerContact = playerContacts.single()
    if (!isOwnedBy(
            player,
            playerContact,
        )
    ) return ActionExecutionResult.Failure("Player does not own the selected contact")
    if (isSolved(playerContact)) return ActionExecutionResult.Failure("Selected contact is already solved")
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
    // basic validation
    if (playerContact.type == ContactsBoardState.ContactType.Red) return ActionExecutionResult.Failure("Red contacts cannot be connected")
    if (!isOwnedBy(
            player,
            playerContact,
        )
    ) return ActionExecutionResult.Failure("Player does not own the selected contact")
    if (!isOwnedByAnotherPlayer(
            player,
            otherContact,
        )
    ) return ActionExecutionResult.Failure("Selected opposing contact is not owned by another player")
    if (isSolved(playerContact) || isSolved(otherContact)) return ActionExecutionResult.Failure("Selected contact is already solved")

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
    // basic validation
    if (!actionType.matches(
            1,
            otherContacts.size,
        )
    ) return ActionExecutionResult.Failure("Invalid number of selected contacts")
    if (!isOwnedBy(
            player,
            playerContact,
        )
    ) return ActionExecutionResult.Failure("Player does not own the selected contact")
    if (playerContact.type == ContactsBoardState.ContactType.Red) return ActionExecutionResult.Failure("Red contacts cannot be connected")
    if (otherContacts.any {
            !isOwnedByAnotherPlayer(
                player,
                it,
            )
        }) return ActionExecutionResult.Failure("Selected opposing contact is not owned by another player")
    if (playerContact.run { isSolved(this) } || otherContacts.any(::isSolved)) return ActionExecutionResult.Failure("Selected contact is already solved")

    val targetRack = racks.singleOrNull { rack -> otherContacts.all { it.id in rack.contactIds } }
        ?: return ActionExecutionResult.Failure("Selected opposing contacts must belong to one rack")
    val newState = copy(
        resolveMultiConnect = ContactsBoardState.ResolveMultiConnect(
            targetPlayer = targetRack.owner,
            originalContact = playerContact.id,
            targetContacts = otherContacts.map { it.id }.toSet(),
        ),
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
    if (playerContacts.any {
            !isOwnedBy(
                player,
                it,
            )
        }) return ActionExecutionResult.Failure("Player does not own the selected contact")
    if (!isOwnedByAnotherPlayer(
            player,
            otherContact,
        )
    ) return ActionExecutionResult.Failure("Selected opposing contact is not owned by another player")
    if (playerContacts.any(::isSolved) || isSolved(otherContact)) return ActionExecutionResult.Failure("Selected contact is already solved")

    val playerContact = playerContacts.firstOrNull { contactsMatch(it, otherContact) } ?: playerContacts.first()
    // Delegate to standard connect handler
    return handleStandardConnect(player, playerContact, otherContact)
}

fun ContactsBoardState.handleSoloConnectRest(
    player: ContactsBoardState.Player,
    playerContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult {
    // replicate previous solo connect validation
    if (playerContacts.isEmpty()) return ActionExecutionResult.Failure("Invalid number of selected contacts")
    if (playerContacts.any { it.type == ContactsBoardState.ContactType.Red }) return ActionExecutionResult.Failure("Selected not be red. Use FinishRed action")
    if (playerContacts.map { it.matchKey }
            .toSet().size != 1) return ActionExecutionResult.Failure("Selected contacts must have the same number or all yellow")
    if (playerContacts.any {
            !isOwnedBy(
                player,
                it,
            ) || isSolved(it)
        }) return ActionExecutionResult.Failure("Selected contact must be an unsolved contact owned by the player")
    val matchKey = playerContacts.first().matchKey
    val remainingNotOwnedContacts =
        pool.filter { it.matchKey == matchKey && !isOwnedBy(player, it) && !isSolved(it) }.toSet()
    if (remainingNotOwnedContacts.isNotEmpty()) return ActionExecutionResult.Failure("Some other player still has that contact")
    val remainingOwnedContacts =
        pool.filter { it.matchKey == matchKey && isOwnedBy(player, it) && !isSolved(it) }.toSet()
    if (playerContacts != remainingOwnedContacts) return ActionExecutionResult.Failure("All remaining contacts with the number must be selected")

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
    // replicate previous finish reds validation
    if (playerContacts.isEmpty()) return ActionExecutionResult.Failure("Invalid number of selected contacts")
    if (playerContacts.any { it.type != ContactsBoardState.ContactType.Red }) return ActionExecutionResult.Failure("All selected contacts must be red")
    if (playerContacts.any {
            !isOwnedBy(
                player,
                it,
            ) || isSolved(it)
        }) return ActionExecutionResult.Failure("Selected contact must be an unsolved contact owned by the player")
    val unsolvedPlayerContacts =
        playerRacks(player).flatMap { rack -> rackContacts(rack) }.filter { !isSolved(it) }.toSet()
    if (unsolvedPlayerContacts.any { it.type != ContactsBoardState.ContactType.Red }) return ActionExecutionResult.Failure(
        "All other contacts must be solved",
    )
    if (playerContacts != unsolvedPlayerContacts) return ActionExecutionResult.Failure("All remaining red contacts must be selected")

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

fun ContactsBoardState.applyAction1(
    player: ContactsBoardState.Player,
    actionType: ContactsBoardState.ActionType,
    playerContacts: Set<ContactsBoardState.ContactId>,
    otherContacts: Set<ContactsBoardState.ContactId>,
): ActionExecutionResult {
    return applyAction(
        player = player,
        actionType = actionType,
        playerContacts = playerContacts.map { requireContact(it) }.toSet(),
        otherContacts = otherContacts.map { requireContact(it) }.toSet(),
    )
}

fun ContactsBoardState.applyAction(
    player: ContactsBoardState.Player,
    actionType: ContactsBoardState.ActionType,
    playerContacts: Set<ContactsBoardState.Contact>,
    otherContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult {
    // Arity checks per-dimension to support wildcard (-1) counts such as SoloConnectRest/FinishReds.
    val playerExpected = actionType.playerContactsCount
    val otherExpected = actionType.otherContactsCount
    val playerMismatch = playerExpected != -1 && playerContacts.size != playerExpected
    val otherMismatch = otherExpected != -1 && otherContacts.size != otherExpected
    if (playerMismatch || otherMismatch) {
        return ActionExecutionResult.Failure(
            "Invalid number of selected contacts: player (${playerContacts.size}/${playerExpected}) other (${otherContacts.size}/${otherExpected})",
        )
    }

    return when (actionType) {
        ContactsBoardState.ActionType.AddHint -> handleAddHint(player, playerContacts)
        ContactsBoardState.ActionType.StandardConnect -> handleStandardConnect(
            player,
            playerContacts.single(),
            otherContacts.single(),
        )

        ContactsBoardState.ActionType.DoubleConnect,
        ContactsBoardState.ActionType.TripleConnect,
            -> handleMultiConnect(player, actionType, playerContacts.single(), otherContacts)

        ContactsBoardState.ActionType.MyDoubleConnect -> handleMyDoubleConnect(
            player,
            actionType,
            playerContacts,
            otherContacts.single(),
        )

        ContactsBoardState.ActionType.SoloConnectRest -> handleSoloConnectRest(player, playerContacts)
        ContactsBoardState.ActionType.FinishReds -> handleFinishReds(player, playerContacts)
        ContactsBoardState.ActionType.ResolveMultiConnect -> handleResolveMultiConnect(player, playerContacts.single())
    }
}
