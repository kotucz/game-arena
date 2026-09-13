package cz.kotu.gamearena

import android.app.Application
import cz.kotu.common.initNotifications

class GameArenaApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initNapier()
    }
}
