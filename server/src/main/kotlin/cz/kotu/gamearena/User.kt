package cz.kotu.gamearena

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

@Entity(tableName = "users")
data class User(
    @androidx.room.PrimaryKey val username: String,
    val email: String = "",
    val firebaseUid: String? = null,
)

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun findByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE firebaseUid = :firebaseUid LIMIT 1")
    suspend fun findByFirebaseUid(firebaseUid: String): User?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(user: User)
}

@Entity(tableName = "sessions")
data class Session(
    @androidx.room.PrimaryKey val tokenHash: String,
    val username: String,
    val expiresAt: Long,
    val userId: String = username,
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
