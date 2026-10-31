package cz.kotu.gamearena

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PlayerAvatarTest {
    @Test
    fun resolvesServerRelativePictureUrlAgainstConfiguredServer() {
        assertEquals(
            "http://192.168.1.20:8080/media/players/alice.webp",
            resolvePictureUrl("/media/players/alice.webp", "http://192.168.1.20:8080/"),
        )
    }

    @Test
    fun preservesAbsolutePictureUrl() {
        assertEquals(
            "https://example.com/alice.webp",
            resolvePictureUrl("https://example.com/alice.webp", "http://localhost:8080"),
        )
    }

    @Test
    fun keepsRelativeUrlForSameOriginWebClient() {
        assertEquals("/media/players/alice.webp", resolvePictureUrl("/media/players/alice.webp", ""))
    }

    @Test
    fun blankPictureUrlIsNotLoaded() {
        assertNull(resolvePictureUrl("  ", "http://localhost:8080"))
    }
}
