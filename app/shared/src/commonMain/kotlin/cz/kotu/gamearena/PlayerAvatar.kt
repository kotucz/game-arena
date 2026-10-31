package cz.kotu.gamearena

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import coil3.compose.AsyncImage

@Composable
fun PlayerAvatar(
    pictureUrl: String?,
    username: String,
    modifier: Modifier = Modifier,
) {
    val resolvedPictureUrl = remember(pictureUrl) {
        resolvePictureUrl(pictureUrl, defaultApiBaseUrl())
    }
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clearAndSetSemantics { contentDescription = "Avatar of $username" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxSize(0.65f),
        )
        AsyncImage(
            model = resolvedPictureUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape),
        )
    }
}

@Suppress("HttpUrlsUsage")
internal fun resolvePictureUrl(pictureUrl: String?, baseUrl: String): String? {
    val url = pictureUrl?.takeIf(String::isNotBlank) ?: return null
    if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
        return url
    }
    if (baseUrl.isBlank()) return url
    return "${baseUrl.trimEnd('/')}/${url.trimStart('/')}"
}
