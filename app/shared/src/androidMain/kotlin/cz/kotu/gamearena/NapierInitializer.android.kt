package cz.kotu.gamearena

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

actual fun initNapier() {
    Napier.base(DebugAntilog())
}
