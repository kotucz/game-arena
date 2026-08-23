package cz.kotu.gamearena

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
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

@Database(entities = [User::class, Session::class, StoredGame::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun sessionDao(): SessionDao
    abstract fun gameDao(): GameDao
}

fun createDatabase(): AppDatabase {
    val databaseFile = File(System.getenv("DATABASE_PATH") ?: "data/gamearena.db")
    databaseFile.parentFile?.mkdirs()
    return Room.databaseBuilder<AppDatabase>(databaseFile.path)
        .setDriver(BundledSQLiteDriver())
        .addMigrations(
            object : Migration(1, 2) {
                override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                    connection.prepare("CREATE TABLE IF NOT EXISTS sessions (tokenHash TEXT NOT NULL PRIMARY KEY, username TEXT NOT NULL, expiresAt INTEGER NOT NULL)").use {
                        it.step()
                    }
                }
            },
            object : Migration(2, 3) {
                override fun migrate(connection: androidx.sqlite.SQLiteConnection) {
                    connection.prepare(
                        "CREATE TABLE IF NOT EXISTS games (id TEXT NOT NULL PRIMARY KEY, type TEXT NOT NULL, playersJson TEXT NOT NULL, createdAtMillis INTEGER NOT NULL, stateJson TEXT NOT NULL, logsJson TEXT NOT NULL DEFAULT '[]', updatedAtMillis INTEGER NOT NULL)"
                    ).use {
                        it.step()
                    }
                }
            },
        )
        .build()
}
