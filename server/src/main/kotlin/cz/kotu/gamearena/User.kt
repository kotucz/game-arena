package cz.kotu.gamearena

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

@Entity(tableName = "users")
data class User(
    @androidx.room.PrimaryKey val username: String,
    val passwordHash: String,
    val email: String,
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User)
}

@Entity(tableName = "sessions")
data class Session(
    @androidx.room.PrimaryKey val tokenHash: String,
    val username: String,
    val expiresAt: Long,
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE tokenHash = :tokenHash LIMIT 1")
    suspend fun findByTokenHash(tokenHash: String): Session?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: Session)

    @Query("DELETE FROM sessions WHERE tokenHash = :tokenHash")
    suspend fun deleteByTokenHash(tokenHash: String)
}

object PasswordHasher {
    private const val iterations = 210_000
    private const val keyLength = 256
    private const val saltLength = 16
    private val random = SecureRandom()

    fun hash(password: String): String {
        val salt = ByteArray(saltLength).also(random::nextBytes)
        val derived = derive(password, salt, iterations)
        return listOf(iterations.toString(), encode(salt), encode(derived)).joinToString("$")
    }

    fun matches(password: String, encoded: String): Boolean {
        val parts = encoded.split('$')
        if (parts.size != 3) return false
        val rounds = parts[0].toIntOrNull() ?: return false
        return try {
            val expected = Base64.getDecoder().decode(parts[2])
            val actual = derive(password, Base64.getDecoder().decode(parts[1]), rounds)
            MessageDigest.isEqual(expected, actual)
        } catch (_: IllegalArgumentException) {
            false
        }
    }

    private fun derive(password: String, salt: ByteArray, rounds: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, rounds, keyLength)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun encode(bytes: ByteArray): String = Base64.getEncoder().encodeToString(bytes)
}

object SessionTokens {
    const val cookieName = "gamearena_session"
    const val lifetimeSeconds = 30L * 24 * 60 * 60
    private val random = SecureRandom()

    fun create(): String = ByteArray(32).also(random::nextBytes).let {
        Base64.getUrlEncoder().withoutPadding().encodeToString(it)
    }

    fun hash(token: String): String = Base64.getUrlEncoder().withoutPadding().encodeToString(
        MessageDigest.getInstance("SHA-256").digest(token.toByteArray(Charsets.UTF_8))
    )
}
