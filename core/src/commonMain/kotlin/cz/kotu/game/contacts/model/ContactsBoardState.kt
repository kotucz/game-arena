package cz.kotu.game.contacts.model

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

internal typealias ContactQuery = Sequence<ContactsBoardState.Contact>

// internal constructor for testing
@Serializable
@ConsistentCopyVisibility
data class ContactsBoardState internal constructor(
    val pool: List<Contact>,
    val racks: List<Rack>,

    val solved: Set<ContactId>,

    val faults: Int = 0,

    val lastActionResult: ActionResult = ActionResult(),
) {
    @Serializable
    @JvmInline
    value class Player(
        val username: String,
    )

    @Serializable
    data class ContactsGameConfig(
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

        val value get() = ContactValue(number, type)
    }

    @Serializable
    data class ContactValue(
        val number: Int,
        val type: ContactType,
    ) : Comparable<ContactValue> {
        override fun compareTo(other: ContactValue): Int {
            return compareValuesBy(this, other, ContactValue::number, { it.type.ordinal })
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
        val originalPlayer: Player,
        val targetPlayer: Player,
        val originalContact: ContactId,
        val targetContacts: Set<ContactId>,
    )

    @Serializable
    data class ActionResult(
        val errorContacts: Set<ContactId> = emptySet(),
    )

    companion object {
        fun empty(): ContactsBoardState = ContactsBoardState(
            pool = emptyList(),
            racks = emptyList(),
            solved = emptySet(),
        )

        fun create(players: List<Player>, config: ContactsGameConfig): ContactsBoardState {
            val numRacks = 4
            require(players.isNotEmpty()) { "At least one player is required" }
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
                val player = players[index % players.size]
                Rack(
                    owner = player,
                    contactIds = contacts.sortedBy { contactsById.getValue(it) },
                )
            }

            return ContactsBoardState(
                pool = pool,
                racks = racks,
                solved = setOf(),
            )
        }
    }

    internal fun contacts(): ContactQuery = pool.asSequence()

    internal fun ContactQuery.unsolved(): ContactQuery = filter { !isSolved(it) }

    internal fun ContactQuery.ownedBy(player: Player): ContactQuery = filter { isOwnedBy(player, it) }

    internal fun ContactQuery.ownedByOthers(player: Player): ContactQuery = filter { isOwnedByAnotherPlayer(player, it) }

    internal fun ContactQuery.excludingReds(): ContactQuery = filter { it.type != ContactType.Red }

    internal fun ContactQuery.matching(contact: Contact): ContactQuery = filter { contactsMatch(it, contact) }

    internal fun ContactQuery.ids(): Set<ContactId> = map { it.id }.toSet()

    fun contact(contactId: ContactId): Contact? {
        return pool.firstOrNull { it.id == contactId }
    }

    fun requireContact(contactId: ContactId): Contact {
        return requireNotNull(contact(contactId)) { "Unknown contact id: $contactId" }
    }

    fun hasUnsolvedContacts(player: Player): Boolean {
        return contacts().ownedBy(player).unsolved().any()
    }

    fun unsolvedContacts(player: Player): List<Contact> {
        return contacts().ownedBy(player).unsolved().toList()
    }

    fun hasUnsolvedMatchOnAnotherPlayer(player: Player, contact: Contact): Boolean {
        return contacts().unsolved().ownedByOthers(player).matching(contact).any()
    }

    fun canStandardConnect(player: Player): Boolean {
        return contacts().unsolved().ownedBy(player).excludingReds()
            .any { hasUnsolvedMatchOnAnotherPlayer(player, it) }
    }

    /** Complete selections for each matching group the player can solo connect. */
    fun soloConnectRestGroups(player: Player): List<Set<ContactId>> {
        return contacts().unsolved().ownedBy(player).excludingReds().groupBy { it.matchKey }.values
            .filter { canSoloConnectRest(player, it.first()) }
            .map { contacts -> contacts.map { it.id }.toSet() }
    }

    fun canSoloConnectRest(player: Player): Boolean {
        return soloConnectRestGroups(player).isNotEmpty()
    }

    /** Whether the player owns every unsolved contact matching this non-Red contact. */
    fun canSoloConnectRest(player: Player, contact: Contact): Boolean {
        return contact.type != ContactType.Red && !isSolved(contact) && isOwnedBy(player, contact) &&
            !hasUnsolvedMatchOnAnotherPlayer(player, contact)
    }

    fun canFinishReds(player: Player): Boolean {
        val remainingContacts = unsolvedContacts(player)
        return remainingContacts.isNotEmpty() && remainingContacts.all { it.type == ContactType.Red }
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

    fun isContactVisibleToPlayer(contact: Contact, player: Player): Boolean {
        return isOwnedBy(player, contact) || isSolved(contact)
    }

    fun getHint(contact: Contact): String? {
        return racks.firstNotNullOfOrNull { it.hint(contact) }
    }

    fun contactsMatch(first: Contact, second: Contact): Boolean {
        return first.matchKey == second.matchKey
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

    fun withLastActionResult(actionResult: ActionResult): ContactsBoardState {
        return copy(lastActionResult = actionResult)
    }

}

val ContactsBoardState.Contact.matchKey: String
    get() = when (type) {
        ContactsBoardState.ContactType.Blue -> number.toString()
        ContactsBoardState.ContactType.Yellow -> "Y"
        ContactsBoardState.ContactType.Red -> "R"
    }
