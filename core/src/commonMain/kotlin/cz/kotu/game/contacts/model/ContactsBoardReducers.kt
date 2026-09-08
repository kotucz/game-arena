package cz.kotu.game.contacts.model

// Reducers and action executors operating on immutable ContactsBoardState

fun ContactsBoardState.handleAddHint(
    player: ContactsBoardState.Player,
    playerContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult.Success {
    if (playerContacts.size != 1) throw InvalidActionException("Invalid number of selected contacts")
    val playerContact = playerContacts.single()
    if (!isOwnedBy(
            player,
            playerContact,
        )
    ) throw InvalidActionException("Player does not own the selected contact")
    if (isSolved(playerContact)) throw InvalidActionException("Selected contact is already solved")
    val newState = this.withHintFor(playerContact)
        .withLastActionResult(ContactsBoardState.ActionResult())
    return ActionExecutionResult.Success(newState, Next.EndOfTurn(player)) {
        player(player)
        text("hinted")
        contact(playerContact)
    }
}

fun ContactsBoardState.handleStandardConnect(
    player: ContactsBoardState.Player,
    playerContact: ContactsBoardState.Contact,
    otherContact: ContactsBoardState.Contact,
): ActionExecutionResult.Success {
    // basic validation
    if (playerContact.type == ContactsBoardState.ContactType.Red) throw InvalidActionException("Red contacts cannot be connected")
    if (!isOwnedBy(
            player,
            playerContact,
        )
    ) throw InvalidActionException("Player does not own the selected contact")
    if (!isOwnedByAnotherPlayer(
            player,
            otherContact,
        )
    ) throw InvalidActionException("Selected opposing contact is not owned by another player")
    if (isSolved(playerContact) || isSolved(otherContact)) throw InvalidActionException("Selected contact is already solved")

    // mismatch -> fault
    if (!contactsMatch(playerContact, otherContact)) {
        val boom = otherContact.type == ContactsBoardState.ContactType.Red
        val newState = withFaultFor(otherContact)
            .withLastActionResult(ContactsBoardState.ActionResult(errorContacts = setOf(otherContact.id)))
        // TODO game over: too many faults
        return ActionExecutionResult.Success(
            newState,
            next = if (boom) Next.GameOver("Game over: Red connected!") else Next.EndOfTurn(player),
        ) {
            player(player)
            text("mismatched")
            text(playerContact.matchKey)
            text("with")
            contact(otherContact)
            if (boom) text("BOOM! Red Connected! [Game Over]") else text("FAILURE!")
        }
    }

    val newState = withSolvedContacts(playerContact, otherContact)
        .withLastActionResult(ContactsBoardState.ActionResult())
    return ActionExecutionResult.Success(newState, next = Next.EndOfTurn(player)) {
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
): ActionExecutionResult.Success {
    // basic validation
    if (!actionType.matches(
            1,
            otherContacts.size,
        )
    ) throw InvalidActionException("Invalid number of selected contacts")
    if (!isOwnedBy(
            player,
            playerContact,
        )
    ) throw InvalidActionException("Player does not own the selected contact")
    if (playerContact.type == ContactsBoardState.ContactType.Red) throw InvalidActionException("Red contacts cannot be connected")
    if (otherContacts.any {
            !isOwnedByAnotherPlayer(
                player,
                it,
            )
        }) throw InvalidActionException("Selected opposing contact is not owned by another player")
    if (playerContact.run { isSolved(this) } || otherContacts.any(::isSolved)) throw InvalidActionException("Selected contact is already solved")

    val targetRack = racks.singleOrNull { rack -> otherContacts.all { it.id in rack.contactIds } }
        ?: throw InvalidActionException("Selected opposing contacts must belong to one rack")

    val newState = this // no change
    return ActionExecutionResult.Success(
        newState,
        next = Next.ResolveMultiConnect(
            resolveMultiConnect = ContactsBoardState.ResolveMultiConnect(
                originalPlayer = player,
                targetPlayer = targetRack.owner,
                originalContact = playerContact.id,
                targetContacts = otherContacts.map { it.id }.toSet(),
            ),
        ),
    ) {
        player(player)
        text("connecting")
        text(playerContact.matchKey)
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
): ActionExecutionResult.Success {
    if (!actionType.matches(playerContacts.size, 1)) {
        throw InvalidActionException("Invalid number of selected contacts")
    }
    if (playerContacts.any {
            !isOwnedBy(
                player,
                it,
            )
        }) throw InvalidActionException("Player does not own the selected contact")
    if (!isOwnedByAnotherPlayer(
            player,
            otherContact,
        )
    ) throw InvalidActionException("Selected opposing contact is not owned by another player")
    if (playerContacts.any(::isSolved) || isSolved(otherContact)) throw InvalidActionException("Selected contact is already solved")

    val playerContact = playerContacts.firstOrNull { contactsMatch(it, otherContact) } ?: playerContacts.first()
    // Delegate to standard connect handler
    return handleStandardConnect(player, playerContact, otherContact)
}

fun ContactsBoardState.handleSoloConnectRest(
    player: ContactsBoardState.Player,
    playerContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult.Success {
    // replicate previous solo connect validation
    if (playerContacts.isEmpty()) throw InvalidActionException("Invalid number of selected contacts")
    if (playerContacts.any { it.type == ContactsBoardState.ContactType.Red }) throw InvalidActionException("Selected not be red. Use FinishRed action")
    if (playerContacts.map { it.matchKey }
            .toSet().size != 1) throw InvalidActionException("Selected contacts must have the same number or all yellow")
    if (playerContacts.any {
            !isOwnedBy(
                player,
                it,
            ) || isSolved(it)
        }) throw InvalidActionException("Selected contact must be an unsolved contact owned by the player")
    val matchKey = playerContacts.first().matchKey
    val remainingNotOwnedContacts =
        pool.filter { it.matchKey == matchKey && !isOwnedBy(player, it) && !isSolved(it) }.toSet()
    if (remainingNotOwnedContacts.isNotEmpty()) throw InvalidActionException("Some other player still has that contact")
    val remainingOwnedContacts =
        pool.filter { it.matchKey == matchKey && isOwnedBy(player, it) && !isSolved(it) }.toSet()
    if (playerContacts != remainingOwnedContacts) throw InvalidActionException("All remaining contacts with the number must be selected")

    val newState = withSolvedContacts(*playerContacts.toTypedArray())
        .withLastActionResult(ContactsBoardState.ActionResult())
    return ActionExecutionResult.Success(newState, Next.EndOfTurn(player)) {
        player(player)
        text("solo connected")
        contacts(playerContacts)
    }
}

fun ContactsBoardState.handleFinishReds(
    player: ContactsBoardState.Player,
    playerContacts: Set<ContactsBoardState.Contact>,
): ActionExecutionResult.Success {
    // replicate previous finish reds validation
    if (playerContacts.isEmpty()) throw InvalidActionException("Invalid number of selected contacts")
    if (playerContacts.any { it.type != ContactsBoardState.ContactType.Red }) throw InvalidActionException("All selected contacts must be red")
    if (playerContacts.any {
            !isOwnedBy(
                player,
                it,
            ) || isSolved(it)
        }) throw InvalidActionException("Selected contact must be an unsolved contact owned by the player")
    val unsolvedPlayerContacts =
        playerRacks(player).flatMap { rack -> rackContacts(rack) }.filter { !isSolved(it) }.toSet()
    if (unsolvedPlayerContacts.any { it.type != ContactsBoardState.ContactType.Red }) throw InvalidActionException(
        "All other contacts must be solved",
    )
    if (playerContacts != unsolvedPlayerContacts) throw InvalidActionException("All remaining red contacts must be selected")

    val newState = withSolvedContacts(*playerContacts.toTypedArray())
        .withLastActionResult(ContactsBoardState.ActionResult())
    return ActionExecutionResult.Success(newState, Next.EndOfTurn(player)) {
        player(player)
        text("finished reds")
        contacts(playerContacts)
    }
}

fun ContactsBoardState.handleResolveMultiConnect(
    player: ContactsBoardState.Player,
    targetContact: ContactsBoardState.ContactId,
    resolveMultiConnect: ContactsBoardState.ResolveMultiConnect,
): ActionExecutionResult.Success {
    if (resolveMultiConnect.targetPlayer != player) throw InvalidActionException("Only the target player can resolve the multi-connect")
    if (targetContact !in resolveMultiConnect.targetContacts) throw InvalidActionException("Selected contact is not a multi-connect target")

    val originalContact = requireContact(resolveMultiConnect.originalContact)
    // determine original player
    val originalPlayer = resolveMultiConnect.originalPlayer

    // Resolve with exactly same validation/result as a normal StandardConnect made by the original contact's owner
    return handleStandardConnect(originalPlayer, originalContact, requireContact(targetContact))
}

fun ContactsBoardState.applyActionIds(
    player: ContactsBoardState.Player,
    actionType: ContactsBoardState.ActionType,
    playerContacts: Set<ContactsBoardState.ContactId>,
    otherContacts: Set<ContactsBoardState.ContactId>,
): ActionExecutionResult.Success {
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
): ActionExecutionResult.Success {
    // Arity checks per-dimension to support wildcard (-1) counts such as SoloConnectRest/FinishReds.
    val playerExpected = actionType.playerContactsCount
    val otherExpected = actionType.otherContactsCount
    val playerMismatch = playerExpected != -1 && playerContacts.size != playerExpected
    val otherMismatch = otherExpected != -1 && otherContacts.size != otherExpected
    if (playerMismatch || otherMismatch) {
        throw InvalidActionException(
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
        ContactsBoardState.ActionType.ResolveMultiConnect -> throw InvalidActionException("Use handleResolveMultiConnect")
    }
}
