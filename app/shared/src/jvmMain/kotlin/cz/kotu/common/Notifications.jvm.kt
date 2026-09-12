package cz.kotu.common

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration

actual fun initNotifications() {
    KMPNotifier.initialize(
        NotificationPlatformConfiguration.Desktop(),
        LocalNotifications,
    )
}