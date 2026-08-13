package cz.kotu.game.contacts.model

@ConsistentCopyVisibility
data class ContactsBoardState private constructor(
    val pool: List<Contact>,
    val racks: List<Rack>,

    val solved: Set<Contact>,
) {
    data class Player(
        val username: String,
    )

    data class Contact(
        val id: Int,
        val number: Int)

    data class Rack(
        val owner: Player,
        val contacts: List<Contact>,
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
                    contacts = contacts.toList(),
                )
            }

            return ContactsBoardState(
                pool = pool,
                racks = racks,
                solved = setOf(),
            )
        }
    }
}
