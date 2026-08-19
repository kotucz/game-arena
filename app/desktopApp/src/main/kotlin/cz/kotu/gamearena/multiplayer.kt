package cz.kotu.gamearena

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import cz.kotu.tools.DebugHttpClientFactory
import cz.kotu.tools.MultiPlayerScreen
import java.util.prefs.Preferences

fun main() = application {
    Window(
        state = rememberSavableWindowState(
            key = "multiplayer.kt",
            defaultSize = DpSize(1280.dp, 800.dp),
            defaultPosition = WindowPosition(Alignment.TopStart)
        ),
        onCloseRequest = ::exitApplication,
        title = "Game Arena - Multiplayer Debug",
    ) {
        val debugHttpClientFactory: DebugHttpClientFactory = ::createDebugAuthHttpClient
        MultiPlayerScreen(debugHttpClientFactory)
    }
}

@Composable
fun rememberSavableWindowState(
    key: String,
    defaultSize: DpSize = DpSize(1280.dp, 800.dp),
    defaultPosition: WindowPosition = WindowPosition(Alignment.Center)
): WindowState {
    val prefs = remember(key) { Preferences.userRoot().node("app/window/$key") }

    // Load initial values from Preferences (or fall back to defaults)
    val widthDp = prefs.getFloat("width", defaultSize.width.value).dp
    val heightDp = prefs.getFloat("height", defaultSize.height.value).dp

    val posX = prefs.getFloat("pos_x", Float.NaN)
    val posY = prefs.getFloat("pos_y", Float.NaN)

    val initialPosition = if (!posX.isNaN() && !posY.isNaN()) {
        WindowPosition(posX.dp, posY.dp)
    } else {
        defaultPosition
    }

    val windowState = rememberWindowState(
        size = DpSize(widthDp, heightDp),
        position = initialPosition
    )

    // Save preferences when window state changes or on disposal
    DisposableEffect(windowState) {
        onDispose {
            val size = windowState.size
            val position = windowState.position

            prefs.putFloat("width", size.width.value)
            prefs.putFloat("height", size.height.value)

            if (position is WindowPosition.Absolute) {
                prefs.putFloat("pos_x", position.x.value)
                prefs.putFloat("pos_y", position.y.value)
            }
            prefs.flush()
        }
    }

    return windowState
}