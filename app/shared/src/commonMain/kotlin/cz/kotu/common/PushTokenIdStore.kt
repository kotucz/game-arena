package cz.kotu.common

import com.russhwolf.settings.Settings
import cz.kotu.gamearena.AppScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.uuid.Uuid
import me.tatarka.inject.annotations.Inject

@AppScope
@Inject
class PushTokenIdStore(private val settings: Settings) {
    private val mutex = Mutex()

    suspend fun getOrCreate(): String = mutex.withLock {
        settings.getStringOrNull(KEY) ?: Uuid.random().toString().also { settings.putString(KEY, it) }
    }

    private companion object {
        const val KEY = "push_token_id"
    }
}
