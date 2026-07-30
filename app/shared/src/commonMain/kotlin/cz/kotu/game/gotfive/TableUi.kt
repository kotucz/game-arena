package cz.kotu.game.gotfive

import androidx.compose.runtime.Composable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import cz.kotu.game.gotfive.model.Tile
import cz.kotu.game.gotfive.model.Tiles

@Composable
fun Table(gameViewModel: GameViewModel, modifier: Modifier = Modifier) {
    val notes = gameViewModel.notes

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val spacing = 4.dp
        val tileSize = minOf(
            (maxWidth - spacing * 11) / 12,
            (maxHeight - spacing * 4) / 5,
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(spacing),
        ) {
            repeat(5) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                ) {
                    repeat(12) { column ->
                        TileView(
                            tile = Tiles.all[column * 5 + row],
                            selected = Tiles.all[column * 5 + row] in notes,
                            onClick = { gameViewModel.toggleNote(Tiles.all[column * 5 + row]) },
                            modifier = Modifier.size(tileSize),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TileView(
    tile: Tile,
    modifier: Modifier = Modifier,
    selected: Boolean = true,
    onClick: () -> Unit = {},
) {
    val baseColor = Color(
        red = tile.color.red / 255f,
        green = tile.color.green / 255f,
        blue = tile.color.blue / 255f,
    )
    val tileColor = if (selected) {
        baseColor
    } else {
        val luminance =
            baseColor.red * 0.299f +
                baseColor.green * 0.587f +
                baseColor.blue * 0.114f
        Color(luminance, luminance, luminance)
    }

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tileColor)
            .clickable(onClick = onClick)
            .then(if (selected) Modifier else Modifier.alpha(0.35f))
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = tile.number.toString(),
            color = Color.White,
            style = MaterialTheme.typography.headlineMedium,
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(tile.dots) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                )
            }
        }
    }
}
