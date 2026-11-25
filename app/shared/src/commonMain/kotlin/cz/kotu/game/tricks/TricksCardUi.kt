package cz.kotu.game.tricks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cz.kotu.common.Flippable
import cz.kotu.game.tricks.model.Card
import cz.kotu.game.tricks.model.Suit

fun suitColor(suit: Suit): Color = when (suit) {
    Suit.Blue -> Color(0xFF1976D2)
    Suit.Green -> Color(0xFF388E3C)
    Suit.Yellow -> Color(0xFFE6A100)
    Suit.Pink -> Color(0xFFD81B60)
    Suit.Trump -> Color(0xFF263238)
}

@Composable
fun TricksCardView(
    card: Card?,
    modifier: Modifier = Modifier,
    isFaceUp: Boolean = true,
    animateFlip: Boolean = false,
    isPlayable: Boolean = true,
    isSelected: Boolean = false,
    width: Dp = 68.dp,
    height: Dp = 96.dp,
    flipDurationMillis: Int = 350,
    onClick: (() -> Unit)? = null,
) {
    if (card == null || !isFaceUp) {
        TricksCardBack(modifier = modifier, width = width, height = height)
        return
    }

    if (!animateFlip) {
        TricksCardFace(
            card = card,
            modifier = modifier,
            isPlayable = isPlayable,
            isSelected = isSelected,
            width = width,
            height = height,
            onClick = onClick,
        )
        return
    }

    var revealed by remember(card) { mutableStateOf(false) }
    LaunchedEffect(card) {
        revealed = true
    }
    Flippable(
        isFlipped = !revealed,
        front = {
            TricksCardFace(
                card = card,
                isPlayable = isPlayable,
                isSelected = isSelected,
                width = width,
                height = height,
                onClick = onClick,
            )
        },
        back = {
            TricksCardBack(width = width, height = height)
        },
        modifier = modifier,
        durationMillis = flipDurationMillis,
    )
}

@Composable
private fun TricksCardFace(
    card: Card,
    modifier: Modifier = Modifier,
    isPlayable: Boolean,
    isSelected: Boolean,
    width: Dp,
    height: Dp,
    onClick: (() -> Unit)?,
) {
    val color = suitColor(card.suit)
    val shape = RoundedCornerShape(8.dp)

    Card(
        modifier = modifier
            .size(width = width, height = height)
            .alpha(if (isPlayable) 1.0f else 0.45f)
            .border(
                width = if (isSelected) 3.dp else 1.5.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else color.copy(alpha = 0.8f),
                shape = shape,
            )
            .then(
                if (isPlayable && onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPlayable) 4.dp else 1.dp),
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Header color bar
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp)
                    .background(color.copy(alpha = 0.12f), shape = RoundedCornerShape(6.dp))
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(6.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = card.suit.label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = color,
                )

                Text(
                    text = card.rank.value.toString(),
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color,
                )

                Text(
                    text = if (card.suit.isTrump) "★" else "●",
                    fontSize = 12.sp,
                    color = color,
                )
            }
        }
    }
}

@Composable
private fun TricksCardBack(
    modifier: Modifier = Modifier,
    width: Dp,
    height: Dp,
) {
    val isMini = width < 30.dp
    val shape = RoundedCornerShape(if (isMini) 3.dp else 8.dp)
    Card(
        modifier = modifier
            .size(width = width, height = height)
            .border(
                width = if (isMini) 0.5.dp else 1.5.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                shape = shape,
            ),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isMini) 1.dp else 3.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(if (isMini) 2.dp else 4.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(if (isMini) 2.dp else 6.dp),
                )
                .border(
                    width = if (isMini) 0.5.dp else 1.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(if (isMini) 2.dp else 6.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "🂠",
                fontSize = if (isMini) 10.sp else 26.sp,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
