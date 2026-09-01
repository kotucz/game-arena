package cz.kotu.game.contacts

import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

@Composable
fun Flippable(
    isFlipped: Boolean,
    front: @Composable () -> Unit,
    back: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    durationMillis: Int = 300,
    cameraDistanceDp: Dp = 12.dp,
) {
    val transition = updateTransition(targetState = isFlipped, label = "FlippableTransition")
    val rotation by transition.animateFloat(transitionSpec = { tween(durationMillis) }, label = "rotation") { flipped ->
        if (flipped) 180f else 0f
    }

    val density = LocalDensity.current
    val cameraDistancePx = with(density) { cameraDistanceDp.toPx() }

    Box(
        modifier = modifier.graphicsLayer {
            rotationY = rotation
            // cameraDistance is provided in pixels; ensure it's a reasonable value for perspective
            cameraDistance = cameraDistancePx
        },
        contentAlignment = Alignment.Center,
    ) {
        // When the tile is rotated beyond 90 degrees, show the back content.
        if (rotation <= 90f || rotation >= 270f) {
            front()
        } else {
            // Flip inner content 180deg so it reads correctly when the parent rotated to back side
            Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                back()
            }
        }
    }
}