package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

// internal constructor for testing
@Serializable
@ConsistentCopyVisibility
data class ContactsBoardState internal constructor(
    val pool: List<Contact>,
    val racks: List<Rack>,

    val solved: Set<ContactId>,

    val faults: Int = 0,

    val allowedActionTypes: Set<ActionType> = setOf(
        ActionType.StandardConnect,
        ActionType.DoubleConnect,
        ActionType.TripleConnect,
        ActionType.MyDoubleConnect,
        ActionType.SoloConnectRest,
        ActionType.FinishReds,
        ActionType.AddHint,
    ),

    val resolveMultiConnect: ResolveMultiConnect? = null,
) {
    @Serializable
    data class Player(
        val username: String,
    )

    data class ContactsGameConfig(
        val players: List<Player>,
        val blueCount: Int = 12, // x4
        val yellowCount: Int = 0,
        val redCount: Int = 0,
    )

    @Serializable
    enum class ActionType(
        val playerContactsCount: Int,
        val otherContactsCount: Int,
    ) {
        AddHint(1, 0),
        StandardConnect(1, 1),
        DoubleConnect(1, 2),
        TripleConnect(1, 3),
        MyDoubleConnect(2, 1),
        SoloConnectRest(-1, 0),
        FinishReds(-1, 0),
        ResolveMultiConnect(1, 0),
        ;

        fun matches(playerContactsCount: Int, otherContactsCount: Int): Boolean {
            return this.playerContactsCount == playerContactsCount && this.otherContactsCount == otherContactsCount
        }
    }

    @JvmInline
    @Serializable
    value class ContactId(
        val value: Int,
    ) : Comparable<ContactId> {
        override fun compareTo(other: ContactId): Int = value.compareTo(other.value)
    }

    enum class ContactType {
        Blue,
        Yellow,
        Red,
    }

    @Serializable
    data class Contact(
        val id: ContactId,
        val number: Int,
        val type: ContactType = ContactType.Blue,
    ) : Comparable<Contact> {
        override fun compareTo(other: Contact): Int {
            return compareValuesBy(this, other, Contact::number, { it.type.ordinal })
        }
    }

    @Serializable
    data class Rack(
        val owner: Player,
        val contactIds: List<ContactId>,
        val hints: Map<ContactId, String> = emptyMap(),
    ) {
        fun hint(contact: Contact): String? {
            return hints[contact.id]
        }
    }

    /**
     * Target multi connect player will get to decide which of his contacts match best with the one of the original player
     **/
    @Serializable
    data class ResolveMultiConnect(
        val targetPlayer: Player,
        val originalContact: ContactId,
        val targetContacts: Set<ContactId>,
    )

    companion object {
        fun empty(): ContactsBoardState = ContactsBoardState(
            pool = emptyList(),
            racks = emptyList(),
            solved = emptySet(),
            allowedActionTypes = emptySet(),
        )

        fun create(config: ContactsGameConfig): ContactsBoardState {
            val numRacks = 4
            require(config.players.isNotEmpty()) { "At least one player is required" }
            require(config.yellowCount >= 0) { "yellowCount must not be negative" }
            require(config.redCount >= 0) { "redCount must not be negative" }
            require(config.yellowCount + config.redCount < config.blueCount) {
                "The total number of yellow and red contacts must not exceed ${config.blueCount}"
            }

            val specialTypes = buildList {
                repeat(config.yellowCount) { add(ContactType.Yellow) }
                repeat(config.redCount) { add(ContactType.Red) }
            }.shuffled()
            val specialNumbers = (1..<config.blueCount).shuffled().take(specialTypes.size)
                .zip(specialTypes)
                .toMap()
            val pool = mutableListOf<Contact>()

            var contId = 1

            for (num in 1..config.blueCount) {
                // 4 instances per number
                repeat(4) {
                    pool.add(Contact(id = ContactId(contId), number = num))
                    contId++
                }
                specialNumbers[num]?.let { type ->
                    pool.add(Contact(id = ContactId(contId), number = num, type = type))
                    contId++
                }
            }

            val rackContacts: List<MutableList<ContactId>> = (1..numRacks).map { mutableListOf() }

            // distribute all contacts from pool among racks

            pool.shuffled().forEachIndexed { index, randContact ->
                rackContacts[index % numRacks].add(randContact.id)
            }

            val contactsById = pool.associateBy { it.id }
            val racks = rackContacts.mapIndexed { index, contacts ->
                val player = config.players[index % config.players.size]
                Rack(
                    owner = player,
                    contactIds = contacts.sortedBy { contactsById.getValue(it) },
                )
            }

            return ContactsBoardState(
                pool = pool,
                racks = racks,
                solved = setOf(),
                allowedActionTypes = ActionType.entries
                    .filterNot { it == ActionType.ResolveMultiConnect }
                    .toSet()
            )
        }
    }

    fun contact(contactId: ContactId): Contact? {
        return pool.firstOrNull { it.id == contactId }
    }

    fun requireContact(contactId: ContactId): Contact {
        return requireNotNull(contact(contactId)) { "Unknown contact id: $contactId" }
    }

    fun playerRacks(player: Player): List<Rack> {
        return racks.filter { it.owner == player }
    }

    fun rackContacts(rack: Rack): List<Contact> {
        return rack.contactIds.map(::requireContact)
    }

    fun isSolved(contactId: ContactId): Boolean {
        return contactId in solved
    }

    fun isSolved(contact: Contact): Boolean {
        return isSolved(contact.id)
    }

    fun isOwnedBy(player: Player, contact: Contact): Boolean {
        return racks.any { it.owner == player && contact.id in it.contactIds }
    }

    fun isOwnedByAnotherPlayer(player: Player, contact: Contact): Boolean {
        return racks.any { it.owner != player && contact.id in it.contactIds }
    }

    fun contactsMatch(first: Contact, second: Contact): Boolean {
        return first.matchKey == second.matchKey
    }

    fun isActionLegal(
        player: Player,
        actionType: ActionType,
        playerContacts: Set<Contact>,
        otherContacts: Set<Contact>,
    ): String? {
        if (actionType !in allowedActionTypes && actionType != ActionType.ResolveMultiConnect) {
            return "Action type is not allowed"
        }

        if (actionType == ActionType.FinishReds) {
            if (playerContacts.isEmpty() || otherContacts.isNotEmpty()) {
                return "Invalid number of selected contacts"
            }
            if (playerContacts.any { it.type != ContactType.Red }) {
                return "All selected contacts must be red"
            }
            if (playerContacts.any { !isOwnedBy(player, it) || isSolved(it) }) {
                return "Selected contact must be an unsolved contact owned by the player"
            }
            val unsolvedPlayerContacts = playerRacks(player)
                .flatMap { rack -> rackContacts(rack) }
                .filter { !isSolved(it) }.toSet()
            if (unsolvedPlayerContacts.any { it.type != ContactType.Red }) {
                return "All other contacts must be solved"
            }
            if (playerContacts != unsolvedPlayerContacts) {
                return "All remaining red contacts must be selected"
            }
            return null
        }

        val anyPlayerContactsRed = playerContacts.any { it.type == ContactType.Red }

        if (actionType == ActionType.SoloConnectRest) {
            if (playerContacts.isEmpty() || otherContacts.isNotEmpty()) {
                return "Invalid number of selected contacts"
            }
            if (anyPlayerContactsRed) {
                return "Selected not be red. Use FinishRed action"
            }
            if (playerContacts.map { it.matchKey }.toSet().size != 1) {
                return "Selected contacts must have the same number or all yellow"
            }
            if (playerContacts.any { !isOwnedBy(player, it) || isSolved(it) }) {
                return "Selected contact must be an unsolved contact owned by the player"
            }
            val matchKey = playerContacts.first().matchKey
            val remainingNotOwnedContacts = pool.filter {
                it.matchKey == matchKey && !isOwnedBy(player, it) && !isSolved(it)
            }.toSet()
            if (remainingNotOwnedContacts.isNotEmpty()) {
                return "Some other player still has that contact"
            }
            val remainingOwnedContacts = pool.filter {
                it.matchKey == matchKey && isOwnedBy(player, it) && !isSolved(it)
            }.toSet()
            if (playerContacts != remainingOwnedContacts) {
                return "All remaining contacts with the number must be selected"
            }
            return null
        }

        if (!actionType.matches(playerContacts.size, otherContacts.size)) {
            return "Invalid number of selected contacts: player (${playerContacts.size}/${actionType.playerContactsCount}) other (${otherContacts.size}/${actionType.otherContactsCount})"
        }

        if (actionType == ActionType.ResolveMultiConnect) {
            val resolution = resolveMultiConnect
                ?: return "There is no multi-connect to resolve"
            val targetContact = playerContacts.single()
            if (resolution.targetPlayer != player) {
                return "Only the target player can resolve the multi-connect"
            }
            if (targetContact.id !in resolution.targetContacts) {
                return "Selected contact is not a multi-connect target"
            }
            return null
        }

        if (anyPlayerContactsRed) {
            return "Red contacts cannot be connected"
        }

        if (playerContacts.any { !isOwnedBy(player, it) }) {
            return "Player does not own the selected contact"
        }
        if (otherContacts.any { !isOwnedByAnotherPlayer(player, it) }) {
            return "Selected opposing contact is not owned by another player"
        }
        if (playerContacts.any(::isSolved) || otherContacts.any(::isSolved)) {
            return "Selected contact is already solved"
        }

        if (actionType == ActionType.DoubleConnect || actionType == ActionType.TripleConnect) {
            val targetRack = racks.singleOrNull { rack ->
                otherContacts.all { it.id in rack.contactIds }
            }
            if (targetRack == null) {
                return "Selected opposing contacts must belong to one rack"
            }
        }

        return null
    }

    fun withSolvedContacts(vararg contacts: Contact): ContactsBoardState {
        return this.copy(
            solved = solved + contacts.map { it.id },
        )
    }

    fun withFaultFor(contact: Contact): ContactsBoardState {
        return withHintFor(contact).copy(faults = faults + 1)
    }

    fun withHintFor(contact: Contact): ContactsBoardState {
        val rackIndex = racks.indexOfFirst { contact.id in it.contactIds }
        if (rackIndex < 0) return this

        val rack = racks[rackIndex]
        val hintText = contact.matchKey
        val updatedRack = rack.copy(
            hints = rack.hints + (contact.id to hintText),
        )

        return copy(
            racks = racks.toMutableList().also { it[rackIndex] = updatedRack },
        )
    }

}

val ContactsBoardState.Contact.matchKey: String
    get() = when (type) {
        ContactsBoardState.ContactType.Blue -> number.toString()
        ContactsBoardState.ContactType.Yellow -> "Y"
        ContactsBoardState.ContactType.Red -> "R"
    }
