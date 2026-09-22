package cz.kotu.gamearena

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.aakira.napier.Napier
import java.io.File

@Entity(tableName = "games")
data class StoredGame(
    @PrimaryKey val id: String,
    val type: String,
    val playersJson: String,
    val createdAtMillis: Long,
    val stateJson: String,
    val logsJson: String = "[]",
    val updatedAtMillis: Long = createdAtMillis,
)

@Dao
interface GameDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(game: StoredGame)

    @Query("SELECT * FROM games")
    suspend fun list(): List<StoredGame>

    @Query("SELECT * FROM games WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): StoredGame?
}

@Entity(
    tableName = "push_tokens",
    indices = [Index(value = ["username", "service"])],
)
data class PushToken(
    /** Opaque client-generated ID (e.g. UUID) so clients can refresh or remove their own row. */
    @PrimaryKey val tokenId: String,
    val username: String,
    /** Stable service discriminator: "fcm" | "webpush" */
    val service: String,
    val token: String,
    val updatedAtMillis: Long,
)

@Dao
interface PushTokenDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pushToken: PushToken)

    /** Remove a specific token. Scoped to username so a user cannot remove another user's token. */
    @Query("DELETE FROM push_tokens WHERE tokenId = :tokenId AND username = :username")
    suspend fun delete(tokenId: String, username: String)

    @Query("SELECT * FROM push_tokens WHERE username = :username")
    suspend fun findByUsername(username: String): List<PushToken>

    @Query("SELECT * FROM push_tokens WHERE username IN (:usernames)")
    suspend fun findByUsernames(usernames: List<String>): List<PushToken>
}

@Database(entities = [User::class, StoredGame::class, PushToken::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun gameDao(): GameDao
    abstract fun pushTokenDao(): PushTokenDao
}

val ALL_MIGRATIONS: Array<Migration> = arrayOf(
    object : Migration(1, 2) {
        override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
            connection.prepare(
                "CREATE TABLE IF NOT EXISTS sessions (tokenHash TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, expiresAt INTEGER NOT NULL)"
            ).use { it.step() }
        }
    },
    object : Migration(2, 3) {
        override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
            connection.prepare(
                "CREATE TABLE IF NOT EXISTS games (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, playersJson TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, stateJson TEXT NOT NULL, logsJson TEXT NOT NULL DEFAULT '[]', updatedAtMillis INTEGER NOT NULL)"
            ).use { it.step() }
        }
    },
    object : Migration(3, 4) {
        override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
            connection.prepare(
                "CREATE TABLE IF NOT EXISTS push_tokens (tokenId TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, service TEXT NOT NULL, token TEXT NOT NULL, updatedAtMillis INTEGER NOT NULL)"
            ).use { it.step() }
            connection.prepare(
                "CREATE INDEX IF NOT EXISTS idx_push_tokens_username_service ON push_tokens (username, service)"
            ).use { it.step() }
        }
    },
    object : Migration(4, 5) {
        override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
            connection.prepare(
                """
                CREATE TABLE IF NOT EXISTS users_new (
                    username TEXT NOT NULL PRIMARY KEY,
                    email TEXT NOT NULL DEFAULT '',
                    firebaseUid TEXT
                )
                """.trimIndent()
            ).use { it.step() }
            connection.prepare(
                """
                INSERT OR IGNORE INTO users_new (username, email, firebaseUid)
                SELECT username, email, firebaseUid FROM users
                """.trimIndent()
            ).use { it.step() }
            connection.prepare("DROP TABLE users").use { it.step() }
            connection.prepare("ALTER TABLE users_new RENAME TO users").use { it.step() }
        }
    },
    object : Migration(5, 6) {
        override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
            connection.prepare("DROP TABLE IF EXISTS sessions").use { it.step() }
        }
    },
)

fun createDatabase(databaseFile: File): AppDatabase {
    databaseFile.parentFile?.mkdirs()
    Napier.i("createDatabase $databaseFile")
    return Room.databaseBuilder<AppDatabase>(databaseFile.path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(*ALL_MIGRATIONS)
        .build()
}
