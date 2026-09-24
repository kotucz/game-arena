package cz.kotu.common

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.push.firebase.FirebasePush
import cz.kotu.gamearena.app.shared.R

actual fun initNotifications() {
    KMPNotifier.initialize(
        NotificationPlatformConfiguration.Android(
            notificationIconResId = R.drawable.ic_notification,
        ),
        LocalNotifications, FirebasePush,
    )
}

internal actual suspend fun fetchPushToken(): String? = FirebasePush.notifier.getToken()

internal actual fun getPersistentPushTokenId(): String? = null
