package cz.kotu.common

import com.mmk.kmpnotifier.KMPNotifier
import com.mmk.kmpnotifier.local.localNotifier

class Notifications {
    init {
        initNotifications()
    }
    fun showNotification() {
        KMPNotifier.localNotifier.notify(title = "Game Arena", "You are on turn in game")
    }
}

expect fun initNotifications()
