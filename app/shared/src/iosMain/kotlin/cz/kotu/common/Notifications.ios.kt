package cz.kotu.common

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.LocalNotifications
import com.mmk.kmpnotifier.notification.configuration.NotificationPlatformConfiguration
import com.mmk.kmpnotifier.push.firebase.FirebasePush

actual fun initNotifications() {
    KMPNotifier.initialize(
        NotificationPlatformConfiguration.Ios(),
        LocalNotifications, FirebasePush,
    )
}