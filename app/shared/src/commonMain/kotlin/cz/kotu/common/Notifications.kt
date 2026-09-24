package cz.kotu.common

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.localNotifier
import com.mmk.kmpnotifier.notification.PayloadData
import com.mmk.kmpnotifier.push.PushListener
import com.mmk.kmpnotifier.push.firebase.addPushListener
import cz.kotu.gamearena.AppScope
import cz.kotu.gamearena.AuthManager
import cz.kotu.gamearena.NotificationClient
import io.github.aakira.napier.Napier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.tatarka.inject.annotations.Inject

@AppScope
class Notifications @Inject constructor(
    private val authManager: AuthManager,
    private val notificationClient: NotificationClient,
    private val pushTokenIdStore: PushTokenIdStore,
    private val appScope: CoroutineScope,
) {
    private val tokenState = MutableStateFlow<String?>(null)
    private val permissionGrantedState = MutableStateFlow(false)

    init {
        KMPNotifier.addListener(
            object : KMPNotifier.Listener {
                override fun onNotificationClicked(data: PayloadData) {
                    super.onNotificationClicked(data)
                    Napier.i("Push notification clicked")
                }

                override fun onAction(
                    actionId: String,
                    notificationId: Int,
                    payload: PayloadData,
                ) {
                    super.onAction(actionId, notificationId, payload)
                    Napier.i("Push notification action clicked")
                }

            },
        )
        KMPNotifier.addPushListener(
            listener = object : PushListener {
                override fun onNewToken(token: String) {
                    super.onNewToken(token)
                    Napier.i("New push token: $token")
                    onPushToken(token)
                }

                override fun onPayloadData(data: PayloadData) {
                    super.onPayloadData(data)
                    Napier.i("onPayloadData")
                }

                override fun onPushNotification(title: String?, body: String?) {
                    super.onPushNotification(title, body)
                    Napier.i("onPushNotification")
                }

                override fun onPushNotificationWithPayloadData(
                    title: String?,
                    body: String?,
                    data: PayloadData,
                ) {
                    super.onPushNotificationWithPayloadData(title, body, data)
                    Napier.i("onPushNotificationWithPayloadData")
                }

            },
        )

        initNotifications()

        observeAndSyncTokens()
    }

    private fun observeAndSyncTokens() {
        Napier.d { "observeAndSyncTokens" }
        val authFlow = authManager.currentUsername
        val tokenFlow = tokenState
        val permissionFlow = permissionGrantedState

        combine(authFlow, tokenFlow, permissionFlow) { user, token, isGranted ->
            Napier.d { "push token check1: $user $isGranted $token" }
            Triple(user, token, isGranted)
        }
            .distinctUntilChanged()
            .onEach { (username, token, isGranted) ->
                Napier.d { "push token check: $username $isGranted $token" }
                if (!username.isNullOrBlank() && !token.isNullOrBlank() && isGranted) {
                    // All conditions met: sync token to server
                    try {
                        val clientTokenId = pushTokenIdStore.getOrCreate()
                        notificationClient.registerToken(clientTokenId, "fcm", token)
                    } catch (e: Exception) {
                        // Handle network failure or retry with backoff
                        Napier.e("Failed to register token", e)
                    }
                }
            }
            .launchIn(appScope)
    }

    fun onNotificationPermission(
        granted: Boolean,
        requestPushToken: suspend () -> String?,
    ) {
        permissionGrantedState.value = granted
        if (granted) {
            appScope.launch {
                requestPushToken()?.let(::onPushToken)
            }
        }
    }

    fun onPushToken(token: String) {
        tokenState.value = token
    }

    fun showNotification() {
        KMPNotifier.localNotifier.notify(title = "Game Arena", "You are on turn in game")
    }

}

expect fun initNotifications()
