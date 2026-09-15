package cz.kotu.gamearena

import android.app.Application

class GameArenaApplication : Application() {

    val appComponent by lazy { AppComponent::class.create() }

    override fun onCreate() {
        super.onCreate()
        initNapier()
    }
}
