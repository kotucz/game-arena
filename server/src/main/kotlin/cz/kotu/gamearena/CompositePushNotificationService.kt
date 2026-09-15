package cz.kotu.gamearena

class CompositePushNotificationService(
    private val delegates: List<PushNotificationService>
) : PushNotificationService {
    override suspend fun sendToUser(
        username: String,
        title: String,
        body: String,
        data: Map<String, String>
    ): Int = delegates.sumOf { it.sendToUser(username, title, body, data) }

    override suspend fun sendToUsers(
        usernames: Collection<String>,
        title: String,
        body: String,
        data: Map<String, String>
    ): Int = delegates.sumOf { it.sendToUsers(usernames, title, body, data) }
}
