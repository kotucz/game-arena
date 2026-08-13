package cz.kotu.game.contacts.model

// internal constructor for testing
@ConsistentCopyVisibility
data class ContactsBoardState internal constructor(
    val pool: List<Contact>,
    val racks: List<Rack>,

    val solved: Set<Contact>,

    val faults: Int = 0,
) {
    data class Player(
        val username: String,
    )

    data class Contact(
        val id: Int,
        val number: Int,
    ) : Comparable<Contact> {
        override fun compareTo(other: Contact): Int = number.compareTo(other.number)
    }

    data class Rack(
        val owner: Player,
        val contacts: List<Contact>,
        val hints: Map<Contact, String> = emptyMap(),
    )

    companion object {
        fun create(players: List<Player>): ContactsBoardState {
            val numRacks = 4

            val pool: MutableList<Contact> = mutableListOf()

            var contId = 1

            for (num in 1 .. 12) {
                // 4 instances per number
                for (i in 1 .. 4) {
                    pool.add(Contact(id = contId, num))
                    contId++
                }
            }


            val rackContacts: List<MutableList<Contact>> = (1..numRacks).map { mutableListOf() }

            // distribute all contacts from pool among racks

            pool.shuffled().forEachIndexed { index, randContact ->
                rackContacts[index % numRacks].add(randContact)
            }

            val racks = rackContacts.mapIndexed { index, contacts ->
                val player = players[index % players.size]
                Rack(
                    owner = player,
                    contacts = contacts.sorted(),
                )
            }

            return ContactsBoardState(
                pool = pool,
                racks = racks,
                solved = setOf(),
            )
        }
    }

    fun withSolvedContacts(vararg contacts: Contact): ContactsBoardState {
        return this.copy(
            solved = solved + contacts.toSet(),
        )
    }

    fun withFaultFor(contact: Contact): ContactsBoardState {
        val rackIndex = racks.indexOfFirst { contact in it.contacts }
        if (rackIndex < 0) return this

        val rack = racks[rackIndex]
        val updatedRack = rack.copy(
            hints = rack.hints + (contact to contact.number.toString()),
        )

        return copy(
            racks = racks.toMutableList().also { it[rackIndex] = updatedRack },
            faults = faults + 1,
        )
    }

}
